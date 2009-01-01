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
/**
 * Contains classes used to display graphical charts of timing statistics. Implementations of
 * {@link org.perf4j.chart.StatisticsChartGenerator} can display graphs backed by
 * {@link org.perf4j.GroupedTimingStatistics} data. Graphs are most often utilized through an appender or handler
 * (like the {@link org.perf4j.log4j.GraphingStatisticsAppender}) or a servlet (one of the
 * {@link org.perf4j.servlet.AbstractGraphingServlet} subclasses).
 */
package org.perf4j.chart;