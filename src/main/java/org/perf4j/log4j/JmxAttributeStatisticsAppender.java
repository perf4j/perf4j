/* Copyright Homeaway, Inc 2005-2007. All Rights Reserved.
 * No unauthorized use of this software.
 */
package org.perf4j.log4j;

import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.spi.LoggingEvent;
import org.perf4j.GroupedTimingStatistics;
import org.perf4j.helpers.StatisticsExposingMBean;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import java.lang.management.ManagementFactory;
import java.util.Arrays;

/**
 * This appender is designed to be attached to an {@link AsyncCoalescingStatisticsAppender}. It takes the incoming
 * GroupedTimingStatistics log messages and uses this data to update the value of a JMX MBean. The attributes on this
 * MBean can then be monitored by external tools.
 *
 * @author Alex Devine
 */
public class JmxAttributeStatisticsAppender extends AppenderSkeleton {
    // --- configuration options ---
    /**
     * The object name of the MBean exposed through the JMX server.
     */
    private String mBeanName = StatisticsExposingMBean.DEFAULT_MBEAN_NAME;
    /**
     * A comma separated list of the tag names to be exposed as JMX attributes.
     */
    private String tagNamesToExpose;

    // --- state variables ---
    /**
     * This is the MBean that is registered with the MBeanServer
     */
    private StatisticsExposingMBean mBean;

    // --- options ---
    /**
     * The <b>MBeanName</b> option is used to specify the ObjectName under which the StatisticsExposingMBean in the
     * MBeanServer. If not specified, defaults to org.perf4j:type=StatisticsExposingMBean,name=Perf4J.
     *
     * @return The value of the MBeanName option
     */
    public String getMBeanName() {
        return mBeanName;
    }

    /**
     * Sets the value of the <b>MBeanName</b> option. This must be a valid JMX ObjectName.
     *
     * @param mBeanName The new value for the MBeanName option.
     */
    public void setMBeanName(String mBeanName) {
        this.mBeanName = mBeanName;
    }

    /**
     * The <b>TagNamesToExpose</b> option is a comma-separated list of the tag names whose statistics values (e.g.
     * mean, min, max, etc.) should be exposed as MBeanAttributes. See the
     * {@link org.perf4j.helpers.StatisticsExposingMBean} for more details.
     *
     * @return The value of the TagNamesToExpose expose
     */
    public String getTagNamesToExpose() {
        return tagNamesToExpose;
    }

    /**
     * Sets the value of the TagNamesToExpose option.
     *
     * @param tagNamesToExpose The new value for the TagNamesToExpose option.
     */
    public void setTagNamesToExpose(String tagNamesToExpose) {
        this.tagNamesToExpose = tagNamesToExpose;
    }

    public void activateOptions() {
        if (tagNamesToExpose == null) {
            throw new RuntimeException("You must set the TagNamesToExpose option before activating this appender");
        }

        //create the mBean and register it
        String[] tagNames = tagNamesToExpose.split(",");
        mBean = new StatisticsExposingMBean(Arrays.asList(tagNames));

        try {
            MBeanServer mBeanServer = getMBeanServer();
            mBeanServer.registerMBean(mBean, new ObjectName(mBeanName));
        } catch (Exception e) {
            throw new RuntimeException("Error registering statistics MBean: " + e.getMessage(), e);
        }
    }

    // --- appender interface methods ---

    protected void append(LoggingEvent event) {
        Object logMessage = event.getMessage();
        if (logMessage instanceof GroupedTimingStatistics && mBean != null) {
            mBean.updateCurrentTimingStatistics((GroupedTimingStatistics) logMessage);
        }
    }

    public boolean requiresLayout() {
        return false;
    }

    public void close() {
        try {
            MBeanServer mBeanServer = getMBeanServer();
            mBeanServer.unregisterMBean(new ObjectName(mBeanName));
        } catch (Exception e) {
            //fine, if we can't unresiter it's not a big deal
        }
    }

    // --- helper methods ---
    /**
     * Gets the MBeanServer that should be used to register the StatisticsExposingMBean. Defaults to the Java Platform
     * MBeanServer. Subclasses could override this to use a different server.
     *
     * @return The MBeanServer to use for registrations.
     */
    protected MBeanServer getMBeanServer() {
        return ManagementFactory.getPlatformMBeanServer();
    }
}
