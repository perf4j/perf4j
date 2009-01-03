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

import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.ProceedingJoinPoint;
import org.perf4j.StopWatch;
import org.apache.commons.jexl.Expression;
import org.apache.commons.jexl.ExpressionFactory;
import org.apache.commons.jexl.JexlContext;
import org.apache.commons.jexl.JexlHelper;

/**
 * This is the base class for TimingAspects. Subclasses just need to implement the {@link #shouldLog} and
 * {@link #log} methods to use their logging framework of choice (e.g. log4j or java.logging) to persist the
 * StopWatch log message.
 * 
 * @author Alex Devine
 */
@Aspect
public abstract class AbstractTimingAspect {

    /**
     * This advice is used to add the StopWatch logging statements around method executions that have been tagged
     * with the Profiled annotation.
     *
     * @param pjp The ProceedingJoinPoint encapulates the method around which this aspect advice runs.
     * @param profiled The profiled annotation that was attached to the method.
     * @return The return value from the method that was executed.
     * @throws Throwable Any exceptions thrown by the underlying method.
     */
    @Around(value = "execution(* *(..)) && @annotation(profiled)", argNames = "pjp,profiled")
    public Object doPerfLogging(ProceedingJoinPoint pjp, Profiled profiled) throws Throwable {
        //if we're not going to end up logging the stopwatch, just run the wrapped method
        if (!shouldLog(profiled.logger(), profiled.level())) {
            return pjp.proceed();
        }

        String tag = getStopWatchTag(pjp, profiled);
        String message = getStopWatchMessage(pjp, profiled);

        StopWatch stopWatch = new StopWatch();

        try {
            Object retVal = pjp.proceed();
            if (profiled.logFailuresSeparately()) {
                log(profiled.logger(), profiled.level(), stopWatch.stop(tag + ".success", message));
            }
            return retVal;
        } catch (Throwable t) {
            if (profiled.logFailuresSeparately()) {
                log(profiled.logger(), profiled.level(), stopWatch.stop(tag + ".failure", message));
            }
            throw t;
        } finally {
            if (!profiled.logFailuresSeparately()) {
                log(profiled.logger(), profiled.level(), stopWatch.stop(tag, message));
            }
        }
    }

    /**
     * Helper method gets the tag to use for StopWatch logging. Performs JEXL evaluation if necessary.
     *
     * @param pjp The ProceedingJoinPoint encapulates the method around which this aspect advice runs.
     * @param profiled The profiled annotation that was attached to the method.
     * @return The value to use as the StopWatch tag.
     */
    protected String getStopWatchTag(ProceedingJoinPoint pjp, Profiled profiled) {
        String tag;
        if (Profiled.DEFAULT_TAG_NAME.equals(profiled.tag())) {
            // if the tag name is not explicitly set on the Profiled annotation,
            // use the name of the method being annotated.
            tag = pjp.getSignature().getName();
        } else if (profiled.el() && profiled.tag().indexOf("{") >= 0) {
            tag = evaluateJexl(profiled.tag(), pjp.getArgs());
        } else {
            tag = profiled.tag();
        }
        return tag;
    }

    /**
     * Helper method get the message to use for StopWatch logging. Performs JEXL evaluation if necessary.
     *
     * @param pjp The ProceedingJoinPoint encapulates the method around which this aspect advice runs.
     * @param profiled The profiled annotation that was attached to the method.
     * @return The value to use as the StopWatch message.
     */
    protected String getStopWatchMessage(ProceedingJoinPoint pjp, Profiled profiled) {
        String message;
        if (profiled.el() && profiled.message().indexOf("{") >= 0) {
            message = evaluateJexl(profiled.message(), pjp.getArgs());
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
     * @param text The text to be parsed.
     * @param args The args that were passed to the method to be profiled.
     * @return The evaluated string.
     * @see Profiled#el()
     */
    protected String evaluateJexl(String text, Object[] args) {
        StringBuilder retVal = new StringBuilder(text.length());

        //create a JexlContext to be used in all evaluations
        JexlContext jexlContext = JexlHelper.createContext();
        for (int i = 0; i < args.length; i++) {
            jexlContext.getVars().put("$" + i, args[i]);
        }

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

            String expression = text.substring(bracketIndex + 1, lastCloseBracketIndex);
            if (expression.length() > 0) {
                try {
                    Expression jexlExpression = ExpressionFactory.createExpression(expression);
                    Object result = jexlExpression.evaluate(jexlContext);
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
     * Subclasses should implement this method to determine whether or not the logger specified is enabled for the
     * specified level.
     *
     * @param loggerName This name identifies the logger to use to persist the log message
     * @param levelName  The level at which the StopWatch will be persisted
     * @return true if the specified logger is enabled for the specified level
     */
    protected abstract boolean shouldLog(String loggerName, String levelName);

    /**
     * Subclasses must implement this method to persist the StopWatch logging message.
     *
     * @param loggerName      This name identifies the logger to use to persist the log message
     * @param levelName       The level at which the message should be logged
     * @param stopWatchString The StopWatch log to be saved.
     */
    protected abstract void log(String loggerName, String levelName, String stopWatchString);
}
