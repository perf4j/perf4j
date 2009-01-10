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
 * Perf4J is a performance logging and monitoring framework for Java. It allows developers to make simple timing
 * calls around code blocks, and these timing statements can then be aggregated, analyzed and graphed by the
 * Perf4J tools.
 * <p>
 * Here is a sample of how to integrate timing statements in code:
 * <pre>
 * // Note in the line below you usually want to instantiate a StopWatch that corresponds
 * // to the logging framework of your choice, like a {@link org.perf4j.log4j.Log4JStopWatch} or an {@link org.perf4j.slf4j.Slf4JStopWatch}.
 * {@link org.perf4j.StopWatch} stopWatch = new {@link org.perf4j.LoggingStopWatch}("tagName");
 * ... some code ...
 * stopWatch.stop(); // perf4j lets you use the logging framework of your choice
 * </pre>
 * To analyze the logged timing statements you run the log output file through the {@link org.perf4j.LogParser},
 * which generates statistical aggregates like mean, standard deviation and transactions per second. Optionally, if you
 * are using the Log4J or java.util.logging frameworks, you can set up helper appenders or handlers which will perform
 * the real-time aggregation and graph generation for you (<b>IMPORTANT</b> custom java.util.logging Handlers are not
 * yet available, to be completed in the next revision of Perf4J). See the {@link org.perf4j.log4j} and
 * {@link org.perf4j.javalog} packages for more information.
 * <p>
 * In addition, many developers will find it most useful to use Perf4J's profiling annotations in the
 * {@link org.perf4j.aop} package instead of inserting timing statements directly in code.
 * These annotations, together with an AOP framework like AspectJ or Spring AOP, allow developers to add timed blocks
 * without cluttering the main logic of the code.
 */
package org.perf4j;