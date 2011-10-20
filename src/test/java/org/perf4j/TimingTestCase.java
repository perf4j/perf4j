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

import java.util.List;
import java.util.ArrayList;
import java.util.Date;
import java.io.StringWriter;
import java.io.PrintWriter;

/**
 * Base test class just sets up some simple StopWatches and a dummy log of those stop watches.
 */
public abstract class TimingTestCase extends TestCase {
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
