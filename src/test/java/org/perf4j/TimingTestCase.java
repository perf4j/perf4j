/* Copyright Homeaway, Inc 2005-2007. All Rights Reserved.
 * No unauthorized use of this software.
 */
package org.perf4j;

import junit.framework.TestCase;

import java.util.List;
import java.util.ArrayList;
import java.util.Date;
import java.io.StringWriter;
import java.io.PrintWriter;

/**
 * Base test class just sets up some simple StopWatches and a dummy log of those stop watches.
 */
public class TimingTestCase extends TestCase {
    protected List<StopWatch> testStopWatches;
    protected String testLog;

    protected void setUp() throws Exception {
        testStopWatches = new ArrayList<StopWatch>();
        testStopWatches.add(new StopWatch(System.currentTimeMillis(), 1000L, "tag", "message1"));
        testStopWatches.add(new StopWatch(System.currentTimeMillis(), 2000L, "tag", "message2"));
        testStopWatches.add(new StopWatch(System.currentTimeMillis(), 3000L, "tag2", null));
        testStopWatches.add(new StopWatch(System.currentTimeMillis(), 4000L, "tag2", "message3"));
        testStopWatches.add(new StopWatch(System.currentTimeMillis() + 60000L, 5000L, "tag3", null));

        StringWriter testLogWriter;
        PrintWriter printWriter = new PrintWriter(testLogWriter = new StringWriter());
        printWriter.println(testStopWatches.get(0));
        printWriter.println(new Date() + " " + testStopWatches.get(1));
        printWriter.println("unrelated log message");
        printWriter.println(testStopWatches.get(2));
        printWriter.println("SomePrefixString " + testStopWatches.get(3));
        printWriter.println(testStopWatches.get(4));
        printWriter.println();
        printWriter.flush();
        testLog = testLogWriter.toString();
    }
}
