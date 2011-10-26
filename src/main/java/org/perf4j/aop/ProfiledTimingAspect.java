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

@Aspect
public abstract class ProfiledTimingAspect extends AbstractTimingAspect {

    /**
     * This advice is used to add the StopWatch logging statements around method executions that have been tagged
     * with the Profiled annotation.
     *
     * @param pjp      The ProceedingJoinPoint encapulates the method around which this aspect advice runs.
     * @param profiled The profiled annotation that was attached to the method.
     * @return The return value from the method that was executed.
     * @throws Throwable Any exceptions thrown by the underlying method.
     */
    @Around("execution(* *(..)) && @annotation(profiled)")
    public Object doPerfLogging(final ProceedingJoinPoint pjp, Profiled profiled) throws Throwable {

        if (profiled == null) {
            profiled = DefaultProfiled.INSTANCE;
        }

        return runProfiledMethod(pjp, profiled);
    }

}
