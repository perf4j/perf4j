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
 * This helper wrapper class can be used to add timing statements to an existing Runnable instance, logging how long
 * it takes for the run method to execute. Note that instances of this class are only serializable if the wrapped
 * Runnable is serializable.
 *
 * @author Alex Devine
 */
public class TimedRunnable implements Runnable, Serializable {
    private static final long serialVersionUID = -3506292767816484297L;
    private Runnable wrappedTask;
    private LoggingStopWatch stopWatch;

    /**
     * Wraps the existing Runnable in order to time its run method.
     *
     * @param task      The existing Runnable whose run method is to be timed and executed. May not be null.
     * @param stopWatch The LoggingStopWatch to use to time the run method execution. Note that this stop watch should
     *                  already have its tag and message set to what should be logged when the task is run. May not
     *                  be null.
     */
    public TimedRunnable(Runnable task, LoggingStopWatch stopWatch) {
        this.wrappedTask = task;
        this.stopWatch = stopWatch;
    }

    /**
     * Gets the Runnable task that is wrapped by this TimedRunnable.
     *
     * @return The wrapped Runnable whose execution time is to be logged.
     */
    public Runnable getWrappedTask() {
        return wrappedTask;
    }

    /**
     * Gets the LoggingStopWatch that will be used to time the run method execution.
     *
     * @return The LoggingStopWatch to use to log execution time.
     */
    public LoggingStopWatch getStopWatch() {
        return stopWatch;
    }

    /**
     * Executes the run method of the underlying task, using the LoggingStopWatch to track the execution time.
     */
    public void run() {
        try {
            stopWatch.start();
            wrappedTask.run();
        } finally {
            stopWatch.stop();
        }
    }
}
