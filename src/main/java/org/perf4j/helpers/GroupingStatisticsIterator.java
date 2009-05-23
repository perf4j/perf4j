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
import org.perf4j.StopWatch;

import java.util.Iterator;
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
public class
        GroupingStatisticsIterator implements Iterator<GroupedTimingStatistics> {
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
     * Keeps track of the CURRENT GroupedTimingStatistics while we iterate over the underlying StopWatches
     */
    private GroupedTimingStatistics currentGroupedTimingStatistics = new GroupedTimingStatistics();
    /**
     * The end time, in milliseconds since the epoch, of the next time slice.
     */
    private long nextTimeSliceEndTime = 0L;

    /**
     * Creates a GroupingStatisticsIterator that groups StopWatch instances pulled from the specified
     * stopWatchIterator into GroupedTimingStatistics. A timeslice of 30 seconds is used and rollup statistics are not
     * created.
     *
     * @param stopWatchIterator The StopWatch Iterator that provides the StopWatch instances. If stopWatchIterator
     *                          returns a null value, will check to see if a timeslice is over and return
     *                          GroupedTimingStatistics if necessary.
     */
    public GroupingStatisticsIterator(Iterator<StopWatch> stopWatchIterator) {
        this(stopWatchIterator, 30000L, false);
    }

    /**
     * Creates a GroupingStatisticsIterator that groups StopWatch instances pulled from the specified
     * stopWatchIterator into GroupedTimingStatistics.
     *
     * @param stopWatchIterator      The StopWatch Iterator that provides the StopWatch instances. If stopWatchIterator
     * 								 returns a null value, will check to see if a timeslice is over and return
     * 								 GroupedTimingStatistics if necessary.
     * @param timeSlice              The length of each time slice, in milliseconds.
     * @param createRollupStatistics Whether or not entries for "rollup" tags should be created
     */
    public GroupingStatisticsIterator(Iterator<StopWatch> stopWatchIterator,
                                      long timeSlice,
                                      boolean createRollupStatistics) {
        this.stopWatchIterator = stopWatchIterator;
        this.timeSlice = timeSlice;
        this.createRollupStatistics = createRollupStatistics;
        this.currentGroupedTimingStatistics.setCreateRollupStatistics(createRollupStatistics);
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
     * @throws UnsupportedOperationException Always thrown.
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
            
            // if stopwatch is null, then the timeslice might be over (use current time)
            long startTime = stopWatch == null ? System.currentTimeMillis() : stopWatch.getStartTime();
            
            //the first time we pull a stop watch we need to set the first end time
            if (nextTimeSliceEndTime == 0L) {
                nextTimeSliceEndTime = ((startTime / timeSlice) * timeSlice) + timeSlice;
            }

            if (startTime >= nextTimeSliceEndTime) {
                //then we're over a new time boundary, so update the current timing statistics and return it.
                currentGroupedTimingStatistics.setStartTime(nextTimeSliceEndTime - timeSlice);
                currentGroupedTimingStatistics.setStopTime(nextTimeSliceEndTime);
                GroupedTimingStatistics retVal = currentGroupedTimingStatistics;

                //set the state for the next slice
                currentGroupedTimingStatistics = new GroupedTimingStatistics();
                currentGroupedTimingStatistics.setCreateRollupStatistics(createRollupStatistics);
                if (stopWatch != null) {
                	// only add if we got a new stopwatch, not if timeslice just expired
                	currentGroupedTimingStatistics.addStopWatch(stopWatch);
                }                
                nextTimeSliceEndTime = ((startTime / timeSlice) * timeSlice) + timeSlice;
                return retVal;
            } else if (stopWatch != null) {
                currentGroupedTimingStatistics.addStopWatch(stopWatch);
            }
        }

        //if here then there are no more stopwatches left, so clean up the last batch
        if (!currentGroupedTimingStatistics.getStatisticsByTag().isEmpty()) {
            currentGroupedTimingStatistics.setStartTime(nextTimeSliceEndTime - timeSlice);
            currentGroupedTimingStatistics.setStopTime(nextTimeSliceEndTime);
            GroupedTimingStatistics retVal = currentGroupedTimingStatistics;

            //create an empty GroupedTimingStatistics so we know to return null in the next call to this method.
            currentGroupedTimingStatistics = new GroupedTimingStatistics();
            currentGroupedTimingStatistics.setCreateRollupStatistics(createRollupStatistics);

            return retVal;
        } else {
            //The StopWatch iterator is done and we already printed the last GroupedTimingStatistics batch
            return null;
        }
    }
}
