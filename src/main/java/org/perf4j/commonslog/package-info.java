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
/**
 * Provides a {@link org.perf4j.commonslog.CommonsLogStopWatch} to use as your StopWatch implementation if the
 * Apache Commons Logging framework is your logging framework of choice. Note that since Commons Logging is just a thin
 * facade over an underlying logging implentation like log4j or java.util.logging, you must still configure that
 * underlying framework. However, using a CommonsLogStopWatch ensures that a Commons Logging Log instance will be used
 * to make the log calls.
 *
 * @see <a href="http://commons.apache.org/logging/">Apache Commons Logging</a>
 */
package org.perf4j.commonslog;