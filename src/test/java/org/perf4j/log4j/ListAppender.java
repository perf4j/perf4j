/* Copyright (c) 2011 Brett Randall.
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
package org.perf4j.log4j;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Appender;
import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.spi.LoggingEvent;

/**
 * Simple {@link Appender} which stored logging events in a {@link List}.
 * Equivalent to same class in logback. Used for testing.
 * 
 * @author Brett Randall
 * 
 */
public class ListAppender extends AppenderSkeleton {

    public List<LoggingEvent> list;

    public ListAppender() {
        list = new ArrayList<LoggingEvent>();
    }

    @Override
    protected void append(LoggingEvent loggingEvent) {
        list.add(loggingEvent);
    }

    @Override
    public boolean requiresLayout() {
        return false;
    }

    @Override
    public void close() {
        if (list != null) {
            list.clear();
            list = null;
        }
    }

}
