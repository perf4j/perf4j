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
package org.perf4j.helpers;

import org.perf4j.GroupedTimingStatistics;
import org.perf4j.StopWatch;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.NoSuchElementException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * This class provides the implementation for the AsyncCoalescingStatisticsAppenders made available for different
 * logging frameworks. This class itself is generic in that it does not use any logging-framework-specific APIs, but
 * is intended to be wrapped by classes that DO use those specific APIs.
 *
 * @see org.perf4j.log4j.AsyncCoalescingStatisticsAppender
 */
public class GenericAsyncCoalescingStatisticsAppender {

    /**
     * The GroupedTimingStatisticsHandler defines a callback interface so that logging-framework-specific
     * implementations can decide what to do with the coalesced GroupedTimingStatistics.
     */
    public interface GroupedTimingStatisticsHandler {
        /**
         * This callback method is called for each GroupedTimingStatistics instance that is formed by coalescing
         * individual StopWatch messages from the logs. Implementations will most likely pass this instance to
         * downstream appenders or handlers.
         *
         * @param statistics The GroupedTimingStatistics instance.
         */
        void handle(GroupedTimingStatistics statistics);

        /**
         * This method is called whenever an error occurs that should be handled in a logging-framework specific
         * manner.
         *
         * @param errorMessage The message that describes the error.
         */
        void error(String errorMessage);
    }

    // --- configuration options ---
    /**
     * The name of this appender.
     */
    private String name = "";
    /**
     * TimeSlice option
     */
    private long timeSlice = 30000L;
    /**
     * CreateRollupStatistics option
     */
    private boolean createRollupStatistics = false;
    /**
     * The QueueSize option, used to set the capacity of the loggedMessages queue
     */
    private int queueSize = 1024;
    /**
     * Wait time for queue to clear when shutting down, in milliseconds.
     */
    private long shutdownWaitMillis = 10000L;
    /**
     * The fully qualified class name of the class to use for StopWatch parsing, defaults to the standard
     * org.perf4j.helpers.StopWatchParser
     */
    private String stopWatchParserClassName = StopWatchParser.class.getName();

    // --- contained objects ---
    /**
     * All GroupedTimingStatistics created by this appender are passed to the handler object for further handling.
     * This variable is set by the param passed to the start() method.
     */
    private GroupedTimingStatisticsHandler handler = null;
    /**
     * StopWatch log messages are pushed onto this queue, which is initialized in start().
     */
    private BlockingQueue<String> loggedMessages = null;
    /**
     * This parser is used to convert String log messages to StopWatches
     */
    private StopWatchParser stopWatchParser;
    /**
     * This thread pumps logs from the loggedMessages queue. It is created in start().
     */
    private Thread drainingThread = null;
    /**
     * This int keeps track of the total number of messages that had to be discarded due to the queue being full.
     */
    private volatile int numDiscardedMessages = 0;

    // --- options ---
    /**
     * The name of this appender.
     *
     * @return The name of this appender.
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the name of this appender.
     *
     * @param name The new appender name.
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * The <b>TimeSlice</b> option represents the length of time, in milliseconds, of the window in which appended
     * log events are coalesced to a single GroupedTimingStatistics and sent to the GroupedTimingStatisticsHandler.
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

    /**
     * The <b>ShutdownWaitMillis</b> option is used to control how long this class will block, waiting for the queue
     * to drain, when shutting-down the Appender.
     *
     * After this timeout expires, no new messages will be drained from the log queue, the log queue will be
     * truncated and shutdown of the Appender will complete.
     *
     * @return The ShutdownWaitMillis option.
     */
    public long getShutdownWaitMillis() {
        return shutdownWaitMillis;
    }

    /**
     * Sets the value of the <b>ShutdownWaitMillis</b> option.
     *
     * @param shutdownWaitMillis The new ShutdownWaitMillis option.
     */
    public void setShutdownWaitMillis(long shutdownWaitMillis) {
        this.shutdownWaitMillis = shutdownWaitMillis;
    }

    /**
     * The <b>StopWatchParserClassName</b> option is used to determine the class used to parse stop watch messages
     * into StopWatch instances. This defaults to the standard "org.perf4j.helpers.StopWatchParser" class.
     *
     * @return The StopWatchParserClassName option.
     */
    public String getStopWatchParserClassName() {
        return stopWatchParserClassName;
    }

    /**
     * Sets the value of the <b>StopWatchParserClassName</b> option.
     *
     * @param stopWatchParserClassName The new StopWatchParserClassName option.
     */
    public void setStopWatchParserClassName(String stopWatchParserClassName) {
        this.stopWatchParserClassName = stopWatchParserClassName;
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

    // --- main lifecycle methods ---
    /**
     * The start method should only be called once, before the append method is called, to initialize options.
     *
     * @param handler The GroupedTimingStatisticsHandler used to process GroupedTimingStatistics created by aggregating
     *                StopWatch log message.
     */
    public void start(GroupedTimingStatisticsHandler handler) {
        //start should only be called once, but just in case:
        if (drainingThread != null) {
            stopDrainingThread();
        }

        this.handler = handler;
        stopWatchParser = newStopWatchParser();
        numDiscardedMessages = 0;
        loggedMessages = new ArrayBlockingQueue<String>(getQueueSize());

        drainingThread = new Thread(new Dispatcher(), "perf4j-async-stats-appender-sink-" + getName());
        drainingThread.setDaemon(true);
        drainingThread.start();
    }

    /**
     * The append method should be called each time a StopWatch log message is handled by the logging framework.
     *
     * @param message The log message, may not be null. If this message is not a valid StopWatch log message it will
     *        be discarded.
     */
    public void append(String message) {
        //Do a quick check to cull out any messages not meant for us
        if (stopWatchParser.isPotentiallyValid(message)) {
            if (!loggedMessages.offer(message)) {
                ++numDiscardedMessages;
                handler.error(message);
            }
        }
    }

    /**
     * This method should be called on shutdown to flush any pending messages in the queue and create a final
     * GroupedTimingStatistics instance if necessary.
     */
    public void stop() {
        stopDrainingThread();
    }

    // --- Helper Methods ---
    /**
     * Helper method stops the draining thread and waits for it to finish.
     */
    private void stopDrainingThread() {
        try {
            //pushing an empty string on the queue tells the draining thread that we're closing
            loggedMessages.put("");
            //wait for the draining thread to finish
            drainingThread.join(shutdownWaitMillis);
            drainingThread.interrupt();
            if (loggedMessages.size() > 0) {
                handler.error("Shutdown, queued/undrained stopwatch count: " + loggedMessages.size());
            }
        } catch (Exception e) {
            handler.error("Unexpected error stopping AsyncCoalescingStatisticsAppender draining thread: "
                    + e.getMessage());
        } finally {
            loggedMessages.clear();
        }
    }

    /**
     * Helper method instantiates a new StopWatchParser based on the StopWatchParserClassName option.
     *
     * @return The newly created StopWatchParser
     */
    private StopWatchParser newStopWatchParser() {
        try {
            return (StopWatchParser) Class.forName(stopWatchParserClassName).newInstance();
        } catch (Exception e) {
            throw new RuntimeException("Could not create StopWatchParser: " + e.getMessage(), e);
        }
    }

    // --- Support Classes ---
    /**
     * This Dispatcher Runnable uses a StopWatchesFromQueueIterator to pull StopWatch logging message off the
     * loggedMessages queue, which are grouped to create GroupedTimingStatistics by the GroupingStatisticsIterator.
     * The GroupedTimingStatisticsHandler is then called to deal with the created GroupedTimingStatistics.
     */
    private class Dispatcher implements Runnable {
        public void run() {
            GroupingStatisticsIterator statsIterator =
                    new GroupingStatisticsIterator(new StopWatchesFromQueueIterator(),
                                                   timeSlice,
                                                   createRollupStatistics);

            while (statsIterator.hasNext()) {
                try {
                    handler.handle(statsIterator.next());
                } catch (Exception e) {
                    handler.error("Error calling the GroupedTimingStatisticsHandler: " + e.getMessage());
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
