/* Copyright Homeaway, Inc 2005-2007. All Rights Reserved.
 * No unauthorized use of this software.
 */
package org.perf4j.log4j.aop;

import org.aspectj.lang.annotation.Aspect;
import org.perf4j.aop.AbstractTimingAspect;
import org.apache.log4j.Logger;

/**
 * This TimingAspect implementation uses Log4j to persist StopWatch log messages.
 * 
 * @author Alex Devine
 */
@Aspect
public class TimingAspect extends AbstractTimingAspect {

    protected void log(String loggerName, String stopWatchString) {
        Logger.getLogger(loggerName).info(stopWatchString);
    }
}
