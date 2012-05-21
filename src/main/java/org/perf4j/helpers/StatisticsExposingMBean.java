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
import org.perf4j.TimingStatistics;

import javax.management.*;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class provides a wrapper around GroupedTimingStatistics data so that this performance information can be
 * exposed through JMX.
 *
 * @author Alex Devine
 * @author Xu Huisheng
 */
public class StatisticsExposingMBean extends NotificationBroadcasterSupport implements DynamicMBean {
    /**
     * Logging classes use this as the default ObjectName of this MBean when registering it with an MBeanServer.
     */
    public static final String DEFAULT_MBEAN_NAME = "org.perf4j:type=StatisticsExposingMBean,name=Perf4J";

    /**
     * The type of the Notifications sent when a statistics value is outside of the acceptable range.
     */
    public static final String OUT_OF_RANGE_NOTIFICATION_TYPE = "org.perf4j.threshold.exceeded";

    /**
     * When mbean was deployed multi-times, just throw an Exception.
     */
    public static final String COLLISION_DONOTHING = "DONOTHING";

    /**
     * When mbean was deployed multi-times, using the new one to replace the old one.
     */
    public static final String COLLISION_REPLACE = "REPLACE";

    /**
     * When mbean was deployed multi-times, ignore the new one, still using the old one.
     */
    public static final String COLLISION_IGNORE = "IGNORE";

    /**
     * The name under which this MBean is registered in the MBean server.
     */
    protected ObjectName mBeanName;

    /**
     * This MBeanInfo exposes this MBean's management interface to the MBeanServer.
     */
    protected MBeanInfo managementInterface;

    /**
     * The tags whose statistics values are being exposed.
     */
    protected Collection<String> tagsToExpose;

    /**
     * These AcceptableRangeConfigurations force a notification to be sent if a statistic is updated to a value
     * outside the allowable range. This Map maps acceptable ranges to whether or not the LAST check of the attribute
     * value was good or bad. This is used to ensure only a single notification is sent when an attribute crosses the
     * threshold to go out of range.
     */
    protected Map<AcceptableRangeConfiguration, Boolean> acceptableRanges;
    /**
     * This single thread pool is used to send notifications if any values are outside of the acceptable ranges
     * (this is necessary because the JMX spec states that the sendNotification method may be synchronous). This
     * member variable will be null if no acceptable ranges are specified.
     */
    protected ExecutorService outOfRangeNotifierThread;
    /**
     * This sequence number is required by the JMX Notification API.
     */
    protected long outOfRangeNotificationSeqNo;
    /**
     * The current underlying timing statistics whose values are exposed as MBean attributes.
     */
    protected GroupedTimingStatistics currentTimingStatistics;
    /**
     * Pattern used to parse requested attribute names into the tag name and the statistic name
     */
    protected Pattern attributeNamePattern = Pattern.compile("(.*)(Mean|StdDev|Min|Max|Count|TPS)");

    /**
     * Creates a new StatisticsExposingMBean whose management interface exposes performance attributes for the tags
     * specified, and that sends notifications if attributes are outside of the acceptable ranges.
     *
     * @param mBeanName        The name under which this MBean is registered in the MBean server
     * @param tagsToExpose     The names of the tags whose statistics should exposed. For each tag specified there will
     *                         be 6 attributes whose getters are exposed: tagNameMean, tagNameStdDev, tagNameMin,
     *                         tagNameMax, and tagNameCount and tagNameTPS
     * @param acceptableRanges These acceptable ranges are used to send notifications if any of the monitored
     *                         attributes go outside of the range.
     */
    public StatisticsExposingMBean(String mBeanName,
                                   Collection<String> tagsToExpose,
                                   Collection<AcceptableRangeConfiguration> acceptableRanges) {
        //set mBeanName
        if (mBeanName == null) {
            mBeanName = DEFAULT_MBEAN_NAME;
        }
        try {
            this.mBeanName = new ObjectName(mBeanName);
        } catch (MalformedObjectNameException mone) {
            throw new IllegalArgumentException(mone);
        }

        //set acceptableRanges
        if (acceptableRanges == null || acceptableRanges.isEmpty()) {
            this.acceptableRanges = Collections.emptyMap();
        } else {
            this.acceptableRanges = new LinkedHashMap<AcceptableRangeConfiguration, Boolean>();
            // initialize the last known value of the attribute as good
            for (AcceptableRangeConfiguration acceptableRange : acceptableRanges) {
                this.acceptableRanges.put(acceptableRange, Boolean.TRUE);
                //ensure the attributeName on the range is valid
                if (!attributeNamePattern.matcher(acceptableRange.getAttributeName()).matches()) {
                    throw new IllegalArgumentException(
                            "Acceptable range attribute name " + acceptableRange.getAttributeName()
                            + " invalid - must match pattern " + attributeNamePattern.pattern()
                    );
                }
            }
            this.outOfRangeNotifierThread = Executors.newSingleThreadExecutor();
        }

        this.tagsToExpose = new ArrayList<String>(tagsToExpose);

        this.managementInterface = createMBeanInfoFromTagNames(tagsToExpose);

        this.currentTimingStatistics = new GroupedTimingStatistics(); //just set empty so it's never null
    }

    /**
     * This method should be called to update the underlying timing statistics, which will correspondingly change the
     * values of the exposed attributes.
     *
     * @param currentTimingStatistics The TimingStatistics to set, may not be null
     */
    public synchronized void updateCurrentTimingStatistics(GroupedTimingStatistics currentTimingStatistics) {
        if (currentTimingStatistics == null) {
            throw new IllegalArgumentException("timing statistics may not be null");
        }
        this.currentTimingStatistics = currentTimingStatistics;

        sendNotificationsIfValuesNotAcceptable();
    }

    /**
     * This MBean operation method allows the caller to add a tag whose statistics should be exposed as attributes
     * at runtime.
     *
     * @param tagName The name of the tag whose statistics should be exposed.
     */
    public void exposeTag(String tagName) {
        this.tagsToExpose.add(tagName);
        this.managementInterface = createMBeanInfoFromTagNames(this.tagsToExpose);
    }

    /**
     * This MBean operation method allows the caller to remove, at runtime, a tag whose statistics are exposed.
     *
     * @param tagName The name of the tag whose statistics should be removed as attributes from this MBean.
     * @return Whether or not the specified tag was previously exposed on this MBean.
     */
    public boolean removeTag(String tagName) {
        boolean retVal = this.tagsToExpose.remove(tagName);
        this.managementInterface = createMBeanInfoFromTagNames(this.tagsToExpose);
        return retVal;
    }

    public synchronized Object getAttribute(String attribute)
            throws AttributeNotFoundException, MBeanException, ReflectionException {
        Matcher matcher = attributeNamePattern.matcher(attribute);
        if (matcher.matches()) {
            String tagName = matcher.group(1);
            String statisticName = matcher.group(2);

            TimingStatistics timingStats = currentTimingStatistics.getStatisticsByTag().get(tagName);
            long windowLength = currentTimingStatistics.getStopTime() - currentTimingStatistics.getStartTime();

            return getStatsValueRetrievers().get(statisticName).getStatsValue(timingStats, windowLength);
        } else {
            throw new AttributeNotFoundException("No attribute named " + attribute);
        }

    }

    public void setAttribute(Attribute attribute)
            throws AttributeNotFoundException, InvalidAttributeValueException, MBeanException, ReflectionException {
        throw new AttributeNotFoundException("Statistics attributes are not writable");
    }

    public synchronized AttributeList getAttributes(String[] attributeNames) {
        AttributeList retVal = new AttributeList();
        for (String attributeName : attributeNames) {
            try {
                retVal.add(new Attribute(attributeName, getAttribute(attributeName)));
            } catch (Exception e) {
                //ignore - the absence of the attribute in the return list indicates there was an error
            }
        }
        return retVal;
    }

    public AttributeList setAttributes(AttributeList attributes) {
        //we don't support setting, so just return an empty list
        return new AttributeList();
    }

    public Object invoke(String actionName, Object[] params, String[] signature)
            throws MBeanException, ReflectionException {
        if ("exposeTag".equals(actionName)) {
            exposeTag(params[0].toString());
            return null;
        } else if ("removeTag".equals(actionName)) {
            return removeTag(params[0].toString());
        } else {
            throw new UnsupportedOperationException("Unsupported operation: " + actionName);
        }
    }

    public MBeanInfo getMBeanInfo() {
        return managementInterface;
    }

    public MBeanNotificationInfo[] getNotificationInfo() {
        return managementInterface.getNotifications();
    }

    /**
     * Overridable helper method gets the Map of statistic name to StatsValueRetriever.
     *
     * @return The StatsValueRetriever Map.
     */
    protected Map<String, StatsValueRetriever> getStatsValueRetrievers() {
        return StatsValueRetriever.DEFAULT_RETRIEVERS;
    }

    /**
     * Helper method creates an MBeanInfo object that contains 6 read only attributes for each tag name, each
     * attribute representing a different statistic.
     *
     * @param tagNames The name of the tags whose statistics should be exposed as MBeanAttributes.
     * @return The MBeanInfo that represents the management interface for this MBean.
     */
    protected MBeanInfo createMBeanInfoFromTagNames(Collection<String> tagNames) {
        MBeanAttributeInfo[] attributes = new MBeanAttributeInfo[tagNames.size() * getStatsValueRetrievers().size()];

        int i = 0;
        for (String tagName : tagNames) {
            for (Map.Entry<String, StatsValueRetriever> statNameAndValueRetriever :
                    getStatsValueRetrievers().entrySet()) {
                String statName = statNameAndValueRetriever.getKey();
                StatsValueRetriever statsValueRetriever = statNameAndValueRetriever.getValue();

                attributes[i++] = new MBeanAttributeInfo(tagName + statName,
                                                         statsValueRetriever.getValueClass().getName(),
                                                         "Returns " + statName + " for tag " + tagName,
                                                         true /* readable */,
                                                         false /* not writable */,
                                                         false /* not "is" getter */);
            }
        }

        MBeanOperationInfo[] operations = new MBeanOperationInfo[2]; //exposeTag and removeTag
        operations[0] = new MBeanOperationInfo("exposeTag",
                                               "Allows the caller to add a monitored tag at runtime",
                                               new MBeanParameterInfo[]{
                                                       new MBeanParameterInfo("tagName",
                                                                              String.class.getName(),
                                                                              "The name of the tag to expose")
                                               },
                                               "void",
                                               MBeanOperationInfo.ACTION);
        operations[1] = new MBeanOperationInfo("removeTag",
                                               "Allows the caller to remove a monitored tag at runtime",
                                               new MBeanParameterInfo[]{
                                                       new MBeanParameterInfo("tagName",
                                                                              String.class.getName(),
                                                                              "The name of the tag to remove")
                                               },
                                               "boolean",
                                               MBeanOperationInfo.ACTION);

        MBeanNotificationInfo[] notificationInfos;
        if (acceptableRanges.isEmpty()) {
            //then we don't send any out-of-range notifications
            notificationInfos = new MBeanNotificationInfo[0];
        } else {
            notificationInfos = new MBeanNotificationInfo[]{
                    new MBeanNotificationInfo(
                            new String[]{OUT_OF_RANGE_NOTIFICATION_TYPE},
                            Notification.class.getName(),
                            "Notification sent if any statistics move outside of the specified acceptable ranges"
                    )
            };
        }

        return new MBeanInfo(getClass().getName(),
                             "Timing Statistics",
                             attributes,
                             null /* no constructors */,
                             operations,
                             notificationInfos);
    }

    /**
     * This helper method sends notifications if any of the acceptable ranges detects an attribute value that is
     * outside of the specified range. This method should only be called when the lock on this object's monitor is held.
     */
    protected void sendNotificationsIfValuesNotAcceptable() {
        //send notifications if any values are outside the acceptable range, but only if the LAST check was good
        for (Map.Entry<AcceptableRangeConfiguration, Boolean> acceptableRangeAndWasGood : acceptableRanges.entrySet()) {
            AcceptableRangeConfiguration acceptableRange = acceptableRangeAndWasGood.getKey();
            boolean lastCheckWasGood = acceptableRangeAndWasGood.getValue();

            double attributeValue;
            try {
                attributeValue = ((Number) getAttribute(acceptableRange.getAttributeName())).doubleValue();
            } catch (Exception e) {
                //shouldn't happen
                continue;
            }

            boolean isValueInRange = acceptableRange.isInRange(attributeValue);

            //update the lastCheckGood value and send the notification
            acceptableRangeAndWasGood.setValue(isValueInRange);

            if (lastCheckWasGood && !isValueInRange) {
                sendOutOfRangeNotification(attributeValue, acceptableRange);
            }
        }
    }

    /**
     * Helper method is used to send the JMX notification because the attribute value doesn't fall within the
     * acceptable range. This method should only be called when the lock on this object's monitor is held.
     *
     * @param attributeValue  The attribute value that falls outside the threshold
     * @param acceptableRange The AcceptableRangeConfiguration used to constrain the acceptable value
     */
    protected void sendOutOfRangeNotification(final double attributeValue,
                                              final AcceptableRangeConfiguration acceptableRange) {
        outOfRangeNotifierThread.execute(new Runnable() {
            public void run() {
                String errorMessage = "Attribute value " + attributeValue + " not in range " + acceptableRange;
                sendNotification(new Notification(OUT_OF_RANGE_NOTIFICATION_TYPE,
                                                  mBeanName,
                                                  ++outOfRangeNotificationSeqNo,
                                                  System.currentTimeMillis(),
                                                  errorMessage));
            }
        });
    }
}
