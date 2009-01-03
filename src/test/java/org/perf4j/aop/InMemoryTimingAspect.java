/* Copyright Homeaway, Inc 2005-2007. All Rights Reserved.
 * No unauthorized use of this software.
 */
package org.perf4j.aop;

import org.aspectj.lang.annotation.Aspect;
import org.apache.log4j.Level;

import java.util.List;
import java.util.ArrayList;

/**
 * This class is used by the AOP tests to check when the aspect was called
 */
@Aspect
public class InMemoryTimingAspect extends AbstractTimingAspect {
    public static List<String> logStrings = new ArrayList<String>();

    protected boolean shouldLog(String loggerName, String levelName) {
        return Level.toLevel(levelName).toInt() >= Level.INFO_INT;
    }

    protected void log(String loggerName, String levelName, String stopWatchString) {
        InMemoryTimingAspect.logStrings.add(stopWatchString);
    }

    public static String getLastLoggedString() {
        if (logStrings.size() > 0) {
            return logStrings.get(logStrings.size() - 1);
        } else {
            return null;
        }
    }
}
