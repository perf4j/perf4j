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
package org.perf4j;

import java.util.SortedMap;
import java.util.TreeMap;

import junit.framework.TestCase;

/**
 * PERFFORJ-55.
 */
public class GroupedTimingStatisticsTest extends TestCase {

    private final boolean createRollupStatistics = false;
    private final long startTime = System.currentTimeMillis();
    private final long stopTime = startTime + 100;
    private final SortedMap<String, TimingStatistics> statisticsByTag = new TreeMap<String, TimingStatistics>();

    public void testOutputHasPaddingForTagHeadingWithEmptyGroupedTimingStatistics() throws Exception {
        GroupedTimingStatistics groupStatistics = new GroupedTimingStatistics(statisticsByTag, startTime, stopTime, createRollupStatistics);
        assertOutputContains(groupStatistics.toString(), "Tag     Avg(ms)         Min         Max     Std-Dev       Count");
    }

    public void testOutputHasPaddingAllowingForTheLongestTag() throws Exception {
        statisticsByTag.put("a short tag", new TimingStatistics(0.1, 0.2, 3, 4, 5));
        statisticsByTag.put("a very very very very very very very very very very very very long tag",
                new TimingStatistics(0.1, 0.2, 3, 4, 5));
        statisticsByTag.put("another short tag", new TimingStatistics(0.1, 0.2, 3, 4, 5));

        GroupedTimingStatistics groupStatistics = new GroupedTimingStatistics(statisticsByTag, startTime, stopTime, createRollupStatistics);
        String output = groupStatistics.toString();

        assertOutputContains(output, "Tag                                                                        Avg(ms)         Min         Max     Std-Dev       Count");
        assertOutputContains(output, "a short tag                                                                    0.1           4           3         0.2           5");
        assertOutputContains(output, "a very very very very very very very very very very very very long tag         0.1           4           3         0.2           5");
        assertOutputContains(output, "another short tag                                                              0.1           4           3         0.2           5");
    }

    public void testOutputContainsHeadingForTotalTimeSpentPerTag() throws Exception {
        GroupedTimingStatistics groupStatistics = new GroupedTimingStatistics(statisticsByTag, startTime, stopTime, createRollupStatistics);
        assertOutputContains(groupStatistics.toString(), "Tag     Avg(ms)         Min         Max     Std-Dev       Count       Total");
    }

    public void testOutputContainsTotalTimeSpentPerTagAsMeanxCountRoundedToZeroDecimalPlaces() throws Exception {
        statisticsByTag.put("a", new TimingStatistics(1.1, 0.2, 3, 4, 5));

        GroupedTimingStatistics groupStatistics = new GroupedTimingStatistics(statisticsByTag, startTime, stopTime, createRollupStatistics);
        System.out.println(groupStatistics.toString());
        assertOutputContains(groupStatistics.toString(), "Tag     Avg(ms)         Min         Max     Std-Dev       Count       Total");
        assertOutputContains(groupStatistics.toString(), "a           1.1           4           3         0.2           5           6");
    }




    private void assertOutputContains(String output, String expectedToContain) {
        String message = "Expected toString() output to contain the given string, matching formatting.\n" + expectedToContain +
        "\nActual output:\n" + output;

        assertTrue(message, output.contains(expectedToContain));
    }
}