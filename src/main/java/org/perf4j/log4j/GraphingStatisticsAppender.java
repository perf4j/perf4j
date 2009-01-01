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
package org.perf4j.log4j;

import org.apache.log4j.Appender;
import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.Logger;
import org.apache.log4j.helpers.AppenderAttachableImpl;
import org.apache.log4j.spi.AppenderAttachable;
import org.apache.log4j.spi.LoggingEvent;
import org.perf4j.GroupedTimingStatistics;
import org.perf4j.StopWatch;
import org.perf4j.chart.GoogleChartGenerator;
import org.perf4j.chart.StatisticsChartGenerator;
import org.perf4j.helpers.StatsValueRetriever;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * This appender is designed to be attached to an {@link AsyncCoalescingStatisticsAppender}. It takes the incoming
 * GroupedTimingStatistics log messages and uses this data to update a graphical view of the logged statistics. If
 * ANOTHER appender is then attached to this appender then the graph URLs will be written to the appender on a scheduled
 * basis. Alternatively, the graph can be viewed by setting up a
 * {@link org.perf4j.log4j.servlet.GraphingServlet} to expose the graph images.
 *
 * @author Alex Devine
 */
public class GraphingStatisticsAppender extends AppenderSkeleton implements AppenderAttachable {
    /**
     * This class keeps track of all appenders of this type that have been created. This allows static access to
     * the appenders from the org.perf4j.log4j.servlet.GraphingServlet class.
     */
    protected final static Map<String, GraphingStatisticsAppender> APPENDERS_BY_NAME =
            Collections.synchronizedMap(new LinkedHashMap<String, GraphingStatisticsAppender>());

    // --- configuration options ---
    /**
     * The type of data to display on the graph. Defaults to "Mean" to display mean values. Acceptable values are any
     * constant name from the {@link org.perf4j.helpers.StatsValueRetriever} class, such as Mean, Min, Max, Count,
     * StdDev or TPS.
     */
    private String graphType = StatsValueRetriever.MEAN_VALUE_RETRIEVER.getValueName();
    /**
     * A comma-separated list of the tag names that should be graphed. If not set then a separate series will be
     * displayed on the graph for each tag name logged.
     */
    private String tagNamesToGraph = null;
    /**
     * Gets the number of data points that will be written on each graph before the graph URL is written to any
     * attached appenders. Thus, this option is only relevant if there are attached appenders.
     * Defaults to <tt>StatisticsChartGenerator.DEFAULT_MAX_DATA_POINTS</tt>.
     */
    private int dataPointsPerGraph = StatisticsChartGenerator.DEFAULT_MAX_DATA_POINTS;

    // --- contained objects/state variables ---
    /**
     * The chart genertor, initialized in the <tt>activateOptions</tt> method, that stores the data for the chart.
     */
    protected StatisticsChartGenerator chartGenerator;
    /**
     * Keeps track of the number of logged GroupedTimingStatistics, which is used to determine when a graph should
     * be written to any attached appenders.
     */
    protected AtomicLong numLoggedStatistics = new AtomicLong();
    /**
     * Any downstream appenders are contained in this AppenderAttachableImpl
     */
    protected final AppenderAttachableImpl downstreamAppenders = new AppenderAttachableImpl();

    // --- options ---

    /**
     * The <b>GraphType</b> option is used to specify the data that should be displayed on the graph. Acceptable
     * values are Mean, Min, Max, Count, StdDev and TPS (for transactions per second). Defaults to Mean if not
     * explicitly set.
     *
     * @return The value of the GraphType option
     */
    public String getGraphType() {
        return graphType;
    }

    /**
     * Sets the value of the <b>GraphType</b> option. This must be a valid type, one of
     * Mean, Min, Max, Count, StdDev or TPS (for transactions per second).
     *
     * @param graphType The new value for the GraphType option.
     */
    public void setGraphType(String graphType) {
        this.graphType = graphType;
    }

    /**
     * The <b>TagNamesToGraph</b> option is used to specify which tags should be logged as a data series on the
     * graph. If not specified ALL tags will be drawn on the graph, one series for each tag.
     *
     * @return The value of the TagNamesToGraph option
     */
    public String getTagNamesToGraph() {
        return tagNamesToGraph;
    }

    /**
     * Sets the value of the <b>TagNamesToGraph</b> option.
     *
     * @param tagNamesToGraph The new value for the TagNamesToGraph option.
     */
    public void setTagNamesToGraph(String tagNamesToGraph) {
        this.tagNamesToGraph = tagNamesToGraph;
    }

    /**
     * The <b>DataPointsPerGraph</b> option is used to specify how much data should be displayed on each graph before
     * it is written to any attached appenders. Defaults to <tt>StatisticsChartGenerator.DEFAULT_MAX_DATA_POINTS</tt>.
     *
     * @return The value of the DataPointsPerGraph option
     */
    public int getDataPointsPerGraph() {
        return dataPointsPerGraph;
    }

    /**
     * Sets the value of the <b>DataPointsPerGraph</b> option.
     *
     * @param dataPointsPerGraph The new value for the DataPointsPerGraph option.
     */
    public void setDataPointsPerGraph(int dataPointsPerGraph) {
        if (dataPointsPerGraph <= 0) {
            throw new IllegalArgumentException("The DataPointsPerGraph option must be positive");
        }
        this.dataPointsPerGraph = dataPointsPerGraph;
    }

    public void activateOptions() {
        chartGenerator = createChartGenerator();

        //update the static APPENDERS_BY_NAME object
        if (getName() != null) {
            APPENDERS_BY_NAME.put(getName(), this);
        }
    }

    /**
     * Helper method creates a new StatisticsChartGenerator based on the options set on this appender. By default
     * a GoogleChartGenerator is created, though subclasses may override this method to create a different type of
     * chart generator.
     *
     * @return A newly created StatisticsChartGenerator.
     */
    protected StatisticsChartGenerator createChartGenerator() {
        StatsValueRetriever statsValueRetriever = StatsValueRetriever.DEFAULT_RETRIEVERS.get(getGraphType());
        if (statsValueRetriever == null) {
            throw new RuntimeException("Unknown GraphType: " + getGraphType() +
                                       ". See the StatsValueRetriever class for the list of acceptable types.");
        }

        //create the chart generator and set the enabled tags
        GoogleChartGenerator retVal = new GoogleChartGenerator(statsValueRetriever);
        if (getTagNamesToGraph() != null) {
            Set<String> enabledTags = new HashSet<String>(Arrays.asList(getTagNamesToGraph().split(",")));
            retVal.setEnabledTags(enabledTags);
        }

        return retVal;
    }

    // --- exposed objects ---

    /**
     * Gets the contained StatisticsChartGenerator that is used to generate the graphs.
     *
     * @return The StatisticsChartGenerator used by this appender.
     */
    public StatisticsChartGenerator getChartGenerator() {
        return chartGenerator;
    }

    /**
     * This static method returns any created GraphingStatisticsAppender by its name.
     *
     * @param appenderName the name of the GraphingStatisticsAppender to return
     * @return the specified GraphingStatisticsAppender, or null if not found
     */
    public static GraphingStatisticsAppender getAppenderByName(String appenderName) {
        return APPENDERS_BY_NAME.get(appenderName);
    }

    /**
     * This static method returns an unmodifiable collection of all GraphingStatisticsAppenders that have been created.
     *
     * @return The collection of GraphingStatisticsAppenders created in this VM.
     */
    public static Collection<GraphingStatisticsAppender> getAllGraphingStatisticsAppenders() {
        return Collections.unmodifiableCollection(APPENDERS_BY_NAME.values());
    }

    // --- appender attachable methods ---

    public void addAppender(Appender appender) {
        synchronized (downstreamAppenders) {
            downstreamAppenders.addAppender(appender);
        }
    }

    public Enumeration getAllAppenders() {
        synchronized (downstreamAppenders) {
            return downstreamAppenders.getAllAppenders();
        }
    }

    public Appender getAppender(String name) {
        synchronized (downstreamAppenders) {
            return downstreamAppenders.getAppender(name);
        }
    }

    public boolean isAttached(Appender appender) {
        synchronized (downstreamAppenders) {
            return downstreamAppenders.isAttached(appender);
        }
    }

    public void removeAllAppenders() {
        synchronized (downstreamAppenders) {
            downstreamAppenders.removeAllAppenders();
        }
    }

    public void removeAppender(Appender appender) {
        synchronized (downstreamAppenders) {
            downstreamAppenders.removeAppender(appender);
        }
    }

    public void removeAppender(String name) {
        synchronized (downstreamAppenders) {
            downstreamAppenders.removeAppender(name);
        }
    }

    // --- appender methods ---

    protected void append(LoggingEvent event) {
        Object logMessage = event.getMessage();
        if (logMessage instanceof GroupedTimingStatistics && chartGenerator != null) {
            chartGenerator.appendData((GroupedTimingStatistics) logMessage);

            //output the graph if necessary to any attached appenders
            if ((numLoggedStatistics.incrementAndGet() % getDataPointsPerGraph() == 0) &&
                (downstreamAppenders.getAllAppenders() != null) /* getAllAppenders returns null when no appenders */) {

                synchronized (downstreamAppenders) {
                    downstreamAppenders.appendLoopOnAppenders(
                            new LoggingEvent(Logger.class.getName(),
                                             Logger.getLogger(StopWatch.DEFAULT_LOGGER_NAME),
                                             event.timeStamp,
                                             event.getLevel(),
                                             chartGenerator.getChartUrl(),
                                             null)
                    );
                }
            }
        }
    }

    public boolean requiresLayout() {
        return false;
    }

    public void close() {
        //close any downstream appenders
        synchronized (downstreamAppenders) {
            for (Enumeration enumer = downstreamAppenders.getAllAppenders();
                 enumer != null && enumer.hasMoreElements();) {
                ((Appender) enumer.nextElement()).close();
            }
        }
    }
}
