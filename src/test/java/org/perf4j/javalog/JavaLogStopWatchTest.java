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
package org.perf4j.javalog;

import org.perf4j.LoggingStopWatch;
import org.perf4j.LoggingStopWatchTest;
import org.perf4j.StopWatch;

import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Tests the JavaLogStopWatch. See the superclass for the test method that is run.
 */
public class JavaLogStopWatchTest extends LoggingStopWatchTest {
    private ConsoleHandler stdErrHandler;

    protected void setUp() throws Exception {
        super.setUp();

        Logger defaultLogger = Logger.getLogger(StopWatch.DEFAULT_LOGGER_NAME);
        stdErrHandler = new ConsoleHandler();
        defaultLogger.addHandler(stdErrHandler);
    }

    protected void tearDown() throws Exception {
        Logger defaultLogger = Logger.getLogger(StopWatch.DEFAULT_LOGGER_NAME);
        defaultLogger.removeHandler(stdErrHandler);
        super.tearDown();
    }

    protected JavaLogStopWatch createStopWatch(String loggerName,
                                               String normalPriorityName,
                                               String exceptionPriorityName,
                                               String tag,
                                               String message) {
        if (loggerName == null && normalPriorityName == null && exceptionPriorityName == null
            && tag == null && message == null) {
            return new JavaLogStopWatch();
        }
        if (loggerName == null && normalPriorityName == null && exceptionPriorityName == null && message == null) {
            return new JavaLogStopWatch(tag);
        }
        if (loggerName == null && normalPriorityName == null && exceptionPriorityName == null) {
            return new JavaLogStopWatch(tag, message);
        }
        if (normalPriorityName == null && exceptionPriorityName == null && tag == null && message == null) {
            return new JavaLogStopWatch(Logger.getLogger(loggerName));
        }
        if (exceptionPriorityName == null && tag == null && message == null) {
            return new JavaLogStopWatch(Logger.getLogger(loggerName),
                                        JavaLogStopWatch.mapLevelName(normalPriorityName));
        }
        if (tag == null && message == null) {
            return new JavaLogStopWatch(Logger.getLogger(loggerName),
                                        JavaLogStopWatch.mapLevelName(normalPriorityName),
                                        JavaLogStopWatch.mapLevelName(exceptionPriorityName));
        }
        if (normalPriorityName == null && exceptionPriorityName == null && message == null) {
            return new JavaLogStopWatch(tag, Logger.getLogger(loggerName));
        }
        if (exceptionPriorityName == null && message == null) {
            return new JavaLogStopWatch(tag,
                                        Logger.getLogger(loggerName),
                                        JavaLogStopWatch.mapLevelName(normalPriorityName));
        }
        if (message == null) {
            return new JavaLogStopWatch(tag,
                                        Logger.getLogger(loggerName),
                                        JavaLogStopWatch.mapLevelName(normalPriorityName),
                                        JavaLogStopWatch.mapLevelName(exceptionPriorityName));
        }
        if (normalPriorityName == null && exceptionPriorityName == null) {
            return new JavaLogStopWatch(tag, message, Logger.getLogger(loggerName));
        }
        if (exceptionPriorityName == null) {
            return new JavaLogStopWatch(tag,
                                        message,
                                        Logger.getLogger(loggerName),
                                        JavaLogStopWatch.mapLevelName(normalPriorityName));
        }
        return new JavaLogStopWatch(tag,
                                    message,
                                    Logger.getLogger(loggerName),
                                    JavaLogStopWatch.mapLevelName(normalPriorityName),
                                    JavaLogStopWatch.mapLevelName(exceptionPriorityName));
    }

    protected void customTests() {
        JavaLogStopWatch stopWatch = new JavaLogStopWatch(0L, 1000L, "tag", "message",
                                                          Logger.getLogger("org.perf4j"), Level.FINEST, Level.INFO);
        assertEquals(0L, stopWatch.getStartTime());
        assertEquals(1000L, stopWatch.getElapsedTime());
        assertEquals("tag", stopWatch.getTag());
        assertEquals("message", stopWatch.getMessage());
        assertEquals("org.perf4j", stopWatch.getLogger().getName());
        assertEquals(Level.FINEST, stopWatch.getNormalPriority());
        assertEquals(Level.INFO, stopWatch.getExceptionPriority());

        stopWatch.setLogger(Logger.getLogger("org.perf4j.AnotherTestLogger"));
        assertEquals("org.perf4j.AnotherTestLogger", stopWatch.getLogger().getName());
        stopWatch.setNormalPriority(Level.FINER);
        assertEquals(Level.FINER, stopWatch.getNormalPriority());
        stopWatch.setExceptionPriority(Level.WARNING);
        assertEquals(Level.WARNING, stopWatch.getExceptionPriority());
        Logger.getLogger("org.perf4j.AnotherTestLogger").setLevel(Level.FINEST);
        assertTrue(stopWatch.isLogging());
        Logger.getLogger("org.perf4j.AnotherTestLogger").setLevel(Level.INFO);
        assertFalse(stopWatch.isLogging());
    }

    protected void checkProperties(LoggingStopWatch stopWatch,
                                   String expectedLoggerName,
                                   String expectedNormalPriority,
                                   String expectedExceptionPriority,
                                   String expectedTag,
                                   String expectedMessage) {
        super.checkProperties(stopWatch, expectedLoggerName, expectedNormalPriority, expectedExceptionPriority,
                              expectedTag, expectedMessage);
        JavaLogStopWatch javaLogStopWatch = (JavaLogStopWatch) stopWatch;
        assertEquals(expectedLoggerName, javaLogStopWatch.getLogger().getName());
        assertEquals(JavaLogStopWatch.mapLevelName(expectedNormalPriority), javaLogStopWatch.getNormalPriority());
        assertEquals(JavaLogStopWatch.mapLevelName(expectedExceptionPriority), javaLogStopWatch.getExceptionPriority());
    }
}

