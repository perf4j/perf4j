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

import org.apache.commons.io.FileUtils;

import java.io.*;

/**
 * Test the LogParser class, as well as the StopWatchLogIterator class and main method.
 */
public class LogParserTest extends TimingTestCase {

    public void testLogParserMain() throws Exception {
        PrintStream realOut = System.out;
        ByteArrayOutputStream fakeOut = new ByteArrayOutputStream();
        System.setOut(new PrintStream(fakeOut, true));
        try {
            //usage
            realOut.println("-- Usage Test --");
            LogParser.runMain(new String[]{"--help"});
            realOut.println(fakeOut.toString());
            assertTrue(fakeOut.toString().indexOf("Usage") >= 0);
            fakeOut.reset();

            //log on std in, write to std out
            InputStream realIn = System.in;
            ByteArrayInputStream fakeIn = new ByteArrayInputStream(testLog.getBytes());
            System.setIn(fakeIn);
            try {
                realOut.println("-- Std in -> Std out Test --");
                LogParser.runMain(new String[0]);
                realOut.println(fakeOut.toString());
                assertTrue(fakeOut.toString().indexOf("tag") >= 0 &&
                           fakeOut.toString().indexOf("tag2") >= 0 &&
                           fakeOut.toString().indexOf("tag3") >= 0);
                fakeOut.reset();
            } finally {
                System.setIn(realIn);
            }

            //Log from a file
            FileUtils.writeStringToFile(new File("./target/logParserTest.log"), testLog);

            //log from file, write to std out
            realOut.println("-- File in -> Std out Test --");
            LogParser.runMain(new String[]{"./target/logParserTest.log"});
            realOut.println(fakeOut.toString());
            assertTrue(fakeOut.toString().indexOf("tag") >= 0 &&
                       fakeOut.toString().indexOf("tag2") >= 0 &&
                       fakeOut.toString().indexOf("tag3") >= 0);
            fakeOut.reset();

            //CSV format test
            realOut.println("-- File in -> Std out Test with CSV --");
            LogParser.runMain(new String[]{"-f", "csv", "./target/logParserTest.log"});
            realOut.println(fakeOut.toString());
            assertTrue(fakeOut.toString().indexOf("\"tag\",") >= 0 &&
                       fakeOut.toString().indexOf("\"tag2\",") >= 0 &&
                       fakeOut.toString().indexOf("\"tag3\",") >= 0);
            fakeOut.reset();

            //log from file, write to file
            realOut.println("-- File in -> File out Test --");
            LogParser.runMain(new String[]{"-o", "./target/statistics.out", "./target/logParserTest.log"});
            String statsOut = FileUtils.readFileToString(new File("./target/statistics.out"));
            realOut.println(statsOut);
            assertTrue(statsOut.indexOf("tag") >= 0 &&
                       statsOut.indexOf("tag2") >= 0 &&
                       statsOut.indexOf("tag3") >= 0);

            //log from file, write to file, different timeslice
            realOut.println("-- File in -> File out with different timeslice Test --");
            LogParser.runMain(new String[]{"-o", "./target/statistics.out", "--timeslice", "120000", "./target/logParserTest.log"});
            statsOut = FileUtils.readFileToString(new File("./target/statistics.out"));
            realOut.println(statsOut);
            assertTrue(statsOut.indexOf("tag") >= 0 &&
                       statsOut.indexOf("tag2") >= 0 &&
                       statsOut.indexOf("tag3") >= 0);

            //missing param test
            realOut.println("-- Missing param test --");
            assertEquals(1, LogParser.runMain(new String[]{"./target/logParserTest.log", "-o"}));

            //unknown arg test
            realOut.println("-- Unknown arg test --");
            assertEquals(1, LogParser.runMain(new String[]{"./target/logParserTest.log", "--foo"}));
            realOut.println(fakeOut);
            assertTrue(fakeOut.toString().indexOf("Unknown") >= 0);

            //graphing test
            realOut.println("-- File in -> File out with graphing --");
            LogParser.runMain(new String[]{"-o", "./target/statistics.out",
                                           "-g", "./target/perfGraphs.out",
                                           "./src/test/resources/org/perf4j/dummyLog.txt"});
            statsOut = FileUtils.readFileToString(new File("./target/statistics.out"));
            realOut.println(statsOut);
            String graphsOut = FileUtils.readFileToString(new File("./target/perfGraphs.out"));
            realOut.println(graphsOut);
            assertTrue(graphsOut.indexOf("chtt=TPS") > 0 && graphsOut.indexOf("chtt=Mean") > 0);
        } finally {
            System.setOut(realOut);
        }
    }
}
