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
package org.perf4j.log4j.servlet;

import org.perf4j.chart.StatisticsChartGenerator;
import org.perf4j.log4j.GraphingStatisticsAppender;
import org.perf4j.servlet.AbstractGraphingServlet;

import java.util.ArrayList;
import java.util.List;

/**
 * This graphing servlet implementation looks for graphs from {@link org.perf4j.log4j.GraphingStatisticsAppender}s that
 * have been created by the log4j framework. Thus, in order to add live performance graphs to a web application, you
 * should first create the necessary GraphingStatisticsAppenders in your log4j.xml config file. Then, you should create
 * an instance of this servlet in your web.xml file and set a "graphNames" init parameter to be a comma-separated list
 * of the appender names whose graphs you wish to display.
 *
 * @author Alex Devine
 */
public class GraphingServlet extends AbstractGraphingServlet {

    private static final long serialVersionUID = -2819660868996798604L;

    /**
     * Finds the specified graph by using the
     * {@link org.perf4j.log4j.GraphingStatisticsAppender#getAppenderByName(String)} method to find the appender with
     * the specified name.
     *
     * @param name the name of the GraphingStatisticsAppender whose chart generator should be returned.
     * @return The specified chart generator, or null if no appender with the specified name was found.
     */
    protected StatisticsChartGenerator getGraphByName(String name) {
        GraphingStatisticsAppender appender = GraphingStatisticsAppender.getAppenderByName(name);
        return (appender == null) ? null : appender.getChartGenerator();
    }

    /**
     * This method looks for all known GraphingStatisticsAppenders and returns their names.
     *
     * @return The list of known GraphingStatisticsAppender names.
     */
    protected List<String> getAllKnownGraphNames() {
        List<String> retVal = new ArrayList<String>();
        for (GraphingStatisticsAppender appender : GraphingStatisticsAppender.getAllGraphingStatisticsAppenders()) {
            retVal.add(appender.getName());
        }
        return retVal;
    }
}
