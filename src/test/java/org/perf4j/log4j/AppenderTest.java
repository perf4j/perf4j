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
package org.perf4j.log4j;

import junit.framework.TestCase;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.xml.DOMConfigurator;
import org.perf4j.StopWatch;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;
import java.lang.reflect.Field;

/**
 * This class tests the log4j appenders.
 */
public class AppenderTest extends TestCase {
    public void testAppenders() throws Exception {
        DOMConfigurator.configure(getClass().getResource("log4j.xml"));

        AsyncCoalescingStatisticsAppender appender =
                (AsyncCoalescingStatisticsAppender) Logger.getLogger(StopWatch.DEFAULT_LOGGER_NAME)
                        .getAppender("coalescingStatistics");

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
        appender.close();

        //simple verification ensures that the total number of logged messages is correct.
        //tagName  avg           min     max     std dev       count, which is group 1
        String regex = "tag\\d\\s*\\d+\\.\\d\\s*\\d+\\s*\\d+\\s*\\d+\\.\\d\\s*(\\d+)";
        Pattern statLinePattern = Pattern.compile(regex);
        Scanner scanner = new Scanner(new File("target/statisticsLog.log"));

        int totalCount = 0;
        while (scanner.findWithinHorizon(statLinePattern, 0) != null) {
            totalCount += Integer.parseInt(scanner.match().group(1));
        }
        assertEquals(testThreads.length * TestLoggingThread.STOP_WATCH_COUNT, totalCount);
    }
    
    // http://jira.codehaus.org/browse/PERFFORJ-21
    public void testAppendersTimesliceOver() throws Exception {
    	// need to do immediateflush on the fileappender since close will not be called
        DOMConfigurator.configure(getClass().getResource("log4j-timeslicebug.xml"));

        AsyncCoalescingStatisticsAppender appender =
                (AsyncCoalescingStatisticsAppender) Logger.getLogger(StopWatch.DEFAULT_LOGGER_NAME)
                        .getAppender("coalescingStatistics");

        //log from a bunch of threads
        TestLoggingThread[] testThreads = new TestLoggingThread[10];
        for (int i = 0; i < testThreads.length; i++) {
            testThreads[i] = new TestLoggingThread();
            testThreads[i].start();
        }

        for (TestLoggingThread testThread : testThreads) {
            testThread.join();
        }
        
        // we should see all the logging after waiting this long
        Thread.sleep(2*appender.getTimeSlice());

        //simple verification ensures that the total number of logged messages is correct.
        //tagName  avg           min     max     std dev       count, which is group 1
        String regex = "tag\\d+\\s*\\d+\\.\\d\\s*\\d+\\s*\\d+\\s*\\d+\\.\\d\\s*(\\d+)";
        Pattern statLinePattern = Pattern.compile(regex);
        Scanner scanner = new Scanner(new File("target/statisticsLog-timeslicebug.log"));

        int totalCount = 0;
        while (scanner.findWithinHorizon(statLinePattern, 0) != null) {
            totalCount += Integer.parseInt(scanner.match().group(1));
        }
        assertEquals(testThreads.length * TestLoggingThread.STOP_WATCH_COUNT, totalCount);
    }

    //test for http://jira.codehaus.org/browse/PERFFORJ-22
    public void testFlushOnShutdown() throws Exception {
        DOMConfigurator.configure(getClass().getResource("log4j-shutdownbug.xml"));

        //make a bunch of logs, but not enough to go over the timeslice.
        for (int i = 0; i < 5; i++) {
            StopWatch stopWatch = new Log4JStopWatch("tag1");
            Thread.sleep(10 * i);
            stopWatch.stop();
        }

        //at this point none of the file appenders will have written anything because they haven't been flushed
        assertEquals("", FileUtils.readFileToString(new File("target/stats-shutdownbug.log")));
        assertEquals("", FileUtils.readFileToString(new File("target/graphs-shutdownbug.log")));

        //now, to simulate shutdown, get the async appender and run the shutdown hook. We need to use reflection
        //because the shutdown hook is private.
        AsyncCoalescingStatisticsAppender appender =
                (AsyncCoalescingStatisticsAppender) Logger.getLogger(StopWatch.DEFAULT_LOGGER_NAME)
                        .getAppender("coalescingStatistics");
        Field shutdownField = appender.getClass().getDeclaredField("shutdownHook");
        shutdownField.setAccessible(true);
        AsyncCoalescingStatisticsAppender.ShutdownHook shutdownHook = (AsyncCoalescingStatisticsAppender.ShutdownHook) shutdownField.get(appender);
        assertNotNull("shutdownHook should not be null", shutdownHook);
        assertNotNull("shutdownHook appender should not be null", shutdownHook.getAppender());
        shutdownHook.run();

        //now there should be data in the files
        assertFalse("".equals(FileUtils.readFileToString(new File("target/stats-shutdownbug.log"))));
        assertFalse("".equals(FileUtils.readFileToString(new File("target/graphs-shutdownbug.log"))));

        // closing should remove shutdown hook
        appender.close();

        // hard to test hook removal, but we set it to null at the same time, so we'll rely on that
        shutdownHook = (AsyncCoalescingStatisticsAppender.ShutdownHook) shutdownField.get(appender);
        assertNull("shutdownHook should be null", shutdownHook);
    }

    public void testOverflowHandling() throws Exception {
        Logger logger = Logger.getLogger("AppenderTest.overflowTest");
        AsyncCoalescingStatisticsAppender appender = new AsyncCoalescingStatisticsAppender();
        appender.setName("overflowTestAppender");
        appender.setTimeSlice(1000);
        appender.setQueueSize(2); //set low queue size so we overflow
        logger.addAppender(appender);
        logger.setAdditivity(false);
        logger.setLevel(Level.INFO);
        appender.activateOptions();

        for (int i = 0; i < 1000; i++) {
            StopWatch stopWatch = new StopWatch("testOverflow");
            Math.random(); //this should happen super fast, faster than the appender's Dispatcher thread can drain
            logger.info(stopWatch.stop());
        }

        //again close the appender
        appender.close();
        assertTrue("Expected some stop watch messages to get discarded", appender.getNumDiscardedMessages() > 0);
    }

    public void testCsvRenderer() throws Exception {
        DOMConfigurator.configure(getClass().getResource("log4jWCsv.xml"));

        Logger logger = Logger.getLogger("org.perf4j.CsvAppenderTest");

        for (int i = 0; i < 20; i++) {
            StopWatch stopWatch = new Log4JStopWatch(logger);
            Thread.sleep(i * 10);
            stopWatch.stop("csvTest");
        }

        //close the appender
        logger.getAppender("coalescingStatistics").close();

        //verify the statisticsLog.csv file
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        for (Object line : FileUtils.readLines(new File("./target/statisticsLog.csv"))) {
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
                StopWatch stopWatch = new Log4JStopWatch(loggingTag);
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
