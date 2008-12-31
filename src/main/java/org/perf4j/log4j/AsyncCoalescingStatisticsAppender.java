/* Copyright Homeaway, Inc 2005-2007. All Rights Reserved.
 * No unauthorized use of this software.
 */
package org.perf4j.log4j;

import org.apache.log4j.Appender;
import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.helpers.AppenderAttachableImpl;
import org.apache.log4j.helpers.LogLog;
import org.apache.log4j.spi.AppenderAttachable;
import org.apache.log4j.spi.LoggingEvent;
import org.perf4j.GroupedTimingStatistics;
import org.perf4j.StopWatch;
import org.perf4j.helpers.GroupingStatisticsIterator;
import org.perf4j.helpers.StopWatchLogIterator;

import java.io.IOException;
import java.io.PipedReader;
import java.io.PipedWriter;
import java.io.PrintWriter;
import java.util.Enumeration;

/**
 * This log4j Appender groups StopWatch log messages together to form GroupedTimingStatistics. At a scheduled interval
 * the StopWatch log messages that currently exist in the buffer are pulled to create a single
 * GroupedTimingStatistics instance that is then sent to any attached appenders.
 * <p/>
 * Note that any LoggingEvents which do NOT contain StopWatch objects are discarded.
 * 
 * @author Alex Devine
 */
public class AsyncCoalescingStatisticsAppender extends AppenderSkeleton implements AppenderAttachable {
    // --- configuration options ---
    /**
     * TimeSlice option
     */
    private long timeSlice = 30000L;
    /**
     * DownstreamLogLevel option, converted to a Level object
     */
    private Level downstreamLogLevel = Level.INFO;
    /**
     * CreateRollupStatistics option
     */
    private boolean createRollupStatistics = false;

    // --- contained objects ---
    /**
     * The downstream appenders are contained in this AppenderAttachableImpl
     */
    private final AppenderAttachableImpl downstreamAppenders = new AppenderAttachableImpl();
    /**
     * StopWatch log messages are sent to this logging writer, which wraps a PipedWriter and is the front end of the
     * logging pipe. This writer is created in activateOptions().
     */
    private PrintWriter loggingWriter = null;
    /**
     * The drainingThread pulls StopWatch messages from this drainingReader, using them to create
     * GroupedTimingStatistics messages to send to downstream appenders. This reader is created in activateOptions().
     */
    private PipedReader drainingReader = null;
    /**
     * This thread pumps logs from the drainingReader. It is created in activateOptions().
     */
    private Thread drainingThread = null;

    // --- options ---
    /**
     * The <b>TimeSlice</b> option represents the length of time, in milliseconds, of the window in which appended
     * LogEvents are coalesced to a single GroupedTimingStatistics and sent to downstream appenders.
     * Defaults to 30,000 milliseconds.
     *
     * @return the TimeSlice option.
     */
    public long getTimeSlice() {
        return timeSlice;
    }

    /**
     * Sets the value of the <b>TimeSlice</b> option.
     *
     * @param timeSlice The new TimeSlice option, in milliseconds.
     */
    public void setTimeSlice(long timeSlice) {
        this.timeSlice = timeSlice;
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
        return createRollupStatistics;
    }

    /**
     * Sets the value of the <b>CreateRollupStatistics</b> option.
     *
     * @param createRollupStatistics The new CreateRollupStatistics option.
     */
    public void setCreateRollupStatistics(boolean createRollupStatistics) {
        this.createRollupStatistics = createRollupStatistics;
    }

    public synchronized void activateOptions() {
        //activate options should only be called once, but just in case:
        if (loggingWriter != null) {
            closeLoggingPipe();
        }

        try {
            PipedWriter pipeInput = new PipedWriter();
            loggingWriter = new PrintWriter(pipeInput);
            drainingReader = new PipedReader(pipeInput);
            drainingThread = new Thread(new Dispatcher(), "perf4j-async-stats-appender-sink");
            drainingThread.start();
        } catch (IOException ioe) {
            assert false; //this should never happen given how we set up the piped reader.
        }
    }

    // --- appender attachable methods ---

    public void addAppender(Appender appender) {
        synchronized (downstreamAppenders) {
            downstreamAppenders.addAppender(appender);
        }
    }

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
        String message = String.valueOf(event.getMessage());
        //the next check isn't absolutely required, it just helps cut down on non-StopWatch messages not intended
        //for us. If it startsWith "start" it *looks* like a StopWatch message.
        if (message.startsWith("start")) {
            loggingWriter.println(message);
        }
    }

    public boolean requiresLayout() {
        return false;
    }

    public void close() {
        closeLoggingPipe();

        //close the downstream appenders
        synchronized (downstreamAppenders) {
            for (Enumeration enumer = downstreamAppenders.getAllAppenders();
                 enumer != null && enumer.hasMoreElements();) {
                ((Appender) enumer.nextElement()).close();
            }
        }
    }

    // --- Helper Methods ---
    /**
     * Helper method closed both ends of the logging pipe and waits for the drainingThread to finish.
     */
    private void closeLoggingPipe() {
        try {
            loggingWriter.flush();
            loggingWriter.close();
            //wait for the draining thread to finish
            drainingThread.join(10000L);
        } catch (Exception e) {
            LogLog.warn("Unexpected error closing AsyncCoalescingStatisticsAppender logging pipe", e);
        }
    }

    // --- Support Classes ---
    /**
     * This Dispatcher Runnable uses the StopWatchLogIterator to pull StopWatch logging message off the
     * drainingReader pipe, which are grouped to create GroupedTimingStatistics by the GroupingStatisticsIterator.
     * Downstream appenders are then notified for each GroupedTimingStatistics object created.
     */
    private class Dispatcher implements Runnable {
        public void run() {
            //Create the iterator to groups the StopWatches into GroupedTimingStatistics - it reads from the
            //drainingReader end of the logging pipe
            StopWatchLogIterator stopWatchIterator = new StopWatchLogIterator(drainingReader);
            GroupingStatisticsIterator statsIterator =
                    new GroupingStatisticsIterator(stopWatchIterator, timeSlice, createRollupStatistics);

            while (statsIterator.hasNext()) {
                GroupedTimingStatistics stats = statsIterator.next();

                LoggingEvent coalescedLoggingEvent = new LoggingEvent(Logger.class.getName(),
                                                                      Logger.getLogger(StopWatch.DEFAULT_LOGGER_NAME),
                                                                      System.currentTimeMillis(),
                                                                      downstreamLogLevel,
                                                                      stats,
                                                                      null);
                try {
                    synchronized (downstreamAppenders) {
                        downstreamAppenders.appendLoopOnAppenders(coalescedLoggingEvent);
                    }
                } catch (Exception e) {
                    errorHandler.error("Exception calling append with GroupedTimingStatistics on downstream appender",
                                       e, -1, coalescedLoggingEvent);
                }
            }
        }
    }
}
