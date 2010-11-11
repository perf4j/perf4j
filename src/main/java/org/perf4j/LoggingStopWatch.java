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

/**
 * A LoggingStopWatch prevents the need to explicitly send the StopWatch string to a Logger when stopping. Instead,
 * all of the stop() and lap() methods themselves are responsible for persisting the StopWatch:
 * <pre>
 * LoggingStopWatch stopWatch = new LoggingStopWatch();
 * ...some code
 * stopWatch.stop("codeBlock1"); //calling stop writes the StopWatch string to std err.
 * </pre>
 * This class just writes all StopWatch messages to the standard error stream, but subclasses will use Loggers from
 * various logging frameworks to persist the StopWatch.
 *
 * @author Alex Devine
 */
@SuppressWarnings("serial")
public class LoggingStopWatch extends StopWatch {
	/**
     * This threshold determines if a log call will be made. Only elapsed times greater than this amount will be logged.
     */
    private long timeThreshold = 0L;
    
    /**
     * Set this to true if both normal and slow suffixes should be appended to every StopWatch tag.
     */
    private boolean normalAndSlowSuffixesEnabled = false;
    
    /**
     * If normalAndSlowSuffixesEnabled == true then this suffix will be appended to the tag for elapsedTimes < timeThreshold.
     * If normalAndSlowSuffixesEnabled == true and timeThreshold == 0 then this suffix will ALWAYS be appended to the tag.
     */
    private String normalSuffix = ".normal";
    
    /**
     * If normalAndSlowSuffixesEnabled == true then this suffix will be appended to the tag for elapsedTimes >= timeThreshold.
     * If normalAndSlowSuffixesEnabled == true and timeThreshold == 0 then this suffix will NEVER be appended to the tag.
     */
    private String slowSuffix = ".slow";

    // --- Constructors ---

    /**
     * Creates a LoggingStopWatch with a blank tag, no message and started at the instant of creation.
     */
    public LoggingStopWatch() {
        super();
    }

    /**
     * Creates a LoggingStopWatch with the specified tag, no message and started at the instant of creation.
     *
     * @param tag The tag name for this timing call. Tags are used to group timing logs, thus each block of code being
     *            timed should have a unique tag. Note that tags can take a hierarchical format using dot notation.
     */
    public LoggingStopWatch(String tag) {
        super(tag);
    }

    /**
     * Creates a LoggingStopWatch with the specified tag and message, started an the instant of creation.
     *
     * @param tag     The tag name for this timing call. Tags are used to group timing logs, thus each block of code
     *                being timed should have a unique tag. Note that tags can take a hierarchical format using dot
     *                notation.
     * @param message Additional text to be printed with the logging statement of this LoggingStopWatch.
     */
    public LoggingStopWatch(String tag, String message) {
        super(tag, message);
    }

    /**
     * Creates a LoggingStopWatch with a specified start and elapsed time, tag, and message. This constructor should
     * normally not be called by third party code; it is intended to allow for deserialization of StopWatch logs.
     *
     * @param startTime   The start time in milliseconds
     * @param elapsedTime The elapsed time in milliseconds
     * @param tag         The tag used to group timing logs of the same code block
     * @param message     Additional message text
     */
    public LoggingStopWatch(long startTime, long elapsedTime, String tag, String message) {
        super(startTime, elapsedTime, tag, message);
    }

    // --- Bean Properties ---
    /**
     * Gets a threshold level, in milliseconds, below which logging calls will not be made. Defaults to 0, meaning that
     * the log method is always called on stop or lap regardless of the elapsed time.
     *
     * @return The time threshold in milliseconds.
     */
    public long getTimeThreshold() {
        return timeThreshold;
    }

    /**
     * Sets a threshold level, in milliseconds, below which logging calls will not be made. You can set this to a
     * high positive value if you only want logging to occur for abnormally slow execution times. Note, though, that
     * you may wish to leave the threshold at 0 and attach a
     * {@link org.perf4j.log4j.JmxAttributeStatisticsAppender} in the logging configuration to be notified when
     * times are outside acceptable thresholds.
     *
     * @param timeThreshold The minimum elapsed time, in milliseconds, below which log calls will not be made.
     * @return this instance, for use with method chaining if desired
     * @see org.perf4j.log4j.JmxAttributeStatisticsAppender#getNotificationThresholds()
     */
    public LoggingStopWatch setTimeThreshold(long timeThreshold) {
        this.timeThreshold = timeThreshold;
        return this;
    }
    
    /**
     * Determines whether or not to append normalSuffix or slowSuffix to every tag logged by this stopwatch.
     * @return whether or not to append normalSuffix or slowSuffix to every tag logged by this stopwatch.
     */
    public boolean isNormalAndSlowSuffixesEnabled() {
		return normalAndSlowSuffixesEnabled;
	}
    
    /**
     * Sets whether to append normalSuffix and slowSuffix when timeThreshold &gt; 0 AND elapsedTime &gt;= timeThreshold
     * @param normalAndSlowSuffixesEnabled true enables logging extra suffixes to normal and slow events; false (default) suppresses the suffixes 
     */
    public LoggingStopWatch setNormalAndSlowSuffixesEnabled(boolean normalAndSlowSuffixesEnabled) {
		this.normalAndSlowSuffixesEnabled = normalAndSlowSuffixesEnabled;
		return this;
	}
    
    /**
     * The suffix to append to the tag if normalAndSlowSuffixesEnabled=true and elapsedTime &lt; timeThreshold and timeThreshold &gt; 0.
     * Default is ".normal".
     * @return the suffix to append if normalAndSlowSuffixesEnabled=true and the event was normal and under the threshold
     */
    public String getNormalSuffix() {
		return normalSuffix;
	}
    
    /**
     * Sets the suffix to use when normalAndSlowSuffixesEnabled=true and timeThreshold &gt; 0 and elapsedTime &lt; timeThreshold.
     * Setting this to "" is equivalent to setting to null.
     * @param normalSuffix the suffix to append if normalAndSlowSuffixesEnabled and the elapsedtime is under the threshold
     */
    public LoggingStopWatch setNormalSuffix(String normalSuffix) {
    	if (normalSuffix == null || "".equals(normalSuffix)) {
    		throw new IllegalArgumentException("normalSuffix cannot be blank. param=" + normalSuffix);
    	}
		this.normalSuffix = normalSuffix;
		return this;
	}
    
    /**
     * The suffix to append to the tag if normalAndSlowSuffixesEnabled=true and elapsedTime &gt;= timeThreshold and timeThreshold &gt; 0.
     * Default is ".slow"
     * @return the suffix to append if normalAndSlowSuffixesEnabled=true and the event was slow and over the threshold.
     */
    public String getSlowSuffix() {
		return slowSuffix;
	}
    
    /**
     * Sets the suffix to use when normalAndSlowSuffixesEnabled=true and timeThreshold &gt;  0 and elapsedTime &gt;= timeThreshold.
     * Setting this to "" is equivalent to setting to null.
     * @param slowSuffix the suffix to append if normalAndSlowSuffixesEnabled and the elapsedtime is under the threshold
     */
    public LoggingStopWatch setSlowSuffix(String slowSuffix) {
    	if (slowSuffix == null || "".equals(slowSuffix)) {
    		throw new IllegalArgumentException("slowSuffix cannot be blank. param=" + slowSuffix);
    	}
		this.slowSuffix = slowSuffix;
		return this;
	}
    
    /* 
     * If normalAndSlowSuffixesEnabled AND timeThreshold >0 AND elapsedTime >= timeThreshold
     * then append slow suffix.<br/>
     * If normalAndSlowSuffixesEnabled AND (timeThreshold <=0 OR elapsedTime < timeThreshold)
     * then append normal suffix.<br/>
     * Otherwise, use the superclass's tag.
     */
    public String getTag() {
    	long timeThreshold = getTimeThreshold(); // so that child classes can override
    	return isNormalAndSlowSuffixesEnabled() ? 
                super.getTag() + (getElapsedTime() >= timeThreshold ? getSlowSuffix() : getNormalSuffix()) : 
                super.getTag(); 
    }

    // Just overridden to make use of covariant return types
    public LoggingStopWatch setTag(String tag) {
        super.setTag(tag);
        return this;
    }

    // Just overridden to make use of covariant return types
    public LoggingStopWatch setMessage(String message) {
        super.setMessage(message);
        return this;
    }

    // --- Stop/Lap/Helper Methods ---
    /**
     * This stop method is overridden to perform the logging itself instead of needing to make a separate call to
     * persist the timing information.
     *
     * @return this.toString(), however, this should not be passed to a logger as it will have already been logged.
     */
    public String stop() {
        String retVal = super.stop();
        doLogInternal(retVal, null);
        return retVal;
    }

    /**
     * In cases where a code block terminated by throwing an exception, you may wish to have the exception logged in
     * addition to the time it took to execute the block, in which case this method will write out the exception's
     * stack trace in addition to the StopWatch timing method.
     *
     * @param exception The exception that was thrown by the timed code block
     * @return this.toString(), however, this should not be passed to a logger as it will have already been logged.
     */
    public String stop(Throwable exception) {
        String retVal = super.stop();
        doLogInternal(retVal, exception);
        return retVal;
    }

    /**
     * Identical to {@link #stop(String)}, but also allows you to specify an exception to be logged.
     *
     * @param tag       The grouping tag for this StopWatch
     * @param exception The exception that was thrown by the timed code block
     * @return this.toString(), however, this should not be passed to a logger as it will have already been logged.
     */
    public String stop(String tag, Throwable exception) {
        setTag(tag);
        return stop(exception);
    }

    /**
     * Identical to {@link #stop(String, String)}, but also allows you to specify an exception to be logged.
     *
     * @param tag       The grouping tag for this StopWatch
     * @param message   A descriptive message about the timed block
     * @param exception The exception that was thrown by the timed code block
     * @return this.toString(), however, this should not be passed to a logger as it will have already been logged.
     */
    public String stop(String tag, String message, Throwable exception) {
        setTag(tag);
        setMessage(message);
        return stop(exception);
    }

    /**
     * Identical to {@link #lap(String)}, but also allows you to specify an exception to be logged.
     *
     * @param tag       The grouping tag for the PREVIOUS code block that was timed.
     * @param exception The exception that was thrown by the timed code block.
     * @return this.toString(), however, this should not be passed to a logger as it will have already been logged.
     */
    public String lap(String tag, Throwable exception) {
        String retVal = stop(tag, exception);
        start();
        return retVal;
    }

    /**
     * Identical to {@link #lap(String, String)}, but also allows you to specify an exception to be logged.
     *
     * @param tag       The grouping tag for the PREVIOUS code block that was timed.
     * @param message   A descriptive message about the timed block
     * @param exception The exception that was thrown by the timed code block
     * @return this.toString(), however, this should not be passed to a logger as it will have already been logged.
     */
    public String lap(String tag, String message, Throwable exception) {
        String retVal = stop(tag, message, exception);
        start();
        return retVal;
    }

    /**
     * Determines whether or not logging is currently enabled for normal log messages for this StopWatch. This
     * implementation always returns true, but subclasses should override this method if logging can be disabled. For
     * example, a StopWatch that uses log4j Loggers will return false if the Logger is not currently enabled for the
     * Level at which the log method is called.
     *
     * @return true if calls to one of the stop() or lap() methods that do NOT take an exception will result in the
     *         StopWatch being written to a persisting log.
     */
    public boolean isLogging() { return true; }

    // --- Template Methods ---
    /**
     * This log method can be overridden by subclasses in order to persist the StopWatch, for example by using a
     * log4j Logger. The default implementation here just writes the StopWatch to the standard error stream.
     *
     * @param stopWatchAsString The serialized StopWatch string
     * @param exception         An exception, if any, that was also passed to the stop() or lap() methods - may be null.
     */
    protected void log(String stopWatchAsString, Throwable exception) {
        System.err.println(stopWatchAsString);
        if (exception != null) {
            exception.printStackTrace();
        }
    }

    // --- Object Methods ---
    
    public LoggingStopWatch clone() {
        return (LoggingStopWatch) super.clone();
    }

    // --- Private Methods ---
    // Helper method only calls log if elapsed time is greater than the time threshold
    private void doLogInternal(String stopWatchAsString, Throwable exception) {
    	//if normalAndSlowSuffixesEnabled then always log with the suffixes added
    	//getTag() should take care of appending the correct tag, and should already be part of stopWatchAsString
        //Otherwise we default to the backward-compatible behavior: namely:
    	//in most cases timeThreshold will be 0, so just short circuit out as fast as possible
    	long elapsedTime = getElapsedTime(); // to allow for subclasses to override this value
    	long timeThreshold = getTimeThreshold(); // to allow for subclasses to override this value
    	if (timeThreshold == 0 || isNormalAndSlowSuffixesEnabled() || elapsedTime >= timeThreshold) {
            log(stopWatchAsString, exception);
        }
    }
}
