/* Copyright Homeaway, Inc 2005-2007. All Rights Reserved.
 * No unauthorized use of this software.
 */
package org.perf4j;

import junit.framework.TestCase;

/**
 * Tests the StopWatch
 */
public class StopWatchTest extends TestCase {

    public void testStopWatch() throws Exception {
        long now = System.currentTimeMillis();

        StopWatch stopWatch = new StopWatch();
        assertEquals("", stopWatch.getTag());
        assertNull(stopWatch.getMessage());
        assertTrue(stopWatch.getStartTime() >= now);

        //without stopping, elapsed should increase
        long elapsedTime = stopWatch.getElapsedTime();
        Thread.sleep(20);
        assertTrue(stopWatch.getElapsedTime() > elapsedTime);

        //after stopping, elapsed time should freeze
        stopWatch.stop("tag");
        elapsedTime = stopWatch.getElapsedTime();
        Thread.sleep(20);
        assertEquals(elapsedTime, stopWatch.getElapsedTime());
        assertEquals("tag", stopWatch.getTag());

        now = System.currentTimeMillis();

        //after starting again, elapsed time should be reset
        stopWatch.start("tag2", "someMessage");
        assertEquals("tag2", stopWatch.getTag());
        assertEquals("someMessage", stopWatch.getMessage());
        assertTrue(stopWatch.getStartTime() >= now);
        elapsedTime = stopWatch.getElapsedTime();
        Thread.sleep(20);
        assertTrue(stopWatch.getElapsedTime() > elapsedTime);

        //test other methods and constructors
        stopWatch = new StopWatch("tag3");
        assertEquals("tag3", stopWatch.getTag());
        assertNull(stopWatch.getMessage());
        stopWatch.setTag("tag4");
        stopWatch.setMessage("anotherMessage");
        assertEquals("tag4", stopWatch.getTag());
        assertEquals("anotherMessage", stopWatch.getMessage());

        now = System.currentTimeMillis();

        stopWatch.start("tag5");
        Thread.sleep(10);
        assertTrue(stopWatch.getStartTime() >= now);
        String stopMessage = stopWatch.stop();
        assertEquals("start[" + stopWatch.getStartTime()
                     + "] time[" + stopWatch.getElapsedTime()
                     + "] tag[" + stopWatch.getTag()
                     + "] message[" + stopWatch.getMessage()
                     + "]", stopMessage);
        elapsedTime = stopWatch.getElapsedTime();
        Thread.sleep(10);
        //after stopping again, elapsed time should increase - we allow stopping in successing
        stopWatch.stop("tag6", "finalMessage");
        assertEquals("tag6", stopWatch.getTag());
        assertEquals("finalMessage", stopWatch.getMessage());
        assertTrue(stopWatch.getElapsedTime() > elapsedTime);

        //object methods
        assertTrue(stopWatch.equals(stopWatch));
        assertFalse(stopWatch.equals("object"));
        StopWatch clone = stopWatch.clone();
        assertEquals(stopWatch, clone);
        assertEquals(stopWatch.hashCode(), clone.hashCode());
        clone.setTag("foo");
        assertFalse(stopWatch.equals(clone));
    }
}
