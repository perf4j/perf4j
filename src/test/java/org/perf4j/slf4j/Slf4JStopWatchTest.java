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
package org.perf4j.slf4j;

import org.perf4j.LoggingStopWatch;
import org.perf4j.LoggingStopWatchTest;
import org.slf4j.LoggerFactory;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.PatternLayout;
import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.core.ConsoleAppender;

/**
 * Tests the Slf4JStopWatch. See the superclass for the test method that is run.
 */
public class Slf4JStopWatchTest extends LoggingStopWatchTest {
    
    private ch.qos.logback.classic.Logger rootLogger;
    private ConsoleAppender<LoggingEvent> consoleAppender;

    protected void setUp() throws Exception {
        super.setUp();
        LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
        
        rootLogger = lc.getLogger(LoggerContext.ROOT_NAME);
        consoleAppender = new ConsoleAppender<LoggingEvent>();
        consoleAppender.setName("stderr");
        consoleAppender.setContext(lc);
        consoleAppender.setTarget(ConsoleAppender.SYSTEM_ERR);
        PatternLayout pl = new PatternLayout();
        pl.setContext(lc);
        pl.setPattern("%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n");
        pl.start();

        consoleAppender.setLayout(pl);
        consoleAppender.start();
        rootLogger.addAppender(consoleAppender);
    }
    
    protected void tearDown() throws Exception {
        rootLogger.detachAppender(consoleAppender);
        consoleAppender.stop();
        super.tearDown();
    }

    protected Slf4JStopWatch createStopWatch(String loggerName,
                                             String normalPriorityName,
                                             String exceptionPriorityName,
                                             String tag,
                                             String message) {
        if (loggerName == null && normalPriorityName == null && exceptionPriorityName == null
            && tag == null && message == null) {
            return new Slf4JStopWatch();
        }
        if (loggerName == null && normalPriorityName == null && exceptionPriorityName == null && message == null) {
            return new Slf4JStopWatch(tag);
        }
        if (loggerName == null && normalPriorityName == null && exceptionPriorityName == null) {
            return new Slf4JStopWatch(tag, message);
        }
        if (normalPriorityName == null && exceptionPriorityName == null && tag == null && message == null) {
            return new Slf4JStopWatch(LoggerFactory.getLogger(loggerName));
        }
        if (exceptionPriorityName == null && tag == null && message == null) {
            return new Slf4JStopWatch(LoggerFactory.getLogger(loggerName),
                                      Slf4JStopWatch.mapLevelName(normalPriorityName));
        }
        if (tag == null && message == null) {
            return new Slf4JStopWatch(LoggerFactory.getLogger(loggerName),
                                      Slf4JStopWatch.mapLevelName(normalPriorityName),
                                      Slf4JStopWatch.mapLevelName(exceptionPriorityName));
        }
        if (normalPriorityName == null && exceptionPriorityName == null && message == null) {
            return new Slf4JStopWatch(tag, LoggerFactory.getLogger(loggerName));
        }
        if (exceptionPriorityName == null && message == null) {
            return new Slf4JStopWatch(tag,
                                      LoggerFactory.getLogger(loggerName),
                                      Slf4JStopWatch.mapLevelName(normalPriorityName));
        }
        if (message == null) {
            return new Slf4JStopWatch(tag,
                                      LoggerFactory.getLogger(loggerName),
                                      Slf4JStopWatch.mapLevelName(normalPriorityName),
                                      Slf4JStopWatch.mapLevelName(exceptionPriorityName));
        }
        if (normalPriorityName == null && exceptionPriorityName == null) {
            return new Slf4JStopWatch(tag, message, LoggerFactory.getLogger(loggerName));
        }
        if (exceptionPriorityName == null) {
            return new Slf4JStopWatch(tag,
                                      message,
                                      LoggerFactory.getLogger(loggerName),
                                      Slf4JStopWatch.mapLevelName(normalPriorityName));
        }
        return new Slf4JStopWatch(tag,
                                  message,
                                  LoggerFactory.getLogger(loggerName),
                                  Slf4JStopWatch.mapLevelName(normalPriorityName),
                                  Slf4JStopWatch.mapLevelName(exceptionPriorityName));
    }

    protected void customTests() {
        Slf4JStopWatch stopWatch = new Slf4JStopWatch(0L, 1000L, "tag", "message",
                                                      LoggerFactory.getLogger("org.perf4j"),
                                                      Slf4JStopWatch.TRACE_LEVEL,
                                                      Slf4JStopWatch.INFO_LEVEL);
        assertEquals(0L, stopWatch.getStartTime());
        assertEquals(1000L, stopWatch.getElapsedTime());
        assertEquals("tag", stopWatch.getTag());
        assertEquals("message", stopWatch.getMessage());
        assertEquals(LoggerFactory.getLogger("org.perf4j"), stopWatch.getLogger());
        assertEquals(Slf4JStopWatch.TRACE_LEVEL, stopWatch.getNormalPriority());
        assertEquals(Slf4JStopWatch.INFO_LEVEL, stopWatch.getExceptionPriority());

        stopWatch.setLogger(LoggerFactory.getLogger("org.perf4j.AnotherTestLogger"));
        assertEquals(LoggerFactory.getLogger("org.perf4j.AnotherTestLogger"), stopWatch.getLogger());
        stopWatch.setNormalPriority(Slf4JStopWatch.ERROR_LEVEL);
        assertEquals(Slf4JStopWatch.ERROR_LEVEL, stopWatch.getNormalPriority());
        stopWatch.setExceptionPriority(Slf4JStopWatch.WARN_LEVEL);
        assertEquals(Slf4JStopWatch.WARN_LEVEL, stopWatch.getExceptionPriority());
        assertTrue(stopWatch.isLogging());

        //check closest known level stuff
        stopWatch.setNormalPriority(Slf4JStopWatch.ERROR_LEVEL - 10);
        assertTrue(stopWatch.isLogging());
    }

    protected void checkProperties(LoggingStopWatch stopWatch,
                                   String expectedLoggerName,
                                   String expectedNormalPriority,
                                   String expectedExceptionPriority,
                                   String expectedTag,
                                   String expectedMessage) {
        super.checkProperties(stopWatch, expectedLoggerName, expectedNormalPriority, expectedExceptionPriority,
                              expectedTag, expectedMessage);
        Slf4JStopWatch commonsLogStopWatch = (Slf4JStopWatch) stopWatch;
        assertEquals(LoggerFactory.getLogger(expectedLoggerName), commonsLogStopWatch.getLogger());
        assertEquals(Slf4JStopWatch.mapLevelName(expectedNormalPriority),
                     commonsLogStopWatch.getNormalPriority());
        assertEquals(Slf4JStopWatch.mapLevelName(expectedExceptionPriority),
                     commonsLogStopWatch.getExceptionPriority());
    }
}