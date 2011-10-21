package org.perf4j.aop;

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
    public Object proceed() throws Throwable;

    /**
     * Returns an object whose method was annotated (profiled).
     *
     * @return an object whose method was annotated
     */
    public Object getExecutingObject();

    /**
     * Returns a parameters (arguments) array of processing method.
     *
     * @return array of parameters
     */
    public Object[] getParameters();

    /**
     * Returns a processing method name.
     *
     * @return processing method name
     */
    public String getMethodName();

    /**
     * Returns the declaring class of the method that was annotated.
     *
     * @return the declaring class of the method that was annotated
     */
    public Class<?> getDeclaringClass();
}
