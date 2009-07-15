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
package org.perf4j.commonslog;

import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.impl.LogFactoryImpl;
import org.apache.commons.logging.impl.SimpleLog;
import org.perf4j.LoggingStopWatch;
import org.perf4j.LoggingStopWatchTest;
import org.perf4j.StopWatch;

/**
 * Tests the CommonsLogStopWatch. See the superclass for the test method that is run.
 */
public class CommonsLogStopWatchTest extends LoggingStopWatchTest {
    private String originalLogFactoryAttribute;

    protected void setUp() throws Exception {
        super.setUp();

        originalLogFactoryAttribute = (String) LogFactory.getFactory().getAttribute(LogFactoryImpl.LOG_PROPERTY);
        LogFactory.getFactory().setAttribute(LogFactoryImpl.LOG_PROPERTY, SimpleLog.class.getName());
    }

    protected void tearDown() throws Exception {
        LogFactory.getFactory().setAttribute(LogFactoryImpl.LOG_PROPERTY, originalLogFactoryAttribute);
        super.tearDown();
    }
    
    public void testStopWatch() throws Exception {
        //We override the testStopWatch method because the way we configure the LogFactory doesn't work in 
        //TeamCity, so we skip this test in TeamCity builds.
        LogFactory.getLog(StopWatch.DEFAULT_LOGGER_NAME).info("GOING_TO_STD_ERR");
        if (fakeErr.toString().indexOf("GOING_TO_STD_ERR") >= 0) {
            //then things are set up correctly, run the test
            super.testStopWatch();
        } else {
            System.out.println("Logging isn't going to our std err as expected - skipping CommonsLogStopWatchTest");
        }
    }    

    protected CommonsLogStopWatch createStopWatch(String loggerName,
                                                  String normalPriorityName,
                                                  String exceptionPriorityName,
                                                  String tag,
                                                  String message) {
        if (loggerName == null && normalPriorityName == null && exceptionPriorityName == null
            && tag == null && message == null) {
            return new CommonsLogStopWatch();
        }
        if (loggerName == null && normalPriorityName == null && exceptionPriorityName == null && message == null) {
            return new CommonsLogStopWatch(tag);
        }
        if (loggerName == null && normalPriorityName == null && exceptionPriorityName == null) {
            return new CommonsLogStopWatch(tag, message);
        }
        if (normalPriorityName == null && exceptionPriorityName == null && tag == null && message == null) {
            return new CommonsLogStopWatch(LogFactory.getLog(loggerName));
        }
        if (exceptionPriorityName == null && tag == null && message == null) {
            return new CommonsLogStopWatch(LogFactory.getLog(loggerName),
                                           CommonsLogStopWatch.mapLevelName(normalPriorityName));
        }
        if (tag == null && message == null) {
            return new CommonsLogStopWatch(LogFactory.getLog(loggerName),
                                           CommonsLogStopWatch.mapLevelName(normalPriorityName),
                                           CommonsLogStopWatch.mapLevelName(exceptionPriorityName));
        }
        if (normalPriorityName == null && exceptionPriorityName == null && message == null) {
            return new CommonsLogStopWatch(tag, LogFactory.getLog(loggerName));
        }
        if (exceptionPriorityName == null && message == null) {
            return new CommonsLogStopWatch(tag,
                                           LogFactory.getLog(loggerName),
                                           CommonsLogStopWatch.mapLevelName(normalPriorityName));
        }
        if (message == null) {
            return new CommonsLogStopWatch(tag,
                                           LogFactory.getLog(loggerName),
                                           CommonsLogStopWatch.mapLevelName(normalPriorityName),
                                           CommonsLogStopWatch.mapLevelName(exceptionPriorityName));
        }
        if (normalPriorityName == null && exceptionPriorityName == null) {
            return new CommonsLogStopWatch(tag, message, LogFactory.getLog(loggerName));
        }
        if (exceptionPriorityName == null) {
            return new CommonsLogStopWatch(tag,
                                           message,
                                           LogFactory.getLog(loggerName),
                                           CommonsLogStopWatch.mapLevelName(normalPriorityName));
        }
        return new CommonsLogStopWatch(tag,
                                       message,
                                       LogFactory.getLog(loggerName),
                                       CommonsLogStopWatch.mapLevelName(normalPriorityName),
                                       CommonsLogStopWatch.mapLevelName(exceptionPriorityName));
    }

    protected void customTests() {
        CommonsLogStopWatch stopWatch = new CommonsLogStopWatch(0L, 1000L, "tag", "message",
                                                                LogFactory.getLog("org.perf4j"),
                                                                CommonsLogStopWatch.TRACE_LEVEL,
                                                                CommonsLogStopWatch.INFO_LEVEL);
        assertEquals(0L, stopWatch.getStartTime());
        assertEquals(1000L, stopWatch.getElapsedTime());
        assertEquals("tag", stopWatch.getTag());
        assertEquals("message", stopWatch.getMessage());
        assertEquals(LogFactory.getLog("org.perf4j"), stopWatch.getLogger());
        assertEquals(CommonsLogStopWatch.TRACE_LEVEL, stopWatch.getNormalPriority());
        assertEquals(CommonsLogStopWatch.INFO_LEVEL, stopWatch.getExceptionPriority());

        stopWatch.setLogger(LogFactory.getLog("org.perf4j.AnotherTestLogger"));
        assertEquals(LogFactory.getLog("org.perf4j.AnotherTestLogger"), stopWatch.getLogger());
        stopWatch.setNormalPriority(CommonsLogStopWatch.FATAL_LEVEL);
        assertEquals(CommonsLogStopWatch.FATAL_LEVEL, stopWatch.getNormalPriority());
        stopWatch.setExceptionPriority(CommonsLogStopWatch.WARN_LEVEL);
        assertEquals(CommonsLogStopWatch.WARN_LEVEL, stopWatch.getExceptionPriority());
        assertTrue(stopWatch.isLogging());

        //check closest known level stuff
        stopWatch.setNormalPriority(CommonsLogStopWatch.FATAL_LEVEL - 10);
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
        CommonsLogStopWatch commonsLogStopWatch = (CommonsLogStopWatch) stopWatch;
        assertEquals(LogFactory.getLog(expectedLoggerName), commonsLogStopWatch.getLogger());
        assertEquals(CommonsLogStopWatch.mapLevelName(expectedNormalPriority),
                     commonsLogStopWatch.getNormalPriority());
        assertEquals(CommonsLogStopWatch.mapLevelName(expectedExceptionPriority),
                     commonsLogStopWatch.getExceptionPriority());
    }

    protected void checkSerializationAndCloning(LoggingStopWatch stopWatch) {
        LoggingStopWatch cloneCopy = stopWatch.clone();
        assertEquals(stopWatch, cloneCopy);

        //we don't test serialization because by default most Commons Logging Log implementations are NOT serializable
    }
}