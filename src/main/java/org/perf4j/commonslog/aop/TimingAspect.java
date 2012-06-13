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
package org.perf4j.commonslog.aop;

import org.apache.commons.logging.LogFactory;
import org.aspectj.lang.annotation.Aspect;
import org.perf4j.aop.ProfiledTimingAspect;
import org.perf4j.commonslog.CommonsLogStopWatch;

/**
 * This TimingAspect implementation uses an Apache Commons Logging Log instance to persist StopWatch log messages.
 *
 * @author Alex Devine
 */
@Aspect
public class TimingAspect extends ProfiledTimingAspect {

    protected CommonsLogStopWatch newStopWatch(String loggerName, String levelName) {
        int levelInt = CommonsLogStopWatch.mapLevelName(levelName);
        return new CommonsLogStopWatch(LogFactory.getLog(loggerName), levelInt, levelInt);
    }

}
