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
 * Provides annotations that work with an aspect-oriented framework like AspectJ, Spring AOP or EJB interceptors to
 * allow timing of code blocks without explicit logging statements. The {@link org.perf4j.aop.Profiled} annotation can
 * be added to method declarations to indicate that method execution should be timed, e.g.:
 * <pre>
 * &#064;Profiled(tag = "servlet{$this.servletName}_{$0.pathInfo}")
 * protected void doGet(HttpServletRequest req, HttpServletResponse res) {
 * ...
 * }
 * </pre>
 * Timing code can then be enabled using the concrete subclasses of {@link org.perf4j.aop.AbstractTimingAspect}, such
 * as the log4j {@link org.perf4j.log4j.aop.TimingAspect}. You will need to use an aspect framework such as AspectJ
 * or Spring AOP to enable these aspects.
 * <p>
 * In addition, if you are using an aspect framework that doesn't use the AspectJ annotations, you can wrap or subclass
 * {@link org.perf4j.aop.AgnosticTimingAspect} in a manner to work with your framework's requirements.
 *
 * @see <a href="http://perf4j.codehaus.org/devguide.html#Unobtrusive_Logging_with_Profiled_and_AOP">The Perf4J Developer Guide AOP Overview</a>
 * @see <a href="http://www.eclipse.org/aspectj/doc/released/devguide/ajc-ref.html">ajc, the AspectJ compiler/weaver</a>
 * @see <a href="http://www.eclipse.org/aspectj/doc/released/devguide/ltw.html">Load-Time Weaving with AspectJ</a>
 * @see <a href="http://static.springframework.org/spring/docs/2.5.x/reference/aop.html">Spring AOP</a>
 */
package org.perf4j.aop;