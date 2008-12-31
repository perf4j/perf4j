/* Copyright Homeaway, Inc 2005-2007. All Rights Reserved.
 * No unauthorized use of this software.
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