/**
 * Defines the concrete aspect classes used to inject timing code around methods that have been marked with the
 * {@link org.perf4j.aop.Profiled} annotation. To enable the aspects you must use AspectJ or Spring AOP.
 *
 * @see <a href="http://www.eclipse.org/aspectj/doc/released/devguide/ajc-ref.html">ajc, the AspectJ compiler/weaver</a>
 * @see <a href="http://www.eclipse.org/aspectj/doc/released/devguide/ltw.html">Load-Time Weaving with AspectJ</a>
 * @see <a href="http://static.springframework.org/spring/docs/2.5.x/reference/aop.html">Spring AOP</a>
 */
package org.perf4j.log4j.aop;