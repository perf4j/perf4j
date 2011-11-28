/* Copyright (c) 2011 Brett Randall.
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
package org.perf4j.helpers;

import org.perf4j.GroupedTimingStatistics;
import org.perf4j.helpers.GenericAsyncCoalescingStatisticsAppender.GroupedTimingStatisticsHandler;

import junit.framework.TestCase;

public class GenericAsyncCoalescingStatisticsAppenderTest extends TestCase {

	/**
	 * Test implementation of GroupedTimingStatisticsHandler which blocks handle() for 100s. This
	 * allows testing of thread interrupt.
	 */
    private static class TestGroupedTimingStatisticsHandler implements GroupedTimingStatisticsHandler {

        private volatile boolean wasInterrupted = false;

        public void handle(GroupedTimingStatistics statistics) {
            try {
                // simulate very slow, blocking handler, easy to interrupt
                Thread.sleep(100000L);
            } catch (InterruptedException e) {
                System.err.println("Interrupted");
                wasInterrupted = true;
            }
        }

        public void error(String errorMessage) {
            System.err.println("Error logged");
        }
    }

    /**
     * Tests that the thread calling handler handle() (the Dispatcher thread) is interrupted when
     * logging-shutdown timeout has expired.
     */
    public void testInterruptsThread() throws InterruptedException {

        GenericAsyncCoalescingStatisticsAppender appender = new GenericAsyncCoalescingStatisticsAppender();
        TestGroupedTimingStatisticsHandler handler = new TestGroupedTimingStatisticsHandler();

        // very short timeslice, 1ms
        appender.setTimeSlice(1L);
        // very short shutdown wait, 1ms
        appender.setShutdownWaitMillis(1L);

        // start the handler and log a single message
        appender.start(handler);
        appender.append("start[1230068856846] time[2] tag[tag1]");

        // sleep to make sure handler thread started and a logging event queued (blocked)
        Thread.sleep(100L);
        appender.stop();
        // sleep to make sure interrupt has been processed
        Thread.sleep(100L);

        assertTrue("Handler was not interrupted", handler.wasInterrupted);
    }
}
