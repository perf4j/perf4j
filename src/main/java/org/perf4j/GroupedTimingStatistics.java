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
     * Default constructor allows you to set statistics later using the addStopWatch and setter methods.
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

    // --- Utility Methods ---
    /**
     * This method updates the calculated statistics when a new logged StopWatch is added.
     *
     * @param stopWatch The StopWatch being used to update the statistics.
     * @return this GroupedTimingStatistics instance
     */
    public GroupedTimingStatistics addStopWatch(StopWatch stopWatch) {
        String tag = stopWatch.getTag();

        addStopWatchToStatsByTag(tag, stopWatch);

        //create rollup statistics if desired by splitting up the tag
        if (createRollupStatistics) {
            int indexOfDot = -1;
            while ((indexOfDot = tag.indexOf('.', indexOfDot + 1)) >= 0) {
                addStopWatchToStatsByTag(tag.substring(0, indexOfDot), stopWatch);
            }
        }

        return this;
    }

    /**
     * Updates these statistics with all of the StopWatches in the specified collection.
     *
     * @param stopWatches The collection of StopWatches to add to this GroupedTimingStatistics data set.
     * @return this GroupedTimingStatistics instance
     */
    public GroupedTimingStatistics addStopWatches(Collection<StopWatch> stopWatches) {
        for (StopWatch stopWatch : stopWatches) {
            addStopWatch(stopWatch);
        }

        return this;
    }

    /**
     * The TimeZone to use when displaying start/stop time information
     */
    private static TimeZone timeZone = TimeZone.getDefault();

    /**
     * Returns the <tt>TimeZone</tt> that should be used for display of timestamp values.
     *
     * @return the timezone in which the statistics should be presented
     */
    public static TimeZone getTimeZone() {
        return timeZone;
    }

    /**
     * Sets the <tt>TimeZone</tt> which should be used to present this data.
     *
     * @param tz the timezone to use
     */
    public static void setTimeZone(TimeZone tz) {
        timeZone = tz;
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

    private void addStopWatchToStatsByTag(String tag, StopWatch stopWatch) {
        TimingStatistics stats = statisticsByTag.get(tag);
        if (stats == null) {
            statisticsByTag.put(tag, stats = new TimingStatistics());
        }
        stats.addSampleTime(stopWatch.getElapsedTime());
    }

    // --- Object Methods ---

    public String toString() {
        StringBuilder retVal = new StringBuilder();
        //output the time window
        Calendar startTimeCal = new GregorianCalendar(getTimeZone());
        startTimeCal.setTimeInMillis(startTime);
        Calendar stopTimeCal = new GregorianCalendar(getTimeZone());
        startTimeCal.setTimeInMillis(stopTime);
        retVal.append(String.format("Performance Statistics   %tT - %tT%n", startTimeCal, stopTimeCal));
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
            for (Map.Entry<String, TimingStatistics> tagAndStats : retVal.statisticsByTag.entrySet()) {
                tagAndStats.setValue(tagAndStats.getValue().clone());
            }
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

        return startTime == that.startTime &&
               stopTime == that.stopTime &&
               statisticsByTag.equals(that.statisticsByTag);
    }

    public int hashCode() {
        int result;
        result = statisticsByTag.hashCode();
        result = 31 * result + (int) (startTime ^ (startTime >>> 32));
        result = 31 * result + (int) (stopTime ^ (stopTime >>> 32));
        return result;
    }
}
