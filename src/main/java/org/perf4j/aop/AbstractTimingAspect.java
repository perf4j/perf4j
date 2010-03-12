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
package org.perf4j.aop;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.perf4j.LoggingStopWatch;

/**
 * This is the base class for TimingAspects that use the AspectJ framework (a better name for this class work probably
 * be AspectJTimingAspect, but for backwards compatibility reasons it keeps the AbstractTimingAspect name).
 * Subclasses just need to implement the {@link #newStopWatch} method to use their logging framework of choice
 * (e.g. log4j or java.logging) to persist the StopWatch log message.
 *
 * @author Alex Devine
 */
@Aspect
public abstract class AbstractTimingAspect extends AgnosticTimingAspect {
    /**
     * This advice is used to add the StopWatch logging statements around method executions that have been tagged
     * with the Profiled annotation.
     *
     * @param pjp      The ProceedingJoinPoint encapulates the method around which this aspect advice runs.
     * @param profiled The profiled annotation that was attached to the method.
     * @return The return value from the method that was executed.
     * @throws Throwable Any exceptions thrown by the underlying method.
     */
    @Around(value = "execution(* *(..)) && @annotation(profiled)", argNames = "pjp,profiled")
    public Object doPerfLogging(final ProceedingJoinPoint pjp, Profiled profiled) throws Throwable {
        //We just delegate to the super class, wrapping the AspectJ-specific ProceedingJoinPoint as an AbstractJoinPoint
        return runProfiledMethod(
                new AbstractJoinPoint() {
                    public Object proceed() throws Throwable { return pjp.proceed(); }

                    public Object getExecutingObject() { return pjp.getThis(); }

                    public Object[] getParameters() { return pjp.getArgs(); }

                    public String getMethodName() { return pjp.getSignature().getName(); }
                },
                profiled,
                newStopWatch(profiled.logger() + "", profiled.level())
        );
    }

    /**
     * Subclasses should implement this method to return a LoggingStopWatch that should be used to time the wrapped
     * code block.
     *
     * @param loggerName The name of the logger to use for persisting StopWatch messages.
     * @param levelName  The level at which the message should be logged.
     * @return The new LoggingStopWatch.
     */
    protected abstract LoggingStopWatch newStopWatch(String loggerName, String levelName);
}
