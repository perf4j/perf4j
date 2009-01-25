/* Copyright Homeaway, Inc 2005-2007. All Rights Reserved.
 * No unauthorized use of this software.
 */
package org.perf4j.helpers;

import junit.framework.TestCase;
import org.perf4j.StopWatch;

/**
 * Tests the StopWatchParser class.
 */
public class StopWatchParserTest extends TestCase {

    public void testStopWatchParser() throws Exception {
        StopWatchParser parser = new StopWatchParser();

        StopWatch stopWatch = new StopWatch(123, 456, "tag", "message");
        assertEquals(stopWatch, parser.parseStopWatch(stopWatch.toString()));

        stopWatch = new StopWatch(789, 101112, "tag2", null);
        assertEquals(stopWatch, parser.parseStopWatch(stopWatch.toString()));

        assertNull(parser.parseStopWatch("not a stop watch string"));

        assertNull(parser.match("not a stop watch string"));
    }
}
