/**
 * Provides annotations that work with an aspect-oriented framework like AspectJ or Spring AOP to allow timing
 * of code blocks without explicit logging statements. The {@link org.perf4j.aop.Profiled} annotation can be added to
 * method declarations to indicate that method execution should be timed, e.g.:
 * <pre>
 * &#064;Profiled(tag = "servlet{$0.pathInfo}")
 * protected void doGet(HttpServletRequest req, HttpServletResponse res) {
 * ...
 * }
 * </pre>
 * Timing code can then be enabled using the concrete subclasses of {@link org.perf4j.aop.AbstractTimingAspect}, such
 * as the log4j {@link org.perf4j.log4j.aop.TimingAspect}. You will need to use an aspect framework such as AspectJ
 * or Spring AOP to enable these aspects. See the Perf4J Developer Guide for more information.
 *
 * @see <a href="http://www.eclipse.org/aspectj/doc/released/devguide/ajc-ref.html">ajc, the AspectJ compiler/weaver</a>
 * @see <a href="http://www.eclipse.org/aspectj/doc/released/devguide/ltw.html">Load-Time Weaving with AspectJ</a>
 * @see <a href="http://static.springframework.org/spring/docs/2.5.x/reference/aop.html">Spring AOP</a>
 */
package org.perf4j.aop;