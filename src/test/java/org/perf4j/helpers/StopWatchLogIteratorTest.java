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
