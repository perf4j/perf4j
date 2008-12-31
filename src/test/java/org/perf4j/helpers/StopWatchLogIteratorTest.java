/* Copyright Homeaway, Inc 2005-2007. All Rights Reserved.
 * No unauthorized use of this software.
 */
package org.perf4j.helpers;

import org.perf4j.TimingTestCase;
import org.perf4j.StopWatch;

import java.io.StringReader;
import java.util.List;
import java.util.ArrayList;
import java.util.NoSuchElementException;

/**
 * This class tests the StopWatchLogIterator.
 */
public class StopWatchLogIteratorTest extends TimingTestCase {

    public void testStopWatchIterator() throws Exception {
        StopWatchLogIterator logIterator = new StopWatchLogIterator(new StringReader(testLog));

        List<StopWatch> deserializedStopWatches = new ArrayList<StopWatch>();

        while (logIterator.hasNext()) {
            deserializedStopWatches.add(logIterator.next());
        }

        assertEquals(testStopWatches.size(), deserializedStopWatches.size());

        for (int i = 0; i < testStopWatches.size(); i++) {
            assertEquals(testStopWatches.get(i), deserializedStopWatches.get(i));
        }
    }

    public void testMultipleCallsToHasNext() throws Exception {
        for (StopWatchLogIterator iter = new StopWatchLogIterator(new StringReader(testLog)); iter.hasNext();) {
            assertTrue(iter.hasNext());
            assertNotNull(iter.next());
            if (!iter.hasNext()) {
                try {
                    iter.next();
                    fail();
                } catch (NoSuchElementException nsee) { /*expected*/ }
            }
        }
    }

    public void testCallingNextFirst() throws Exception {
        StopWatchLogIterator logIterator = new StopWatchLogIterator(new StringReader(testLog));

        while (true) {
            assertNotNull(logIterator.next());
            if (!logIterator.hasNext()) {
                break;
            }
        }
    }

    public void testCallingNextUntilImDone() throws Exception {
        StopWatchLogIterator logIterator = new StopWatchLogIterator(new StringReader(testLog));

        int count = 0;
        while (true) {
            try {
                assertNotNull(logIterator.next());
                count++;
            } catch (NoSuchElementException nsee) {
                //this should fail when there are no more stop watches left
                assertEquals(testStopWatches.size(), count);
                assertFalse(logIterator.hasNext());
                break;
            }
        }
    }

    public void testEmptyIterator() throws Exception {
        assertFalse(new StopWatchLogIterator(new StringReader("")).hasNext());
    }

    public void testRemove() throws Exception {
        //remove is not supported
        try {
            new StopWatchLogIterator(new StringReader(testLog)).remove();
            fail();
        } catch (UnsupportedOperationException uoe) {
            //expected
        }
    }
}
