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
package org.perf4j.log4j.aop;

import org.aspectj.lang.annotation.Aspect;
import org.perf4j.aop.AbstractTimingAspect;
import org.apache.log4j.Logger;
import org.apache.log4j.Level;

/**
 * This TimingAspect implementation uses Log4j to persist StopWatch log messages at the info level.
 * 
 * @author Alex Devine
 */
@Aspect
public class TimingAspect extends AbstractTimingAspect {

    protected boolean shouldLog(String loggerName, String levelName) {
        return Logger.getLogger(loggerName).isEnabledFor(Level.toLevel(levelName));
    }

    protected void log(String loggerName, String levelName, String stopWatchString) {
        Logger.getLogger(loggerName).log(Level.toLevel(levelName), stopWatchString);
    }
}
