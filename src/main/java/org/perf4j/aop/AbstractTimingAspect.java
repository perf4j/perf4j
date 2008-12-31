/* Copyright Homeaway, Inc 2005-2007. All Rights Reserved.
 * No unauthorized use of this software.
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
 * This is the base class for TimingAspects. Subclasses just need to implement the {@link #log} method to use
 * their logging framework of choice (e.g. log4j or java.logging) to persist the StopWatch log message.
 * 
 * @author Alex Devine
 */
@Aspect
public abstract class AbstractTimingAspect {

    @Around(value = "execution(* *(..)) && @annotation(profiled)", argNames = "pjp,profiled")
    public Object doPerfLogging(ProceedingJoinPoint pjp, Profiled profiled) throws Throwable {
        // get the tag and message, which depends on the el() (expressionLanguage) element of the Profiled annotation.
        String tag, message;
        if (Profiled.DEFAULT_TAG_NAME.equals(profiled.tag())) {
            // if the tag name is not explicitly set on the Profiled annotation,
            // use the name of the method being annotated.
            tag = pjp.getSignature().getName();
        } else if (profiled.el() && profiled.tag().indexOf("{") >= 0) {
            tag = evaluateJexl(profiled.tag(), pjp.getArgs());
        } else {
            tag = profiled.tag();
        }

        if (profiled.el() && profiled.message().indexOf("{") >= 0) {
            message = evaluateJexl(profiled.message(), pjp.getArgs());
            if ("".equals(message)) {
                message = null;
            }
        } else {
            message = "".equals(profiled.message()) ? null : profiled.message();
        }

        StopWatch stopWatch = new StopWatch();

        try {
            Object retVal = pjp.proceed();
            if (profiled.logFailuresSeparately()) {
                log(profiled.logger(), stopWatch.stop(tag + ".success", message));
            }
            return retVal;
        } catch (Throwable t) {
            if (profiled.logFailuresSeparately()) {
                log(profiled.logger(), stopWatch.stop(tag + ".failure", message));
            }
            throw t;
        } finally {
            if (!profiled.logFailuresSeparately()) {
                log(profiled.logger(), stopWatch.stop(tag, message));
            }
        }
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
     * Subclasses must implement this method to persist the StopWatch logging message.
     *
     * @param loggerName      This name identifies the logger to use to persist the log message
     * @param stopWatchString The StopWatch log to be saved.
     */
    protected abstract void log(String loggerName, String stopWatchString);
}
