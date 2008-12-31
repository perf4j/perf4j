/**
 * Contains classes used to display graphical charts of timing statistics. Implementations of
 * {@link org.perf4j.chart.StatisticsChartGenerator} can display graphs backed by
 * {@link org.perf4j.GroupedTimingStatistics} data. Graphs are most often utilized through an appender or handler
 * (like the {@link org.perf4j.log4j.GraphingStatisticsAppender}) or a servlet (one of the
 * {@link org.perf4j.servlet.AbstractGraphingServlet} subclasses).
 */
package org.perf4j.chart;