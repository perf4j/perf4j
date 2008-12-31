/* Copyright Homeaway, Inc 2005-2007. All Rights Reserved.
 * No unauthorized use of this software.
 */
package org.perf4j.helpers;

import org.perf4j.GroupedTimingStatistics;
import org.perf4j.StopWatch;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * This Iterator wraps a {@link StopWatchLogIterator} to return a single {@link GroupedTimingStatistics} object for
 * each time slice detected. Thus, this iterator is a "gearing" iterator - if there are on average 100 StopWatches
 * logged during each time slice, the underlying StopWatchLogIterator will return 100 StopWatches for each single
 * GroupedTimingStatistics object returned by this Iterator.
 * <p/>
 * Note that it's assumed that the StopWatch Iterator is ordered according to start time. If this is not true, then
 * this class will create GroupedTimingStatistics that may reflect StopWatch data from a previous time slice.
 *
 * @author Alex Devine
 */
public class GroupingStatisticsIterator implements Iterator<GroupedTimingStatistics> {
    /**
     * The underlying StopWatch iterator
     */
    private Iterator<StopWatch> stopWatchIterator;
    /**
     * The length of each time slice, in milliseconds.
     */
    private long timeSlice;
    /**
     * Whether or not entries for "rollup" tags should be created in each GroupedTimingStatistics returned.
     */
    private boolean createRollupStatistics;

    /**
     * This hasNext is really a tri-state var - null indicates I don't know if there's a next one or not.
     */
    private Boolean hasNext = null;
    /**
     * The next GroupedTimingStatistics to be returned.
     */
    private GroupedTimingStatistics nextGroupedTimingStatistics = null;
    /**
     * The stopWatches to be part of the next GroupedTimingStatistics.
     */
    private List<StopWatch> stopWatchesInSlice = new ArrayList<StopWatch>();
    /**
     * The end time, in milliseconds since the epoch, of the next time slice.
     */
    private long nextTimeSliceEndTime = 0L;

    /**
     * Creates a GroupingStatisticsIterator that groups StopWatch instances pulled from the specified
     * stopWatchIterator into GroupedTimingStatistics. A timeslice of 30 seconds is used and rollup statistics are not
     * created.
     *
     * @param stopWatchIterator The StopWatch Iterator that provides the StopWatch instances.
     */
    public GroupingStatisticsIterator(Iterator<StopWatch> stopWatchIterator) {
        this(stopWatchIterator, 30000L, false);
    }

    /**
     * Creates a GroupingStatisticsIterator that groups StopWatch instances pulled from the specified
     * stopWatchIterator into GroupedTimingStatistics.
     *
     * @param stopWatchIterator      The StopWatch Iterator that provides the StopWatch instances.
     * @param timeSlice              The length of each time slice, in milliseconds.
     * @param createRollupStatistics Whether or not entries for "rollup" tags should be created
     */
    public GroupingStatisticsIterator(Iterator<StopWatch> stopWatchIterator,
                                      long timeSlice,
                                      boolean createRollupStatistics) {
        this.stopWatchIterator = stopWatchIterator;
        this.timeSlice = timeSlice;
        this.createRollupStatistics = createRollupStatistics;
    }

    public boolean hasNext() {
        //if I don't know the state of next, pull the next statistics to determine the state of next
        if (hasNext == null) {
            nextGroupedTimingStatistics = getNext();
            hasNext = (nextGroupedTimingStatistics != null);
        }
        return hasNext;
    }

    public GroupedTimingStatistics next() {
        //if I already determined I don't have a next, throw an exception
        if (Boolean.FALSE.equals(hasNext)) {
            throw new NoSuchElementException();
        }

        //if I don't know what to return yet, find out - note this only happens if I call next() before a call
        //to hasNext().
        if (nextGroupedTimingStatistics == null) {
            nextGroupedTimingStatistics = getNext();

            //if there's still nothing I'm done
            if (nextGroupedTimingStatistics == null) {
                hasNext = false;
                throw new NoSuchElementException();
            }
        }

        //before I return, clear the state of the variables used to determine the next value.
        GroupedTimingStatistics retVal = nextGroupedTimingStatistics;
        hasNext = null;
        nextGroupedTimingStatistics = null;
        return retVal;
    }

    /**
     * Remove is not supported.
     *
     * @exception UnsupportedOperationException Always thrown.
     */
    public void remove() {
        throw new UnsupportedOperationException();
    }

    /**
     * Helper method runs over the StopWatch Iterator to group the StopWatches into GroupedTimingStatistics.
     *
     * @return The next GroupedTimingStatistics from the underlying StopWatch Iterator, or null if there are no
     *         StopWatch instances left.
     */
    private GroupedTimingStatistics getNext() {
        while (stopWatchIterator.hasNext()) {
            StopWatch stopWatch = stopWatchIterator.next();

            //the first time we pull a stop watch we need to set the first end time
            if (nextTimeSliceEndTime == 0L) {
                nextTimeSliceEndTime = ((stopWatch.getStartTime() / timeSlice) * timeSlice) + timeSlice;
            }

            if (stopWatch.getStartTime() >= nextTimeSliceEndTime) {
                //then we're over a new time boundary, so create the GroupedTimingStatistics and return it
                GroupedTimingStatistics retVal = new GroupedTimingStatistics(stopWatchesInSlice,
                                                                             nextTimeSliceEndTime - timeSlice,
                                                                             nextTimeSliceEndTime,
                                                                             createRollupStatistics);

                //set the state for the next slice
                stopWatchesInSlice.clear();
                stopWatchesInSlice.add(stopWatch);
                nextTimeSliceEndTime = ((stopWatch.getStartTime() / timeSlice) * timeSlice) + timeSlice;

                return retVal;
            } else {
                stopWatchesInSlice.add(stopWatch);
            }
        }

        //if here then there are no more stopwatches left, so clean up the last batch
        if (!stopWatchesInSlice.isEmpty()) {
            GroupedTimingStatistics retVal = new GroupedTimingStatistics(stopWatchesInSlice,
                                                                         nextTimeSliceEndTime - timeSlice,
                                                                         nextTimeSliceEndTime,
                                                                         createRollupStatistics);

            //clear stopWatchesInSlice so we know to return null the next call to this method.
            stopWatchesInSlice.clear();

            return retVal;
        } else {
            //The StopWatch iterator is done and we already printed the last GroupedTimingStatistics batch
            return null;
        }
    }
}
