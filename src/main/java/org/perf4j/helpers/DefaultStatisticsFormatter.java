/* Copyright (c) 2012
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

import org.perf4j.GroupedTimingStatistics;

/**
 * Default {@link StatisticsFormatter} that has no header or footer and delegates
 * all formatting to a {@link GroupedTimingStatisticsFormatter}.
 * 
 * @see GroupedTimingStatisticsFormatter
 * @author Kyle Cronin
 */
public class DefaultStatisticsFormatter implements StatisticsFormatter {
	
	private final GroupedTimingStatisticsFormatter formatter;
	
	// --- Constructors ---
	
	/**
	 * Construct new formatter that delegates formatting to the specified 
	 * {@link GroupedTimingStatisticsFormatter}
	 * 
	 * @param formatter grouped timing statistics formatter to delegate formatting to
	 */
	public DefaultStatisticsFormatter(GroupedTimingStatisticsFormatter formatter) {
		this.formatter = formatter;
	}

	// --- Object Methods ---
	
	public String format(GroupedTimingStatistics stats) {
		return formatter.format(stats);
	}

	public String header() {
		return "";
	}

	public String footer() {
		return "";
	}

}
