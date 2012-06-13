package org.perf4j.logback;
/* Copyright (c) 2008-2009 HomeAway, Inc.
 * All rights reserved.  http://www.perf4j.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;
import junit.framework.TestCase;
import org.apache.commons.io.FileUtils;
import org.perf4j.StopWatch;
import org.perf4j.slf4j.Slf4JStopWatch;
import org.slf4j.LoggerFactory;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;

/**
 * This class tests the logback appenders.
 */
public class AppenderTest extends TestCase {
    public void testAppenders() throws Exception {
        LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
        JoranConfigurator configurator = new JoranConfigurator();
        configurator.setContext(lc);
        // the context was probably already configured by default configuration 
        // rules
        lc.reset();
        configurator.doConfigure(getClass().getResource("logback.xml"));

        AsyncCoalescingStatisticsAppender appender = (AsyncCoalescingStatisticsAppender) 
            lc.getLogger(StopWatch.DEFAULT_LOGGER_NAME).getAppender("coalescingStatistics");

        //log from a bunch of threads
        TestLoggingThread[] testThreads = new TestLoggingThread[10];
        for (int i = 0; i < testThreads.length; i++) {
            testThreads[i] = new TestLoggingThread();
            testThreads[i].start();
        }

        for (TestLoggingThread testThread : testThreads) {
            testThread.join();
        }

        //close the output appender, which prevents us from returning until this method completes.
        appender.stop();

        //simple verification ensures that the total number of logged messages is correct.
        //tagName  avg           min     max     std dev       count, which is group 1
        String regex = "tag\\d\\s*\\d+\\.\\d\\s*\\d+\\s*\\d+\\s*\\d+\\.\\d\\s*(\\d+)";
        Pattern statLinePattern = Pattern.compile(regex);
        Scanner scanner = new Scanner(new File("target/statisticsLogback.log"));

        int totalCount = 0;
        while (scanner.findWithinHorizon(statLinePattern, 0) != null) {
            totalCount += Integer.parseInt(scanner.match().group(1));
        }
        assertEquals(testThreads.length * TestLoggingThread.STOP_WATCH_COUNT, totalCount);
    }
    
    public void testOverflowHandling() throws Exception {
        LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
        Logger logger = lc.getLogger("AppenderTest.overflowTest");
        AsyncCoalescingStatisticsAppender appender = new AsyncCoalescingStatisticsAppender();
        appender.setName("overflowTestAppender");
        appender.setTimeSlice(1000);
        appender.setQueueSize(2); //set low queue size so we overflow
        logger.addAppender(appender);
        logger.setAdditive(false);
        logger.setLevel(Level.INFO);
        appender.start();

        for (int i = 0; i < 1000; i++) {
            StopWatch stopWatch = new StopWatch("testOverflow");
            Math.random(); //this should happen super fast, faster than the appender's Dispatcher thread can drain
            logger.info(stopWatch.stop());
        }

        //again close the appender
        appender.stop();
        assertTrue("Expected some stop watch messages to get discarded", appender.getNumDiscardedMessages() > 0);
    }

    public void testCsvRenderer() throws Exception {
        LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
        JoranConfigurator configurator = new JoranConfigurator();
        configurator.setContext(lc);
        // the context was probably already configured by default configuration 
        // rules
        lc.reset();
        configurator.doConfigure(getClass().getResource("logbackWCsv.xml"));

        Logger logger = lc.getLogger("org.perf4j.CsvAppenderTest");

        for (int i = 0; i < 20; i++) {
            StopWatch stopWatch = new Slf4JStopWatch(logger);
            Thread.sleep(i * 10);
            stopWatch.stop("csvTest");
        }

        //close the appender
        logger.getAppender("coalescingStatistics").stop();

        //verify the statisticsLog.csv file
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        for (Object line : FileUtils.readLines(new File(
                "./target/statisticsLogback.csv"))) {
            String[] values = line.toString().split(",");
            //first column is the tag
            assertEquals("\"csvTest\"", values[0]);
            //next 2 columns are the dates - ensure they can be parsed
            assertTrue(dateFormat.parse(values[1]).before(dateFormat.parse(values[2])));
            //next 3 columns are mean, min and max
            double mean = Double.parseDouble(values[3]);
            int min = Integer.parseInt(values[4]);
            int max = Integer.parseInt(values[5]);
            assertTrue(mean >= min && mean <= max);
            //next column is stddev - ust make sure it's parseable
            Double.parseDouble(values[6]);
            //next column is count
            assertTrue(Integer.parseInt(values[7]) < 20);
            //final column is TPS - just make sure it's parseable
            Double.parseDouble(values[8]);
        }

        //verify the pivotedStatisticsLog.csv file
        for (Object line : FileUtils.readLines(new File("./target/pivotedStatisticsLog.csv"))) {
            String[] values = line.toString().split(",");
            //first 2 columns are the dates - ensure they can be parsed
            assertTrue(dateFormat.parse(values[0]).before(dateFormat.parse(values[1])));
            //next column is mean, ensure it can be parsed
            Double.parseDouble(values[2]);
            //last column should be empty, so make sure last char on string is comma
            assertEquals(',', line.toString().charAt(line.toString().length() - 1));
        }
    }

    protected static class TestLoggingThread extends Thread {
        protected static final AtomicInteger index = new AtomicInteger();

        public static final int STOP_WATCH_COUNT = 20;

        public void run() {
            String loggingTag = "tag" + index.getAndIncrement();

            for (int i = 0; i < STOP_WATCH_COUNT; i++) {
                StopWatch stopWatch = new Slf4JStopWatch(loggingTag);
                int sleepTime = (int) (Math.random() * 400);
                try { Thread.sleep(sleepTime); } catch (Exception e) { /*do nothing*/ }
                stopWatch.stop();
            }

            //this last sleep here seems necessary because otherwise Thread.join() above is returning before
            //we're really done.
            try { Thread.sleep(10); } catch (Exception e) { /*ignore*/ }
        }
    }
}
