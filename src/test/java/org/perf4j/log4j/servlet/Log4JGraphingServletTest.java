/* Copyright Homeaway, Inc 2005-2007. All Rights Reserved.
 * No unauthorized use of this software.
 */
package org.perf4j.log4j.servlet;

import junit.framework.TestCase;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

/**
 * Tests the log4j GraphingServlet.
 */
public class Log4JGraphingServletTest extends TestCase {

    /**
     * Test for http://jira.codehaus.org/browse/PERFFORJ-28
     *
     * @throws Exception Thrown on error
     */
    public void testUnknownGraphName() throws Exception {
        GraphingServlet servlet = new GraphingServlet();

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/perf4j");
        MockHttpServletResponse response = new MockHttpServletResponse();

        request.addParameter("graphName", "unknownGraph");

        servlet.service(request, response);

        //we should get a message that the graph name was unknown, not an NPE
        String content = response.getContentAsString();
        assertTrue("Didn't find expected warning message in response: " + content,
                   content.indexOf("Unknown graph name: unknownGraph") >= 0);
    }
}
