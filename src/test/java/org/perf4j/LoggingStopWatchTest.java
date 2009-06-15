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
import org.apache.commons.io.output.ByteArrayOutputStream;

import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintStream;

/**
 * Tests the LoggingStopWatch. Can also be used to test subclasses if the loggers are set up to log to stderr.
 */
public class LoggingStopWatchTest extends TestCase {

    protected PrintStream realErr;
    protected ByteArrayOutputStream fakeErr;

    protected void setUp() throws Exception {
        realErr = System.err;
        fakeErr = new ByteArrayOutputStream();
        System.setErr(new PrintStream(fakeErr, true /*autoflush*/));
    }

    protected void tearDown() throws Exception {
        System.setErr(realErr);
    }

    /**
     * Tests the StopWatch. This method is designed in such a way that it should be used to test subclasses as well
     * if createStopWatch and checkProperties are properly implemented.
     */
    public void testStopWatch() throws Exception {
        checkProperties(createStopWatch(null, null, null, null, null),
                        StopWatch.DEFAULT_LOGGER_NAME, "INFO", "WARN", "", null);
        checkProperties(createStopWatch(null, null, null, "tag", null),
                        StopWatch.DEFAULT_LOGGER_NAME, "INFO", "WARN", "tag", null);
        checkProperties(createStopWatch(null, null, null, "tag2", "message2"),
                        StopWatch.DEFAULT_LOGGER_NAME, "INFO", "WARN", "tag2", "message2");
        checkProperties(createStopWatch("org.perf4j.TestLogger", null, null, null, null),
                        "org.perf4j.TestLogger", "INFO", "WARN", "", null);
        checkProperties(createStopWatch("org.perf4j.TestLogger", "DEBUG", null, null, null),
                        "org.perf4j.TestLogger", "DEBUG", "WARN", "", null);
        checkProperties(createStopWatch("org.perf4j.TestLogger", "DEBUG", "ERROR", null, null),
                        "org.perf4j.TestLogger", "DEBUG", "ERROR", "", null);
        checkProperties(createStopWatch("org.perf4j.TestLogger", null, null, "taga", null),
                        "org.perf4j.TestLogger", "INFO", "WARN", "taga", null);
        checkProperties(createStopWatch("org.perf4j.TestLogger", "DEBUG", null, "tagb", null),
                        "org.perf4j.TestLogger", "DEBUG", "WARN", "tagb", null);
        checkProperties(createStopWatch("org.perf4j.TestLogger", "DEBUG", "ERROR", "tagc", null),
                        "org.perf4j.TestLogger", "DEBUG", "ERROR", "tagc", null);
        checkProperties(createStopWatch("org.perf4j.TestLogger", null, null, "taga", "m1"),
                        "org.perf4j.TestLogger", "INFO", "WARN", "taga", "m1");
        checkProperties(createStopWatch("org.perf4j.TestLogger", "DEBUG", null, "tagb", "m2"),
                        "org.perf4j.TestLogger", "DEBUG", "WARN", "tagb", "m2");
        checkProperties(createStopWatch("org.perf4j.TestLogger", "DEBUG", "FATAL", "tagc", "m3"),
                        "org.perf4j.TestLogger", "DEBUG", "FATAL", "tagc", "m3");

        checkSerializationAndCloning(createStopWatch("org.perf4j.TestLogger", "DEBUG", "WARN", "tagd", "m4"));

        LoggingStopWatch stopWatch = createStopWatch(null, null, null, "tag", "messageText");
        stopWatch.stop();
        checkExpectedLogWritten("message[messageText]");
        stopWatch.stop("foo");
        checkExpectedLogWritten("tag[foo]");
        stopWatch.stop("you", "bar");
        checkExpectedLogWritten("tag[you] message[bar]");
        stopWatch.stop(new Exception("zoo"));
        checkExpectedLogWritten("java.lang.Exception: zoo");
        stopWatch.stop("blue", new Exception("goo"));
        checkExpectedLogWritten("tag[blue]", "java.lang.Exception: goo");
        stopWatch.stop("loo", "car", new Exception("moo"));
        checkExpectedLogWritten("tag[loo] message[car]", "java.lang.Exception: moo");
        stopWatch.lap("coo");
        checkExpectedLogWritten("tag[coo]");
        stopWatch.lap("ewe", "far");
        checkExpectedLogWritten("tag[ewe] message[far]");
        stopWatch.lap("poo", new Exception("shoe"));
        checkExpectedLogWritten("tag[poo]", "java.lang.Exception: shoe");
        stopWatch.lap("new", "mar", new Exception("rue"));
        checkExpectedLogWritten("tag[new] message[mar]", "java.lang.Exception: rue");

        //test for PERFFORJ-30 - Add capability to set a time threshold in LoggingStopWatch and Profiled annotation
        stopWatch.stop();
        String fakeErrBefore = fakeErr.toString();
        stopWatch.setTimeThreshold(100).start("timeThresholdCheck");
        Thread.sleep(10);
        stopWatch.stop();
        assertEquals("Stopwatch log was set when it shouldn't have been", fakeErrBefore, fakeErr.toString());
        //now it should get logged after running
        stopWatch.start();
        Thread.sleep(110);
        stopWatch.stop();
        checkExpectedLogWritten("tag[timeThresholdCheck]");

        customTests();
    }

    /**
     * Subclasses should override this to test items specific to the subclass of LoggingStopWatch.
     */
    protected void customTests() {
        LoggingStopWatch stopWatch = new LoggingStopWatch(0L, 1000L, "tag", "message");
        assertEquals(0L, stopWatch.getStartTime());
        assertEquals(1000L, stopWatch.getElapsedTime());
        assertEquals("tag", stopWatch.getTag());
        assertEquals("message", stopWatch.getMessage());

        assertTrue(stopWatch.isLogging());
    }

    /**
     * This method is meant to be overridden in such a way that test subclasses return the proper LoggingStopWatch
     * subclass implementation.
     */
    protected LoggingStopWatch createStopWatch(String loggerName,
                                               String normalPriorityName,
                                               String exceptionPriorityName,
                                               String tag,
                                               String message) {
        //main LoggingStopWatch class doesn't care about logger or priorities
        if (tag == null && message == null) {
            return new LoggingStopWatch();
        }
        if (message == null) {
            return new LoggingStopWatch(tag);
        }
        return new LoggingStopWatch(tag, message);
    }

    /**
     * Test subclasses should override this method to validate the additional properties.
     */
    protected void checkProperties(LoggingStopWatch stopWatch,
                                   String expectedLoggerName,
                                   String expectedNormalPriority,
                                   String expectedExceptionPriority,
                                   String expectedTag,
                                   String expectedMessage) {
        assertEquals(expectedTag, stopWatch.getTag());
        assertEquals(expectedMessage, stopWatch.getMessage());
    }

    protected void checkSerializationAndCloning(LoggingStopWatch stopWatch) {
        LoggingStopWatch cloneCopy = stopWatch.clone();
        assertEquals(stopWatch, cloneCopy);

        try {
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            new ObjectOutputStream(os).writeObject(stopWatch);
            LoggingStopWatch serializedCopy =
                    (LoggingStopWatch) new ObjectInputStream(new ByteArrayInputStream(os.toByteArray())).readObject();
            assertEquals(stopWatch, serializedCopy);
            assertEquals(cloneCopy, serializedCopy);
        } catch (Exception e) {
            fail("Unexpected Exception: " + e);
        }
    }

    protected void checkExpectedLogWritten(String... textToFind) {
        String fakeErrLog = fakeErr.toString();
        for (String text : textToFind) {
            assertTrue("'" + text + "' not found in log " + fakeErrLog, fakeErrLog.indexOf(text) >= 0);
        }
        fakeErr.reset();
    }
}
