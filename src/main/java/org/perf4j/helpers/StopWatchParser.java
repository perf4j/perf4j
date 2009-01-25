/* Copyright Homeaway, Inc 2005-2007. All Rights Reserved.
 * No unauthorized use of this software.
 */
package org.perf4j.helpers;

import org.perf4j.StopWatch;

import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This helper class is used to parse StopWatches from log message.
 */
public class StopWatchParser {

    /**
     * The default regex used to parse StopWatches from Strings. The following is true of the
     * capturing groups of this pattern:
     * <ol>
     * <li> The start time in milliseconds, parseable as a long
     * <li> The elapsed time in milliseconds, parseable as a long
     * <li> The tag name
     * <li> Optional, if not null the message text.
     * </ol>
     */
    public static final String DEFAULT_MATCH_PATTERN =
            "start\\[(\\d+)\\] time\\[(\\d+)\\] tag\\[(.*?)\\](?: message\\[(.*?)\\])?";

    /**
     * The regex Pattern object used to parse Strings.
     */
    private Pattern pattern;

    /**
     * Creates a StopWatchParser that uses the DEFAULT_MATCH_PATTERN to parse StopWatch message strings.
     */
    public StopWatchParser() {
        this(DEFAULT_MATCH_PATTERN);
    }

    /**
     * Creates a StopWatchParser that uses the specified regex pattern string to parse StopWatch message strings.
     *
     * @param matchPattern The regex pattern String to use to parse log messages
     * @throws java.util.regex.PatternSyntaxException
     *          Thrown if matchPattern is not a valid regex pattern.
     */
    public StopWatchParser(String matchPattern) {
        pattern = Pattern.compile(matchPattern);
    }

    /**
     * Gets the Pattern object used by this StopWatchParser to parse StopWatch message strings.
     *
     * @return The Pattern object.
     */
    public Pattern getPattern() {
        return pattern;
    }

    /**
     * This method parses a StopWatch from the given message string.
     *
     * @param message The message to parse, which was likely created with the StopWatch stop, lap or toString methods.
     * @return The parsed StopWatch, or null if the StopWatch couldn't be parsed.
     */
    public StopWatch parseStopWatch(String message) {
        MatchResult result = match(message);
        return (result != null) ? parseStopWatchFromLogMatch(result) : null;
    }


    /**
     * Gets the MatchResult object that is returned when the Pattern used by this parser matches the specified message.
     *
     * @param message The StopWatch message to parse.
     * @return The MatchResult from matching the message, or null if it didn't match.
     */
    public MatchResult match(String message) {
        Matcher matcher = getPattern().matcher(message);
        return matcher.find() ? matcher.toMatchResult() : null;
    }

    /**
     * Helper method returns a new StopWatch from the MatchResult returned when a log messages matches.
     *
     * @param matchResult The regex match result
     * @return A new StopWatch that reflects the data from the match result.
     */
    public StopWatch parseStopWatchFromLogMatch(MatchResult matchResult) {
        return new StopWatch(Long.parseLong(matchResult.group(1)) /*start time*/,
                             Long.parseLong(matchResult.group(2)) /*elapsed time*/,
                             matchResult.group(3) /*tag*/,
                             matchResult.group(4) /*message, may be null*/);
    }
}
