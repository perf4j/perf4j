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

import java.io.Serializable;
import java.util.*;

/**
 * Represents a set of TimingStatistics calculated for a specific time period for a set of tags.
 *
 * @author Alex Devine
 */
public class GroupedTimingStatistics implements Serializable, Cloneable {
    private SortedMap<String, TimingStatistics> statisticsByTag = new TreeMap<String, TimingStatistics>();
    private long startTime;
    private long stopTime;
    private boolean createRollupStatistics;

    // --- Constructors ---

    /**
     * Default constructor allows you to set statistics later using the setter methods.
     */
    public GroupedTimingStatistics() {}

    /**
     * Creates a GroupedTimingStatistics instance for a set of tags for a specified time span.
     *
     * @param statisticsByTag        This Map maps String tag times to the aggregated TimingStatistics for that tag.
     * @param startTime              The start time (as reported by System.currentTimeMillis()) of the time span
     *                               for which the statistics apply.
     * @param stopTime               The end time of the time span for which the statistics apply.
     * @param createRollupStatistics Whether or not the statisticsByTag contains "rollup statistics". Rollup statistics
     *                               allow users to time different execution paths of the same code block. For example,
     *                               when timing a code block, one may which to log execution time with a
     *                               "codeBlock.success" tag when execution completes normally and a "codeBlock.failure"
     *                               tag when an exception is thrown. If rollup statistics are used, then in addition
     *                               to the codeBlock.success and codeBlock.failure tags, a codeBlock tag is created
     *                               that represents StopWatch logs from EITHER the success or failure tags.
     */
    public GroupedTimingStatistics(SortedMap<String, TimingStatistics> statisticsByTag,
                                   long startTime,
                                   long stopTime,
                                   boolean createRollupStatistics) {
        this.statisticsByTag = statisticsByTag;
        this.startTime = startTime;
        this.stopTime = stopTime;
        this.createRollupStatistics = createRollupStatistics;
    }

    /**
     * Creates a GroupedTimingStatistics by separating a collection of StopWatch instances by tag and calculating
     * statistics for each tag.
     *
     * @param timeRecords            The collection of logged StopWatch instances
     * @param startTime              The start time (as reported by System.currentTimeMillis()) of the time span
     *                               for which the statistics apply.
     * @param stopTime               The end time of the time span for which the statistics apply.
     * @param createRollupStatistics Whether or not the statisticsByTag contains "rollup statistics". Rollup statistics
     *                               allow users to time different execution paths of the same code block. For example,
     *                               when timing a code block, one may which to log execution time with a
     *                               "codeBlock.success" tag when execution completes normally and a "codeBlock.failure"
     *                               tag when an exception is thrown. If rollup statistics are used, then in addition
     *                               to the codeBlock.success and codeBlock.failure tags, a codeBlock tag is created
     *                               that represents StopWatch logs from EITHER the success or failure tags.
     */
    public GroupedTimingStatistics(Collection<StopWatch> timeRecords,
                                   long startTime,
                                   long stopTime,
                                   boolean createRollupStatistics) {
        this.startTime = startTime;
        this.stopTime = stopTime;
        this.createRollupStatistics = createRollupStatistics;

        //segregate the time records by tag
        Map<String, Collection<StopWatch>> recordsByTag = new HashMap<String, Collection<StopWatch>>();
        for (StopWatch timeRecord : timeRecords) {
            String tag = timeRecord.getTag();
            addTimeRecordToMapByTag(tag, timeRecord, recordsByTag);

            //create rollup statistics if desired by splitting up the tag
            if (createRollupStatistics) {
                int indexOfDot = -1;
                while ((indexOfDot = tag.indexOf('.', indexOfDot + 1)) >= 0) {
                    addTimeRecordToMapByTag(tag.substring(0, indexOfDot), timeRecord, recordsByTag);
                }
            }
        }

        //create statistics
        for (Map.Entry<String, Collection<StopWatch>> tagWithRecords : recordsByTag.entrySet()) {
            statisticsByTag.put(tagWithRecords.getKey(), new TimingStatistics(tagWithRecords.getValue()));
        }
    }

    // --- Bean Properties ---

    public SortedMap<String, TimingStatistics> getStatisticsByTag() {
        return statisticsByTag;
    }

    public void setStatisticsByTag(SortedMap<String, TimingStatistics> statisticsByTag) {
        this.statisticsByTag = statisticsByTag;
    }

    public long getStartTime() {
        return startTime;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public long getStopTime() {
        return stopTime;
    }

    public void setStopTime(long stopTime) {
        this.stopTime = stopTime;
    }

    public boolean isCreateRollupStatistics() {
        return createRollupStatistics;
    }

    public void setCreateRollupStatistics(boolean createRollupStatistics) {
        this.createRollupStatistics = createRollupStatistics;
    }

    // --- Helper Methods ---

    private void addTimeRecordToMapByTag(String tag,
                                         StopWatch timeRecord,
                                         Map<String, Collection<StopWatch>> recordsByTag) {
        Collection<StopWatch> recordsForTag = recordsByTag.get(tag);
        if (recordsForTag == null) {
            recordsByTag.put(tag, recordsForTag = new ArrayList<StopWatch>());
        }
        recordsForTag.add(timeRecord);
    }

    // --- Object Methods ---

    public String toString() {
        StringBuilder retVal = new StringBuilder();
        String newLine = System.getProperty("line.separator");
        //output the time window
        retVal.append(String.format("Performance Statistics   %tT - %tT%n", startTime, stopTime));
        //output the header
        retVal.append(String.format("%-48s%12s%12s%12s%12s%12s%n",
                                    "Tag", "Avg(ms)", "Min", "Max", "Std Dev", "Count"));
        //output each statistics
        for (Map.Entry<String, TimingStatistics> tagWithTimingStatistics : statisticsByTag.entrySet()) {
            String tag = tagWithTimingStatistics.getKey();
            TimingStatistics timingStatistics = tagWithTimingStatistics.getValue();
            retVal.append(String.format("%-48s%12.1f%12d%12d%12.1f%12d%n",
                                        tag,
                                        timingStatistics.getMean(),
                                        timingStatistics.getMin(),
                                        timingStatistics.getMax(),
                                        timingStatistics.getStandardDeviation(),
                                        timingStatistics.getCount()));
        }

        return retVal.toString();
    }

    public GroupedTimingStatistics clone() {
        try {
            GroupedTimingStatistics retVal = (GroupedTimingStatistics) super.clone();
            retVal.statisticsByTag = new TreeMap<String, TimingStatistics>(retVal.statisticsByTag);
            return retVal;
        } catch (CloneNotSupportedException cnse) {
            throw new Error("Unexpected CloneNotSupportedException");
        }
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof GroupedTimingStatistics)) {
            return false;
        }

        GroupedTimingStatistics that = (GroupedTimingStatistics) o;

        if (startTime != that.startTime) {
            return false;
        }
        if (stopTime != that.stopTime) {
            return false;
        }
        if (!statisticsByTag.equals(that.statisticsByTag)) {
            return false;
        }

        return true;
    }

    public int hashCode() {
        int result;
        result = statisticsByTag.hashCode();
        result = 31 * result + (int) (startTime ^ (startTime >>> 32));
        result = 31 * result + (int) (stopTime ^ (stopTime >>> 32));
        return result;
    }
}
