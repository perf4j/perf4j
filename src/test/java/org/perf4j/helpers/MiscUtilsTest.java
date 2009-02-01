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
import static org.perf4j.helpers.MiscUtils.*;

import java.util.GregorianCalendar;

/**
 * Tests the helper methods of the MiscUtils class
 */
public class MiscUtilsTest extends TestCase {
    public void testEscapeStringForCsv() throws Throwable {
        assertEquals("\"foo\"", escapeStringForCsv("foo", new StringBuilder()).toString());
        assertEquals("\"foo,bar\"", escapeStringForCsv("foo,bar", new StringBuilder()).toString());
        assertEquals("\"\"\"foo\"\"\"", escapeStringForCsv("\"foo\"", new StringBuilder()).toString());
        assertEquals("\"foo,\"\"bar\"\"\"", escapeStringForCsv("foo,\"bar\"", new StringBuilder()).toString());
    }

    public void testPadIntToTwoDigits() throws Throwable {
        assertEquals("00", padIntToTwoDigits(0, new StringBuilder()).toString());
        assertEquals("01", padIntToTwoDigits(1, new StringBuilder()).toString());
        assertEquals("09", padIntToTwoDigits(9, new StringBuilder()).toString());
        assertEquals("10", padIntToTwoDigits(10, new StringBuilder()).toString());
        assertEquals("99", padIntToTwoDigits(99, new StringBuilder()).toString());
    }

    public void testFormatDateIso8601() throws Throwable {
        assertEquals("2008-01-01 13:01:10",
                     formatDateIso8601(new GregorianCalendar(2008, 0, 1, 13, 1, 10).getTimeInMillis()));
        assertEquals("2010-10-31 08:59:59",
                     formatDateIso8601(new GregorianCalendar(2010, 9, 31, 8, 59, 59).getTimeInMillis()));
    }
}
