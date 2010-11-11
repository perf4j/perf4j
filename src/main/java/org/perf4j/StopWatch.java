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
package org.perf4j;

import java.io.Serializable;

/**
 * The StopWatch class is used to time code blocks in Perf4J. The general usage pattern is to create a StopWatch
 * before a section of code that is to be timed and then stop it before it is passed to a logging method:
 * <p/>
 * <pre>
 * StopWatch stopWatch = new StopWatch();
 * try {
 *     ...code being timed...
 *     log.info(stopWatch.stop("methodBeingTimed.success"));
 * } catch (Exception e) {
 *     log.error(stopWatch.stop("methodBeingTimed.fail"), e);
 * }
 * </pre>
 * <p/>
 * Note that a StopWatch is reusable. That is, you can call <tt>start()</tt> and <tt>stop()</tt> in succession
 * and the <tt>getElapsedTime()</tt> method will refer to the time since the most recent <tt>start()</tt> call.
 * <p/>
 * In general, most clients will find it simpler and cleaner to use the {@link LoggingStopWatch} class or one of its
 * subclasses in preference to this class.
 *
 * @author Alex Devine
 */
public class StopWatch implements Serializable, Cloneable {
	private static final long serialVersionUID = 8453041765823416495L;

	public static final String DEFAULT_LOGGER_NAME = "org.perf4j.TimingLogger";

    private static final long NANOS_IN_A_MILLI = 1000000L;

    private long startTime;
    private long nanoStartTime;
    private long elapsedTime;
    private String tag;
    private String message;

    /**
     * Creates a StopWatch with a blank tag, no message and started at the instant of creation.
     */
    public StopWatch() {
        this("", null);
    }

    /**
     * Creates a StopWatch with the specified tag, no message and started at the instant of creation.
     *
     * @param tag The tag name for this timing call. Tags are used to group timing logs, thus each block of code being
     *            timed should have a unique tag. Note that tags can take a hierarchical format using dot notation.
     */
    public StopWatch(String tag) {
        this(tag, null);
    }

    /**
     * Creates a StopWatch with the specified tag and message, started an the instant of creation.
     *
     * @param tag     The tag name for this timing call. Tags are used to group timing logs, thus each block of code
     *                being timed should have a unique tag. Note that tags can take a hierarchical format using dot
     *                notation.
     * @param message Additional text to be printed with the logging statement of this StopWatch.
     */
    public StopWatch(String tag, String message) {
        this(System.currentTimeMillis(), -1L, tag, message);
    }

    /**
     * Creates a StopWatch with a specified start and elapsed time, tag, and message. This constructor should normally
     * not be called by third party code; it is intended to allow for deserialization of StopWatch logs.
     *
     * @param startTime   The start time in milliseconds
     * @param elapsedTime The elapsed time in milliseconds
     * @param tag         The tag used to group timing logs of the same code block
     * @param message     Additional message text
     */
    public StopWatch(long startTime, long elapsedTime, String tag, String message) {
        this.startTime = startTime;
        this.nanoStartTime = (elapsedTime == -1L) ? System.nanoTime() : -1L;
        this.elapsedTime = elapsedTime;
        this.tag = tag;
        this.message = message;
    }

    // --- Bean Properties ---

    /**
     * Gets the time when this instance was created, or when one of the <tt>start()</tt> messages was last called.
     *
     * @return The start time in milliseconds since the epoch.
     */
    public long getStartTime() { return startTime; }

    /**
     * Gets the time in milliseconds between when this StopWatch was last started and stopped. Is <tt>stop()</tt> was
     * not called, then the time returned is the time since the StopWatch was started.
     *
     * @return The elapsed time in milliseconds.
     */
    public long getElapsedTime() {
        return (elapsedTime == -1L) ?
               (System.nanoTime() - nanoStartTime) / NANOS_IN_A_MILLI :
               elapsedTime;
    }

    /**
     * Gets the tag used to group this StopWatch instance with other instances used to time the same code block.
     *
     * @return The grouping tag.
     */
    public String getTag() { return tag; }

    /**
     * Sets the grouping tag for this StopWatch instance.
     *
     * @param tag The grouping tag.
     * @return this instance, for method chaining if desired
     */
    public StopWatch setTag(String tag) {
        this.tag = tag;
        return this;
    }

    /**
     * Gets any additional message that was set on this StopWatch instance.
     *
     * @return The message associated with this StopWatch, which may be null.
     */
    public String getMessage() { return message; }

    /**
     * Sends a message on this StopWatch instance to be printed when this instance is logged.
     *
     * @param message The message associated with this StopWatch, which may be null.
     * @return this instance, for method chaining if desired.
     */
    public StopWatch setMessage(String message) {
        this.message = message;
        return this;
    }

    // --- Start/Stop/Lap methods ---

    /**
     * Starts this StopWatch, which sets its startTime property to the current time and resets the elapsedTime property.
     * For single-use StopWatch instance you should not need to call this method as a StopWatch is automatically
     * started when it is created. Note any existing tag and message are not changed.
     */
    public void start() {
        startTime = System.currentTimeMillis();
        nanoStartTime = System.nanoTime();
        elapsedTime = -1L;
    }

    /**
     * Starts this StopWatch and sets its tag to the specified value. For single-use StopWatch instance you should
     * not need to call this method as a StopWatch is automatically started when it is created. Note any existing
     * message on this StopWatch is not changed.
     *
     * @param tag The grouping tag for this StopWatch
     */
    public void start(String tag) {
        start();
        this.tag = tag;
    }

    /**
     * Starts this StopWatch and sets its tag and message to the specified values. For single-use StopWatch instance
     * you should not need to call this method as a StopWatch is automatically started when it is created.
     *
     * @param tag     The grouping tag for this StopWatch
     * @param message A descriptive message about the code being timed, may be null
     */
    public void start(String tag, String message) {
        start();
        this.tag = tag;
        this.message = message;
    }

    /**
     * Stops this StopWatch, which "freezes" its elapsed time. You should normally call this method (or one of the
     * other stop methods) before passing this instance to a logger.
     *
     * @return this.toString(), which is a message suitable for logging
     */
    public String stop() {
        elapsedTime = (System.nanoTime() - nanoStartTime) / NANOS_IN_A_MILLI;
        return this.toString();
    }

    /**
     * Stops this StopWatch and sets its grouping tag.
     *
     * @param tag The grouping tag for this StopWatch
     * @return this.toString(), which is a message suitable for logging
     */
    public String stop(String tag) {
        this.tag = tag;
        return stop();
    }

    /**
     * Stops this StopWatch and sets its grouping tag and message.
     *
     * @param tag     The grouping tag for this StopWatch
     * @param message A descriptive message about the code being timed, may be null
     * @return this.toString(), which is a message suitable for logging
     */
    public String stop(String tag, String message) {
        this.tag = tag;
        this.message = message;
        return stop();
    }

    /**
     * The lap method is useful when using a single StopWatch to time multiple consecutive blocks. It calls stop()
     * and then immediately calls start(), e.g.:
     * <p/>
     * <pre>
     * StopWatch stopWatch = new StopWatch();
     * ...some code
     * log.info(stopWatch.lap("block1"));
     * ...some more code
     * log.info(stopWatch.lap("block2"));
     * ...even more code
     * log.info(stopWatch.stop("block3"));
     * </pre>
     *
     * @param tag The grouping tag to use for the execution block that was just stopped.
     * @return A message suitable for logging the previous execution block's execution time.
     */
    public String lap(String tag) {
        String retVal = stop(tag);
        start();
        return retVal;
    }

    /**
     * The lap method is useful when using a single StopWatch to time multiple consecutive blocks. It calls stop()
     * and then immediately calls start(), e.g.:
     * <p/>
     * <pre>
     * StopWatch stopWatch = new StopWatch();
     * ...some code
     * log.info(stopWatch.lap("block1", "message about block 1"));
     * ...some more code
     * log.info(stopWatch.lap("block2", "message about block 2"));
     * ...even more code
     * log.info(stopWatch.stop("block3", "message about block 3"));
     * </pre>
     *
     * @param tag     The grouping tag to use for the execution block that was just stopped.
     * @param message A descriptive message about the code being timed, may be null
     * @return A message suitable for logging the previous execution block's execution time.
     */
    public String lap(String tag, String message) {
        String retVal = stop(tag, message);
        start();
        return retVal;
    }

    // --- Object Methods ---
    
    public String toString() {
    	String message = getMessage();
    	return "start[" + getStartTime() +
        "] time[" + getElapsedTime() +
        "] tag[" + getTag() +
        ((message == null) ? "]" : "] message[" + message + "]");
    }

    public StopWatch clone() {
        try {
            return (StopWatch) super.clone();
        } catch (CloneNotSupportedException cnse) {
            throw new Error("Unexpected CloneNotSupportedException");
        }
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof StopWatch)) {
            return false;
        }

        StopWatch stopWatch = (StopWatch) o;

        if (elapsedTime != stopWatch.elapsedTime) {
            return false;
        }
        if (startTime != stopWatch.startTime) {
            return false;
        }
        if (nanoStartTime != stopWatch.nanoStartTime) {
            return false;
        }
        if (message != null ? !message.equals(stopWatch.message) : stopWatch.message != null) {
            return false;
        }
        if (tag != null ? !tag.equals(stopWatch.tag) : stopWatch.tag != null) {
            return false;
        }

        return true;
    }

    public int hashCode() {
        int result = (int) (startTime ^ (startTime >>> 32));
        result = 31 * result + (int) (nanoStartTime ^ (nanoStartTime >>> 32));
        result = 31 * result + (int) (elapsedTime ^ (elapsedTime >>> 32));
        result = 31 * result + (tag != null ? tag.hashCode() : 0);
        result = 31 * result + (message != null ? message.hashCode() : 0);
        return result;
    }
}
