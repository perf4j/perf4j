/* Copyright Homeaway, Inc 2005-2007. All Rights Reserved.
 * No unauthorized use of this software.
 */
package org.perf4j.helpers;

import org.perf4j.GroupedTimingStatistics;
import org.perf4j.TimingStatistics;

import javax.management.*;
import java.util.Collection;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class provides a wrapper around GroupedTimingStatistics data so that this performance information can be
 * exposed through JMX.
 *
 * @author Alex Devine
 */
public class StatisticsExposingMBean implements DynamicMBean {
    /**
     * Logging classes use this as the default ObjectName of this MBean when registering it with an MBeanServer.
     */
    public static final String DEFAULT_MBEAN_NAME = "org.perf4j:type=StatisticsExposingMBean,name=Perf4J";

    /**
     * This MBeanInfo exposes this MBean's management interface to the MBeanServer.
     */
    protected MBeanInfo managementInterface;
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
     * specified.
     *
     * @param tagsToExpose The names of the tags whose statistics should exposed. For each tag specified there will be
     *                     6 attributes whose getters are exposed: tagNameMean, tagNameStdDev, tagNameMin,
     *                     tagNameMax, and tagNameCount and tagNameTPS
     */
    public StatisticsExposingMBean(Collection<String> tagsToExpose) {
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

    public Object invoke(String s, Object[] objects, String[] strings) throws MBeanException, ReflectionException {
        //we don't support any operations
        return null;
    }

    public MBeanInfo getMBeanInfo() {
        return managementInterface;
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
                                                         statsValueRetriever.getValueClass().toString(),
                                                         "Returns " + statName + " for tag " + tagName,
                                                         true /* readable */,
                                                         false /* not writable */,
                                                         false /* not "is" getter */);
            }
        }

        return new MBeanInfo(getClass().getName(),
                             "Timing Statistics",
                             attributes,
                             null /* no constructors */,
                             null /* no operations */,
                             null /* no notifications */);
    }
}
