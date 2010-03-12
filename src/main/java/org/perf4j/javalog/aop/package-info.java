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
 * Defines the concrete aspect class used to inject timing code around methods that have been marked with the
 * {@link org.perf4j.aop.Profiled} annotation. The {@link org.perf4j.javalog.aop.TimingAspect} should be used if you
 * use java.util.logging as your logging framework of choice.  Alternatively, if using EJB interceptors, you should
 * use the {@link org.perf4j.commonslog.aop.EjbTimingAspect} interceptor.
 *
 * @see <a href="http://perf4j.codehaus.org/devguide.html#Unobtrusive_Logging_with_Profiled_and_AOP">The Perf4J Developer Guide AOP Overview</a>
 * @see <a href="http://www.eclipse.org/aspectj/doc/released/devguide/ajc-ref.html">ajc, the AspectJ compiler/weaver</a>
 * @see <a href="http://www.eclipse.org/aspectj/doc/released/devguide/ltw.html">Load-Time Weaving with AspectJ</a>
 * @see <a href="http://static.springframework.org/spring/docs/2.5.x/reference/aop.html">Spring AOP</a>
 */
package org.perf4j.javalog.aop;