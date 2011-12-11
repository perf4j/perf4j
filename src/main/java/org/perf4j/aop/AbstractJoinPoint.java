package org.perf4j.aop;

import java.util.Map;

/**
 * AOP-framework agnostic join point.
 *
 * An AOP implementation agnostic interface which offers all information required to do a measure, proceed original
 * method and log result in customizable way. Specific Join Point implementations in AOP libraries/frameworks
 * should implement it wrapping their own internal structures.
 *
 * @author Marcin Zajączkowski, 2010-01-14
 *
 * @since 0.9.13
 */
public interface AbstractJoinPoint {

    /**
     * Calls profiled method and returns its result.
     *
     * @return result of proceeding
     * @throws Throwable thrown exception
     */
    Object proceed() throws Throwable;

    /**
     * Returns an object whose method was annotated (profiled).
     *
     * @return an object whose method was annotated
     */
    Object getExecutingObject();

    /**
     * Returns a parameters (arguments) array of processing method.
     *
     * @return array of parameters
     */
    Object[] getParameters();

    /**
     * Returns a processing method name.
     *
     * @return processing method name
     */
    String getMethodName();

    /**
     * Returns the declaring class of the method that was annotated.
     *
     * @return the declaring class of the method that was annotated
     */
    Class<?> getDeclaringClass();

    /**
     * Returns the context data of the invocation context.
     *
     * @return the context data of the invocation context
     */
    Map<String, Object> getContextData();
}
