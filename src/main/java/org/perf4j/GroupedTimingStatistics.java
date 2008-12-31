/* Copyright Homeaway, Inc 2005-2007. All Rights Reserved.
 * No unauthorized use of this software.
 */
package org.perf4j;

import java.io.Serializable;
import java.util.*;

/**
 * Represents a set of TimingStatistics calculated for a specific time period for a set of tags.
 * TODO - javadoc
 *
 * @author Alex Devine
 */
public class GroupedTimingStatistics implements Serializable, Cloneable {
    private SortedMap<String, TimingStatistics> statisticsByTag = new TreeMap<String, TimingStatistics>();
    private long startTime;
    private long stopTime;
    private boolean createRollupStatistics;

    // --- Constructors ---

    public GroupedTimingStatistics() {}

    public GroupedTimingStatistics(SortedMap<String, TimingStatistics> statisticsByTag,
                                   long startTime,
                                   long stopTime,
                                   boolean createRollupStatistics) {
        this.statisticsByTag = statisticsByTag;
        this.startTime = startTime;
        this.stopTime = stopTime;
        this.createRollupStatistics = createRollupStatistics;
    }

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
