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
package org.perf4j.slf4j;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.perf4j.LoggingStopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This LoggingStopWatch uses an SLF4J Logger instance to persist the StopWatch messages.
 *
 * @author Alex Devine
 */
@SuppressWarnings("serial")
public class Slf4JStopWatch extends LoggingStopWatch {
    /**
     * Specifying this level will cause the <tt>trace()</tt> method to be used for logging.
     */
    public static final int TRACE_LEVEL = 5000;
    /**
     * Specifying this level will cause the <tt>debug()</tt> method to be used for logging.
     */
    public static final int DEBUG_LEVEL = 10000;
    /**
     * Specifying this level will cause the <tt>info()</tt> method to be used for logging.
     */
    public static final int INFO_LEVEL = 20000;
    /**
     * Specifying this level will cause the <tt>warn()</tt> method to be used for logging.
     */
    public static final int WARN_LEVEL = 30000;
    /**
     * Specifying this level will cause the <tt>error()</tt> method to be used for logging.
     */
    public static final int ERROR_LEVEL = 40000;

    private transient Logger logger;
    private int normalPriority;
    private int exceptionPriority;

    // --- Constructors ---

    /**
     * Creates a Slf4JStopWatch with a blank tag, no message and started at the instant of creation. The Logger
     * with the name "org.perf4j.TimingLogger" is used to log stop watch messages using the info() method,
     * or using the warn() method if an exception is passed to one of the stop or lap methods.
     */
    public Slf4JStopWatch() {
        this("", null, LoggerFactory.getLogger(DEFAULT_LOGGER_NAME), INFO_LEVEL, WARN_LEVEL);
    }

    /**
     * Creates a Slf4JStopWatch with a blank tag, no message and started at the instant of creation, using the
     * specified Logger to log stop watch messages using the info() method, or using the warn() method if an exception
     * is passed to one of the stop or lap methods.
     *
     * @param logger The Logger to use when persisting StopWatches in one of the stop or lap methods.
     */
    public Slf4JStopWatch(Logger logger) {
        this("", null, logger, INFO_LEVEL, WARN_LEVEL);
    }

    /**
     * Creates a Slf4JStopWatch with a blank tag, no message and started at the instant of creation, using the
     * specified Logger to log stop watch messages at the normalPriority level specified, or using the warn()
     * method if an exception is passed to one of the stop or lap methods.
     *
     * @param logger         The Logger to use when persisting StopWatches in one of the stop or lap methods.
     * @param normalPriority The level at which this StopWatch is logged if one of the stop or lap methods that does
     *                       NOT take an exception is called. Should be one of the ..._LEVEL constants from this class.
     */
    public Slf4JStopWatch(Logger logger, int normalPriority) {
        this("", null, logger, normalPriority, WARN_LEVEL);
    }

    /**
     * Creates a Slf4JStopWatch with a blank tag, no message and started at the instant of creation, using the
     * specified Logger to log stop watch messages at the normalPriority level specified, or at the exceptionPriority
     * level if an exception is passed to one of the stop or lap methods.
     *
     * @param logger            The Logger to use when persisting StopWatches in one of the stop or lap methods.
     * @param normalPriority    The level at which this StopWatch is logged if one of the stop or lap methods that does
     *                          NOT take an exception is called. Should be one of the ..._LEVEL constants from this
     *                          class.
     * @param exceptionPriority The level at which this StopWatch is logged if one of the stop or lap methods that DOES
     *                          take an exception is called. Should be one of the ..._LEVEL constants from this class.
     */
    public Slf4JStopWatch(Logger logger, int normalPriority, int exceptionPriority) {
        this("", null, logger, normalPriority, exceptionPriority);
    }

    /**
     * Creates a Slf4JStopWatch with the tag specified, no message and started at the instant of creation. The Logger
     * with the name "org.perf4j.TimingLogger" is used to log stop watch messages using the info() method, or using the
     * warn() method if an exception is passed to one of the stop or lap methods.
     *
     * @param tag The tag name for this timing call. Tags are used to group timing logs, thus each block
     *            of code being timed should have a unique tag. Note that tags can take a hierarchical
     *            format using dot notation.
     */
    public Slf4JStopWatch(String tag) {
        this(tag, null, LoggerFactory.getLogger(DEFAULT_LOGGER_NAME), INFO_LEVEL, WARN_LEVEL);
    }

    /**
     * Creates a Slf4JStopWatch with the tag specified, no message and started at the instant of creation, using
     * the specified Logger to log stop watch using the info() method, or using the warn() method if an exception is
     * passed to one of the stop or lap methods.
     *
     * @param tag    The tag name for this timing call. Tags are used to group timing logs, thus each block
     *               of code being timed should have a unique tag. Note that tags can take a hierarchical
     *               format using dot notation.
     * @param logger The Logger to use when persisting StopWatches in one of the stop or lap methods.
     */
    public Slf4JStopWatch(String tag, Logger logger) {
        this(tag, null, logger, INFO_LEVEL, WARN_LEVEL);
    }

    /**
     * Creates a Slf4JStopWatch with the tag specified, no message and started at the instant of creation, using
     * the specified Logger to log stop watch messages at the normalPriority level specified, or using the warn()
     * level if an exception is passed to one of the stop or lap methods.
     *
     * @param tag            The tag name for this timing call. Tags are used to group timing logs, thus each block
     *                       of code being timed should have a unique tag. Note that tags can take a hierarchical
     *                       format using dot notation.
     * @param logger         The Logger to use when persisting StopWatches in one of the stop or lap methods.
     * @param normalPriority The level at which this StopWatch is logged if one of the stop or lap methods that does
     *                       NOT take an exception is called. Should be one of the ..._LEVEL constants from this class.
     */
    public Slf4JStopWatch(String tag, Logger logger, int normalPriority) {
        this(tag, null, logger, normalPriority, WARN_LEVEL);
    }

    /**
     * Creates a Slf4JStopWatch with the tag specified, no message and started at the instant of creation, using
     * the specified Logger to log stop watch messages at the normalPriority level specified, or at the
     * exceptionPriority level if an exception is passed to one of the stop or lap methods.
     *
     * @param tag               The tag name for this timing call. Tags are used to group timing logs, thus each block
     *                          of code being timed should have a unique tag. Note that tags can take a hierarchical
     *                          format using dot notation.
     * @param logger            The Logger to use when persisting StopWatches in one of the stop or lap methods.
     * @param normalPriority    The level at which this StopWatch is logged if one of the stop or lap methods that does
     *                          NOT take an exception is called. Should be one of the ..._LEVEL constants from this
     *                          class.
     * @param exceptionPriority The level at which this StopWatch is logged if one of the stop or lap methods that DOES
     *                          take an exception is called. Should be one of the ..._LEVEL constants from this class.
     */
    public Slf4JStopWatch(String tag, Logger logger, int normalPriority, int exceptionPriority) {
        this(tag, null, logger, normalPriority, exceptionPriority);
    }

    /**
     * Creates a Slf4JStopWatch with the tag and message specified and started at the instant of creation. The
     * Logger with the name "org.perf4j.TimingLogger" is used to log stop watch messages using the info() method,
     * or using the warn() method if an exception is passed to one of the stop or lap methods.
     *
     * @param tag     The tag name for this timing call. Tags are used to group timing logs, thus each block
     *                of code being timed should have a unique tag. Note that tags can take a hierarchical
     *                format using dot notation.
     * @param message Additional text to be printed with the logging statement of this StopWatch.
     */
    public Slf4JStopWatch(String tag, String message) {
        this(tag, message, LoggerFactory.getLogger(DEFAULT_LOGGER_NAME), INFO_LEVEL, WARN_LEVEL);
    }

    /**
     * Creates a Slf4JStopWatch with the tag and message specified and started at the instant of creation, using
     * the specified Logger to log stop watch messages using the info() method, or using the warn() method if an
     * exception is passed to one of the stop or lap methods.
     *
     * @param tag     The tag name for this timing call. Tags are used to group timing logs, thus each block
     *                of code being timed should have a unique tag. Note that tags can take a hierarchical
     *                format using dot notation.
     * @param message Additional text to be printed with the logging statement of this StopWatch.
     * @param logger  The Logger to use when persisting StopWatches in one of the stop or lap methods.
     */
    public Slf4JStopWatch(String tag, String message, Logger logger) {
        this(tag, message, logger, INFO_LEVEL, WARN_LEVEL);
    }

    /**
     * Creates a Slf4JStopWatch with the tag and message specified and started at the instant of creation, using
     * the specified Logger to log stop watch messages at the normalPriority level specified, or using the warn() method
     * if an exception is passed to one of the stop or lap methods.
     *
     * @param tag            The tag name for this timing call. Tags are used to group timing logs, thus each block
     *                       of code being timed should have a unique tag. Note that tags can take a hierarchical
     *                       format using dot notation.
     * @param message        Additional text to be printed with the logging statement of this StopWatch.
     * @param logger         The Logger to use when persisting StopWatches in one of the stop or lap methods.
     * @param normalPriority The level at which this StopWatch is logged if one of the stop or lap methods that does
     *                       NOT take an exception is called. Should be one of the ..._LEVEL constants from this class.
     */
    public Slf4JStopWatch(String tag, String message, Logger logger, int normalPriority) {
        this(tag, message, logger, normalPriority, WARN_LEVEL);
    }

    /**
     * Creates a Slf4JStopWatch with the tag and message specified and started at the instant of creation, using the
     * specified Logger to log stop watch messages at the normalPriority level specified, or at the exceptionPriority
     * level if an exception is passed to one of the stop or lap methods.
     *
     * @param tag               The tag name for this timing call. Tags are used to group timing logs, thus each block
     *                          of code being timed should have a unique tag. Note that tags can take a hierarchical
     *                          format using dot notation.
     * @param message           Additional text to be printed with the logging statement of this StopWatch.
     * @param logger            The Logger to use when persisting StopWatches in one of the stop or lap methods.
     * @param normalPriority    The level at which this StopWatch is logged if one of the stop or lap methods that does
     *                          NOT take an exception is called. Should be one of the ..._LEVEL constants from this
     *                          class.
     * @param exceptionPriority The level at which this StopWatch is logged if one of the stop or lap methods that DOES
     *                          take an exception is called. Should be one of the ..._LEVEL constants from this class.
     */
    public Slf4JStopWatch(String tag, String message, Logger logger, int normalPriority, int exceptionPriority) {
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
    public Slf4JStopWatch(long startTime, long elapsedTime, String tag, String message,
                               Logger logger, int normalPriority, int exceptionPriority) {
        super(startTime, elapsedTime, tag, message);
        this.logger = logger;
        this.normalPriority = normalPriority;
        this.exceptionPriority = exceptionPriority;
    }

    // --- Bean Methods ---

    /**
     * Gets the Apache Commons Logging Logger that is used to persist logging statements when one of the stop or lap
     * methods is called.
     *
     * @return The Logger used for StopWatch persistence.
     */
    public Logger getLogger() { return logger; }

    /**
     * Sets the Apache Commons Logging Logused to persist StopWatch instances.
     *
     * @param logger The Logger this instance should use for persistence. May not be null.
     * @return this instance, for use with method chaining if desired
     */
    public Slf4JStopWatch setLogger(Logger logger) {
        this.logger = logger;
        return this;
    }

    /**
     * Gets the level at which log statements will be made when one of the stop or lap methods that does NOT take an
     * exception is called. The value returned corresponds to one of the ..._LEVEL constants of this class.
     *
     * @return The level used when logging "normal" stop or lap calls.
     */
    public int getNormalPriority() { return normalPriority; }

    /**
     * Sets the level at which log statements will be made when one of the stop or lap methods that does NOT take an
     * exception is called.
     *
     * @param normalPriority The level used when logging "normal" stop or lap calls. This value should be one of the
     *                       ..._LEVEL constants of this class.
     * @return this instance, for use with method chaining if desired
     */
    public Slf4JStopWatch setNormalPriority(int normalPriority) {
        this.normalPriority = normalPriority;
        return this;
    }

    /**
     * Gets the level at which log statements will be made when one of the stop or lap methods that DOES take an
     * exception is called. The value returned corresponds to one of the ..._LEVEL constants of this class.
     *
     * @return The level used when logging "exceptional" stop or lap calls.
     */
    public int getExceptionPriority() { return exceptionPriority; }

    /**
     * Sets the level at which log statements will be made when one of the stop or lap methods that DOES take an
     * exception is called.
     *
     * @param exceptionPriority The level used when logging "exceptional" stop or lap calls. This value should be one
     *                          of the ..._LEVEL constants of this class.
     * @return this instance, for use with method chaining if desired
     */
    public Slf4JStopWatch setExceptionPriority(int exceptionPriority) {
        this.exceptionPriority = exceptionPriority;
        return this;
    }

    // Just overridden to make use of covariant return types
    public Slf4JStopWatch setTimeThreshold(long timeThreshold) {
        super.setTimeThreshold(timeThreshold);
        return this;
    }

    // Just overridden to make use of covariant return types
    public Slf4JStopWatch setTag(String tag) {
        super.setTag(tag);
        return this;
    }

    // Just overridden to make use of covariant return types
    public Slf4JStopWatch setMessage(String message) {
        super.setMessage(message);
        return this;
    }
    
    // Just overridden to make use of covariant return types
    public Slf4JStopWatch setNormalAndSlowSuffixesEnabled(boolean normalAndSlowSuffixesEnabled) {
    	super.setNormalAndSlowSuffixesEnabled(normalAndSlowSuffixesEnabled);
    	return this;
    }
    
    // Just overridden to make use of covariant return types
    public Slf4JStopWatch setNormalSuffix(String normalSuffix) {
    	super.setNormalSuffix(normalSuffix);
    	return this;
    }
    
    // Just overridden to make use of covariant return types
    public Slf4JStopWatch setSlowSuffix(String slowSuffix) {
    	super.setSlowSuffix(slowSuffix);
    	return this;
    }

    // --- Helper Methods ---

    /**
     * This method returns true if the Logger it uses is enabled at the normalPriority level of this StopWatch.
     *
     * @return true if this StopWatch will output log messages when one of the stop or lap messages that does NOT
     *         take an exception is called.
     */
    public boolean isLogging() {
        return isLogging(normalPriority);
    }

    /**
     * The log message is overridden to use the Apache Commons Logging Logger to persist the stop watch.
     *
     * @param stopWatchAsString The stringified view of the stop watch for logging.
     * @param exception         An exception, if any, that was passed to the stop or lap method. If this is null then
     *                          logging will occur at normalPriority, if non-null it will occur at exceptionPriority.
     */
    protected void log(String stopWatchAsString, Throwable exception) {
        log(stopWatchAsString, exception, (exception == null) ? normalPriority : exceptionPriority);
    }

    /**
     * Since Commons Logging doesn't have a first class notion of Level objects or integer values, this method
     * converts the level value to one of the <tt>isXYZEnabled()</tt> methods on the Log.
     *
     * @param atLevel The level at which a log message is logged.
     * @return true if the Logger used by this StopWatch will output messages at the level specified.
     */
    protected boolean isLogging(int atLevel) {
        switch (atLevel) {
        case TRACE_LEVEL:
            return logger.isTraceEnabled();
        case DEBUG_LEVEL:
            return logger.isDebugEnabled();
        case INFO_LEVEL:
            return logger.isInfoEnabled();
        case WARN_LEVEL:
            return logger.isWarnEnabled();
        case ERROR_LEVEL:
            return logger.isErrorEnabled();
        default:
            // if here it means the level was set to a non-standard value.
            // Only expend the time to find the closest known level if we need to.
            return isLogging(closestKnownLevel(atLevel));
        }
    }

    /**
     * Since Commons Logging doesn't have a first class notion of Level objects or integer values, this method
     * maps the atLevel parameter to one of the <tt>trace()</tt>, <tt>debug()</tt>, <tt>info()</tt>, <tt>warn()</tt>,
     * <tt>error()</tt>, or <tt>fatal()</tt> methods on the Log.
     *
     * @param stopWatchAsString The stringified view of the stop watch for logging.
     * @param exception         An exception, if any, that was passed to the stop or lap method. If this is null then
     *                          logging will occur at normalPriority, if non-null it will occur at exceptionPriority.
     * @param atLevel           The level at which logging should occur.
     */
    protected void log(String stopWatchAsString, Throwable exception, int atLevel) {
        switch (atLevel) {
        case TRACE_LEVEL:
            logger.trace(stopWatchAsString, exception);
            break;
        case DEBUG_LEVEL:
            logger.debug(stopWatchAsString, exception);
            break;
        case INFO_LEVEL:
            logger.info(stopWatchAsString, exception);
            break;
        case WARN_LEVEL:
            logger.warn(stopWatchAsString, exception);
            break;
        case ERROR_LEVEL:
            logger.error(stopWatchAsString, exception);
            break;
        default:
            // if here it means the level was set to a non-standard value.
            // Only expend the time to find the closest known level if we need to.
            log(stopWatchAsString, exception, closestKnownLevel(atLevel));
        }
    }

    private int closestKnownLevel(int level) {
        if (level <= TRACE_LEVEL) {
            return TRACE_LEVEL;
        }
        if (level <= DEBUG_LEVEL) {
            return DEBUG_LEVEL;
        }
        if (level <= INFO_LEVEL) {
            return INFO_LEVEL;
        }
        if (level <= WARN_LEVEL) {
            return WARN_LEVEL;
        }
        return ERROR_LEVEL;
    }

    // --- Static Utility Methods ---
    /**
     * This utility method provides the standard mapping between log4j level names (which Perf4J uses as the standard
     * set of possible levels) to the corresponding ..._LEVEL constant from this class.
     *
     * @param levelName The name of the logging level, should be one of TRACE, DEBUG, INFO, WARN, ERROR or FATAL.
     * @return The corresponding ..._LEVEL constant from this class.
     */
    public static int mapLevelName(String levelName) {
        levelName = levelName.toUpperCase();
        if ("TRACE".equals(levelName)) { return TRACE_LEVEL; }
        if ("DEBUG".equals(levelName)) { return DEBUG_LEVEL; }
        if ("INFO".equals(levelName)) { return INFO_LEVEL; }
        if ("WARN".equals(levelName)) { return WARN_LEVEL; }
        if ("ERROR".equals(levelName)) { return ERROR_LEVEL; }
        if ("FATAL".equals(levelName)) { return ERROR_LEVEL; } //SLF4J has no FATAL level
        return INFO_LEVEL;
    }

    // --- Object Methods ---

    public Slf4JStopWatch clone() {
        return (Slf4JStopWatch) super.clone();
    }

    private void writeObject(ObjectOutputStream stream) throws IOException {
        stream.defaultWriteObject();
        stream.writeUTF(logger.getName());
    }

    private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
        stream.defaultReadObject();
        this.logger = LoggerFactory.getLogger(stream.readUTF());
    }
}
