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
package org.perf4j;

import org.perf4j.helpers.*;
import org.perf4j.chart.StatisticsChartGenerator;
import org.perf4j.chart.GoogleChartGenerator;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 * LogParser provides the main method for reading a log of StopWatch output and generating statistics and graphs
 * from that output. Run "java -jar pathToPerf4jJar --help" for instructions.
 *
 * @author Alex Devine
 */
public class LogParser {
    /**
     * The input log that is being parsed.
     */
    private Reader inputLog;
    /**
     * The stream where the GroupedTimingStatistics data will be printed - if null, no statistics will be printed
     */
    private PrintStream statisticsOutput;
    /**
     * The stream where graphing output will be printed - if null, no graphing output will be printed. May be the
     * same stream as statisticsOutput.
     */
    private PrintStream graphingOutput;
    /**
     * The chart generator used to send the mean time graph to graphingOutput.
     */
    private StatisticsChartGenerator meanTimeChartGenerator;
    /**
     * The chart generator used to send the TPS graph to graphingOutput.
     */
    private StatisticsChartGenerator tpsChartGenerator;
    /**
     * The length of time, in milliseconds, of the timeslice of each GroupedTimingStatistics.
     */
    private long timeSlice;
    /**
     * Whether or not "rollup statistics" should be created for each GroupedTimingStatistics created.
     */
    private boolean createRollupStatistics;
    /**
     * The formatter to use to print statistics.
     */
    private GroupedTimingStatisticsFormatter statisticsFormatter;

    // --- Constructors ---
    /**
     * Default constructor reads input from standard in, writes statistics output to standard out, does not write
     * graph output, has a time slice window of 30 seconds, and does not create rollup statistics.
     */
    public LogParser() {
        this(new InputStreamReader(System.in),
             System.out,
             null /* no graph output */,
             30000L,
             false /* don't create rollup statistics */,
             new GroupedTimingStatisticsTextFormatter());
    }

    /**
     * Creates a new LogParser to parse log data from the input.
     *
     * @param inputLog               The log being parsed, which should contain {@link org.perf4j.StopWatch} log messages.
     * @param statisticsOutput       The stream where calculated statistics information should be written - if null,
     *                               statistics data is not written.
     * @param graphingOutput         The stream where graphing data should be written - if null, graphs are not written.
     * @param timeSlice              The length of time, in milliseconds, of the timeslice of each statistics data created.
     * @param createRollupStatistics Whether or not "rollup statistics" should be created for each timeslice of data.
     * @param statisticsFormatter    The formatter to use to print GroupedTimingStatistics
     */
    public LogParser(Reader inputLog, PrintStream statisticsOutput, PrintStream graphingOutput,
                     long timeSlice, boolean createRollupStatistics,
                     GroupedTimingStatisticsFormatter statisticsFormatter) {
        this.inputLog = inputLog;
        this.statisticsOutput = statisticsOutput;
        this.graphingOutput = graphingOutput;
        this.timeSlice = timeSlice;
        this.createRollupStatistics = createRollupStatistics;
        if (graphingOutput != null) {
            this.meanTimeChartGenerator = newMeanTimeChartGenerator();
            this.tpsChartGenerator = newTpsChartGenerator();
        }
        this.statisticsFormatter = statisticsFormatter;
    }

    // --- Instance Methods ---

    /**
     * Reads all the data from the inputLog, parses it, and writes the statistics data and graphing data as desired
     * to the output streams.
     */
    public void parseLog() {

        Iterator<StopWatch> stopWatchIter = new StopWatchLogIterator(inputLog);

        int i = 0;
        for (GroupingStatisticsIterator statsIter = new GroupingStatisticsIterator(stopWatchIter,
                                                                                   timeSlice,
                                                                                   createRollupStatistics);
             statsIter.hasNext();) {
            GroupedTimingStatistics statistics = statsIter.next();

            if (statisticsOutput != null) {
                statisticsOutput.print(statisticsFormatter.format(statistics));
            }

            if (graphingOutput != null) {
                meanTimeChartGenerator.appendData(statistics);
                tpsChartGenerator.appendData(statistics);
                if ((++i % StatisticsChartGenerator.DEFAULT_MAX_DATA_POINTS == 0) ||
                    (!statsIter.hasNext())) {
                    printGraphOutput();
                }
            }
        }
    }

    protected StatisticsChartGenerator newMeanTimeChartGenerator() {
        return new GoogleChartGenerator();
    }

    protected StatisticsChartGenerator newTpsChartGenerator() {
        return new GoogleChartGenerator(StatsValueRetriever.TPS_VALUE_RETRIEVER);
    }

    protected void printGraphOutput() {
        graphingOutput.println("<br/><br/><img src=\"" + meanTimeChartGenerator.getChartUrl() + "\"/>");
        graphingOutput.println("<br/><br/><img src=\"" + tpsChartGenerator.getChartUrl() + "\"/>");
    }
    
    // --- Main and Static Methods ---

    public static void main(String[] args) {
        System.exit(runMain(args));
    }

    public static int runMain(String[] args) {
        try {
            List<String> argsList = new ArrayList<String>(Arrays.asList(args));

            if (printUsage(argsList)) {
                return 0;
            }

            PrintStream statisticsOutput = openStatisticsOutput(argsList);
            PrintStream graphingOutput = openGraphingOutput(argsList);
            long timeSlice = getTimeSlice(argsList);
            boolean rollupStatistics = getRollupStatistics(argsList);
            GroupedTimingStatisticsFormatter formatter = getStatisticsFormatter(argsList);
            Reader input = openInput(argsList);

            if (!argsList.isEmpty()) {
                printUnknownArgs(argsList);
                return 1;
            }

            new LogParser(input, statisticsOutput, graphingOutput, timeSlice, rollupStatistics, formatter).parseLog();

            closeGraphingOutput(graphingOutput);
        } catch ( Exception e ) {
            e.printStackTrace();
            return 1;
        }
        return 0;
    }

    protected static boolean printUsage(List<String> argsList) {
        if (getIndexOfArg(argsList, false, "-h", "--help", "-?", "--usage") >= 0) {
            System.out.println("Usage: LogParser [-o|--out|--output outputFile] " +
                               "[-g|--graph graphingOutputFile] " +
                               "[-t|--timeslice timeslice] " +
                               "[-r] " +
                               "[-f|--format text|csv] " +
                               "[logInputFile]");
            System.out.println("Arguments:");
            System.out.println("  logInputFile - The log file to be parsed. If not specified, log data is read from stdin.");
            System.out.println("  -o|--out|--output outputFile - The file where generated statistics should be written." +
                               " If not specified, statistics are written to stdout.");
            System.out.println("  -g|--graph graphingOutputFile - The file where generated perf graphs should be written." +
                               " If not specified, no graphs are written.");
            System.out.println("  -t|--timeslice timeslice - The length of time (in ms) of each timeslice for which" +
                               " statistics should be generated. Defaults to 30000 ms.");
            System.out.println("  -r - Whether or not statistics rollups should be generated." +
                               " If not specified, rollups are not generated.");
            System.out.println("  -f|--format text|csv - The format for the statistics output, either plain text or CSV." +
                               " Defaults to text.");
            System.out.println("                         If format is csv, then the columns output are tag, start, stop, mean, min, max, stddev, and count.");
            System.out.println();
            System.out.println("Note that out, stdout, err and stderr can be used as aliases to the standard output" +
                               " streams when specifying output files.");
            return true;
        }

        return false;
    }

    protected static PrintStream openStatisticsOutput(List<String> argsList) throws IOException {
        int indexOfOut = getIndexOfArg(argsList, true, "-o", "--output", "--out");
        if (indexOfOut >= 0) {
            String fileName = argsList.remove(indexOfOut + 1);
            argsList.remove(indexOfOut);
            return openStream(fileName);
        } else {
            return System.out;
        }
    }

    protected static PrintStream openGraphingOutput(List<String> argsList) throws IOException {
        int indexOfOut = getIndexOfArg(argsList, true, "-g", "--graph");
        if (indexOfOut >= 0) {
            String fileName = argsList.remove(indexOfOut + 1);
            argsList.remove(indexOfOut);
            PrintStream retVal = openStream(fileName);
            retVal.println("<html>");
            retVal.println("<head><title>Perf4J Performance Graphs</title></head>");
            retVal.println("<body>");
            return retVal;
        } else {
            return null;
        }
    }

    protected static void closeGraphingOutput(PrintStream graphingOutput) throws IOException {
        if (graphingOutput != null) {
            graphingOutput.println("</body></html>");
            if (graphingOutput != System.out && graphingOutput != System.err) {
                graphingOutput.close();
            }
        }
    }

    protected static long getTimeSlice(List<String> argsList) {
        int indexOfOut = getIndexOfArg(argsList, true, "-t", "--timeslice");
        if (indexOfOut >= 0) {
            String timeslice = argsList.remove(indexOfOut + 1);
            argsList.remove(indexOfOut);
            return Long.parseLong(timeslice);
        } else {
            return 30000L;
        }
    }

    protected static boolean getRollupStatistics(List<String> argsList) {
        int indexOfOut = getIndexOfArg(argsList, false, "-r", "--rollup");
        if (indexOfOut >= 0) {
            argsList.remove(indexOfOut);
            return true;
        } else {
            return false;
        }
    }

    protected static GroupedTimingStatisticsFormatter getStatisticsFormatter(List<String> argsList) {
        int indexOfFormat = getIndexOfArg(argsList, true, "-f", "--format");
        if (indexOfFormat >= 0) {
            String formatString = argsList.remove(indexOfFormat + 1);
            argsList.remove(indexOfFormat);
            if ("text".equalsIgnoreCase(formatString)) {
                return new GroupedTimingStatisticsTextFormatter();
            } else if ("csv".equalsIgnoreCase(formatString)) {
                return new GroupedTimingStatisticsCsvFormatter();
            } else {
                throw new IllegalArgumentException("Unknown format type: " + formatString);
            }
        } else {
            return new GroupedTimingStatisticsTextFormatter();
        }
    }

    protected static Reader openInput(List<String> argsList) throws IOException {
        if (argsList.isEmpty()) {
            return new InputStreamReader(System.in);
        } else {
            String fileName = argsList.remove(0);
            return new BufferedReader(new FileReader(fileName));
        }
    }

    protected static void printUnknownArgs(List<String> argsList) {
        System.out.println("Unknown arguments: ");
        for (String arg : argsList) {
            System.out.print(arg + " ");
        }
        System.out.println();
    }

    protected static int getIndexOfArg(List<String> args, boolean needsParam, String... argNames) {
        int retVal = -1;
        boolean foundArg = false;
        for (String argName : argNames) {
            int argIndex = args.indexOf(argName);
            if (argIndex >= 0) {
                if (foundArg) {
                    throw new IllegalArgumentException("You must specify only one of " + Arrays.toString(argNames));
                }
                retVal = argIndex;
                foundArg = true;
            }
        }

        if ((retVal >= 0) && needsParam && (retVal == args.size() - 1)) {
            throw new IllegalArgumentException("You must specify a parameter for the " + args.get(retVal) + " arg");
        }

        return retVal;
    }

    protected static PrintStream openStream(String fileName) throws IOException {
        if ("stdout".equals(fileName) || "out".equals(fileName)) {
            return System.out;
        } else if ("stderr".equals(fileName) || "err".equals(fileName)) {
            return System.err;
        } else {
            return new PrintStream(new FileOutputStream(fileName), true);
        }
    }
}
