/* Copyright (c) 2011 Brett Randall.
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
package org.perf4j.logback;

import org.perf4j.StopWatch;
import org.perf4j.aop.ProfiledObject;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import junit.framework.TestCase;

/**
 * 
 * @author Brett Randall
 * 
 */
public class AopTest extends TestCase {

    public void testAspects() throws Exception {

        LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
        JoranConfigurator configurator = new JoranConfigurator();
        configurator.setContext(lc);

        // the context was probably already configured by default configuration
        // rules
        lc.reset();

        configurator.doConfigure(getClass().getResource("logback.xml"));

        ListAppender<LoggingEvent> listAppender = (ListAppender<LoggingEvent>) lc
                .getLogger(StopWatch.DEFAULT_LOGGER_NAME).getAppender(
                        "listAppender");

        ProfiledObject.simpleTestDefaultTagStatic(10);
        assertTrue(
                "Expected tag not found in "
                        + listAppender.list.get(0).getMessage(),
                listAppender.list.get(0).getMessage()
                        .indexOf("tag[simpleTestDefaultTagStatic]") >= 0);

        new ProfiledObject().simpleTestUnprofiled(10);
        assertTrue(
                "Expected tag not found in "
                        + listAppender.list.get(1).getMessage(),
                listAppender.list.get(1).getMessage()
                        .indexOf("tag[simpleTestUnprofiled]") >= 0);

        assertEquals("Expected two logging events", 2, listAppender.list.size());
    }
}
