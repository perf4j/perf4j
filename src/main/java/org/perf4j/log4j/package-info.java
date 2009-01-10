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
 * Provides the log4j appenders that can be used to automatically aggregate and analyze
 * {@link org.perf4j.StopWatch} timing statements logged to an <tt>org.apache.log4j.Logger</tt>. Normally, though, if
 * log4j is your logging framework of choice you should use the {@link org.perf4j.log4j.Log4JStopWatch} as your
 * StopWatch implementation. Three appenders are provided:
 *
 * <ol>
 * <li>{@link org.perf4j.log4j.AsyncCoalescingStatisticsAppender} - This appender is used to group logged
 * <tt>StopWatch</tt> messages over a specified time span (defaults to 30 seconds) into single
 * {@link org.perf4j.GroupedTimingStatistics} messages. Other appenders are designed to be attached to this
 * appender, and these downstream appenders are then only notified of this single <tt>GroupedTimingStatistics</tt>
 * message at the specified interval. Note that this appender cannot be configured with a log4j.properties file but
 * must instead be configured with a log4j.xml file (if auto-configuration is used in your application).</li>
 * <li>{@link org.perf4j.log4j.JmxAttributeStatisticsAppender} - This appender, when attached to an
 * <tt>AsyncCoalescingStatisticsAppender</tt> described above, can be used to expose timing statistics (such as mean,
 * min and max values) as attributes on a JMX MBean. Since there are many 3rd party tools designed to interact through
 * JMX, this provides a way to allow monitoring and notification when application runtime performance degrades.</li>
 * <li>{@link org.perf4j.log4j.GraphingStatisticsAppender} - This appender is used to output graphs (as a URL to the
 * graph object) backed by the logged <tt>GroupedTimingStatistics</tt> instances (thus, it is also designed to be
 * attached to an <tt>AsyncCoalescingStatisticsAppender</tt>). In addition, these graphs can be made available through
 * a web server using a {@link org.perf4j.log4j.servlet.GraphingServlet} instance in concert with this class.
 * </ol>
 *
 * The following example shows how logging could be configured using a log4j.xml file:
 *
 * <pre>
 * &lt;?xml version="1.0" encoding="UTF-8" ?&gt;
 * &lt;!DOCTYPE log4j:configuration SYSTEM "log4j.dtd"&gt;
 *
 * &lt;log4j:configuration debug="false" xmlns:log4j="http://jakarta.apache.org/log4j/"&gt;
 *   &lt;!-- Main file output appender --&gt;
 *   &lt;appender name="rolling" class="org.apache.log4j.DailyRollingFileAppender"&gt;
 *     &lt;param name="File" value="./logs/application.log"/&gt;
 *     &lt;param name="DatePattern" value="'.'yyyy-MM-dd-HH"/&gt;
 *     &lt;layout class="org.apache.log4j.PatternLayout"&gt;
 *       &lt;param name="ConversionPattern" value="%-5p[%d{yyyy-MM-dd HH:mm:ss}][%-24t] : %m%n"/&gt;
 *     &lt;/layout&gt;
 *   &lt;/appender&gt;
 *
 *   &lt;!-- Perf4J appenders --&gt;
 *   &lt;!-- CoalescingStatistics appender used to group StopWatch logs into GroupedTimingStatistics logs --&gt;
 *   &lt;appender name="CoalescingStatistics" class="org.perf4j.log4j.AsyncCoalescingStatisticsAppender"&gt;
 *     &lt;param name="TimeSlice" value="10000"/&gt; &lt;!-- 10 second time slice --&gt;
 *     &lt;!-- The appenders defined below are attached here --&gt;
 *     &lt;appender-ref ref="Perf4jJMX"/&gt;
 *     &lt;appender-ref ref="PageTimes"/&gt;
 *     &lt;appender-ref ref="PageTPS"/&gt;
 *   &lt;/appender&gt;
 *
 *   &lt;!-- This appender exposes the timing statistics as an MBean through the default platform MBean server --&gt;
 *   &lt;appender name="Perf4jJMX" class="org.perf4j.log4j.JmxAttributeStatisticsAppender"&gt;
 *     &lt;param name="TagNamesToExpose" value="operation1,dbcall,servicecall"/&gt;
 *     &lt;param name="MBeanName" value="org.perf4j.beans:type=Perf4J,name=ApplicationPerf"/&gt;
 *   &lt;/appender&gt;
 *
 *   &lt;!--
 *     This appender exposes mean execution times as a graph. You would most likely want to use
 *     a {@link org.perf4j.log4j.servlet.GraphingServlet} (set up through a web.xml file) in addition to this appender
 *   --&gt;
 *   &lt;appender name="PageTimes" class="org.perf4j.log4j.GraphingStatisticsAppender"&gt;
 *     &lt;param name="GraphType" value="Mean"/&gt;
 *     &lt;param name="TagNamesToGraph" value="operation1,dbcall,servicecall"/&gt;
 *   &lt;/appender&gt;
 *
 *   &lt;!--
 *     This appender exposes transactions per second values as a graph, and would also most likely be used
 *     with a {@link org.perf4j.log4j.servlet.GraphingServlet}.
 *   --&gt;
 *   &lt;appender name="PageTPS" class="org.perf4j.log4j.GraphingStatisticsAppender"&gt;
 *     &lt;param name="GraphType" value="TPS"/&gt;
 *     &lt;param name="TagNamesToGraph" value="search,propDetails,landingPage,propertyCounts"/&gt;
 *   &lt;/appender&gt;
 *
 *   &lt;!-- Loggers --&gt;
 *   &lt;!-- Perf4J logger --&gt;
 *   &lt;logger name="org.perf4j.TimingLogger" additivity="false"&gt;
 *     &lt;level value="info"/&gt;
 *     &lt;appender-ref ref="CoalescingStatistics"/&gt;
 *     &lt;appender-ref ref="rolling"/&gt;
 *   &lt;/logger&gt;
 *
 *   &lt;root&gt;
 *     &lt;level value="ERROR"/&gt;
 *     &lt;appender-ref ref="rolling"/&gt;
 *   &lt;/root&gt;
 * &lt;/log4j:configuration&gt;
 * <pre>
 *
 * @see <a href="http://logging.apache.org/log4j/1.2/index.html">log4j</a>
 */
package org.perf4j.log4j;