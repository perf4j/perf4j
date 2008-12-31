/* Copyright Homeaway, Inc 2005-2007. All Rights Reserved.
 * No unauthorized use of this software.
 */
package org.perf4j.helpers;

import org.perf4j.StopWatch;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Scanner;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;

/**
 * The StopWatchLogIterator class takes input from a Reader and parses it so that deserialized StopWatch instances can
 * be returned with each call to next(). Log messages that are not recognized as StopWatch calls are just ignored.
 *
 * @author Alex Devine
 */
public class StopWatchLogIterator implements Iterator<StopWatch> {
    /**
     * The input scanner that pulls from the input stream.
     */
    private Scanner inputScanner;
    /**
     * The pattern used to match and parse StopWatch log messages. This pattern must be able to match against the
     * toString() result of a StopWatchLog.
     */
    private Pattern stopWatchParsePattern;
    /**
     * State variable points to the next StopWatch to be returned.
     */
    private StopWatch nextStopWatch = null;
    /**
     * State variable keeps track of whether or not there is a next StopWatch. Null means the next state is currently
     * unknown, and the inputScanner will need to be read to determine if there is a next.
     */
    private Boolean hasNext = null;

    /**
     * Creates a new StopWatchLogIterator to parse input from the specified Readable instance.
     *
     * @param log The log containing the data to be parsed.
     */
    public StopWatchLogIterator(Readable log) {
        inputScanner = new Scanner(log);
        stopWatchParsePattern = Pattern.compile(getStopWatchParsePattern());
    }

    public boolean hasNext() {
        //if I don't know the state of next, pull the next log to determine the state of next
        if (hasNext == null) {
            nextStopWatch = getNext();
            hasNext = (nextStopWatch != null);
        }
        return hasNext;
    }

    public StopWatch next() {
        //if I already determined I don't have a next, throw an exception
        if (Boolean.FALSE.equals(hasNext)) {
            throw new NoSuchElementException();
        }

        //if I don't know what to return yet, find out - note this only happens if I call next() before a call
        //to hasNext().
        if (nextStopWatch == null) {
            nextStopWatch = getNext();

            //if there's still nothing I'm done
            if (nextStopWatch == null) {
                hasNext = false;
                throw new NoSuchElementException();
            }
        }

        //before I return, clear the state of the variables used to determine the next value.
        StopWatch retVal = nextStopWatch;
        hasNext = null;
        nextStopWatch = null;
        return retVal;
    }

    /**
     * Remove is not supported.
     *
     * @exception UnsupportedOperationException Always thrown.
     */
    public void remove() {
        throw new UnsupportedOperationException();
    }

    /**
     * Helper method gets the pattern that is used to parse StopWatches from the log. The following is true of the
     * capturing groups of this pattern:
     * <ol>
     * <li> The start time in milliseconds, parseable as a long
     * <li> The elapsed time in milliseconds, parseable as a long
     * <li> The tag name
     * <li> Optional, if not null the message text.
     * </ol>
     *
     * @return The pattern string used to parse the log data.
     */
    protected String getStopWatchParsePattern() {
        return "start\\[(\\d+)\\] time\\[(\\d+)\\] tag\\[(.*?)\\](?: message\\[(.*?)\\])?";
    }

    /**
     * Helper method returns a new StopWatch from the MatchResult returned when a log messages matches the
     * <tt>getStopWatchParsePattern()</tt> pattern string.
     *
     * @param matchResult The regex match result
     * @return A new StopWatch that reflects the data from the match result.
     */
    protected StopWatch parseStopWatchFromLogMatch(MatchResult matchResult) {
        return new StopWatch(Long.parseLong(matchResult.group(1)) /*start time*/,
                             Long.parseLong(matchResult.group(2)) /*elapsed time*/,
                             matchResult.group(3) /*tag*/,
                             matchResult.group(4) /*message, may be null*/);
    }

    /**
     * Helper method uses the scanner to find the next StopWatch from the input. This method may block on input.
     *
     * @return The next parsed StopWatch from the input stream, or null if there are no more StopWatches.
     */
    private StopWatch getNext() {
        String line;
        while ((line = inputScanner.findInLine(stopWatchParsePattern)) == null && inputScanner.hasNextLine()) {
            inputScanner.nextLine();
        }

        return (line != null) ?
               parseStopWatchFromLogMatch(inputScanner.match()) :
               null; //there are no more lines to read if line is null
    }
}