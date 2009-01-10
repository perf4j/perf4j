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

        //test lap methods
        stopWatch.start();
        Thread.sleep(20);
        long elapsedTimeSoFar = stopWatch.getElapsedTime();
        String stopWatchAsString = stopWatch.lap("lapTag1");
        assertTrue("lap didn't restart stopwatch", stopWatch.getElapsedTime() < elapsedTimeSoFar);
        assertTrue(stopWatchAsString.indexOf("tag[lapTag1]") >= 0);
        Thread.sleep(20);
        stopWatchAsString = stopWatch.lap("lapTag2", "lapMessage");
        assertTrue(stopWatchAsString.indexOf("tag[lapTag2]") >= 0);
        assertTrue(stopWatchAsString.indexOf("message[lapMessage]") >= 0);

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
