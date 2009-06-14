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

import junit.framework.TestCase;
import org.perf4j.GroupedTimingStatistics;
import org.perf4j.StopWatch;
import org.perf4j.helpers.StatsValueRetriever;

import java.util.ResourceBundle;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Tests the GoogleChartGenerator
 */
public class GoogleChartGeneratorTest extends TestCase {
    public static final long START_TIME = 1229903820000L;

    private ResourceBundle expectedChartUrls;

    protected void setUp() throws Exception {
        GroupedTimingStatistics.setTimeZone(TimeZone.getTimeZone("GMT-6")); //results originally generated in GMT-6 zone

        expectedChartUrls = ResourceBundle.getBundle("org/perf4j/chart/googleChartTestExpectedValues");
    }

    protected void tearDown() throws Exception {
        //reset the timezone
        GroupedTimingStatistics.setTimeZone(TimeZone.getDefault());
    }

    public void testNoData() throws Exception {
        GoogleChartGenerator chart = new GoogleChartGenerator();

        verifyUrl(chart.getChartUrl(), "noData");
    }

    public void testThreeDataPoints() throws Exception {
        GoogleChartGenerator chart = new GoogleChartGenerator();

        StopWatch stopWatch = new StopWatch(START_TIME + 2000L, 2000L, "tag", null);
        GroupedTimingStatistics statistics = new GroupedTimingStatistics();
        statistics.setStartTime(START_TIME);
        statistics.setStopTime(START_TIME + 30000L);
        chart.appendData(statistics.addStopWatch(stopWatch));

        stopWatch = new StopWatch(START_TIME + 32000L, 3000L, "tag", null);
        statistics = new GroupedTimingStatistics();
        statistics.setStartTime(START_TIME + 30000L);
        statistics.setStopTime(START_TIME + 60000L);
        chart.appendData(statistics.addStopWatch(stopWatch));

        stopWatch = new StopWatch(START_TIME + 62000L, 1500L, "tag", null);
        statistics = new GroupedTimingStatistics();
        statistics.setStartTime(START_TIME + 60000L);
        statistics.setStopTime(START_TIME + 90000L);
        chart.appendData(statistics.addStopWatch(stopWatch));

        verifyUrl(chart.getChartUrl(), "threeDataPoints");
    }

    public void testTwoSeriesThreeDataPoints() throws Exception {
        GoogleChartGenerator chart = new GoogleChartGenerator();
        GoogleChartGenerator tpsChart = new GoogleChartGenerator(StatsValueRetriever.TPS_VALUE_RETRIEVER);

        StopWatch watch1 = new StopWatch(START_TIME + 2000L, 2000L, "tag1", null);
        StopWatch watch2 = new StopWatch(START_TIME + 2000L, 1000L, "tag2", null);
        GroupedTimingStatistics statistics = new GroupedTimingStatistics();
        statistics.setStartTime(START_TIME);
        statistics.setStopTime(START_TIME + 30000L);
        statistics.addStopWatch(watch1).addStopWatch(watch2);
        chart.appendData(statistics);
        tpsChart.appendData(statistics);

        watch1 = new StopWatch(START_TIME + 32000L, 3000L, "tag1", null);
        watch2 = new StopWatch(START_TIME + 32000L, 2500L, "tag2", null);
        StopWatch watch2b = new StopWatch(START_TIME + 32000L, 2000L, "tag2", null);
        statistics = new GroupedTimingStatistics();
        statistics.setStartTime(START_TIME + 30000L);
        statistics.setStopTime(START_TIME + 60000L);
        statistics.addStopWatch(watch1).addStopWatch(watch2).addStopWatch(watch2b);
        chart.appendData(statistics);
        tpsChart.appendData(statistics);

        watch1 = new StopWatch(START_TIME + 62000L, 1500L, "tag1", null);
        statistics = new GroupedTimingStatistics();
        statistics.setStartTime(START_TIME + 60000L);
        statistics.setStopTime(START_TIME + 90000L);
        statistics.addStopWatch(watch1);
        chart.appendData(statistics);
        tpsChart.appendData(statistics);

        verifyUrl(chart.getChartUrl(), "twoSeriesThreeDataPoints");
        verifyUrl(tpsChart.getChartUrl(), "twoSeriesThreeDataPointsTps");
    }

    public void testGermanLocale() throws Exception {
        //Test for PERFFORJ-19, ensure charts are still generated correctly in a locale that uses , for decimal sep.
        Locale realDefault = Locale.getDefault();
        Locale.setDefault(Locale.GERMANY);

        try {
            testNoData();
            testThreeDataPoints();
            testTwoSeriesThreeDataPoints();
        } finally {
            Locale.setDefault(realDefault);
        }
    }

    protected void verifyUrl(String url, String name) {
        System.out.println(name + "=" + url);
        assertEquals(expectedChartUrls.getString(name), url);
    }
}
