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
package org.perf4j.helpers;

import org.perf4j.StopWatch;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Scanner;

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
     * This StopWatchParser is used to pull out StopWatches from the input stream.
     */
    private StopWatchParser stopWatchParser;
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
        stopWatchParser = newStopWatchParser();
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
     * @throws UnsupportedOperationException Always thrown.
     */
    public void remove() {
        throw new UnsupportedOperationException();
    }

    /**
     * This helper method could potentially be overridden to return a different type of StopWatchParser that is used
     * to parse the strings read by this class.
     *
     * @return A new StopWatchParser to use to parse log messages.
     */
    protected StopWatchParser newStopWatchParser() {
        return new StopWatchParser();
    }

    /**
     * Helper method uses the scanner to find the next StopWatch from the input. This method may block on input.
     *
     * @return The next parsed StopWatch from the input stream, or null if there are no more StopWatches.
     */
    private StopWatch getNext() {
        String line;
        while ((line = inputScanner.findInLine(stopWatchParser.getPattern())) == null && inputScanner.hasNextLine()) {
            inputScanner.nextLine();
        }

        return (line != null) ?
               stopWatchParser.parseStopWatchFromLogMatch(inputScanner.match()) :
               null; //there are no more lines to read if line is null
    }
}