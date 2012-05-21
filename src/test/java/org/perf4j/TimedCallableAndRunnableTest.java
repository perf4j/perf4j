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
package org.perf4j;

import junit.framework.TestCase;

import java.util.concurrent.Callable;
import java.io.Serializable;

/**
 * This class tests the TimedCallable and TimedRunnable helper classes.
 */
public class TimedCallableAndRunnableTest extends TestCase {

    public void testTimedCallableAndRunnable() throws Exception {
        TestTask task = new TestTask();

        LoggingStopWatch stopWatch = new LoggingStopWatch();
        TimedRunnable timedRunnable = new TimedRunnable(task, stopWatch);
        assertEquals(task, timedRunnable.getWrappedTask());
        assertEquals(stopWatch, timedRunnable.getStopWatch());

        timedRunnable.run();

        assertTrue(task.wasRun);
        //make sure stop watch was stopped by ensuring that elapsed time doesn't change
        long elapsedTime = timedRunnable.getStopWatch().getElapsedTime();
        Thread.sleep(50);
        assertEquals(elapsedTime, timedRunnable.getStopWatch().getElapsedTime());

        task = new TestTask();
        TimedCallable<Long> timedCallable = new TimedCallable<Long>(task, stopWatch = new LoggingStopWatch());
        assertEquals(task, timedCallable.getWrappedTask());
        assertEquals(stopWatch, timedCallable.getStopWatch());

        assertEquals(100L, (long) timedCallable.call());

        assertTrue(task.wasRun);
        //make sure stop watch was stopped by ensuring that elapsed time doesn't change
        elapsedTime = timedCallable.getStopWatch().getElapsedTime();
        Thread.sleep(50);
        assertEquals(elapsedTime, timedCallable.getStopWatch().getElapsedTime());
    }

    public static class TestTask implements Runnable, Callable<Long>, Serializable {

        private static final long serialVersionUID = 7370796726390725584L;

        public boolean wasRun = false;

        public void run() {
            try {
                call();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        public Long call() throws Exception {
            Thread.sleep(100L);
            wasRun = true;
            return 100L;
        }
    }
}
