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

import org.perf4j.GroupedTimingStatistics;
import org.perf4j.helpers.GroupedTimingStatisticsCsvFormatter;
import org.perf4j.helpers.MiscUtils;
import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.core.LayoutBase;

/**
 * A layout that outputs {@link org.perf4j.GroupedTimingStatistics} instances as comma-separated values. Thus, this
 * layout is designed to be attached to appenders that are themselves attached to an
 * {@link AsyncCoalescingStatisticsAppender}.
 * <p>
 * By default, each GroupedTimingStatistics object is output as a bunch of lines, with one line for each tagged
 * {@link org.perf4j.TimingStatistics} instance contained within the GroupedTimingStatistics object. The following
 * "columns" are output, separated by commas:
 * <ol>
 * <li>tag - the tag name of the code block that the statistics refer to
 * <li>start - the start time of timing window
 * <li>stop - the stop time of the timing window
 * <li>mean - the mean execution time of stop watch logs that completed in the timing window
 * <li>min - the min execution time of stop watch logs that completed in the timing window
 * <li>max - the max execution time of stop watch logs that completed in the timing window
 * <li>stddev - the standard deviation of the execution times of stop watch logs that completed in the timing window
 * <li>count - the count of stop watch logs that completed during the timing window
 * </ol>
 * <p>
 * You can modify the columns output using the <b>Columns</b> option. For example, you could specify the Columns option
 * as "tag,start,stop,mean,count" to only output those specified values. In addition to the values specified above you
 * can also use "tps" to output transactions per second.
 * <p>
 * In addition to the default output of one line per tag for each GroupedTimingStatistics object, this layout also
 * supports a <b>Pivot</b> option which outputs just a single line for an entire GroupedTimingStatistics object. When
 * pivot is true you should set the Columns to specify the values from the specific tags you want to output. For
 * example, setting Pivot to true and setting Columns to "start,stop,codeBlock1Mean,codeBlock2Mean" would cause, for
 * each GroupedTimingStatistics object, a single line to be output with the start and stop times of the window, the
 * mean execution time for all stop watch logs with a codeBlock1 tag, and the mean execution time for all stop watch
 * logs with a codeBlock2 tag.
 *
 * @author Alex Devine
 * @author Xu Huisheng
 */
public class StatisticsCsvLayout extends LayoutBase<LoggingEvent> {
    // --- configuration options ---

    /**
     * Pivot option
     */
    private boolean pivot = false;
    /**
     * Columns option, a comma-separated list of column values to output.
     */
    private String columns = GroupedTimingStatisticsCsvFormatter.DEFAULT_FORMAT_STRING;
    /**
     * PrintNotStatistics option
     */
    private boolean printNonStatistics = false;

    // --- contained objects ---
    /**
     * The csvFormatter is created in the {@link #start} method. The work of actually formatting the
     * GroupedTimingStatistics object is delegated to this object.
     */
    protected GroupedTimingStatisticsCsvFormatter csvFormatter;

    // --- configuration options ---

    /**
     * The <b>Pivot</b> option, which is false by default, determines whether or not a single line will be output for
     * each GroupedTimingStatistics object, or whether one line for each tag within a GroupedTimingStatistics object
     * will be output.
     *
     * @return the Pivot option.
     */
    public boolean isPivot() { return pivot; }

    /**
     * Sets the value of the <b>Pivot</b> option.
     *
     * @param pivot The new Pivot option value.
     */
    public void setPivot(boolean pivot) { this.pivot = pivot; }

    /**
     * The <b>Columns</b> option is a comma-separated list of the values that should be output for each line that
     * is printed. See the class javadoc for the allowed value.
     *
     * @return the Columns option.
     */
    public String getColumns() { return columns; }

    /**
     * Sets the value of the <b>Columns</b> option.
     *
     * @param columns The new Columns option value.
     */
    public void setColumns(String columns) { this.columns = columns; }

    /**
     * Gets the value of the <b>PrintNonStatistics</b> option. In general, this layout should only be used for
     * appenders that deal with GroupedTimingStatistics objects (e.g. a FileAppender attached to an
     * {@link AsyncCoalescingStatisticsAppender}). By default, any logging event where the message is NOT a
     * GroupedTimingStatistics object is not output. However, if this option is set to true, then
     * non-GroupedTimingStatistics messages will be output as their string value.
     *
     * @return the PrintNonStatistics option
     */
    public boolean isPrintNonStatistics() { return printNonStatistics; }

    /**
     * Sets the value of the <b>PrintNonStatistics</b> option.
     *
     * @param printNonStatistics The new PrintNonStatistics option value.
     */
    public void setPrintNonStatistics(boolean printNonStatistics) { this.printNonStatistics = printNonStatistics; }

    public String doLayout(LoggingEvent event) {
        try {
            //we assume that the event is a GroupedTimingStatistics object
            return csvFormatter.format((GroupedTimingStatistics) event.getArgumentArray()[0]);
        } catch (ClassCastException cce) {
            //then it's not a GroupedTimingStatistics object
            if (isPrintNonStatistics()) {
                return MiscUtils.escapeStringForCsv(event.getFormattedMessage(), new StringBuilder())
                        .append(MiscUtils.NEWLINE).toString();
            } else {
                return "";
            }
        }
    }

    @Override
    public void start() {
        super.start();
        csvFormatter = new GroupedTimingStatisticsCsvFormatter(isPivot(), getColumns());
    }

}
