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
package org.perf4j.javalog;

import org.perf4j.LoggingStopWatch;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * This LoggingStopWatch uses a java.util.logging Logger to persist the StopWatch messages. The various constructors
 * allow you to specify the Logger to use (defaults to org.perf4j.TimingLogger), the Level at which messages are
 * normally logged (defaults to INFO) and the Level used for logging if one of the stop or lap methods that takes an
 * exception is called (defaults to WARNING).
 *
 * @author Alex Devine
 */
@SuppressWarnings("serial")
public class JavaLogStopWatch extends LoggingStopWatch {
    private transient Logger logger;
    private Level normalPriority;
    private Level exceptionPriority;

    // --- Constructors ---

    /**
     * Creates a JavaLogStopWatch with a blank tag, no message and started at the instant of creation. The Logger
     * with the name "org.perf4j.TimingLogger" is used to log stop watch messages at the INFO level, or at the WARNING
     * level if an exception is passed to one of the stop or lap methods.
     */
    public JavaLogStopWatch() {
        this("", null, Logger.getLogger(DEFAULT_LOGGER_NAME), Level.INFO, Level.WARNING);
    }

    /**
     * Creates a JavaLogStopWatch with a blank tag, no message and started at the instant of creation, using the
     * specified Logger to log stop watch messages at the INFO level, or at the WARNING
     * level if an exception is passed to one of the stop or lap methods.
     *
     * @param logger The Logger to use when persisting StopWatches in one of the stop or lap methods.
     */
    public JavaLogStopWatch(Logger logger) {
        this("", null, logger, Level.INFO, Level.WARNING);
    }

    /**
     * Creates a JavaLogStopWatch with a blank tag, no message and started at the instant of creation, using the
     * specified Logger to log stop watch messages at the normalPriority level specified, or at the WARNING
     * level if an exception is passed to one of the stop or lap methods.
     *
     * @param logger         The Logger to use when persisting StopWatches in one of the stop or lap methods.
     * @param normalPriority The level at which this StopWatch is logged if one of the stop or lap methods that does
     *                       NOT take an exception is called.
     */
    public JavaLogStopWatch(Logger logger, Level normalPriority) {
        this("", null, logger, normalPriority, Level.WARNING);
    }

    /**
     * Creates a JavaLogStopWatch with a blank tag, no message and started at the instant of creation, using the
     * specified Logger to log stop watch messages at the normalPriority level specified, or at the exceptionPriority
     * level if an exception is passed to one of the stop or lap methods.
     *
     * @param logger            The Logger to use when persisting StopWatches in one of the stop or lap methods.
     * @param normalPriority    The level at which this StopWatch is logged if one of the stop or lap methods that does
     *                          NOT take an exception is called.
     * @param exceptionPriority The level at which this StopWatch is logged if one of the stop or lap methods that DOES
     *                          take an exception is called.
     */
    public JavaLogStopWatch(Logger logger, Level normalPriority, Level exceptionPriority) {
        this("", null, logger, normalPriority, exceptionPriority);
    }

    /**
     * Creates a JavaLogStopWatch with the tag specified, no message and started at the instant of creation. The Logger
     * with the name "org.perf4j.TimingLogger" is used to log stop watch messages at the INFO level, or at the WARNING
     * level if an exception is passed to one of the stop or lap methods.
     *
     * @param tag The tag name for this timing call. Tags are used to group timing logs, thus each block
     *            of code being timed should have a unique tag. Note that tags can take a hierarchical
     *            format using dot notation.
     */
    public JavaLogStopWatch(String tag) {
        this(tag, null, Logger.getLogger(DEFAULT_LOGGER_NAME), Level.INFO, Level.WARNING);
    }

    /**
     * Creates a JavaLogStopWatch with the tag specified, no message and started at the instant of creation, using the
     * specified Logger to log stop watch messages at INFO level, or at the WARNING level if an exception is passed to
     * one of the stop or lap methods.
     *
     * @param tag    The tag name for this timing call. Tags are used to group timing logs, thus each block
     *               of code being timed should have a unique tag. Note that tags can take a hierarchical
     *               format using dot notation.
     * @param logger The Logger to use when persisting StopWatches in one of the stop or lap methods.
     */
    public JavaLogStopWatch(String tag, Logger logger) {
        this(tag, null, logger, Level.INFO, Level.WARNING);
    }

    /**
     * Creates a JavaLogStopWatch with the tag specified, no message and started at the instant of creation, using the
     * specified Logger to log stop watch messages at the normalPriority level specified, or at the WARNING
     * level if an exception is passed to one of the stop or lap methods.
     *
     * @param tag            The tag name for this timing call. Tags are used to group timing logs, thus each block
     *                       of code being timed should have a unique tag. Note that tags can take a hierarchical
     *                       format using dot notation.
     * @param logger         The Logger to use when persisting StopWatches in one of the stop or lap methods.
     * @param normalPriority The level at which this StopWatch is logged if one of the stop or lap methods that does
     *                       NOT take an exception is called.
     */
    public JavaLogStopWatch(String tag, Logger logger, Level normalPriority) {
        this(tag, null, logger, normalPriority, Level.WARNING);
    }

    /**
     * Creates a JavaLogStopWatch with the tag specified, no message and started at the instant of creation, using the
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
    public JavaLogStopWatch(String tag, Logger logger, Level normalPriority, Level exceptionPriority) {
        this(tag, null, logger, normalPriority, exceptionPriority);
    }

    /**
     * Creates a JavaLogStopWatch with the tag and message specified and started at the instant of creation. The Logger
     * with the name "org.perf4j.TimingLogger" is used to log stop watch messages at the INFO level, or at the WARNING
     * level if an exception is passed to one of the stop or lap methods.
     *
     * @param tag     The tag name for this timing call. Tags are used to group timing logs, thus each block
     *                of code being timed should have a unique tag. Note that tags can take a hierarchical
     *                format using dot notation.
     * @param message Additional text to be printed with the logging statement of this StopWatch.
     */
    public JavaLogStopWatch(String tag, String message) {
        this(tag, message, Logger.getLogger(DEFAULT_LOGGER_NAME), Level.INFO, Level.WARNING);
    }

    /**
     * Creates a JavaLogStopWatch with the tag and message specified and started at the instant of creation, using the
     * specified Logger to log stop watch messages at INFO level, or at WARNING level if an exception is passed to one
     * of the stop or lap methods.
     *
     * @param tag     The tag name for this timing call. Tags are used to group timing logs, thus each block
     *                of code being timed should have a unique tag. Note that tags can take a hierarchical
     *                format using dot notation.
     * @param message Additional text to be printed with the logging statement of this StopWatch.
     * @param logger  The Logger to use when persisting StopWatches in one of the stop or lap methods.
     */
    public JavaLogStopWatch(String tag, String message, Logger logger) {
        this(tag, message, logger, Level.INFO, Level.WARNING);
    }

    /**
     * Creates a JavaLogStopWatch with the tag and message specified and started at the instant of creation, using the
     * specified Logger to log stop watch messages at the normalPriority level specified, or at WARNING
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
    public JavaLogStopWatch(String tag, String message, Logger logger, Level normalPriority) {
        this(tag, message, logger, normalPriority, Level.WARNING);
    }

    /**
     * Creates a JavaLogStopWatch with the tag and message specified and started at the instant of creation, using the
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
    public JavaLogStopWatch(String tag, String message, Logger logger, Level normalPriority, Level exceptionPriority) {
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
    public JavaLogStopWatch(long startTime, long elapsedTime, String tag, String message,
                            Logger logger, Level normalPriority, Level exceptionPriority) {
        super(startTime, elapsedTime, tag, message);
        this.logger = logger;
        this.normalPriority = normalPriority;
        this.exceptionPriority = exceptionPriority;
    }

    // --- Bean Methods ---

    /**
     * Gets the java.util.logging Logger that is used to persist logging statements when one of the stop or lap methods
     * is called.
     *
     * @return The Logger used for StopWatch persistence.
     */
    public Logger getLogger() { return logger; }

    /**
     * Sets the java.util.logging Logger used to persist StopWatch instances.
     *
     * @param logger The Logger this instance should use for persistence. May not be null.
     * @return this instance, for use with method chaining if desired
     */
    public JavaLogStopWatch setLogger(Logger logger) {
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
    public JavaLogStopWatch setNormalPriority(Level normalPriority) {
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
    public JavaLogStopWatch setExceptionPriority(Level exceptionPriority) {
        this.exceptionPriority = exceptionPriority;
        return this;
    }
                                                           
    // Just overridden to make use of covariant return types
    public JavaLogStopWatch setTimeThreshold(long timeThreshold) {
        super.setTimeThreshold(timeThreshold);
        return this;
    }

    // Just overridden to make use of covariant return types
    public JavaLogStopWatch setTag(String tag) {
        super.setTag(tag);
        return this;
    }

    // Just overridden to make use of covariant return types
    public JavaLogStopWatch setMessage(String message) {
        super.setMessage(message);
        return this;
    }
    
    // Just overridden to make use of covariant return types
    public JavaLogStopWatch setNormalAndSlowSuffixesEnabled(boolean normalAndSlowSuffixesEnabled) {
    	super.setNormalAndSlowSuffixesEnabled(normalAndSlowSuffixesEnabled);
    	return this;
    }
    
    // Just overridden to make use of covariant return types
    public JavaLogStopWatch setNormalSuffix(String normalSuffix) {
    	super.setNormalSuffix(normalSuffix);
    	return this;
    }
    
    // Just overridden to make use of covariant return types
    public JavaLogStopWatch setSlowSuffix(String slowSuffix) {
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
        return logger.isLoggable(normalPriority);
    }

    /**
     * The log message is overridden to use the java.util.logging Logger to persist the stop watch.
     *
     * @param stopWatchAsString The stringified view of the stop watch for logging.
     * @param exception         An exception, if any, that was passed to the stop or lap method. If this is null then
     *                          logging will occur at normalPriority, if non-null it will occur at exceptionPriority.
     */
    protected void log(String stopWatchAsString, Throwable exception) {
        logger.log((exception == null) ? normalPriority : exceptionPriority, stopWatchAsString, exception);
    }

    // --- Static Utility Methods ---
    /**
     * This utility method provides the standard mapping between log4j level names (which Perf4J uses as the standard
     * set of possible levels) to the closest corresponding java.util.logging Level.
     *
     * @param levelName The name of the logging level, should be one of TRACE, DEBUG, INFO, WARN, ERROR or FATAL.
     * @return The java.util.logging Level that is the closest match to the log4j Level name.
     */
    public static Level mapLevelName(String levelName) {
        levelName = levelName.toUpperCase();
        if ("TRACE".equals(levelName)) { return Level.FINEST; }
        if ("DEBUG".equals(levelName)) { return Level.FINE; }
        if ("INFO".equals(levelName)) { return Level.INFO; }
        if ("WARN".equals(levelName)) { return Level.WARNING; }
        if ("ERROR".equals(levelName)) { return Level.SEVERE; }
        if ("FATAL".equals(levelName)) { return Level.SEVERE; }
        return Level.INFO;
    }

    // --- Object Methods ---

    public JavaLogStopWatch clone() {
        return (JavaLogStopWatch) super.clone();
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
