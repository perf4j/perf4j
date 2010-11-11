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

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.perf4j.LoggingStopWatch;

/**
 * This LoggingStopWatch uses a log4j Logger to persist the StopWatch messages. The various constructors allow you
 * to specify the Logger to use (defaults to org.perf4j.TimingLogger), the Level at which messages are normally logged
 * (defaults to INFO) and the Level used for logging if one of the stop or lap methods that takes an exception is
 * called (defaults to WARN).
 *
 * @author Alex Devine
 */
@SuppressWarnings("serial")
public class Log4JStopWatch extends LoggingStopWatch {
    private transient Logger logger;
    private Level normalPriority;
    private Level exceptionPriority;

    // --- Constructors ---

    /**
     * Creates a Log4JStopWatch with a blank tag, no message and started at the instant of creation. The Logger
     * with the name "org.perf4j.TimingLogger" is used to log stop watch messages at the INFO level, or at the WARN
     * level if an exception is passed to one of the stop or lap methods.
     */
    public Log4JStopWatch() {
        this("", null, Logger.getLogger(DEFAULT_LOGGER_NAME), Level.INFO, Level.WARN);
    }

    /**
     * Creates a Log4JStopWatch with a blank tag, no message and started at the instant of creation, using the
     * specified Logger to log stop watch messages at the INFO level, or at the WARN
     * level if an exception is passed to one of the stop or lap methods.
     *
     * @param logger The Logger to use when persisting StopWatches in one of the stop or lap methods.
     */
    public Log4JStopWatch(Logger logger) {
        this("", null, logger, Level.INFO, Level.WARN);
    }

    /**
     * Creates a Log4JStopWatch with a blank tag, no message and started at the instant of creation, using the
     * specified Logger to log stop watch messages at the normalPriority level specified, or at the WARN
     * level if an exception is passed to one of the stop or lap methods.
     *
     * @param logger         The Logger to use when persisting StopWatches in one of the stop or lap methods.
     * @param normalPriority The level at which this StopWatch is logged if one of the stop or lap methods that does
     *                       NOT take an exception is called.
     */
    public Log4JStopWatch(Logger logger, Level normalPriority) {
        this("", null, logger, normalPriority, Level.WARN);
    }

    /**
     * Creates a Log4JStopWatch with a blank tag, no message and started at the instant of creation, using the
     * specified Logger to log stop watch messages at the normalPriority level specified, or at the exceptionPriority
     * level if an exception is passed to one of the stop or lap methods.
     *
     * @param logger            The Logger to use when persisting StopWatches in one of the stop or lap methods.
     * @param normalPriority    The level at which this StopWatch is logged if one of the stop or lap methods that does
     *                          NOT take an exception is called.
     * @param exceptionPriority The level at which this StopWatch is logged if one of the stop or lap methods that DOES
     *                          take an exception is called.
     */
    public Log4JStopWatch(Logger logger, Level normalPriority, Level exceptionPriority) {
        this("", null, logger, normalPriority, exceptionPriority);
    }

    /**
     * Creates a Log4JStopWatch with the tag specified, no message and started at the instant of creation. The Logger
     * with the name "org.perf4j.TimingLogger" is used to log stop watch messages at the INFO level, or at the WARN
     * level if an exception is passed to one of the stop or lap methods.
     *
     * @param tag The tag name for this timing call. Tags are used to group timing logs, thus each block
     *            of code being timed should have a unique tag. Note that tags can take a hierarchical
     *            format using dot notation.
     */
    public Log4JStopWatch(String tag) {
        this(tag, null, Logger.getLogger(DEFAULT_LOGGER_NAME), Level.INFO, Level.WARN);
    }

    /**
     * Creates a Log4JStopWatch with the tag specified, no message and started at the instant of creation, using the
     * specified Logger to log stop watch messages at INFO level, or at the WARN level if an exception is passed to
     * one of the stop or lap methods.
     *
     * @param tag    The tag name for this timing call. Tags are used to group timing logs, thus each block
     *               of code being timed should have a unique tag. Note that tags can take a hierarchical
     *               format using dot notation.
     * @param logger The Logger to use when persisting StopWatches in one of the stop or lap methods.
     */
    public Log4JStopWatch(String tag, Logger logger) {
        this(tag, null, logger, Level.INFO, Level.WARN);
    }

    /**
     * Creates a Log4JStopWatch with the tag specified, no message and started at the instant of creation, using the
     * specified Logger to log stop watch messages at the normalPriority level specified, or at the WARN
     * level if an exception is passed to one of the stop or lap methods.
     *
     * @param tag            The tag name for this timing call. Tags are used to group timing logs, thus each block
     *                       of code being timed should have a unique tag. Note that tags can take a hierarchical
     *                       format using dot notation.
     * @param logger         The Logger to use when persisting StopWatches in one of the stop or lap methods.
     * @param normalPriority The level at which this StopWatch is logged if one of the stop or lap methods that does
     *                       NOT take an exception is called.
     */
    public Log4JStopWatch(String tag, Logger logger, Level normalPriority) {
        this(tag, null, logger, normalPriority, Level.WARN);
    }

    /**
     * Creates a Log4JStopWatch with the tag specified, no message and started at the instant of creation, using the
     * specified Logger to log stop watch messages at the normalPriority level specified, or at the exceptionPriority
     * level if an exception is passed to one of the stop or lap methods.
     *
     * @param tag               The tag name for this timing call. Tags are used to group timing logs, thus each block
     *                          of code being timed should have a unique tag. Note that tags can take a hierarchical
     *                          format using dot notation.
     * @param logger            The Logger to use when persisting StopWatches in one of the stop or lap methods.
     * @param normalPriority    The level at which this StopWatch is logged if one of the stop or lap methods that does
     *                          NOT take an exception is called.
     * @param exceptionPriority The level at which this StopWatch is logged if one of the stop or lap methods that DOES
     *                          take an exception is called.
     */
    public Log4JStopWatch(String tag, Logger logger, Level normalPriority, Level exceptionPriority) {
        this(tag, null, logger, normalPriority, exceptionPriority);
    }

    /**
     * Creates a Log4JStopWatch with the tag and message specified and started at the instant of creation. The Logger
     * with the name "org.perf4j.TimingLogger" is used to log stop watch messages at the INFO level, or at the WARN
     * level if an exception is passed to one of the stop or lap methods.
     *
     * @param tag     The tag name for this timing call. Tags are used to group timing logs, thus each block
     *                of code being timed should have a unique tag. Note that tags can take a hierarchical
     *                format using dot notation.
     * @param message Additional text to be printed with the logging statement of this StopWatch.
     */
    public Log4JStopWatch(String tag, String message) {
        this(tag, message, Logger.getLogger(DEFAULT_LOGGER_NAME), Level.INFO, Level.WARN);
    }

    /**
     * Creates a Log4JStopWatch with the tag and message specified and started at the instant of creation, using the
     * specified Logger to log stop watch messages at INFO level, or at WARN level if an exception is passed to one
     * of the stop or lap methods.
     *
     * @param tag     The tag name for this timing call. Tags are used to group timing logs, thus each block
     *                of code being timed should have a unique tag. Note that tags can take a hierarchical
     *                format using dot notation.
     * @param message Additional text to be printed with the logging statement of this StopWatch.
     * @param logger  The Logger to use when persisting StopWatches in one of the stop or lap methods.
     */
    public Log4JStopWatch(String tag, String message, Logger logger) {
        this(tag, message, logger, Level.INFO, Level.WARN);
    }

    /**
     * Creates a Log4JStopWatch with the tag and message specified and started at the instant of creation, using the
     * specified Logger to log stop watch messages at the normalPriority level specified, or at WARN
     * level if an exception is passed to one of the stop or lap methods.
     *
     * @param tag            The tag name for this timing call. Tags are used to group timing logs, thus each block
     *                       of code being timed should have a unique tag. Note that tags can take a hierarchical
     *                       format using dot notation.
     * @param message        Additional text to be printed with the logging statement of this StopWatch.
     * @param logger         The Logger to use when persisting StopWatches in one of the stop or lap methods.
     * @param normalPriority The level at which this StopWatch is logged if one of the stop or lap methods that does
     *                       NOT take an exception is called.
     */
    public Log4JStopWatch(String tag, String message, Logger logger, Level normalPriority) {
        this(tag, message, logger, normalPriority, Level.WARN);
    }

    /**
     * Creates a Log4JStopWatch with the tag and message specified and started at the instant of creation, using the
     * specified Logger to log stop watch messages at the normalPriority level specified, or at the exceptionPriority
     * level if an exception is passed to one of the stop or lap methods.
     *
     * @param tag               The tag name for this timing call. Tags are used to group timing logs, thus each block
     *                          of code being timed should have a unique tag. Note that tags can take a hierarchical
     *                          format using dot notation.
     * @param message           Additional text to be printed with the logging statement of this StopWatch.
     * @param logger            The Logger to use when persisting StopWatches in one of the stop or lap methods.
     * @param normalPriority    The level at which this StopWatch is logged if one of the stop or lap methods that does
     *                          NOT take an exception is called.
     * @param exceptionPriority The level at which this StopWatch is logged if one of the stop or lap methods that DOES
     *                          take an exception is called.
     */
    public Log4JStopWatch(String tag, String message, Logger logger, Level normalPriority, Level exceptionPriority) {
        this(System.currentTimeMillis(), -1L, tag, message, logger, normalPriority, exceptionPriority);
    }

    /**
     * This constructor is mainly used for creation of StopWatch instances from logs and for testing. Users should
     * normally not call this constructor in client code.
     *
     * @param startTime         The start time in milliseconds
     * @param elapsedTime       The elapsed time in milliseconds
     * @param tag               The tag used to group timing logs of the same code block
     * @param message           Additional message text
     * @param logger            The Logger to use when persisting StopWatches in one of the stop or lap methods.
     * @param normalPriority    The level at which this StopWatch is logged if one of the stop or lap methods that does
     *                          NOT take an exception is called.
     * @param exceptionPriority The level at which this StopWatch is logged if one of the stop or lap methods that DOES
     *                          take an exception is called.
     */
    public Log4JStopWatch(long startTime, long elapsedTime, String tag, String message,
                          Logger logger, Level normalPriority, Level exceptionPriority) {
        super(startTime, elapsedTime, tag, message);
        this.logger = logger;
        this.normalPriority = normalPriority;
        this.exceptionPriority = exceptionPriority;
    }

    // --- Bean Methods ---

    /**
     * Gets the log4j Logger that is used to persist logging statements when one of the stop or lap methods is called.
     *
     * @return The Logger used for StopWatch persistence.
     */
    public Logger getLogger() { return logger; }

    /**
     * Sets the log4j Logger used to persist StopWatch instances.
     *
     * @param logger The Logger this instance should use for persistence. May not be null.
     * @return this instance, for use with method chaining if desired
     */
    public Log4JStopWatch setLogger(Logger logger) {
        this.logger = logger;
        return this;
    }

    /**
     * Gets the Level at which log statements will be made when one of the stop or lap methods that does NOT take an
     * exception is called.
     *
     * @return The Level used when logging "normal" stop or lap calls.
     */
    public Level getNormalPriority() { return normalPriority; }

    /**
     * Sets the Level at which log statements will be made when one of the stop or lap methods that does NOT take an
     * exception is called.
     *
     * @param normalPriority The Level used when logging "normal" stop or lap calls. May not be null.
     * @return this instance, for use with method chaining if desired
     */
    public Log4JStopWatch setNormalPriority(Level normalPriority) {
        this.normalPriority = normalPriority;
        return this;
    }

    /**
     * Gets the Level at which log statements will be made when one of the stop or lap methods that DOES take an
     * exception is called.
     *
     * @return The Level used when logging "exception" stop or lap calls.
     */
    public Level getExceptionPriority() { return exceptionPriority; }

    /**
     * Sets the Level at which log statements will be made when one of the stop or lap methods that DOES take an
     * exception is called. This should usually be at a level equal to or higher than the normal priority.
     *
     * @param exceptionPriority The Level used when logging "exceptional" stop or lap calls. May not be null.
     * @return this instance, for use with method chaining if desired
     */
    public Log4JStopWatch setExceptionPriority(Level exceptionPriority) {
        this.exceptionPriority = exceptionPriority;
        return this;
    }

    // Just overridden to make use of covariant return types
    public Log4JStopWatch setTimeThreshold(long timeThreshold) {
        super.setTimeThreshold(timeThreshold);
        return this;
    }

    // Just overridden to make use of covariant return types
    public Log4JStopWatch setTag(String tag) {
        super.setTag(tag);
        return this;
    }

    // Just overridden to make use of covariant return types
    public Log4JStopWatch setMessage(String message) {
        super.setMessage(message);
        return this;
    }
    
    // Just overridden to make use of covariant return types
    public Log4JStopWatch setNormalAndSlowSuffixesEnabled(boolean normalAndSlowSuffixesEnabled) {
    	super.setNormalAndSlowSuffixesEnabled(normalAndSlowSuffixesEnabled);
    	return this;
    }
    
    // Just overridden to make use of covariant return types
    public Log4JStopWatch setNormalSuffix(String normalSuffix) {
    	super.setNormalSuffix(normalSuffix);
    	return this;
    }
    
    // Just overridden to make use of covariant return types
    public Log4JStopWatch setSlowSuffix(String slowSuffix) {
    	super.setSlowSuffix(slowSuffix);
    	return this;
    }

    // --- Helper Methods ---
    /**
     * This method returns true if the logger it uses is enabled at the normalPriority level of this StopWatch.
     *
     * @return true if this StopWatch will output log messages when one of the stop or lap messages that does NOT
     *         take an exception is called.
     */
    public boolean isLogging() {
        return logger.isEnabledFor(normalPriority);
    }

    /**
     * The log message is overridden to use the log4j Logger to persist the stop watch.
     *
     * @param stopWatchAsString The stringified view of the stop watch for logging.
     * @param exception         An exception, if any, that was passed to the stop or lap method. If this is null then
     *                          logging will occur at normalPriority, if non-null it will occur at exceptionPriority.
     */
    protected void log(String stopWatchAsString, Throwable exception) {
        logger.log((exception == null) ? normalPriority : exceptionPriority, stopWatchAsString, exception);
    }

    // --- Object Methods ---

    public Log4JStopWatch clone() {
        return (Log4JStopWatch) super.clone();
    }

    private void writeObject(ObjectOutputStream stream) throws IOException {
        stream.defaultWriteObject();
        stream.writeUTF(logger.getName());
    }

    private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
        stream.defaultReadObject();
        this.logger = Logger.getLogger(stream.readUTF());
    }
}
