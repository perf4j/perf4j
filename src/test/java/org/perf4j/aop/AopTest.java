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
package org.perf4j.aop;

import junit.framework.TestCase;

/**
 * Tests the Aspect Oriented Programming support provided by perf4j utilizing AspectJ AOP.
 */
public class AopTest extends TestCase {
    ProfiledObject profiledObject;

    public void setUp() throws Exception {
        super.setUp();
        profiledObject = new ProfiledObject();
    }

    public void testAspects() throws Exception {
        profiledObject.simpleTestDefaultTag(50);
        assertTrue("Expected tag not found in " + InMemoryTimingAspect.getLastLoggedString(),
                   InMemoryTimingAspect.getLastLoggedString().indexOf("tag[simpleTestDefaultTag]") >= 0);

        profiledObject.simpleTest(50);
        assertTrue("Expected tag not found in " + InMemoryTimingAspect.getLastLoggedString(),
                   InMemoryTimingAspect.getLastLoggedString().indexOf("tag[simple]") >= 0);

        profiledObject.simpleTestWithFailuresSeparate(50, false);
        assertTrue("Expected tag not found in " + InMemoryTimingAspect.getLastLoggedString(),
                   InMemoryTimingAspect.getLastLoggedString().indexOf("tag[simpleWithFails.success]") >= 0);

        try {
            profiledObject.simpleTestWithFailuresSeparate(50, true);
        } catch (Exception e) { /* expected */ }
        assertTrue("Expected tag not found in " + InMemoryTimingAspect.getLastLoggedString(),
                   InMemoryTimingAspect.getLastLoggedString().indexOf("tag[simpleWithFails.failure]") >= 0);

        profiledObject.simpleTestWithMessage(50);
        assertTrue("Expected message not found in " + InMemoryTimingAspect.getLastLoggedString(),
                   InMemoryTimingAspect.getLastLoggedString().indexOf("message[message]") >= 0);

        profiledObject.simpleTestWithJexlTag(50, new ProfiledObject.SimpleBean("Alex", 32));
        assertTrue("Expected tag not found in " + InMemoryTimingAspect.getLastLoggedString(),
                   InMemoryTimingAspect.getLastLoggedString().indexOf("tag[expressionTest_50_Alex_32]") >= 0);

        profiledObject.simpleTestWithJexlTagAndMessage(50, new ProfiledObject.SimpleBean("Alex", 32));
        assertTrue("Expected tag not found in " + InMemoryTimingAspect.getLastLoggedString(),
                   InMemoryTimingAspect.getLastLoggedString().indexOf("tag[expressionTest_50_Alex_32]") >= 0);
        assertTrue("Expected message not found in " + InMemoryTimingAspect.getLastLoggedString(),
                   InMemoryTimingAspect.getLastLoggedString().indexOf("message[message_50_Alex_32]") >= 0);

        profiledObject.simpleTestWithJexlMessageOnly(50, new ProfiledObject.SimpleBean("Alex", 32));
        assertTrue("Expected tag not found in " + InMemoryTimingAspect.getLastLoggedString(),
                   InMemoryTimingAspect.getLastLoggedString().indexOf("expressionTest") >= 0);
        assertTrue("Expected message not found in " + InMemoryTimingAspect.getLastLoggedString(),
                   InMemoryTimingAspect.getLastLoggedString().indexOf("message[message_50_Alex_32]") >= 0);

        profiledObject.simpleTestWithLevel(50);
        assertTrue("Shouldn't have logged when level was DEBUG: " + InMemoryTimingAspect.getLastLoggedString(),
                   InMemoryTimingAspect.getLastLoggedString().indexOf("tag[simpleTestWithLevel]") < 0);
    }

    /**
     * Tests that call joinpoints are ignored when using Perf4j's AOP
     *
     * @throws Exception if anything bad happens
     */
    public void testCallsDoNotCreateDuplicateLogEntries() throws Exception {
        //calling the profiled method should result in one log
        InMemoryTimingAspect.logStrings.clear(); //clear the log entries
        profiledObject.simpleTest(50);
        assertEquals(1, InMemoryTimingAspect.logStrings.size());

        //calling the method that CALLS the profiled method should still result in only one call
        InMemoryTimingAspect.logStrings.clear(); //clear the log entries
        profiledObject.simpleMethodCallExample(50);
        assertEquals(1, InMemoryTimingAspect.logStrings.size());
    }
}