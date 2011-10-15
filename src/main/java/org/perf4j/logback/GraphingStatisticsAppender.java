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
package org.perf4j.logback;

import java.io.Flushable;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import org.perf4j.GroupedTimingStatistics;
import org.perf4j.StopWatch;
import org.perf4j.chart.GoogleChartGenerator;
import org.perf4j.chart.StatisticsChartGenerator;
import org.perf4j.helpers.MiscUtils;
import org.perf4j.helpers.StatsValueRetriever;
import org.slf4j.LoggerFactory;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.AppenderBase;
import ch.qos.logback.core.spi.AppenderAttachable;
import ch.qos.logback.core.spi.AppenderAttachableImpl;


/**
 * This appender is designed to be attached to an {@link AsyncCoalescingStatisticsAppender}. It takes the incoming
 * GroupedTimingStatistics log messages and uses this data to update a graphical view of the logged statistics. If
 * ANOTHER appender is then attached to this appender then the graph URLs will be written to the appender on a scheduled
 * basis. Alternatively, the graph can be viewed by setting up a
 * {@link org.perf4j.logback.servlet.GraphingServlet} to expose the graph images.
 *
 * @author Alex Devine
 * @author Xu Huisheng
 */
public class GraphingStatisticsAppender extends AppenderBase<LoggingEvent>
    implements AppenderAttachable<LoggingEvent>, Flushable {
    /**
     * This class keeps track of all appenders of this type that have been created. This allows static access to
     * the appenders from the org.perf4j.logback.servlet.GraphingServlet class.
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
    private StatisticsChartGenerator chartGenerator;

    /**
     * Keeps track of the number of logged GroupedTimingStatistics, which is used to determine when a graph should
     * be written to any attached appenders.
     */
    private AtomicLong numLoggedStatistics = new AtomicLong();

    /**
     * Keeps track of whether there is existing data that hasn't yet been flushed to downstream appenders.
     */
    private volatile boolean hasUnflushedData = false;

    /**
     * Keeps track of the Level of the last appended event. This is just used to determine what level we send to OUR
     * downstream events.
     */
    private Level lastAppendedEventLevel = Level.INFO;

    /**
     * Any downstream appenders are contained in this AppenderAttachableImpl
     */
    private final AppenderAttachableImpl<LoggingEvent> downstreamAppenders = new AppenderAttachableImpl<LoggingEvent>();

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

    // --- lifecycle ---
    @Override
    public void start() {
        super.start();
        chartGenerator = createChartGenerator();

        //update the static APPENDERS_BY_NAME object
        if (getName() != null) {
            APPENDERS_BY_NAME.put(getName(), this);
        }
    }

    @Override
    public void stop() {
        //close any downstream appenders
        synchronized (downstreamAppenders) {
            flush();
            downstreamAppenders.detachAndStopAllAppenders();
        }

        super.stop();
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
            Set<String> enabledTags =
                    new HashSet<String>(Arrays.asList(MiscUtils.splitAndTrim(getTagNamesToGraph(), ",")));
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
    public void addAppender(Appender<LoggingEvent> appender) {
        synchronized (downstreamAppenders) {
            downstreamAppenders.addAppender(appender);
        }
    }

    public Iterator<Appender<LoggingEvent>> iteratorForAppenders() {
        synchronized (downstreamAppenders) {
            return downstreamAppenders.iteratorForAppenders();
        }
    }

    public Appender<LoggingEvent> getAppender(String name) {
        synchronized (downstreamAppenders) {
            return downstreamAppenders.getAppender(name);
        }
    }

    public boolean isAttached(Appender<LoggingEvent> appender) {
        synchronized (downstreamAppenders) {
            return downstreamAppenders.isAttached(appender);
        }
    }

    public void detachAndStopAllAppenders() {
        synchronized (downstreamAppenders) {
            downstreamAppenders.detachAndStopAllAppenders();
        }
    }

    public boolean detachAppender(Appender<LoggingEvent> appender) {
        synchronized (downstreamAppenders) {
            return downstreamAppenders.detachAppender(appender);
        }
    }

    public boolean detachAppender(String name) {
        synchronized (downstreamAppenders) {
            return downstreamAppenders.detachAppender(name);
        }
    }

    // --- appender methods ---
    @Override
    protected void append(LoggingEvent event) {
        if ((event.getArgumentArray() != null)
                && (event.getArgumentArray().length > 0)) {
            Object logMessage = event.getArgumentArray()[0];

            if (logMessage instanceof GroupedTimingStatistics && chartGenerator != null) {
                chartGenerator.appendData((GroupedTimingStatistics) logMessage);
                hasUnflushedData = true;
                lastAppendedEventLevel = event.getLevel();

                //output the graph if necessary to any attached appenders
                if ((numLoggedStatistics.incrementAndGet() % getDataPointsPerGraph()) == 0) {
                    flush();
                }
            }
        }
    }

    // --- Flushable method ---
    /**
     * This flush method writes the graph, with the data that exists at the time it is calld, to any attached appenders.
     */
    public void flush() {
        synchronized (downstreamAppenders) {
            if (hasUnflushedData) {
                downstreamAppenders.appendLoopOnAppenders(new LoggingEvent(
                        Logger.class.getName(),
                        (Logger) LoggerFactory.getLogger(StopWatch.DEFAULT_LOGGER_NAME),
                        lastAppendedEventLevel,
                        chartGenerator.getChartUrl(), null, null));
                hasUnflushedData = false;
            }
        }
    }
}
