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

import org.apache.log4j.Logger;
import org.apache.log4j.xml.DOMConfigurator;
import org.perf4j.StopWatch;

import java.util.concurrent.atomic.AtomicInteger;

import junit.framework.TestCase;

/**
 * This class tests the log4j appenders.
 */
public class AppenderTest extends TestCase {
    public void testAppenders() throws Exception {
        DOMConfigurator.configure(getClass().getResource("log4j.xml"));

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
        Logger.getLogger(StopWatch.DEFAULT_LOGGER_NAME).getAppender("coalescingStatistics").close();
        
        //TODO - verify output
    }

    protected static class TestLoggingThread extends Thread {
        protected static final AtomicInteger index = new AtomicInteger();

        public void run() {
            Logger logger = Logger.getLogger(StopWatch.DEFAULT_LOGGER_NAME);
            String loggingTag = "tag" + index.getAndIncrement();

            for (int i = 0; i < 20; i++) {
                StopWatch stopWatch = new StopWatch(loggingTag);
                int sleepTime = (int) (Math.random() * 400);
                try { Thread.sleep(sleepTime); } catch (Exception e) { /*do nothing*/ }
                logger.info(stopWatch.stop());
            }

            //this last sleep here seems necessary because otherwise Thread.join() above is returning before
            //we're really done.
            try { Thread.sleep(10); } catch (Exception e) { /*ignore*/ }
        }
    }
}
