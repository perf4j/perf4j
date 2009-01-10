/* Copyright Homeaway, Inc 2005-2007. All Rights Reserved.
 * No unauthorized use of this software.
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

        TimedRunnable timedRunnable = new TimedRunnable(task, new LoggingStopWatch());
        assertEquals(task, timedRunnable.getWrappedTask());
        assertEquals(new LoggingStopWatch(), timedRunnable.getStopWatch());

        timedRunnable.run();

        assertTrue(task.wasRun);
        //make sure stop watch was stopped by ensuring that elapsed time doesn't change
        long elapsedTime = timedRunnable.getStopWatch().getElapsedTime();
        Thread.sleep(50);
        assertEquals(elapsedTime, timedRunnable.getStopWatch().getElapsedTime());

        task = new TestTask();
        TimedCallable timedCallable = new TimedCallable(task, new LoggingStopWatch());
        assertEquals(task, timedCallable.getWrappedTask());
        assertEquals(new LoggingStopWatch(), timedCallable.getStopWatch());

        assertEquals(100L, timedCallable.call());

        assertTrue(task.wasRun);
        //make sure stop watch was stopped by ensuring that elapsed time doesn't change
        elapsedTime = timedCallable.getStopWatch().getElapsedTime();
        Thread.sleep(50);
        assertEquals(elapsedTime, timedCallable.getStopWatch().getElapsedTime());
    }

    public static class TestTask implements Runnable, Callable<Long>, Serializable {
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
