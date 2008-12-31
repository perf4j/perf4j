/* Copyright Homeaway, Inc 2005-2007. All Rights Reserved.
 * No unauthorized use of this software.
 */
package org.perf4j.helpers;

import org.perf4j.GroupedTimingStatistics;
import org.perf4j.TimingTestCase;
import org.perf4j.StopWatch;
import org.perf4j.TimingStatistics;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Tests the GroupingStatisticsIterator.
 */
public class GroupingStatisticsIteratorTest extends TimingTestCase {
    public void testEmptyIterator() throws Exception {
        assertFalse(new GroupingStatisticsIterator(new ArrayList<StopWatch>().iterator()).hasNext());
    }

    public void testSingleStopWatch() throws Exception {
        long now = System.currentTimeMillis();
        StopWatch stopWatch = new StopWatch(now, 100L, "tag", "message");
        GroupingStatisticsIterator iter = new GroupingStatisticsIterator(Collections.singleton(stopWatch).iterator());

        assertTrue(iter.hasNext());
        GroupedTimingStatistics stats = iter.next();
        assertTrue(stats.getStartTime() <= now);
        assertTrue(stats.getStopTime() > now);
        assertEquals(1, stats.getStatisticsByTag().size());
        TimingStatistics timingStats = stats.getStatisticsByTag().get("tag");
        assertEquals(100L, timingStats.getMin());
        assertEquals(100L, timingStats.getMax());
        assertEquals(100.0, timingStats.getMean());
        assertEquals(0.0, timingStats.getStandardDeviation());
        assertEquals(1, timingStats.getCount());

        assertFalse(iter.hasNext());
    }

    public void testTwoStopWatchesDifferentTimeslices() throws Exception {
        long now = System.currentTimeMillis();
        long in30Secs = now + 30000L;

        List<StopWatch> stopWatches = new ArrayList<StopWatch>();
        stopWatches.add(new StopWatch(now, 100L, "tag", "message"));
        stopWatches.add(new StopWatch(in30Secs, 200L, "tag", "message2"));

        GroupingStatisticsIterator iter = new GroupingStatisticsIterator(stopWatches.iterator());

        //there should be 2 stats, just get them
        GroupedTimingStatistics stats1 = iter.next();
        GroupedTimingStatistics stats2 = iter.next();
        assertFalse(iter.hasNext());

        assertEquals(1, stats1.getStatisticsByTag().get("tag").getCount());
        assertEquals(1, stats2.getStatisticsByTag().get("tag").getCount());
        assertEquals(100L, stats1.getStatisticsByTag().get("tag").getMax());
        assertEquals(200L, stats2.getStatisticsByTag().get("tag").getMin());
        assertEquals(stats1.getStopTime(), stats2.getStartTime());
    }

    public void testNormalUsage() throws Exception {
        long startOfFirstSlice = System.currentTimeMillis() / 30000L * 30000L;

        List<StopWatch> stopWatches = new ArrayList<StopWatch>();
        //group 1
        stopWatches.add(new StopWatch(startOfFirstSlice, 100L, "tag", "message"));
        stopWatches.add(new StopWatch(startOfFirstSlice + 5000L, 200L, "tag", "message"));
        stopWatches.add(new StopWatch(startOfFirstSlice + 10000L, 300L, "tag", "message"));
        //group 2
        stopWatches.add(new StopWatch(startOfFirstSlice + 40000L, 100L, "tag2", "message"));
        stopWatches.add(new StopWatch(startOfFirstSlice + 45000L, 200L, "tag2", "message"));
        //group 3
        stopWatches.add(new StopWatch(startOfFirstSlice + 75000L, 500L, "tag3", "message"));

        List<GroupedTimingStatistics> groupedTimingStatistics = new ArrayList<GroupedTimingStatistics>();

        for (GroupingStatisticsIterator iter = new GroupingStatisticsIterator(stopWatches.iterator());
             iter.hasNext();) {
            groupedTimingStatistics.add(iter.next());
        }

        assertEquals(3, groupedTimingStatistics.size());
        assertEquals(200.0, groupedTimingStatistics.get(0).getStatisticsByTag().get("tag").getMean());
        assertEquals(2, groupedTimingStatistics.get(1).getStatisticsByTag().get("tag2").getCount());
        assertEquals(500L, groupedTimingStatistics.get(2).getStatisticsByTag().get("tag3").getMax());
    }
    
    public void testRemove() throws Exception {
        //remove is not supported
        try {
            new GroupingStatisticsIterator(new ArrayList<StopWatch>().iterator()).remove();
            fail();
        } catch (UnsupportedOperationException uoe) {
            //expected
        }
    }
}
