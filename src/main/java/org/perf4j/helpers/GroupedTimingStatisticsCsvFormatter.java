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
import org.perf4j.TimingStatistics;

import java.util.Map;

/**
 * This helper formatter class outputs {@link org.perf4j.GroupedTimingStatistics} in a comma-separated value format.
 * This class supports two main types of formats:
 * <p/>
 * <ul>
 * <li>Pivoted output, where each GroupedTimingStatistics is output as a single line. The client must specify
 * which tags from the GroupedTimingStatistics should have its values output.
 * <li>Non-pivoted output, where each individual tag in the GroupedTimingStatistics gets its own row.
 * </ul>
 *
 * @author Alex Devine
 */
public class GroupedTimingStatisticsCsvFormatter implements GroupedTimingStatisticsFormatter {
    /**
     * The default format string for a non-pivoted formatter.
     */
    public static final String DEFAULT_FORMAT_STRING = "tag,start,stop,mean,min,max,stddev,count";

    //whether or not the output is pivoted
    private boolean pivot;

    //valueRetrievers is only used if pivot is false, otherwise it's null.
    private TimingStatsValueRetriever[] valueRetrievers;

    //pivotedValueRetrievers is only used if pivot is true, otherwise it's null.
    private GroupedTimingStatisticsValueRetriever[] pivotedValueRetrievers;

    // --- Constructors ---

    /**
     * Creates a non-pivoted CSV formatter using the default format string.
     */
    public GroupedTimingStatisticsCsvFormatter() {
        this(false, DEFAULT_FORMAT_STRING);
    }

    /**
     * Creates a CSV formatter which allows you to config which values are output.
     *
     * @param pivot        Whether the output is pivoted (one row for each GroupedTimingStatisitcs) or not (one row
     *                     for each tagged TimingStatistics item contained in the GroupedTimingStatisitcs).
     * @param configString The config string defines which values will be output, and should be a comma-separated list
     *                     of the values. Possible values if pivot is false are
     *                     tag, start, stop, mean, min, max, stddev, count and tps. If pivot is true the possible
     *                     values are start, stop, and then one of the statistics prefixed with the tag name. For
     *                     example, a possible configString could be "start,stop,codeBlock1Mean,codeBlock2Max".
     */
    public GroupedTimingStatisticsCsvFormatter(boolean pivot, String configString) {
        this.pivot = pivot;

        String[] configElements = MiscUtils.splitAndTrim(configString, ",");
        if (pivot) {
            pivotedValueRetrievers = new GroupedTimingStatisticsValueRetriever[configElements.length];
            for (int i = 0; i < configElements.length; i++) {
                pivotedValueRetrievers[i] = parsePivotedTimingStatsConfig(configElements[i]);
            }
        } else {
            valueRetrievers = new TimingStatsValueRetriever[configElements.length];
            for (int i = 0; i < configElements.length; i++) {
                valueRetrievers[i] = parseTimingStatsConfig(configElements[i]);
            }
        }
    }

    // --- formatting methods ---

    /**
     * Formats the specified GroupedTimingStatistics instance for CSV output.
     *
     * @param stats the GroupedTimingStatistics instance, may not be null
     * @return The output formatted according to the configString passed in on this formatter's constructor
     *         (or the DEFAULT_FORMAT_STRING).
     */
    public String format(GroupedTimingStatistics stats) {
        String startTime = formatDate(stats.getStartTime());
        String stopTime = formatDate(stats.getStopTime());
        long windowLength = stats.getStopTime() - stats.getStartTime();

        StringBuilder retVal = new StringBuilder();

        if (pivot) {
            for (int i = 0; i < pivotedValueRetrievers.length; i++) {
                if (i > 0) {
                    retVal.append(',');
                }
                pivotedValueRetrievers[i].appendValue(startTime, stopTime, windowLength, stats, retVal);
            }
            retVal.append(MiscUtils.NEWLINE);
        } else {
            //iterate over each TimingStatistics item, creating one row for each
            for (Map.Entry<String, TimingStatistics> tagAndStats : stats.getStatisticsByTag().entrySet()) {
                String tag = tagAndStats.getKey();
                TimingStatistics timingStats = tagAndStats.getValue();

                for (int i = 0; i < valueRetrievers.length; i++) {
                    if (i > 0) {
                        retVal.append(',');
                    }
                    valueRetrievers[i].appendValue(tag, startTime, stopTime, windowLength, timingStats, retVal);
                }
                retVal.append(MiscUtils.NEWLINE);
            }
        }

        return retVal.toString();
    }

    // --- helper methods ---

    /**
     * Formats the specified time in yyyy-MM-dd HH:mm:ss format. Subclasses may override to give a different output.
     *
     * @param timeInMillis The time in milliseconds.
     * @return The formatted date/time String
     */
    protected String formatDate(long timeInMillis) {
        return MiscUtils.formatDateIso8601(timeInMillis);
    }

    /**
     * Helper method parses the specified single element from a config string to return the corresponding
     * GroupedTimingStatisticsValueRetriever.
     *
     * @param configName The element from the config string
     * @return The corresponding GroupedTimingStatisticsValueRetriever
     * @throws IllegalArgumentException Thrown if configName is an unknown value designator
     */
    protected GroupedTimingStatisticsValueRetriever parsePivotedTimingStatsConfig(String configName) {
        if ("start".equalsIgnoreCase(configName)) {
            return new GroupedTimingStatisticsValueRetriever() {
                public void appendValue(String start, String stop, long windowLength,
                                        GroupedTimingStatistics stats, StringBuilder toAppend) {
                    toAppend.append(start);
                }
            };
        } else if ("stop".equalsIgnoreCase(configName)) {
            return new GroupedTimingStatisticsValueRetriever() {
                public void appendValue(String start, String stop, long windowLength,
                                        GroupedTimingStatistics stats, StringBuilder toAppend) {
                    toAppend.append(stop);
                }
            };
        } else if (configName.toLowerCase().endsWith("mean")) {
            final String tag = configName.substring(0, configName.length() - "mean".length());
            return new GroupedTimingStatisticsValueRetriever() {
                public void appendValue(String start, String stop, long windowLength,
                                        GroupedTimingStatistics stats, StringBuilder toAppend) {
                    TimingStatistics timingStats = stats.getStatisticsByTag().get(tag);
                    toAppend.append((timingStats == null) ? "" : timingStats.getMean());
                }
            };
        } else if (configName.toLowerCase().endsWith("min")) {
            final String tag = configName.substring(0, configName.length() - "min".length());
            return new GroupedTimingStatisticsValueRetriever() {
                public void appendValue(String start, String stop, long windowLength,
                                        GroupedTimingStatistics stats, StringBuilder toAppend) {
                    TimingStatistics timingStats = stats.getStatisticsByTag().get(tag);
                    toAppend.append((timingStats == null) ? "" : timingStats.getMin());
                }
            };
        } else if (configName.toLowerCase().endsWith("max")) {
            final String tag = configName.substring(0, configName.length() - "max".length());
            return new GroupedTimingStatisticsValueRetriever() {
                public void appendValue(String start, String stop, long windowLength,
                                        GroupedTimingStatistics stats, StringBuilder toAppend) {
                    TimingStatistics timingStats = stats.getStatisticsByTag().get(tag);
                    toAppend.append((timingStats == null) ? "" : timingStats.getMax());
                }
            };
        } else if (configName.toLowerCase().endsWith("stddev")) {
            final String tag = configName.substring(0, configName.length() - "stddev".length());
            return new GroupedTimingStatisticsValueRetriever() {
                public void appendValue(String start, String stop, long windowLength,
                                        GroupedTimingStatistics stats, StringBuilder toAppend) {
                    TimingStatistics timingStats = stats.getStatisticsByTag().get(tag);
                    toAppend.append((timingStats == null) ? "" : timingStats.getStandardDeviation());
                }
            };
        } else if (configName.toLowerCase().endsWith("count")) {
            final String tag = configName.substring(0, configName.length() - "count".length());
            return new GroupedTimingStatisticsValueRetriever() {
                public void appendValue(String start, String stop, long windowLength,
                                        GroupedTimingStatistics stats, StringBuilder toAppend) {
                    TimingStatistics timingStats = stats.getStatisticsByTag().get(tag);
                    toAppend.append((timingStats == null) ? "" : timingStats.getCount());
                }
            };
        } else if (configName.toLowerCase().endsWith("tps")) {
            final String tag = configName.substring(0, configName.length() - "tps".length());
            return new GroupedTimingStatisticsValueRetriever() {
                public void appendValue(String start, String stop, long windowLength,
                                        GroupedTimingStatistics stats, StringBuilder toAppend) {
                    TimingStatistics timingStats = stats.getStatisticsByTag().get(tag);
                    if (timingStats == null) {
                        toAppend.append("");
                    } else {
                        toAppend.append((timingStats.getCount() * 1000.0) / windowLength);
                    }
                }
            };
        } else {
            throw new IllegalArgumentException("Unknown CSV format config string: " + configName);
        }
    }

    /**
     * Helper method parses the specified single element from a config string to return the corresponding
     * TimingStatsValueRetriever.
     *
     * @param configName The element from the config string
     * @return The corresponding TimingStatsValueRetriever
     * @throws IllegalArgumentException Thrown if configName is an unknown value designator
     */
    protected TimingStatsValueRetriever parseTimingStatsConfig(String configName) {
        if ("tag".equals(configName)) {
            return new TimingStatsValueRetriever() {
                public void appendValue(String tag, String start, String stop, long windowLength,
                                        TimingStatistics timingStats,
                                        StringBuilder toAppend) {
                    MiscUtils.escapeStringForCsv(tag, toAppend);
                }
            };
        } else if ("start".equals(configName)) {
            return new TimingStatsValueRetriever() {
                public void appendValue(String tag, String start, String stop, long windowLength,
                                        TimingStatistics timingStats,
                                        StringBuilder toAppend) {
                    toAppend.append(start);
                }
            };
        } else if ("stop".equals(configName)) {
            return new TimingStatsValueRetriever() {
                public void appendValue(String tag, String start, String stop, long windowLength,
                                        TimingStatistics timingStats,
                                        StringBuilder toAppend) {
                    toAppend.append(stop);
                }
            };
        } else if ("mean".equals(configName)) {
            return new TimingStatsValueRetriever() {
                public void appendValue(String tag, String start, String stop, long windowLength,
                                        TimingStatistics timingStats,
                                        StringBuilder toAppend) {
                    toAppend.append(timingStats.getMean());
                }
            };
        } else if ("min".equals(configName)) {
            return new TimingStatsValueRetriever() {
                public void appendValue(String tag, String start, String stop, long windowLength,
                                        TimingStatistics timingStats,
                                        StringBuilder toAppend) {
                    toAppend.append(timingStats.getMin());
                }
            };
        } else if ("max".equals(configName)) {
            return new TimingStatsValueRetriever() {
                public void appendValue(String tag, String start, String stop, long windowLength,
                                        TimingStatistics timingStats,
                                        StringBuilder toAppend) {
                    toAppend.append(timingStats.getMax());
                }
            };
        } else if ("stddev".equals(configName)) {
            return new TimingStatsValueRetriever() {
                public void appendValue(String tag, String start, String stop, long windowLength,
                                        TimingStatistics timingStats,
                                        StringBuilder toAppend) {
                    toAppend.append(timingStats.getStandardDeviation());
                }
            };
        } else if ("count".equals(configName)) {
            return new TimingStatsValueRetriever() {
                public void appendValue(String tag, String start, String stop, long windowLength,
                                        TimingStatistics timingStats,
                                        StringBuilder toAppend) {
                    toAppend.append(timingStats.getCount());
                }
            };
        } else if ("tps".equals(configName)) {
            return new TimingStatsValueRetriever() {
                public void appendValue(String tag, String start, String stop, long windowLength,
                                        TimingStatistics timingStats,
                                        StringBuilder toAppend) {
                    toAppend.append((timingStats.getCount() * 1000.0) / windowLength);
                }
            };
        } else {
            throw new IllegalArgumentException("Unknown CSV format config string: " + configName);
        }
    }

    // --- Helper interfaces ---

    protected static interface TimingStatsValueRetriever {
        public void appendValue(String tag,
                                String start,
                                String stop,
                                long windowLength,
                                TimingStatistics stats,
                                StringBuilder toAppend);
    }

    protected static interface GroupedTimingStatisticsValueRetriever {
        public void appendValue(String start,
                                String stop,
                                long windowLength,
                                GroupedTimingStatistics stats,
                                StringBuilder toAppend);
    }
}
