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
package org.perf4j.aop;

import org.perf4j.StopWatch;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.ElementType;

/**
 * The Profiled annotation is used in concert with the log4j or javalog TimingAspects to enable unobtrusive
 * performance logging. Methods with this annotation, when enabled with the TimingAspect, will automatically have
 * their execution time logged.
 *
 * @see <a href="http://perf4j.codehaus.org/devguide.html#Adding_the_Profiled_Annotation_to_Method_Declarations">The Perf4J Developer Guide Profiled Annotations Overview</a>
 * @author Alex Devine
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.CONSTRUCTOR})
public @interface Profiled {
    public static final String DEFAULT_TAG_NAME = "@@USE_METHOD_NAME";

    /**
     * The tag that should be set on the {@link org.perf4j.StopWatch} when the execution time is logged. If not
     * specified then the name of the method being annotated will be used for the tag name.
     *
     * @return The StopWatch tag
     */
    String tag() default DEFAULT_TAG_NAME;

    /**
     * The optional message element can be used to set a message on the {@link org.perf4j.StopWatch} that is logged.
     *
     * @return The optional message specified for this annotation.
     */
    String message() default "";

    /**
     * The name of the logger (either a log4J or java.logging Logger, depending on the Aspect in use at runtime) to
     * use to log the {@link org.perf4j.StopWatch}.
     *
     * @return The logger name, defaults to StopWatch.DEFAULT_LOGGER_NAME
     */
    String logger() default StopWatch.DEFAULT_LOGGER_NAME;

    /**
     * The level to use when logging the StopWatch. Defaults to INFO. Acceptable values are taken from names of the
     * standard log4j Levels.
     *
     * @return The level to use when logging the StopWatch
     * @see <a href="http://logging.apache.org/log4j/1.2/apidocs/org/apache/log4j/Level.html">log4j Level class</a>
     */
    String level() default "INFO";

    /**
     * Whether or not the tag and message elements should support Java Expression Language syntax. Setting this to true
     * enables the tag name to be dynamic with respect to the arguments passed to the method being profiled, the
     * return value or exception thrown from the method (if any), and the object on which the method is being called.
     * An Expression Language expression is delimited with curly brackets, and arguments are accessed using the
     * following variable names:
     * <p>
     * <ul>
     * <li>$0, $1, $2, $3, etc. - the parameters passed to the method in declaration order
     * <li>$this - the object whose method is being called - when a static class method is profiled, this
     * will always be null
     * <li>$return - the return value from the method, which will be null if the method has a void return type, or an
     * exception was thrown during method execution
     * <li>$exception - the value of any Throwable thrown by the method - will be null if the method returns normally
     * </ul>
     * <p>
     * For example, suppose you want to profile the <tt>doGet()</tt> method of a servlet, with the tag name dependent
     * on the name of the servlet AND the path info (as returned by getPathInfo()) of the request.
     * You could create the following annotation:
     *
     * <pre>
     * &#064;Profiled(tag = "servlet{$this.servletName}_{$0.pathInfo}", el = true)
     * protected void doGet(HttpServletRequest req, HttpServletResponse res) {
     * ...
     * }
     * </pre>
     *
     * If the doGet() method is called with a request whose getPathInfo() method returns "/sub/path", and the servlet's
     * name if "main", then the tag used when logging a StopWatch will be "servletMain_/sub/path".
     *
     * @return True if expression language support should be enabled, false to disable support - defaults to true.
     * @see <a href="http://commons.apache.org/jexl/">Apache Commons JEXL</a>
     */
    boolean el() default true;

    /**
     * Whether or not separate tags should be used depending on whether or not the annotated method returns normally
     * or by throwing an exception. If true, then when the method returns normally the tag name used is
     * <tt>tag() + ".success"</tt>, when the method throws an exception the tag name used is
     * <tt>tag() + ".failure"</tt>.
     *
     * @return Whether or not failures should be logged under a separate tag, defaults to false.
     */
    boolean logFailuresSeparately() default false;

    /**
     * If the timeThreshold is set to a positive value, then the method execution time will be logged only if took
     * more than timeThreshold milliseconds. Thus, this value can be used if you only want to log method executions that
     * are unexpectedly slow.
     *
     * @return The time threshold for logging, in milliseconds.
     */
    long timeThreshold() default 0;
    
    /**
     * Default is false. When set to true, normalSuffix and slowSuffix values are appended to tags
     * based on whether the tags' elapsed time &gt;= timeThreshold or not.
     * 
     * @return
     */
    boolean normalAndSlowSuffixesEnabled() default false;
}
