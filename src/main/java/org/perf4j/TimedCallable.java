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
import java.util.concurrent.Callable;

/**
 * This helper wrapper class can be used to add timing statements to an existing Callable instance, logging how long
 * it takes for the call method to execute. Note that instances of this class are only serializable if the wrapped
 * Callable is serializable.
 *
 * @author Alex Devine
 */
public class TimedCallable<V> implements Callable<V>, Serializable {
    private static final long serialVersionUID = -7581382177897573004L;
    private Callable<V> wrappedTask;
    private LoggingStopWatch stopWatch;

    /**
     * Wraps the existing Callable in order to time its call method.
     *
     * @param task      The existing Callable whose call method is to be timed and executed. May not be null.
     * @param stopWatch The LoggingStopWatch to use to time the call method execution. Note that this stop watch should
     *                  already have its tag and message set to what should be logged when the task is run. May not
     *                  be null.
     */
    public TimedCallable(Callable<V> task, LoggingStopWatch stopWatch) {
        this.wrappedTask = task;
        this.stopWatch = stopWatch;
    }

    /**
     * Gets the Callable task that is wrapped by this TimedCallable.
     *
     * @return The wrapped Callable whose execution time is to be logged.
     */
    public Callable<V> getWrappedTask() {
        return wrappedTask;
    }

    /**
     * Gets the LoggingStopWatch that will be used to time the call method execution.
     *
     * @return The LoggingStopWatch to use to log execution time.
     */
    public LoggingStopWatch getStopWatch() {
        return stopWatch;
    }

    /**
     * Executes the call method of the underlying task, using the LoggingStopWatch to track the execution time.
     */
    public V call() throws Exception {
        try {
            stopWatch.start();
            return wrappedTask.call();
        } finally {
            stopWatch.stop();
        }
    }
}
