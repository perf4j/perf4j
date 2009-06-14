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

import org.perf4j.GroupedTimingStatistics;

import java.util.Calendar;

/**
 * Miscellaneous static utility functions, mainly having to do with String parsing/formatting.
 *
 * @author Alex Devine
 */
public class MiscUtils {
    /**
     * The value of the line.separator system property.
     */
    public static final String NEWLINE = System.getProperty("line.separator");

    /**
     * Escapes the specified string for use in a comma-separated values file.
     *
     * @param string   The String to escape
     * @param toAppend The StringBuilder to which the escaped String should be appended
     * @return The StringBuilder passed in
     */
    public static StringBuilder escapeStringForCsv(String string, StringBuilder toAppend) {
        //need to escape quotes and commas - always add quotes just to be safe.
        toAppend.append('"');

        int lastQuoteIndex = 0;
        for (int i = 0; i < string.length(); i++) {
            char charAtIndex = string.charAt(i);
            if ('"' == charAtIndex) {
                toAppend.append(string.substring(lastQuoteIndex, i)).append("\"\"");
                lastQuoteIndex = i + 1;
            }
        }
        //append the last section
        toAppend.append(string.substring(lastQuoteIndex));

        return toAppend.append('"');
    }

    /**
     * Pads the specified int to two digits, prefixing with 0 if the value is less than 10.
     *
     * @param i        The value to pad, should be between 0 and 99
     * @param toAppend The StringBuilder to which the padded value should be appended
     * @return The StringBuilder passed in
     */
    public static StringBuilder padIntToTwoDigits(int i, StringBuilder toAppend) {
        if (i < 10) {
            toAppend.append("0");
        }
        return toAppend.append(i);
    }

    /**
     * Formats the specified time in yyyy-MM-dd HH:mm:ss format.
     *
     * @param timeInMillis The time in milliseconds since 1970.
     * @return The formatted date/time String
     */
    public static String formatDateIso8601(long timeInMillis) {
        StringBuilder retVal = new StringBuilder(19);

        Calendar cal = Calendar.getInstance(GroupedTimingStatistics.getTimeZone());
        cal.setTimeInMillis(timeInMillis);
        int year = cal.get(Calendar.YEAR);
        int month = cal.get(Calendar.MONTH);
        int day = cal.get(Calendar.DAY_OF_MONTH);
        int hour = cal.get(Calendar.HOUR_OF_DAY);
        int minute = cal.get(Calendar.MINUTE);
        int second = cal.get(Calendar.SECOND);

        retVal.append(year).append('-');
        MiscUtils.padIntToTwoDigits(month + 1, retVal).append('-');
        MiscUtils.padIntToTwoDigits(day, retVal).append(' ');
        MiscUtils.padIntToTwoDigits(hour, retVal).append(':');
        MiscUtils.padIntToTwoDigits(minute, retVal).append(':');
        return MiscUtils.padIntToTwoDigits(second, retVal).toString();
    }

    /**
     * Splits a string using the specified delimiter, and also trims all the resultant strings in the returned array.
     * This is useful for setting multi-valued options on appenders.
     *
     * @param stringToSplit The String to be split, may not be null
     * @param delimiter     The delimiter to use to split the string, may not be null.
     * @return The split and trimmed Strings
     */
    public static String[] splitAndTrim(String stringToSplit, String delimiter) {
        String[] retVal = stringToSplit.split(delimiter);
        for (int i = 0; i < retVal.length; i++) {
            retVal[i] = retVal[i].trim();
        }
        return retVal;
    }
}
