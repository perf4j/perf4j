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

import java.util.List;
import java.util.ArrayList;
import java.util.Collections;

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
        ProfiledObject.simpleTestDefaultTagStatic(50);
        assertTrue("Expected tag not found in " + InMemoryTimingAspect.getLastLoggedString(),
                   InMemoryTimingAspect.getLastLoggedString().indexOf("tag[simpleTestDefaultTagStatic]") >= 0);

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

        profiledObject.simpleTestWithJexlTagAndMessageClassMethod(50, new ProfiledObject.SimpleBean("Alex", 32));
        assertTrue("Expected tag not found in " + InMemoryTimingAspect.getLastLoggedString(),
                   InMemoryTimingAspect.getLastLoggedString().indexOf("tag[expressionTest_org.perf4j.aop.ProfiledObject#simpleTestWithJexlTagAndMessageClassMethod]") >= 0);
        assertTrue("Expected message not found in " + InMemoryTimingAspect.getLastLoggedString(),
                   InMemoryTimingAspect.getLastLoggedString().indexOf("message_simpleTestWithJexlTagAndMessageClassMethod(50,Alex_32)") >= 0);

        profiledObject.simpleTestWithJexlMessageOnly(50, new ProfiledObject.SimpleBean("Alex", 32));
        assertTrue("Expected tag not found in " + InMemoryTimingAspect.getLastLoggedString(),
                   InMemoryTimingAspect.getLastLoggedString().indexOf("expressionTest") >= 0);
        assertTrue("Expected message not found in " + InMemoryTimingAspect.getLastLoggedString(),
                   InMemoryTimingAspect.getLastLoggedString().indexOf("message[message_50_Alex_32]") >= 0);

        profiledObject.simpleTestWithLevel(50);
        assertTrue("Shouldn't have logged when level was DEBUG: " + InMemoryTimingAspect.getLastLoggedString(),
                   InMemoryTimingAspect.getLastLoggedString().indexOf("tag[simpleTestWithLevel]") < 0);

        profiledObject.simpleTestWithJexlThisAndReturn(100);
        assertTrue("Expected tag not found in " + InMemoryTimingAspect.getLastLoggedString(),
                   InMemoryTimingAspect.getLastLoggedString().indexOf("expressionTest_0]") >= 0);
        assertTrue("Expected message not found in " + InMemoryTimingAspect.getLastLoggedString(),
                   InMemoryTimingAspect.getLastLoggedString().indexOf("message[message: 5, exception: null]") >= 0);

        profiledObject.simpleTestWithTimeThreshold(5);
        assertTrue("Should not have logged when time threshold not crossed",
                   InMemoryTimingAspect.getLastLoggedString().indexOf("tag[simpleWithThreshold]") < 0);
        profiledObject.simpleTestWithTimeThreshold(55);
        assertTrue("Expected tag not found in " + InMemoryTimingAspect.getLastLoggedString(),
                   InMemoryTimingAspect.getLastLoggedString().indexOf("tag[simpleWithThreshold]") >= 0);

        profiledObject.simpleTestWithSuffixesNoThreshold(5);
        assertTrue("Expected tag not found in " + InMemoryTimingAspect.getLastLoggedString(),
                   InMemoryTimingAspect.getLastLoggedString().indexOf("tag[simpleWithSuffixesNoThreshold.slow]") >= 0);
        profiledObject.simpleTestWithSuffixesNoThreshold(55);
        assertTrue("Expected tag not found in " + InMemoryTimingAspect.getLastLoggedString(),
                   InMemoryTimingAspect.getLastLoggedString().indexOf("tag[simpleWithSuffixesNoThreshold.slow]") >= 0);

        profiledObject.simpleTestWithSuffixes(5);
        assertTrue("Expected tag not found in " + InMemoryTimingAspect.getLastLoggedString(),
                   InMemoryTimingAspect.getLastLoggedString().indexOf("tag[simpleWithSuffixes.normal]") >= 0);
        profiledObject.simpleTestWithSuffixes(55);
        assertTrue("Expected tag not found in " + InMemoryTimingAspect.getLastLoggedString(),
                   InMemoryTimingAspect.getLastLoggedString().indexOf("tag[simpleWithSuffixes.slow]") >= 0);

        try {
            profiledObject.simpleTestWithJexlException(100);
        } catch (Exception e) { /* expected */ }
        assertTrue("Expected tag not found in " + InMemoryTimingAspect.getLastLoggedString(),
                   InMemoryTimingAspect.getLastLoggedString().indexOf("expressionTest_null]") >= 0);
        assertTrue("Expected message not found in " + InMemoryTimingAspect.getLastLoggedString(),
                   InMemoryTimingAspect.getLastLoggedString().indexOf("message[message: 5, exception: java.lang.Exception: failure]") >= 0);

        // additionalScope, not @Profiled, see concrete aspect in aop.xml
        profiledObject.simpleTestUnprofiled(50);
        assertTrue("Expected tag not found in " + InMemoryTimingAspect.getLastLoggedString(),
                InMemoryTimingAspect.getLastLoggedString().indexOf("tag[simpleTestUnprofiled]") >= 0);

        // false assertion - this method should not be advised/logged, see aop.xml
        profiledObject.simpleTestUnprofiledNotAdvised(50);
        assertFalse("Expected tag not found in " + InMemoryTimingAspect.getLastLoggedString(),
                InMemoryTimingAspect.getLastLoggedString().indexOf("tag[simpleTestUnprofiledNotAdvised]") >= 0);
        profiledObject.simpleTestDefaultTagMessageFromProperties(5);
        assertTrue("Expected tag not found in " + InMemoryTimingAspect.getLastLoggedString(),
                InMemoryTimingAspect.getLastLoggedString().indexOf("tag[customTag]") >= 0);
        assertTrue("Expected tag not found in " + InMemoryTimingAspect.getLastLoggedString(),
                InMemoryTimingAspect.getLastLoggedString().indexOf("message[customMessage]") >= 0);

        profiledObject.simpleTestDefaultTagMessageFromPropertiesJexl(5);
        assertTrue("Expected tag not found in " + InMemoryTimingAspect.getLastLoggedString(),
                InMemoryTimingAspect.getLastLoggedString().indexOf("tag[org.perf4j.aop.ProfiledObject#simpleTestDefaultTagMessageFromPropertiesJexl]") >= 0);
        assertTrue("Expected tag not found in " + InMemoryTimingAspect.getLastLoggedString(),
                InMemoryTimingAspect.getLastLoggedString().indexOf("message[simpleTestDefaultTagMessageFromPropertiesJexl(5)]") >= 0);

    }

    public void testConcurrentCalls() throws Exception {
        InMemoryTimingAspect.logStrings.clear();
        final List<String> expectedTags = Collections.synchronizedList(new ArrayList<String>(2000));

        //run a bunch of threads concurrently that call a @Profiled method
        Thread[] threads = new Thread[20];
        for (int i = 0; i < threads.length; i++) {
            threads[i] = new Thread() {
                public void run() {
                    for (int i = 0; i < 100; i++) {
                        try {
                            profiledObject.simpleTestWithJexlTag(i, new ProfiledObject.SimpleBean("Alex", 10));
                            expectedTags.add("expressionTest_" + i + "_Alex_10");
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    }
                }
            };
            threads[i].start();
        }

        //wait for all the threads to finish
        for (Thread thread : threads) {
            thread.join();
        }
        Thread.sleep(100);

        //ensure that that all of the expected tags are present in the InMemoryTimingAspect.logStrings
        assertEquals(expectedTags.size(), InMemoryTimingAspect.logStrings.size());
outer:  for (String expectedTag : expectedTags) {
            for (String logString : InMemoryTimingAspect.logStrings) {
                if (logString.indexOf(expectedTag) >= 0) {
                    continue outer;
                }
            }

            //if this far, expected tag wasn't found in logStrings
            fail("Expected tag " + expectedTag + " not found");
        }
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
