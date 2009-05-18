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
import org.apache.log4j.helpers.LogLog;
import org.apache.log4j.spi.AppenderAttachable;
import org.apache.log4j.spi.LoggingEvent;
import org.perf4j.GroupedTimingStatistics;
import org.perf4j.StopWatch;
import org.perf4j.helpers.GroupingStatisticsIterator;
import org.perf4j.helpers.StopWatchParser;

import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.NoSuchElementException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

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
    /**
     * The QueueSize option, used to set the capacity of the loggedMessages queue
     */
    private int queueSize = 1024;

    // --- contained objects ---
    /**
     * The downstream appenders are contained in this AppenderAttachableImpl
     */
    private final AppenderAttachableImpl downstreamAppenders = new AppenderAttachableImpl();
    /**
     * StopWatch log messages are pushed onto this queue, which is initialized in activateOptions().
     */
    private BlockingQueue<String> loggedMessages = null;
    /**
     * This thread pumps logs from the loggedMessages queue. It is created in activateOptions().
     */
    private Thread drainingThread = null;
    /**
     * This int keeps track of the total number of messages that had to be discarded due to the queue being full.
     */
    private volatile int numDiscardedMessages = 0;

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

    /**
     * The <b>QueueSize</b> option is used to control the size of the internal queue used by this appender to store
     * logged messages before they are sent to downstream appenders. Defaults to 1024. If set too small and the queue
     * fills up, then logged StopWatches will be discarded. The number of discarded messages can be accessed using the
     * {@link #getNumDiscardedMessages()} method.
     *
     * @return The QueueSize option.
     */
    public int getQueueSize() {
        return queueSize;
    }

    /**
     * Sets the value of the <b>QueueSize</b> option.
     *
     * @param queueSize The new QueueSize option.
     */
    public void setQueueSize(int queueSize) {
        this.queueSize = queueSize;
    }

    public synchronized void activateOptions() {
        //activate options should only be called once, but just in case:
        if (drainingThread != null) {
            stopDrainingThread();
        }

        numDiscardedMessages = 0;
        loggedMessages = new ArrayBlockingQueue<String>(getQueueSize());
        drainingThread = new Thread(new Dispatcher(), "perf4j-async-stats-appender-sink-" + getName());
        drainingThread.setDaemon(true);
        drainingThread.start();
    }

    // --- attributes ---
    /**
     * Returns the number of StopWatch messages that have been discarded due to the queue being full.
     *
     * @return The number of discarded messages.
     */
    public int getNumDiscardedMessages() {
        return numDiscardedMessages;
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
        //Do a quick check to cull out any messages not meant for us
        if (message.startsWith("start")) {
            pushMessageOntoQueue(message);
        }
    }

    public boolean requiresLayout() {
        return false;
    }

    public void close() {
        stopDrainingThread();

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
     * Helper method pushes a StopWatch message onto the queue
     *
     * @param message the message to add, may not be null.
     */
    private void pushMessageOntoQueue(String message) {
        if (!loggedMessages.offer(message)) {
            ++numDiscardedMessages;
            //let the error handler get this message - by default the error handler will be an OnlyOnceErrorHandler,
            //so we shouldn't have to worry about flooding with bad messages if the queue fills up.
            getErrorHandler().error(message);
        }
    }

    /**
     * Helper method stops the draining thread and waits for it to finish.
     */
    private void stopDrainingThread() {
        try {
            //pushing an empty string on the queue tells the draining thread that we're closing
            loggedMessages.put("");
            //wait for the draining thread to finish
            drainingThread.join(10000L);
        } catch (Exception e) {
            LogLog.warn("Unexpected error stopping AsyncCoalescingStatisticsAppender draining thread", e);
        }
    }

    /**
     * This helper method could potentially be overridden to return a different type of StopWatchParser that is used
     * to parse the log messages send to this appender.
     *
     * @return A new StopWatchParser to use to parse log messages.
     */
    protected StopWatchParser newStopWatchParser() {
        return new StopWatchParser();
    }

    // --- Support Classes ---
    /**
     * This Dispatcher Runnable uses a StopWatchesFromQueueIterator to pull StopWatch logging message off the
     * loggedMessages queue, which are grouped to create GroupedTimingStatistics by the GroupingStatisticsIterator.
     * Downstream appenders are then notified for each GroupedTimingStatistics object created.
     */
    private class Dispatcher implements Runnable {
        public void run() {
            GroupingStatisticsIterator statsIterator = new GroupingStatisticsIterator(new StopWatchesFromQueueIterator(),
                                                                                      timeSlice,
                                                                                      createRollupStatistics);

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
                    getErrorHandler().error("Exception calling append with GroupedTimingStatistics on downstream appender",
                                            e, -1, coalescedLoggingEvent);
                }
            }
        }
    }

    /**
     * This helper class pulls StopWatch log messages off the loggedMessages queue and exposes them through the
     * Iterator interface.
     */
    private class StopWatchesFromQueueIterator implements Iterator<StopWatch> {
        /**
         * Messages are drained to this list in blocks.
         */
        private LinkedList<String> drainedMessages = new LinkedList<String>();
        /**
         * This parser is used to convert String log messages to StopWatches
         */
        private StopWatchParser stopWatchParser = newStopWatchParser();
        /**
         * Keeps track of the NEXT stop watch we will return.
         */
        private StopWatch nextStopWatch;
        /**
         * State variable keeps track of whether we've already determined that the loggedMessages queue has been closed.
         */
        private boolean done;
        /**
         * State variable keeps track of whether we've finished waiting for a timeslice.
         * If true, hasNext will return true and next will return null.
         */
        private boolean timeSliceOver;

        public boolean hasNext() {
            if (nextStopWatch == null) {
                nextStopWatch = getNext(); //then try to get it
            }
            return timeSliceOver || nextStopWatch != null;
        }

        public StopWatch next() {
        	if (timeSliceOver) {
        		timeSliceOver = false;
            	return null;
        	} else if (nextStopWatch == null) {
                nextStopWatch = getNext(); //then try to get it, and barf if there is no more                
                if (nextStopWatch == null) {
                    throw new NoSuchElementException();
                }
            }

            StopWatch retVal = nextStopWatch;
            nextStopWatch = null;
            return retVal;
        }

        public void remove() {
            throw new UnsupportedOperationException();
        }

        private StopWatch getNext() {
            if (done) {
                //if we already found out we're done, short circuit so we won't block
                return null;
            }

            while (true) {
                if (drainedMessages.isEmpty()) {
                    loggedMessages.drainTo(drainedMessages, 64);

                    //drainTo is more efficient but it doesn't block, so if we're still empty call take() to block
                    if (drainedMessages.isEmpty()) {
                        //then wait for a message to show up
                        try {
                        	String message = loggedMessages.poll(timeSlice, TimeUnit.MILLISECONDS);
                        	if (message == null) {
                        		// no new messages, but want to indicate to check the timeslice
                        		timeSliceOver = true;
                        		return null;
                        	} else {
                        		drainedMessages.add(message);
                        	}                            
                        } catch (InterruptedException ie) {
                            //someone interrupted us, we're done
                            done = true;
                            return null;
                        }
                    }
                }

                while (!drainedMessages.isEmpty()) {
                    String message = drainedMessages.removeFirst();
                    if (message.length() == 0) {
                        //the empty message is pushed onto the queue by the enclosing class' close() method
                        //to indicate that we're done
                        done = true;
                        return null;
                    }

                    StopWatch parsedStopWatch = stopWatchParser.parseStopWatch(message);
                    if (parsedStopWatch != null) {
                        return parsedStopWatch;
                    }
                    //otherwise the message wasn't a valid stopWatch, so let the loop continue to get the next one
                }
            }
        }
    }
}
