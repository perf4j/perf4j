/* Copyright Homeaway, Inc 2005-2007. All Rights Reserved.
 * No unauthorized use of this software.
 */
package org.perf4j.helpers;

import org.perf4j.GroupedTimingStatistics;
import org.perf4j.TimingTestCase;

import javax.management.MBeanAttributeInfo;
import javax.management.MBeanInfo;
import java.util.Arrays;

/**
 * Tests the StatisticsExposingMBean.
 */
public class StatisticsExposingMBeanTest extends TimingTestCase {

    public void testStatisticsExposingMBean() throws Exception {
        GroupedTimingStatistics groupedTimingStats = new GroupedTimingStatistics();
        groupedTimingStats.setStartTime(System.currentTimeMillis());
        groupedTimingStats.setStopTime(System.currentTimeMillis() + 1000L);
        groupedTimingStats.addStopWatches(this.testStopWatches);

        StatisticsExposingMBean mBean = new StatisticsExposingMBean(Arrays.asList("tag", "tag3"));
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

        //TODO - more tests - update current statistics, check for unsupported ops.
    }

}
