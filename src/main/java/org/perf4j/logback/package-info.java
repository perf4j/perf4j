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
 * Provides the logback appenders that can be used to automatically aggregate and analyze
 * {@link org.perf4j.StopWatch} timing statements logged to an <tt>org.slf4j.Logger</tt>. Normally, though, if
 * logback is your logging framework of choice you should use the {@link org.perf4j.slf4j.Slf4JStopWatch} as your
 * StopWatch implementation. Three appenders are provided:
 *
 * <ol>
 * <li>{@link org.perf4j.logback.AsyncCoalescingStatisticsAppender} - This appender is used to group logged
 * <tt>StopWatch</tt> messages over a specified time span (defaults to 30 seconds) into single
 * {@link org.perf4j.GroupedTimingStatistics} messages. Other appenders are designed to be attached to this
 * appender, and these downstream appenders are then only notified of this single <tt>GroupedTimingStatistics</tt>
 * message at the specified interval. Note that this appender can be configured with a logback.xml file.</li>
 * <li>{@link org.perf4j.logback.JmxAttributeStatisticsAppender} - This appender, when attached to an
 * <tt>AsyncCoalescingStatisticsAppender</tt> described above, can be used to expose timing statistics (such as mean,
 * min and max values) as attributes on a JMX MBean. Since there are many 3rd party tools designed to interact through
 * JMX, this provides a way to allow monitoring and notification when application runtime performance degrades.</li>
 * <li>{@link org.perf4j.logback.GraphingStatisticsAppender} - This appender is used to output graphs (as a URL to the
 * graph object) backed by the logged <tt>GroupedTimingStatistics</tt> instances (thus, it is also designed to be
 * attached to an <tt>AsyncCoalescingStatisticsAppender</tt>). In addition, these graphs can be made available through
 * a web server using a {@link org.perf4j.logback.servlet.GraphingServlet} instance in concert with this class.
 * </ol>
 *
 * The following example shows how logging could be configured using a logback.xml file:
 *
 * <pre>
 *&lt;configuration&gt;
 *    &lt;!-- Perf4J appenders --&gt;
 *    &lt;!--
 *       This AsyncCoalescingStatisticsAppender groups StopWatch log messages
 *       into GroupedTimingStatistics messages which it sends on the
 *       file appender defined below
 *    --&gt;
 *    &lt;appender name="CoalescingStatistics" class="org.perf4j.logback.AsyncCoalescingStatisticsAppender"&gt;
 *        &lt;param name="TimeSlice" value="60000"/&gt;
 *        &lt;appender-ref ref="graphExecutionTimes"/&gt;
 *        &lt;appender-ref ref="graphExecutionTPS"/&gt;
 *        &lt;!-- We add the JMX Appender reference onto the CoalescingStatistics --&gt;
 *        &lt;appender-ref ref="perf4jJmxAppender"/&gt;
 *    &lt;/appender&gt;
 *
 *    &lt;appender name="graphExecutionTimes" class="org.perf4j.logback.GraphingStatisticsAppender"&gt;
 *        &lt;!-- Possible GraphTypes are Mean, Min, Max, StdDev, Count and TPS --&gt;
 *        &lt;param name="GraphType" value="Mean"/&gt;
 *        &lt;!-- The tags of the timed execution blocks to graph are specified here --&gt;
 *        &lt;param name="TagNamesToGraph" value="DESTROY_TICKET_GRANTING_TICKET,GRANT_SERVICE_TICKET,GRANT_PROXY_GRANTING_TICKET,VALIDATE_SERVICE_TICKET,CREATE_TICKET_GRANTING_TICKET" /&gt;
 *    &lt;/appender&gt;
 *
 *    &lt;appender name="graphExecutionTPS" class="org.perf4j.logback.GraphingStatisticsAppender"&gt;
 *        &lt;param name="GraphType" value="TPS" /&gt;
 *        &lt;param name="TagNamesToGraph" value="DESTROY_TICKET_GRANTING_TICKET,GRANT_SERVICE_TICKET,GRANT_PROXY_GRANTING_TICKET,VALIDATE_SERVICE_TICKET,CREATE_TICKET_GRANTING_TICKET" /&gt;
 *    &lt;/appender&gt;
 *
 *    &lt;!--
 *      This JMX appender creates an MBean and publishes it to the platform MBean server by
 *      default.
 *    --&gt;
 *    &lt;appender name="perf4jJmxAppender" class="org.perf4j.logback.JmxAttributeStatisticsAppender"&gt;
 *        &lt;!--
 *          You must specify the tag names whose statistics should be exposed as
 *          MBean attributes.
 *        --&gt;
 *        &lt;TagNamesToExpose&gt;firstBlock,secondBlock&lt;/TagNamesToExpose&gt;
 *        &lt;!--
 *          The NotificationThresholds param configures the sending of JMX notifications
 *          when statistic values exceed specified thresholds. This config states that
 *          the firstBlock max value should be between 0 and 800ms, and the secondBlock max
 *          value should be less than 1500 ms. You can also set thresholds on the Min,
 *          Mean, StdDev, Count and TPS statistics - e.g. firstBlockMean(&lt;600).
 *        --&gt;
 *        &lt;NotificationThresholds&gt;firstBlockMax(0-800),secondBlockMax(&lt;1500)&lt;/NotificationThresholds&gt;
 *        &lt;!--
 *          You can also specify an optional MBeanName param, which overrides
 *          the default MBean name of org.perf4j:type=StatisticsExposingMBean,name=Perf4J
 *        --&gt;
 *    &lt;/appender&gt;
 *
 *    &lt;!-- This file appender is used to output aggregated performance statistics --&gt;
 *    &lt;appender name="perf4jFileAppender" class="ch.qos.logback.core.rolling.RollingFileAppender"&gt;
 *        &lt;File&gt;target/perf4j.log&lt;/File&gt;
 *        &lt;encoder&gt;
 *            &lt;Pattern&gt;%date %-5level [%thread] %logger{36} [%file:%line] %msg%n&lt;/Pattern&gt;
 *        &lt;/encoder&gt;
 *        &lt;rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy"&gt;
 *            &lt;FileNamePattern&gt;target/perf4j.%d{yyyy-MM-dd}.log&lt;/FileNamePattern&gt;
 *        &lt;/rollingPolicy&gt;
 *    &lt;/appender&gt;
 *
 *    &lt;!-- Loggers --&gt;
 *    &lt;!--
 *      The Perf4J logger. Note that org.perf4j.TimingLogger is the value of the
 *      org.perf4j.StopWatch.DEFAULT_LOGGER_NAME constant. Also, note that
 *      additivity is set to false, which is usually what is desired - this means
 *      that timing statements will only be sent to this logger and NOT to
 *      upstream loggers.
 *    --&gt;
 *    &lt;logger name="org.perf4j.TimingLogger" additivity="false"&gt;
 *        &lt;level value="INFO"/&gt;
 *        &lt;appender-ref ref="CoalescingStatistics"/&gt;
 *        &lt;appender-ref ref="perf4jFileAppender"/&gt;
 *    &lt;/logger&gt;
 *&lt;/configuration&gt;
 * <pre>
 *
 * @see <a href="http://logback.qos.ch/">logback</a>
 */
package org.perf4j.logback;