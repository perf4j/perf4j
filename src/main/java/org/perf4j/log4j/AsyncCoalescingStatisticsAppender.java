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
package org.perf4j.log4j;

import org.apache.log4j.Appender;
import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.helpers.AppenderAttachableImpl;
import org.apache.log4j.spi.AppenderAttachable;
import org.apache.log4j.spi.LoggingEvent;
import org.perf4j.GroupedTimingStatistics;
import org.perf4j.StopWatch;
import org.perf4j.helpers.GenericAsyncCoalescingStatisticsAppender;

import java.util.Enumeration;
import java.io.Flushable;

/**
 * This log4j Appender groups StopWatch log messages together to form GroupedTimingStatistics. At a scheduled interval
 * the StopWatch log messages that currently exist in the buffer are pulled to create a single
 * GroupedTimingStatistics instance that is then sent to any attached appenders.
 * <p/>
 * Note that any LoggingEvents which do NOT contain StopWatch objects are discarded. Also, this appender stores logged
 * messages in a bounded buffer before sending those messages to downstream appenders. If the buffer becomes full then
 * subsequent logs will be discarded until the buffer has time to clear. You can access the number of discarded
 * messages using the getNumDiscardedMessages() method.
 *
 * @author Alex Devine
 */
public class AsyncCoalescingStatisticsAppender extends AppenderSkeleton implements AppenderAttachable {

    protected static class ShutdownHook extends Thread {

        private AsyncCoalescingStatisticsAppender appender;

        public ShutdownHook(AsyncCoalescingStatisticsAppender appender) {
            super("perf4j-async-stats-appender-shutdown");
            this.appender = appender;
        }

        @Override
        public void run() {
            try {
                if (appender != null && !appender.closed)
                    appender.close();
            } finally {
                appender = null;
            }
        }

        public AsyncCoalescingStatisticsAppender getAppender() {
            return appender;
        }

        public void setAppender(AsyncCoalescingStatisticsAppender appender) {
            this.appender = appender;
        }
    }

    // --- configuration options ---
    // note most configuration options are provided by the GenericAsyncCoalescingStatisticsAppender
    /**
     * DownstreamLogLevel option, converted to a Level object
     */
    private Level downstreamLogLevel = Level.INFO;

    // --- contained objects ---
    /**
     * This instance provides the main logic for this appender. This wrapper class just provides the log4j-specific
     * parts.
     */
    private final GenericAsyncCoalescingStatisticsAppender baseImplementation =
            newGenericAsyncCoalescingStatisticsAppender();

    /**
     * The downstream appenders are contained in this AppenderAttachableImpl
     */
    private final AppenderAttachableImpl downstreamAppenders = new AppenderAttachableImpl();

    /**
     * This shutdown hook is needed to flush the appender on JVM shutdown so that all messages are logged.
     */
    private ShutdownHook shutdownHook = null;

    // --- options ---
    /**
     * The <b>TimeSlice</b> option represents the length of time, in milliseconds, of the window in which appended
     * LoggingEvents are coalesced to a single GroupedTimingStatistics and sent to downstream appenders.
     * Defaults to 30,000 milliseconds.
     *
     * @return the TimeSlice option.
     */
    public long getTimeSlice() {
        return baseImplementation.getTimeSlice();
    }

    /**
     * Sets the value of the <b>TimeSlice</b> option.
     *
     * @param timeSlice The new TimeSlice option, in milliseconds.
     */
    public void setTimeSlice(long timeSlice) {
        baseImplementation.setTimeSlice(timeSlice);
    }

    /**
     * The <b>ShutdownWaitMillis</b> option represents the length of time, in milliseconds,
     * that the appender should wait after the logging system shutdown commences, before forcibly
     * clearing asynchronous logging queues and interrupting the background queue-processing thread.
     *
     * If not set explicitly, the default shutdown timeout is 10 seconds.
     *
     * @return the ShutdownWaitMillis option.
     */
    public long getShutdownWaitMillis() {
        return baseImplementation.getShutdownWaitMillis();
    }

    /**
     * Sets the value of the <b>ShutdownWaitMillis</b> option.
     *
     * @param shutdownWaitMillis The new ShutdownWaitMillis option, in milliseconds.
     */
    public void setShutdownWaitMillis(long shutdownWaitMillis) {
        baseImplementation.setShutdownWaitMillis(shutdownWaitMillis);
    }

    /**
     * The <b>DownstreamLogLevel</b> option gets the Level of the GroupedTimingStatistics LoggingEvent that is sent to
     * downstream appenders. Since each GroupedTimingStatistics represents a view of a collection of single StopWatch
     * timing event, each of which may have been logged at different levels, this appender needs to decide on a single
     * Level to use to notify downstream appenders. Defaults to "INFO".
     *
     * @return The DownstreamLogLevel option as a String
     */
    public String getDownstreamLogLevel() {
        return downstreamLogLevel.toString();
    }

    /**
     * Sets the value of the <b>DownstreamLogLevel</b> option. This String must be one of the defined Level constants.
     *
     * @param downstreamLogLevel The new DownstreamLogLevel option.
     */
    public void setDownstreamLogLevel(String downstreamLogLevel) {
        this.downstreamLogLevel = Level.toLevel(downstreamLogLevel);
    }

    /**
     * The <b>CreateRollupStatistics</b> option is used to determine whether "rollup" statistics should be created.
     * If the tag name of a StopWatch in a log message contains periods, then the GroupedTimingStatistics will be
     * created as if each substring of the tag up to the period was also logged with a separate StopWatch instance.
     * For example, suppose a StopWatch was logged with a tag of "requests.specificReq.PASS". For grouping purposes
     * a StopWatch entry would be logged under each of the following tags:
     * <ul>
     * <li>requests
     * <li>requests.specificReq
     * <li>requests.specificReq.PASS
     * </ul>
     * This allows you to view statistics at both an individual and aggregated level. If there were other StopWatch
     * entries with a tag of "requests.specificReq.FAIL", then the data collected at the "requests.specificReq" level
     * would include BOTH PASS and FAIL events.
     *
     * @return The CreateRollupStatistics option.
     */
    public boolean isCreateRollupStatistics() {
        return baseImplementation.isCreateRollupStatistics();
    }

    /**
     * Sets the value of the <b>CreateRollupStatistics</b> option.
     *
     * @param createRollupStatistics The new CreateRollupStatistics option.
     */
    public void setCreateRollupStatistics(boolean createRollupStatistics) {
        baseImplementation.setCreateRollupStatistics(createRollupStatistics);
    }

    /**
     * The <b>QueueSize</b> option is used to control the size of the internal queue used by this appender to store
     * logged messages before they are sent to downstream appenders. Defaults to 1024. If set too small and the queue
     * fills up, then logged StopWatches will be discarded. The number of discarded messages can be accessed using the
     * {@link #getNumDiscardedMessages()} method.
     *
     * @return The QueueSize option.
     */
    public int getQueueSize() {
        return baseImplementation.getQueueSize();
    }

    /**
     * Sets the value of the <b>QueueSize</b> option.
     *
     * @param queueSize The new QueueSize option.
     */
    public void setQueueSize(int queueSize) {
        baseImplementation.setQueueSize(queueSize);
    }

    /**
     * The <b>StopWatchParserClassName</b> option is used to determine the class used to parse stop watch messages
     * into StopWatch instances. This defaults to the standard "org.perf4j.helpers.StopWatchParser" class.
     *
     * @return The StopWatchParserClassName option.
     */
    public String getStopWatchParserClassName() {
        return baseImplementation.getStopWatchParserClassName();
    }

    /**
     * Sets the value of the <b>StopWatchParserClassName</b> option.
     *
     * @param stopWatchParserClassName The new StopWatchParserClassName option.
     */
    public void setStopWatchParserClassName(String stopWatchParserClassName) {
        baseImplementation.setStopWatchParserClassName(stopWatchParserClassName);
    }

    public void setName(String name) {
        super.setName(name);
        baseImplementation.setName(name);
    }

    public synchronized void activateOptions() {
        //Start the underlying generic appender with a handler object that pumps statistics to the downstream appenders
        baseImplementation.start(new GenericAsyncCoalescingStatisticsAppender.GroupedTimingStatisticsHandler() {
            public void handle(GroupedTimingStatistics statistics) {
                LoggingEvent coalescedLoggingEvent =
                        new LoggingEvent(Logger.class.getName(),
                                         Logger.getLogger(StopWatch.DEFAULT_LOGGER_NAME),
                                         System.currentTimeMillis(),
                                         downstreamLogLevel,
                                         statistics,
                                         null);
                try {
                    synchronized (downstreamAppenders) {
                        downstreamAppenders.appendLoopOnAppenders(coalescedLoggingEvent);
                    }
                } catch (Exception e) {
                    getErrorHandler().error(
                            "Exception calling append with GroupedTimingStatistics on downstream appender",
                            e, -1, coalescedLoggingEvent
                    );
                }
            }

            public void error(String errorMessage) {
                getErrorHandler().error(errorMessage);
            }
        });

        //Also, we add a shutdown hook that will attempt to flush any pending log events in the queue.
        if (shutdownHook == null) {
            try {
                Runtime.getRuntime().addShutdownHook(shutdownHook = new ShutdownHook(this));
            } catch (Exception e) { /* likely a security exception, nothing we can do */ }
        }
    }

    // --- attributes ---
    /**
     * Returns the number of StopWatch messages that have been discarded due to the queue being full.
     *
     * @return The number of discarded messages.
     */
    public int getNumDiscardedMessages() {
        return baseImplementation.getNumDiscardedMessages();
    }

    // --- appender attachable methods ---

    public void addAppender(Appender appender) {
        synchronized (downstreamAppenders) {
            downstreamAppenders.addAppender(appender);
        }
    }

    @SuppressWarnings("rawtypes")
    public Enumeration getAllAppenders() {
        synchronized (downstreamAppenders) {
            return downstreamAppenders.getAllAppenders();
        }
    }

    public Appender getAppender(String name) {
        synchronized (downstreamAppenders) {
            return downstreamAppenders.getAppender(name);
        }
    }

    public boolean isAttached(Appender appender) {
        synchronized (downstreamAppenders) {
            return downstreamAppenders.isAttached(appender);
        }
    }

    public void removeAllAppenders() {
        synchronized (downstreamAppenders) {
            downstreamAppenders.removeAllAppenders();
        }
    }

    public void removeAppender(Appender appender) {
        synchronized (downstreamAppenders) {
            downstreamAppenders.removeAppender(appender);
        }
    }

    public void removeAppender(String name) {
        synchronized (downstreamAppenders) {
            downstreamAppenders.removeAppender(name);
        }
    }

    // --- appender methods ---
    protected void append(LoggingEvent event) {
        baseImplementation.append(String.valueOf(event.getMessage()));
    }

    public boolean requiresLayout() {
        return false;
    }

    @SuppressWarnings("rawtypes")
    public void close() {
        try {
            baseImplementation.stop();

            //close the downstream appenders
            synchronized (downstreamAppenders) {
                //first FLUSH any flushable downstream appenders (fix for PERFFORJ-22). Note we CAN NOT just flush and
                //close in one loop because this breaks in the case of a "diamond" relationship between appenders, where,
                //say, this appender has 2 attached GraphingStatisticsAppenders that each write to a SINGLE attached
                //FileAppender.
                for (Enumeration enumer = downstreamAppenders.getAllAppenders();
                        enumer != null && enumer.hasMoreElements();) {
                    Appender appender = (Appender) enumer.nextElement();
                    if (appender instanceof Flushable) {
                        try {
                            ((Flushable)appender).flush();
                        } catch (Exception e) { /* Just eat the exception, we're closing down */ }
                    }
                }

                //THEN close them
                for (Enumeration enumer = downstreamAppenders.getAllAppenders();
                        enumer != null && enumer.hasMoreElements();) {
                    ((Appender) enumer.nextElement()).close();
                }
            }

            // remove shutdown hook
            if (shutdownHook != null) {
                try {
                    Runtime.getRuntime().removeShutdownHook(shutdownHook);
                } catch (Exception e) { /* likely a security exception, nothing we can do */ }
            }
        } finally {
            // dereference this appender in the shutdown hook, allow it to be gc()ed even if removal fails
            if (shutdownHook != null) {
                shutdownHook.setAppender(null);
                shutdownHook = null;
            }
            this.closed = true;
        }
    }

    // --- helper methods ---
    /**
     * Creates the new GenericAsyncCoalescingStatisticsAppender that this instance will wrap.
     *
     * @return The newly created GenericAsyncCoalescingStatisticsAppender.
     */
    protected GenericAsyncCoalescingStatisticsAppender newGenericAsyncCoalescingStatisticsAppender() {
        return new GenericAsyncCoalescingStatisticsAppender();
    }
}
