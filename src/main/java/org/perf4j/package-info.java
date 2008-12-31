/**
 * Perf4J is a performance logging and monitoring framework for Java. It allows developers to make simple timing
 * calls around code blocks, and these timing statements can then be aggregated, analyzed and graphed by the
 * Perf4J tools.
 * <p>
 * Here is a sample of how to integrate timing statements in code:
 * <pre>
 * {@link org.perf4j.StopWatch} stopWatch = new StopWatch("tagName");
 * ... some code ...
 * log.info(stopWatch.stop()); // perf4j lets you use the logging framework of your choice
 * </pre>
 * To analyze the logged timing statements you run the log output file through the {@link org.perf4j.LogParser},
 * which generates statistical aggregates like mean, standard deviation and transactions per second. Optionally, if you
 * are using the Log4J or java.util.logging frameworks, you can set up helper appenders or handlers which will perform
 * the real-time aggregation and graph generation for you (<b>IMPORTANT</b> java.util.logging support is not yet
 * available, to be completed in the next revision of perf4j). See the {@link org.perf4j.log4j} and
 * {@link org.perf4j.javalog} packages for more information.
 * <p>
 * In addition, many developers will find it most useful to use Perf4J's profiling annotations in the
 * {@link org.perf4j.aop} package instead of inserting timing statements directly in code.
 * These annotations, together with an AOP framework like AspectJ or Spring AOP, allow developers to add timed blocks
 * without cluttering the main logic of the code.
 */
package org.perf4j;