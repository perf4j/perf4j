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

import org.apache.commons.jexl.Expression;
import org.apache.commons.jexl.ExpressionFactory;
import org.apache.commons.jexl.JexlContext;
import org.apache.commons.jexl.context.HashMapContext;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.perf4j.LoggingStopWatch;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This is the base class for TimingAspects. Subclasses just need to implement the {@link #newStopWatch} method to use
 * their logging framework of choice (e.g. log4j or java.logging) to persist the StopWatch log message.
 *
 * @author Alex Devine
 */
@Aspect
public abstract class AbstractTimingAspect {

    /**
     * This Map is used to cache compiled JEXL expressions. While theoretically unbounded, in reality the number of
     * possible keys is equivalent to the number of unique JEXL expressions created in @Profiled annotations, which
     * will have to be loaded in memory anyway when the class is loaded.
     */
    private Map<String, Expression> jexlExpressionCache = new ConcurrentHashMap<String, Expression>(64, .75F, 16);

    /**
     * This advice is used to add the StopWatch logging statements around method executions that have been tagged
     * with the Profiled annotation.
     *
     * @param pjp      The ProceedingJoinPoint encapulates the method around which this aspect advice runs.
     * @param profiled The profiled annotation that was attached to the method.
     * @return The return value from the method that was executed.
     * @throws Throwable Any exceptions thrown by the underlying method.
     */
    @Around(value = "execution(* *(..)) && @annotation(profiled)", argNames = "pjp,profiled")
    public Object doPerfLogging(ProceedingJoinPoint pjp, Profiled profiled) throws Throwable {
        //WORKAROUND - the + "" below is needed to workaround a bug in the AspectJ ajc compiler that generates invalid
        //bytecode causing AbstractMethodErrors. 
        LoggingStopWatch stopWatch = newStopWatch(profiled.logger() + "", profiled.level());

        //if we're not going to end up logging the stopwatch, just run the wrapped method
        if (!stopWatch.isLogging()) {
            return pjp.proceed();
        }

        stopWatch.setTimeThreshold(profiled.timeThreshold());

        Object retVal = null;
        Throwable exceptionThrown = null;
        try {
            return retVal = pjp.proceed();
        } catch (Throwable t) {
            throw exceptionThrown = t;
        } finally {
            String tag = getStopWatchTag(profiled, pjp, retVal, exceptionThrown);
            String message = getStopWatchMessage(profiled, pjp, retVal, exceptionThrown);

            if (profiled.logFailuresSeparately()) {
                tag = (exceptionThrown == null) ? tag + ".success" : tag + ".failure";
            }

            stopWatch.stop(tag, message);
        }
    }

    /**
     * Helper method gets the tag to use for StopWatch logging. Performs JEXL evaluation if necessary.
     *
     * @param profiled        The profiled annotation that was attached to the method.
     * @param pjp             The ProceedingJoinPoint encapulates the method around which this aspect advice runs.
     * @param returnValue     The value returned from the execution of the profiled method, or null if the method
     *                        returned void or an exception was thrown.
     * @param exceptionThrown The exception thrown, if any, by the profiled method. Will be null if the method
     *                        completed normally.
     * @return The value to use as the StopWatch tag.
     */
    protected String getStopWatchTag(Profiled profiled,
                                     ProceedingJoinPoint pjp,
                                     Object returnValue,
                                     Throwable exceptionThrown) {
        String tag;
        if (Profiled.DEFAULT_TAG_NAME.equals(profiled.tag())) {
            // if the tag name is not explicitly set on the Profiled annotation,
            // use the name of the method being annotated.
            tag = pjp.getSignature().getName();
        } else if (profiled.el() && profiled.tag().indexOf("{") >= 0) {
            tag = evaluateJexl(profiled.tag(), pjp.getArgs(), pjp.getThis(), returnValue, exceptionThrown);
        } else {
            tag = profiled.tag();
        }
        return tag;
    }

    /**
     * Helper method get the message to use for StopWatch logging. Performs JEXL evaluation if necessary.
     *
     * @param profiled        The profiled annotation that was attached to the method.
     * @param pjp             The ProceedingJoinPoint encapulates the method around which this aspect advice runs.
     * @param returnValue     The value returned from the execution of the profiled method, or null if the method
     *                        returned void or an exception was thrown.
     * @param exceptionThrown The exception thrown, if any, by the profiled method. Will be null if the method
     *                        completed normally.
     * @return The value to use as the StopWatch message.
     */
    protected String getStopWatchMessage(Profiled profiled,
                                         ProceedingJoinPoint pjp,
                                         Object returnValue,
                                         Throwable exceptionThrown) {
        String message;
        if (profiled.el() && profiled.message().indexOf("{") >= 0) {
            message = evaluateJexl(profiled.message(), pjp.getArgs(), pjp.getThis(), returnValue, exceptionThrown);
            if ("".equals(message)) {
                message = null;
            }
        } else {
            message = "".equals(profiled.message()) ? null : profiled.message();
        }
        return message;
    }

    /**
     * Helper method is used to parse out {expressionLanguage} elements from the text and evaluate the strings using
     * JEXL.
     *
     * @param text            The text to be parsed.
     * @param args            The args that were passed to the method to be profiled.
     * @param annotatedObject The value of the object whose method was profiled. Will be null if a class method was
     *                        profiled.
     * @param returnValue     The value returned from the execution of the profiled method, or null if the method
     *                        returned void or an exception was thrown.
     * @param exceptionThrown The exception thrown, if any, by the profiled method. Will be null if the method
     *                        completed normally.
     * @return The evaluated string.
     * @see Profiled#el()
     */
    protected String evaluateJexl(String text,
                                  Object[] args,
                                  Object annotatedObject,
                                  Object returnValue,
                                  Throwable exceptionThrown) {
        StringBuilder retVal = new StringBuilder(text.length());

        //create a JexlContext to be used in all evaluations
        JexlContext jexlContext = new HashMapContext();
        for (int i = 0; i < args.length; i++) {
            jexlContext.getVars().put("$" + i, args[i]);
        }
        jexlContext.getVars().put("$this", annotatedObject);
        jexlContext.getVars().put("$return", returnValue);
        jexlContext.getVars().put("$exception", exceptionThrown);

        // look for {expression} in the passed in text
        int bracketIndex;
        int lastCloseBracketIndex = -1;
        while ((bracketIndex = text.indexOf('{', lastCloseBracketIndex + 1)) >= 0) {
            retVal.append(text.substring(lastCloseBracketIndex + 1, bracketIndex));

            lastCloseBracketIndex = text.indexOf('}', bracketIndex + 1);
            if (lastCloseBracketIndex == -1) {
                //if there wasn't a closing bracket index just go to the end of the string
                lastCloseBracketIndex = text.length();
            }

            String expressionText = text.substring(bracketIndex + 1, lastCloseBracketIndex);
            if (expressionText.length() > 0) {
                try {
                    Object result = getJexlExpression(expressionText).evaluate(jexlContext);
                    retVal.append(result);
                } catch (Exception e) {
                    //we don't want to propagate exceptions up
                    retVal.append("_EL_ERROR_");
                }
            }
        }

        //append the final part
        if (lastCloseBracketIndex < text.length()) {
            retVal.append(text.substring(lastCloseBracketIndex + 1, text.length()));
        }

        return retVal.toString();
    }

    /**
     * Helper method gets a compiled JEXL expression for the specified expression text, either from the cache or by
     * creating a new compiled expression.
     *
     * @param expressionText The JEXL expression text
     * @return A compiled JEXL expression representing the expression text
     * @throws Exception Thrown if there was an error compiling the expression text
     */
    protected Expression getJexlExpression(String expressionText) throws Exception {
        Expression retVal = jexlExpressionCache.get(expressionText);
        if (retVal == null) {
            //Don't need synchronization here - if we end up calling createExpression in 2 separate threads, that's fine
            jexlExpressionCache.put(expressionText, retVal = ExpressionFactory.createExpression(expressionText));
        }
        return retVal;
    }

    /**
     * Subclasses should implement this method to return a LoggingStopWatch that should be used to time the wrapped
     * code block.
     *
     * @param loggerName The name of the logger to use for persisting StopWatch messages.
     * @param levelName  The level at which the message should be logged.
     * @return The new LoggingStopWatch.
     */
    protected abstract LoggingStopWatch newStopWatch(String loggerName, String levelName);
}
