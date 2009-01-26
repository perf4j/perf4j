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
import org.perf4j.TimingStatistics;
import org.perf4j.helpers.StatsValueRetriever;

import java.util.*;
import java.net.URLEncoder;
import java.io.UnsupportedEncodingException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.text.DecimalFormatSymbols;

/**
 * This implementation of StatisticsChartGenerator creates a chart URL in the format expected by the Google Chart API.
 *
 * @see <a href="http://code.google.com/apis/chart/">Google Chart API</a>
 * @author Alex Devine
 */
public class GoogleChartGenerator implements StatisticsChartGenerator {
    /**
     * The DEFAULT_BASE_URL points to Google's charting server at chart.apis.google.com.
     */
    public static final String DEFAULT_BASE_URL = "http://chart.apis.google.com/chart?";

    /**
     * The maximum supported chart size is 300,000 pixels per the Google Chart API.
     */
    public static final int MAX_POSSIBLE_CHART_SIZE = 300000;

    /**
     * The default chart width is 750 pixels.
     */
    public static final int DEFAULT_CHART_WIDTH = 750;

    /**
     * The default chart height is 400 pixels.
     */
    public static final int DEFAULT_CHART_HEIGHT = 400;

    /**
     * The default hex color codes used for the individual data series displayed on the chart.
     */
    public static final String[] DEFAULT_SERIES_COLORS = {
            "ff0000", //red
            "00ff00", //green
            "0000ff", //blue
            "00ffff", //cyan
            "ff00ff", //magenta
            "ffff00", //yellow
            "000000", //black
            "d2b48c", //tan
            "ffa500", //orange
            "a020f0" //purple
    };

    private StatsValueRetriever valueRetriever;
    private String baseUrl;
    private LinkedList<GroupedTimingStatistics> data = new LinkedList<GroupedTimingStatistics>();
    private int width = DEFAULT_CHART_WIDTH;
    private int height = DEFAULT_CHART_HEIGHT;
    private int maxDataPoints = DEFAULT_MAX_DATA_POINTS;
    private Set<String> enabledTags = null;

    // --- Constructors ---

    /**
     * Default constructor creates a chart that displays mean execution values and uses the default Google Chart URL.
     */
    public GoogleChartGenerator() {
        this(StatsValueRetriever.MEAN_VALUE_RETRIEVER, DEFAULT_BASE_URL);
    }

    /**
     * Creates a chart that uses the specified StatsValueRetriever to determine which values from the
     * TimingStatistic object to display. For example, a chart could be used to display mean values, transactions
     * per second, etc.
     *
     * @param statsValueRetriever The StatsPerTagDataValueExtractor that determines which value to display.
     */
    public GoogleChartGenerator(StatsValueRetriever statsValueRetriever) {
        this(statsValueRetriever, DEFAULT_BASE_URL);
    }

    /**
     * Creates a chart that uses the specified StatsValueRetriever to determine which values from the
     * StatsPerTag object to display, and also allows the base chart URL to be overridden from the Google default.
     *
     * @param valueRetriever Determines which value (such as mean/min/max/etc) from the TimingStatistic to display on
     *                       the chart
     * @param baseUrl        A value to override for the default base URL of "http://chart.apis.google.com/chart?"
     */
    public GoogleChartGenerator(StatsValueRetriever valueRetriever, String baseUrl) {
        this.valueRetriever = valueRetriever;
        this.baseUrl = baseUrl;
    }

    // --- Bean properties ---

    /**
     * Gets the width of the chart that will be displayed
     *
     * @return The width of the chart in pixels, defaults to 750.
     */
    public int getWidth() {
        return width;
    }

    /**
     * Sets the width of the chart in pixels. Note that the Google Charting API currently only supports a maximum
     * of 300,000 pixels for display, so width X height must be less than 300,000.
     *
     * @param width the width of the chart in pixels.
     */
    public void setWidth(int width) {
        this.width = width;
    }

    /**
     * Gets the height of the chart that will be displayed
     *
     * @return The height of the chart in pixels, defaults to 400.
     */
    public int getHeight() {
        return height;
    }

    /**
     * Sets the height of the chart in pixels. Note that the Google Charting API currently only supports a maximum
     * of 300,000 pixels for display, so width X height must be less than 300,000.
     *
     * @param height the height of the chart in pixels.
     */
    public void setHeight(int height) {
        this.height = height;
    }

    /**
     * Gets the set of tag names for which values will be displayed on the chart. Each tag is represented as a
     * separate series on the chart.
     *
     * @return The set of enabled tag names, or null if ALL tags found in the GroupedTimingStatistics data will be
     * displayed.
     */
    public Set<String> getEnabledTags() {
        return enabledTags;
    }

    /**
     * Sets the set of tag names for which values will be displayed on the chart.
     *
     * @param enabledTags The set of enabled tag names. If this method is not called, or if enabledTags is null,
     *                    then ALL tags from the GroupedTimingStatistics data will be displayed on the chart.
     */
    public void setEnabledTags(Set<String> enabledTags) {
        this.enabledTags = enabledTags;
    }

    /**
     * Gets the maximum number of data points to display on a chart. If <tt>appendData</tt> is called more than
     * this number of times, then only the last maxDataPoints data items will be shown in any generated charts.
     *
     * @return the maximum number of data points that will be displayed
     */
    public int getMaxDataPoints() {
        return maxDataPoints;
    }

    /**
     * Sets the maximum number of data points to display on a chart.
     *
     * @param maxDataPoints The maximum number of data points.
     */
    public void setMaxDataPoints(int maxDataPoints) {
        this.maxDataPoints = maxDataPoints;
    }

    // --- Data methods ---

    public List<GroupedTimingStatistics> getData() {
        return Collections.unmodifiableList(this.data);
    }

    public synchronized void appendData(GroupedTimingStatistics statistics) {
        if (this.data.size() >= this.maxDataPoints) {
            this.data.removeFirst();
        }
        this.data.add(statistics);
    }

    public synchronized String getChartUrl() {
        if (width * height > MAX_POSSIBLE_CHART_SIZE || width * height <= 0) {
            throw new IllegalArgumentException("The chart size must be between 0 and " + MAX_POSSIBLE_CHART_SIZE
                                               + " pixels. Current size is " + width + " x " + height);
        }

        StringBuilder retVal = new StringBuilder(baseUrl);

        //we use an x/y chart
        retVal.append("cht=lxy");

        //set the size and title
        retVal.append("&chtt=").append(encodeUrl(valueRetriever.getValueName()));
        retVal.append("&chs=").append(width).append("x").append(height);

        //specify the axes that will have labels
        retVal.append("&chxt=x,x,y");

        //convert the data to google chart params
        retVal.append(generateGoogleChartParams());

        return retVal.toString();
    }

    // --- helper methods ---

    /**
     * Helper method takes the list of data values and converts them to a String suitable for appending to a Google
     * Chart URL.
     *
     * @return the chart parameters that encode all of the data necessary to display the chart.
     */
    protected String generateGoogleChartParams() {
        long minTimeValue = Long.MAX_VALUE;
        long maxTimeValue = Long.MIN_VALUE;
        double maxDataValue = Double.MIN_VALUE;
        //this map stores all the data series. The key is the tag name (each tag represents a single series) and the
        //value contains two lists of numbers - the first list contains the X values for each point (which is time in
        //milliseconds) and the second list contains the y values, which are the data values pulled from dataWindows.
        Map<String, List<Number>[]> tagsToXDataAndYData = new TreeMap<String, List<Number>[]>();

        for (GroupedTimingStatistics groupedTimingStatistics : data) {
            Map<String, TimingStatistics> statsByTag = groupedTimingStatistics.getStatisticsByTag();
            long windowStartTime = groupedTimingStatistics.getStartTime();
            long windowLength = groupedTimingStatistics.getStopTime() - windowStartTime;
            //keep track of the min/max time value, this is needed for scaling the chart parameters
            minTimeValue = Math.min(minTimeValue, windowStartTime);
            maxTimeValue = Math.max(maxTimeValue, windowStartTime);

            for (Map.Entry<String, TimingStatistics> tagWithData : statsByTag.entrySet()) {
                String tag = tagWithData.getKey();
                if (this.enabledTags == null || this.enabledTags.contains(tag)) {
                    //get the corresponding value from tagsToXDataAndYData
                    List<Number>[] xAndYData = tagsToXDataAndYData.get(tagWithData.getKey());
                    if (xAndYData == null) {
                        tagsToXDataAndYData.put(tag, xAndYData = new List[]{new ArrayList<Number>(),
                                                                            new ArrayList<Number>()});
                    }

                    //the x data is the start time of the window, the y data is the value
                    Number yValue = this.valueRetriever.getStatsValue(tagWithData.getValue(), windowLength);
                    xAndYData[0].add(windowStartTime);
                    xAndYData[1].add(yValue);

                    //update the max data value, which is needed for scaling
                    maxDataValue = Math.max(maxDataValue, yValue.doubleValue());
                }
            }
        }

        //if it's empty, there's nothing to display
        if (tagsToXDataAndYData.isEmpty()) {
            return "";
        }

        //set up the axis labels - we use the US decimal format locale to ensure the decimal separator is . and not ,
        DecimalFormat decimalFormat = new DecimalFormat("##0.0", new DecimalFormatSymbols(Locale.US));
        SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");

        //the y-axis label goes from 0 to the maximum data value
        String axisRangeParam = "&chxr=2,0," + decimalFormat.format(maxDataValue);

        //for the x-axis (time) labels, ideally we want one label for each data window, but support a maximum of 10
        //labels so the chart doesn't get too crowded
        int stepSize = this.data.size() / 10 + 1;
        StringBuilder timeAxisLabels = new StringBuilder("&chxl=0:");
        StringBuilder timeAxisLabelPositions = new StringBuilder("&chxp=0");

        for (Iterator<GroupedTimingStatistics> iter = data.iterator(); iter.hasNext();) {
            GroupedTimingStatistics groupedTimingStatistics = iter.next();
            long windowStartTime = groupedTimingStatistics.getStartTime();
            String label = dateFormat.format(new Date(windowStartTime));
            double position = 100.0 * (windowStartTime - minTimeValue) / (maxTimeValue - minTimeValue);
            timeAxisLabels.append("|").append(label);
            timeAxisLabelPositions.append(",").append(decimalFormat.format(position));

            //skip over some windows if stepSize is greater than 1
            for (int i = 1; i < stepSize && iter.hasNext(); i++) {
                iter.next();
            }
        }

        //this next line appends a "Time" label in the middle of the bottom of the X axis
        timeAxisLabels.append("|1:|Time");
        timeAxisLabelPositions.append("|1,50");

        //display the gridlines
        double xAxisGridlineStepSize = this.data.size() > 2 ? 100.0 / (this.data.size() - 1) : 50.0;
        String gridlinesParam = "&chg=" + decimalFormat.format(xAxisGridlineStepSize) + ",10";

        //at this point we should be able to normalize the data to 0 - 100 as required by the google chart API
        StringBuilder chartDataParam = new StringBuilder("&chd=t:");
        StringBuilder chartColorsParam = new StringBuilder("&chco=");
        StringBuilder chartShapeMarkerParam = new StringBuilder("&chm=");
        StringBuilder chartLegendParam = new StringBuilder("&chdl=");

        //this loop is run once for each tag, i.e. each data series to be displayed on the chart
        int i = 0;
        for (Iterator<Map.Entry<String, List<Number>[]>> iter = tagsToXDataAndYData.entrySet().iterator();
             iter.hasNext(); i++) {
            Map.Entry<String, List<Number>[]> tagWithXAndYData = iter.next();

            //data param
            List<Number> xValues = tagWithXAndYData.getValue()[0];
            chartDataParam.append(numberValuesToGoogleDataSeriesParam(xValues, minTimeValue, maxTimeValue));
            chartDataParam.append("|");

            List<Number> yValues = tagWithXAndYData.getValue()[1];
            chartDataParam.append(numberValuesToGoogleDataSeriesParam(yValues, 0, maxDataValue));

            //color param
            String color = DEFAULT_SERIES_COLORS[i % DEFAULT_SERIES_COLORS.length];
            chartColorsParam.append(color);

            //the shape marker param puts a diamond (the d) at each data point (the -1) of size 5 pixels.
            chartShapeMarkerParam.append("d,").append(color).append(",").append(i).append(",-1,5.0");

            //legend param
            chartLegendParam.append(tagWithXAndYData.getKey());

            if (iter.hasNext()) {
                chartDataParam.append("|");
                chartColorsParam.append(",");
                chartShapeMarkerParam.append("|");
                chartLegendParam.append("|");
            }
        }

        return chartDataParam.toString()
               + chartColorsParam
               + chartShapeMarkerParam
               + chartLegendParam
               + axisRangeParam
               + timeAxisLabels
               + timeAxisLabelPositions
               + gridlinesParam;
    }

    /**
     * This helper method is used to normalize a list of data values from 0 - 100 as required by the Google Chart
     * Data API, and from this data it constructs the series data URL param.
     *
     * @param values           the values to be normalized
     * @param minPossibleValue the minimum possible value for the values
     * @param maxPossibleValue the maximmum possible value for the values
     * @return A Google Chart API data series using normal text encoding (see the Chart API docs)
     */
    protected String numberValuesToGoogleDataSeriesParam(List<Number> values,
                                                         double minPossibleValue, double maxPossibleValue) {
        StringBuilder retVal = new StringBuilder();

        double valueRange = maxPossibleValue - minPossibleValue;
        DecimalFormat formatter = new DecimalFormat("##0.0");

        for (Iterator<Number> iter = values.iterator(); iter.hasNext();) {
            Number value = iter.next();
            double normalizedNumber = 100.0 * (value.doubleValue() - minPossibleValue) / valueRange;
            retVal.append(formatter.format(normalizedNumber));
            if (iter.hasNext()) {
                retVal.append(",");
            }
        }

        return retVal.toString();
    }

    /**
     * Helper method encodes a string use as a URL parameter value.
     *
     * @param string the string to encode
     * @return the encoded string
     */
    protected String encodeUrl(String string) {
        try {
            return URLEncoder.encode(string, "UTF-8");
        } catch (UnsupportedEncodingException uee) {
            //can't happen;
            return string;
        }
    }

}
