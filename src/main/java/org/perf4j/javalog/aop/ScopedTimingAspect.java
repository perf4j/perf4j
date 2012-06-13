/* Copyright 2011 (c) Brett Randall.
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
package org.perf4j.javalog.aop;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.aspectj.lang.annotation.Aspect;
import org.perf4j.javalog.JavaLogStopWatch;

/**
 * 
 * @author Brett Randall
 * 
 */

@Aspect
public abstract class ScopedTimingAspect extends org.perf4j.aop.ScopedTimingAspect {

    protected JavaLogStopWatch newStopWatch(String loggerName, String levelName) {
        Level level = JavaLogStopWatch.mapLevelName(levelName);
        return new JavaLogStopWatch(Logger.getLogger(loggerName), level, level);
    }

}
