/* Copyright Homeaway, Inc 2005-2007. All Rights Reserved.
 * No unauthorized use of this software.
 */
package org.perf4j.helpers;

import org.perf4j.GroupedTimingStatistics;
import org.perf4j.TimingTestCase;
import org.perf4j.StopWatch;

import javax.management.MBeanAttributeInfo;
import javax.management.MBeanInfo;
import javax.management.NotificationListener;
import javax.management.Notification;
import java.util.Arrays;
import java.util.Queue;
import java.util.LinkedList;

/**
 * Tests the StatisticsExposingMBean.
 */
public class StatisticsExposingMBeanTest extends TimingTestCase {

    public void testStatisticsExposingMBean() throws Exception {
        GroupedTimingStatistics groupedTimingStats = new GroupedTimingStatistics();
        groupedTimingStats.setStartTime(System.currentTimeMillis());
        groupedTimingStats.setStopTime(System.currentTimeMillis() + 1000L);
        groupedTimingStats.addStopWatches(this.testStopWatches);

        StatisticsExposingMBean mBean = new StatisticsExposingMBean(StatisticsExposingMBean.DEFAULT_MBEAN_NAME,
                                                                    Arrays.asList("tag", "tag3"),
                                                                    null /* no notifications */);
        mBean.updateCurrentTimingStatistics(groupedTimingStats);

        MBeanInfo mBeanInfo = mBean.getMBeanInfo();
        MBeanAttributeInfo[] attributeInfos = mBeanInfo.getAttributes();
        assertEquals(mBean.getStatsValueRetrievers().size() * 2, attributeInfos.length);

        assertEquals(groupedTimingStats.getStatisticsByTag().get("tag").getMean(),
                     mBean.getAttribute("tagMean"));
        assertEquals(groupedTimingStats.getStatisticsByTag().get("tag").getStandardDeviation(),
                     mBean.getAttribute("tagStdDev"));
        assertEquals(groupedTimingStats.getStatisticsByTag().get("tag").getMax(),
                     mBean.getAttribute("tagMax"));
        assertEquals(groupedTimingStats.getStatisticsByTag().get("tag").getMin(),
                     mBean.getAttribute("tagMin"));
        assertEquals(groupedTimingStats.getStatisticsByTag().get("tag").getCount(),
                     mBean.getAttribute("tagCount"));
        assertEquals(((double) groupedTimingStats.getStatisticsByTag().get("tag").getCount()) /
                     ((double) (groupedTimingStats.getStopTime() - groupedTimingStats.getStartTime()) / 1000.0),
                     mBean.getAttribute("tagTPS"));

        //test notifications
        DummyNotificationListener notificationListener = new DummyNotificationListener();
        mBean = new StatisticsExposingMBean(StatisticsExposingMBean.DEFAULT_MBEAN_NAME,
                                            Arrays.asList("tag", "tag3"),
                                            Arrays.asList(new AcceptableRangeConfiguration("tagMean(<2000)"),
                                                          new AcceptableRangeConfiguration("unknownTagMean(<100)")));
        mBean.addNotificationListener(notificationListener, null, null);

        //no notification should be sent here, the timing stats are good
        mBean.updateCurrentTimingStatistics(groupedTimingStats);
        Thread.sleep(50); //need to sleep because notification is sent in a separate thread.
        assertNull(notificationListener.lastReceivedNotification);

        //add a stop watch that should make the mean be too high, causing a notification to be sent
        GroupedTimingStatistics badStats = groupedTimingStats.clone();
        badStats.addStopWatch(new StopWatch(groupedTimingStats.getStartTime(), 100000L, "tag", "message"));
        mBean.updateCurrentTimingStatistics(badStats);
        Thread.sleep(50);
        assertEquals(StatisticsExposingMBean.OUT_OF_RANGE_NOTIFICATION_TYPE,
                     notificationListener.lastReceivedNotification.getType());
        notificationListener.lastReceivedNotification = null;

        //now when we update the timing stats again we should NOT receive another notification, because
        //we only want a notification the first time it goes bad.
        mBean.updateCurrentTimingStatistics(badStats);
        Thread.sleep(50);
        assertNull(notificationListener.lastReceivedNotification);

        //one more time go from good to bad - we should get another notification
        mBean.updateCurrentTimingStatistics(groupedTimingStats);
        Thread.sleep(50);
        assertNull(notificationListener.lastReceivedNotification);
        mBean.updateCurrentTimingStatistics(badStats);
        Thread.sleep(50);
        assertEquals(StatisticsExposingMBean.OUT_OF_RANGE_NOTIFICATION_TYPE,
                     notificationListener.lastReceivedNotification.getType());
        notificationListener.lastReceivedNotification = null;

        //test invalid AcceptableRangeConfiguration string
        try {
            new StatisticsExposingMBean(StatisticsExposingMBean.DEFAULT_MBEAN_NAME,
                                        Arrays.asList("tag"),
                                        Arrays.asList(new AcceptableRangeConfiguration("tagNoStat(<100)")));
            fail("Should have thrown an illegal argument exception");
        } catch (IllegalArgumentException iae) { /* expected */ }
        
        //TODO - more tests - update current statistics, check for unsupported ops.
    }

    protected static class DummyNotificationListener implements NotificationListener {
        public Notification lastReceivedNotification;
        
        public void handleNotification(Notification notification, Object handback) {
            lastReceivedNotification = notification;
        }
    }
}
