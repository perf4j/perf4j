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
package org.perf4j.servlet;

import org.perf4j.chart.StatisticsChartGenerator;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * This servlet class serves as the base class for displaying performance charts in a web environment. This class sets
 * up the framework for sending the HTML response. Subclasses are only responsible for determining how to find the
 * graphs to display by implementing the <tt>getGraphByName</tt> and <tt>getAllKnownGraphNames</tt> methods.
 *
 * @author Alex Devine
 */
public abstract class AbstractGraphingServlet extends HttpServlet {
    /**
     * Setting an init parameter "graphNames" to a comma-separated list of the names of graphs to display by default
     * sets this member variable. Subclass implementations determine how graphs are named. For example, the
     * {@link org.perf4j.log4j.servlet.GraphingServlet} uses the names of
     * {@link org.perf4j.log4j.GraphingStatisticsAppender}s to determine which graphs to show.
     */
    protected List<String> graphNames;

    public void init() throws ServletException {
        String graphNamesString = getInitParameter("graphNames");
        if (graphNamesString != null) {
            graphNames = Arrays.asList(graphNamesString.split(","));
        }
    }

    public void destroy() {
        graphNames = null;
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        Map<String, StatisticsChartGenerator> chartsByName = getChartGeneratorsToDisplay(request);

        writeHeader(request, response);
        for (Map.Entry<String, StatisticsChartGenerator> nameAndChart : chartsByName.entrySet()) {
            writeChart(nameAndChart.getKey(), nameAndChart.getValue(), request, response);
        }
        writeFooter(request, response);
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        doGet(request, response);
    }

    /**
     * Helper method writes the HTML header, everything up to the opening body tag. Subclasses may wish to override.
     */
    protected void writeHeader(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.getWriter().println("<html>");
        response.getWriter().println("<head>");
        response.getWriter().println("<title>Perf4J Performance Graphs</title>");
        if (request.getParameter("refreshRate") != null) {
            int refreshRate = Integer.parseInt(request.getParameter("refreshRate"));
            response.getWriter().println("<meta http-equiv=\"refresh\" content=\"" + refreshRate + "\">");
        }
        response.getWriter().println("<head>");
        response.getWriter().println("<body>");
    }

    /**
     * Helper method writes the chart to the page using an img tag. Subclasses may wish to override.
     *
     * @param name           the name of the chart to write
     * @param chartGenerator the chart generator responsible for creating the chart URL
     * @param request        the incoming servlet request
     * @param response       the servlet respone
     */
    protected void writeChart(String name,
                              StatisticsChartGenerator chartGenerator,
                              HttpServletRequest request,
                              HttpServletResponse response) throws ServletException, IOException {
        response.getWriter().println("<br><br>");

        String chartUrl = (chartGenerator == null) ? null : chartGenerator.getChartUrl();
        if (chartUrl != null) {
            response.getWriter().println("<b>" + name + "</b><br>");
            response.getWriter().println("<img src=\"" + chartUrl + "\">");
        } else {
            response.getWriter().println("<b>Unknown graph name: " + name + "</b><br>");
        }
    }

    /**
     * Helper method writes the HTML footer, closing the body and HTML tags. Subclasses may wish to override.
     */
    protected void writeFooter(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.getWriter().println("</body>");
        response.getWriter().println("</html>");
        response.getWriter().flush();
    }

    /**
     * Helper method generates the list of charts that are to be displayed in this call to the servlet. In general
     * subclasses should not need to override this method.
     *
     * @param request The incoming request, which may contain a list of "graphName" parameters, in which case those
     *                graphs will be displayed
     * @return A map of graph name to the chart generator capable of creating the URL for the graph.
     */
    protected Map<String, StatisticsChartGenerator> getChartGeneratorsToDisplay(HttpServletRequest request) {

        // find the names of the graphs to be displayed
        List<String> graphsToDisplay;

        if (request.getParameter("graphName") != null) {
            // option 1 - passed in on the request
            graphsToDisplay = Arrays.asList(request.getParameterValues("graphName"));
        } else if (graphNames != null) {
            // option 2 - specified as an init parameter
            graphsToDisplay = graphNames;
        } else {
            // option 3 - no graphs specified, return all known graphs
            graphsToDisplay = getAllKnownGraphNames();
        }

        Map<String, StatisticsChartGenerator> retVal = new LinkedHashMap<String, StatisticsChartGenerator>();
        for (String graphName : graphsToDisplay) {
            retVal.put(graphName, getGraphByName(graphName));
        }
        return retVal;
    }

    /**
     * Subclasses should implement this method to return a chart generator by its name. Subclasses may use any method
     * necessary to find the underlying repository of charts.
     *
     * @param name the name of the graph to return
     * @return the chart generator capable of creating the requested chart.
     */
    protected abstract StatisticsChartGenerator getGraphByName(String name);

    /**
     * Subclasses should implement this method to return a list of all possible known graph names.
     *
     * @return The list of possible graph names for which <tt>getGraphByName</tt> will return a valid chart generator.
     */
    protected abstract List<String> getAllKnownGraphNames();
}
