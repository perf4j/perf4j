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
package org.perf4j.chart;

import org.perf4j.GroupedTimingStatistics;

import java.util.List;

/**
 * Generates a chart based on data from a set of GroupedTimingStatistics.
 *
 * @author Alex Devine
 */
public interface StatisticsChartGenerator {
    /**
     * The default maximum number of data points (along the X axis) that will be displayed. When appending data using
     * the <tt>appendData</tt> method, implementing classes that respect this limit will only graph the last 20
     * statistics that were passed to <tt>appendData</tt>.
     */
    public static final int DEFAULT_MAX_DATA_POINTS = 20;

    /**
     * Implementing classes should return a URL to the chart that depicts the data sent in to the <tt>appendData</tt>
     * method. Note that the format of the URL is completely up to the implementing class - some implementations may
     * generate a binary file and return a URL to the known server hosting the file, others may generate a Google Chart
     * API-formatted URL, while others may return a data: URL with the image data directly embedded.
     *
     * @return A URL that can be used to display the chart.
     */
    public String getChartUrl();

    /**
     * Appends a set of statistics to the list of data to be displayed on the chart.
     *
     * @param statistics the statistics to be added to the list of data.
     */
    public void appendData(GroupedTimingStatistics statistics);

    /**
     * Gets the data that will be visualized by any charts created by this chart generator.
     *
     * @return The data for this chart generator.
     */
    public List<GroupedTimingStatistics> getData();

}
