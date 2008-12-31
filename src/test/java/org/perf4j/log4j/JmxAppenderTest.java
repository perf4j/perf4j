/* Copyright Homeaway, Inc 2005-2007. All Rights Reserved.
 * No unauthorized use of this software.
 */
package org.perf4j.log4j;

import junit.framework.TestCase;
import org.apache.log4j.Logger;
import org.apache.log4j.xml.DOMConfigurator;
import org.perf4j.StopWatch;
import org.perf4j.helpers.StatisticsExposingMBean;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import java.lang.management.ManagementFactory;

/**
 * Tests the JmxAttributeStatisticsAppender
 */
public class JmxAppenderTest extends TestCase {
    public void testJmxAppender() throws Exception {
        DOMConfigurator.configure(getClass().getResource("log4jWJmx.xml"));

        //log a bunch of messages
        Logger logger = Logger.getLogger(StopWatch.DEFAULT_LOGGER_NAME);
        for (int i = 0; i < 20; i++) {
            long time = (i % 2) == 0 ? 100L : 200L;
            logger.info(new StopWatch(System.currentTimeMillis(), time, "tag" + (i % 2), "logging"));
            Thread.sleep(110);
        }

        //check that the mbean attributes are accessible
        MBeanServer server = ManagementFactory.getPlatformMBeanServer();
        ObjectName statisticsMBeanName = new ObjectName(StatisticsExposingMBean.DEFAULT_MBEAN_NAME);

        assertTrue(((Integer) server.getAttribute(statisticsMBeanName, "tag0Count")) > 0);
        assertEquals(0.0, server.getAttribute(statisticsMBeanName, "tag0StdDev"));
        assertEquals(100.0, server.getAttribute(statisticsMBeanName, "tag0Mean"));
        assertEquals(100L, server.getAttribute(statisticsMBeanName, "tag0Min"));
        assertEquals(100L, server.getAttribute(statisticsMBeanName, "tag0Max"));
        assertTrue(((Double) server.getAttribute(statisticsMBeanName, "tag0TPS")) > 1);

        assertTrue(((Integer) server.getAttribute(statisticsMBeanName, "tag1Count")) > 0);
        assertEquals(0.0, server.getAttribute(statisticsMBeanName, "tag1StdDev"));
        assertEquals(200.0, server.getAttribute(statisticsMBeanName, "tag1Mean"));
        assertEquals(200L, server.getAttribute(statisticsMBeanName, "tag1Min"));
        assertEquals(200L, server.getAttribute(statisticsMBeanName, "tag1Max"));
        assertTrue(((Double) server.getAttribute(statisticsMBeanName, "tag1TPS")) > 1);
    }
}
