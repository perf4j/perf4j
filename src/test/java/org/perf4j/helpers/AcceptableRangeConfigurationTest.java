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
package org.perf4j.helpers;

import junit.framework.TestCase;

/**
 * Tests the AcceptableRangeConfiguration object.
 */
public class AcceptableRangeConfigurationTest extends TestCase {

    public void testConfigStrings() throws Exception {
        checkValidConfigString("tagName(<100)", "tagName", Double.NEGATIVE_INFINITY, 100.0);
        checkValidConfigString("a.tag.name(>20.5)", "a.tag.name", 20.5, Double.POSITIVE_INFINITY);
        checkValidConfigString("a/tag/name(100-200)", "a/tag/name", 100.0, 200.0);
        checkValidConfigString("tag100(-50-50)", "tag100", -50.0, 50.0);
        checkValidConfigString("a-tag(-100--50)", "a-tag", -100.0, -50.0);
        checkValidConfigString("someTag(>-22.22)", "someTag", -22.22, Double.POSITIVE_INFINITY);
        checkValidConfigString("someTag(<-123)", "someTag", Double.NEGATIVE_INFINITY, -123.0);
        checkValidConfigString("spaces ( 100.0 - 200.0 )", "spaces", 100.0, 200.0);

        checkInvalidConfigString("");
        checkInvalidConfigString("(>100)");
        checkInvalidConfigString("tag(50-100");
        checkInvalidConfigString("tag(100.0.0-200.0.0)");
        checkInvalidConfigString("tag(<abc)");
        checkInvalidConfigString("tag(100-)");
        checkInvalidConfigString("tag(-200)");
    }

    public void testIsInRange() throws Exception {
        AcceptableRangeConfiguration arc = new AcceptableRangeConfiguration("tag(<50)");
        assertTrue(arc.isInRange(-50.0));
        assertTrue(arc.isInRange(0.0));
        assertTrue(arc.isInRange(50.0));
        assertFalse(arc.isInRange(51.0));

        arc = new AcceptableRangeConfiguration("tag(>50)");
        assertTrue(arc.isInRange(100.0));
        assertTrue(arc.isInRange(50.0));
        assertFalse(arc.isInRange(30.0));

        arc = new AcceptableRangeConfiguration("tag(100-200)");
        assertFalse(arc.isInRange(50.0));
        assertTrue(arc.isInRange(100.0));
        assertTrue(arc.isInRange(150.0));
        assertTrue(arc.isInRange(200.0));
        assertFalse(arc.isInRange(250.0));
    }

    public void testBeanMethods() throws Exception {
        AcceptableRangeConfiguration arc = new AcceptableRangeConfiguration();
        arc.setAttributeName("foo");
        assertEquals("foo", arc.getAttributeName());
        arc.setMinValue(10.0);
        assertEquals(10.0, arc.getMinValue());
        arc.setMaxValue(20.0);
        assertEquals(20.0, arc.getMaxValue());

        arc = new AcceptableRangeConfiguration("tag", 30.0, 40.0);
        assertEquals("tag", arc.getAttributeName());
        assertEquals(30.0, arc.getMinValue());
        assertEquals(40.0, arc.getMaxValue());
    }

    public void testObjectMethods() throws Exception {
        AcceptableRangeConfiguration arc = new AcceptableRangeConfiguration("tag", 30.0, 40.0);
        assertTrue(arc.equals(arc));
        assertFalse(arc.equals(new Object()));
        assertFalse(arc.equals(new AcceptableRangeConfiguration("tag", 35.0, 40.0)));
        assertFalse(arc.equals(new AcceptableRangeConfiguration("tag", 30.0, 45.0)));
        assertFalse(arc.equals(new AcceptableRangeConfiguration("tag2", 30.0, 40.0)));
        assertTrue(arc.equals(new AcceptableRangeConfiguration("tag(30-40)")));
        assertTrue(arc.equals(arc.clone()));

        assertEquals(arc.hashCode(), arc.clone().hashCode());

        assertEquals("tag(<100.0)", new AcceptableRangeConfiguration("tag(<100)").toString());
        assertEquals("tag(>200.0)", new AcceptableRangeConfiguration("tag(>200)").toString());
        assertEquals("tag(25.5-35.5)", new AcceptableRangeConfiguration("tag", 25.5, 35.5).toString());
    }

    protected void checkValidConfigString(String configString, String attributeName, double minValue, double maxValue) {
        AcceptableRangeConfiguration arc = new AcceptableRangeConfiguration(configString);
        assertEquals(attributeName, arc.getAttributeName());
        assertEquals(minValue, arc.getMinValue());
        assertEquals(maxValue, arc.getMaxValue());
    }

    protected void checkInvalidConfigString(String configString) {
        try {
            new AcceptableRangeConfiguration(configString);
            fail("bad config string should have failed: " + configString);
        } catch (IllegalArgumentException iae) {
            //expected
        }
    }

}
