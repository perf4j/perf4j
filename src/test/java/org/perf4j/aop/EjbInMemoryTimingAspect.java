package org.perf4j.aop;

import org.apache.log4j.Level;
import org.perf4j.LoggingStopWatch;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * This class is used by the EjbAopTest to check when the aspect was called
 */
public class EjbInMemoryTimingAspect extends AbstractEjbTimingAspect {
    public static List<String> logStrings = Collections.synchronizedList(new ArrayList<String>());

    protected LoggingStopWatch newStopWatch(final String loggerName, final String levelName) {
        return new LoggingStopWatch() {
            private static final long serialVersionUID = -8258832873829050541L;

            public boolean isLogging() {
                return Level.toLevel(levelName).toInt() >= Level.INFO_INT;
            }

            protected void log(String stopWatchAsString, Throwable exception) {
                EjbInMemoryTimingAspect.logStrings.add(stopWatchAsString);
            }
        };
    }

    public static String getLastLoggedString() {
        if (logStrings.size() > 0) {
            return logStrings.get(logStrings.size() - 1);
        } else {
            return null;
        }
    }
}
