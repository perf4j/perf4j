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

import java.util.Map;

import org.perf4j.GroupedTimingStatistics;
import org.perf4j.TimingStatistics;

/**
 * This helper formatter class outputs {@link org.perf4j.GroupedTimingStatistics} in a JSON value format. This
 * formatter supports the following JSON formats:
 * <ul>
 * 	<li> json - Alias for {@code json:list}
 *  <li> json:list - formatted as an array of arrays and the most compact format
 *  <li> json:list-objects - formatted as an array of objects with timing properties
 * 	<li> json:google-datatable - formatted as a Google DataTable data object (See <a href="https://developers.google.com/chart/interactive/docs/reference#DataTable">DataTable</a>)
 * </ul>
 * 
 * <h6>List Format</h6>
 * Index Value:
 * <ul>
 * 	<li>0 = Tag name
 * 	<li>1 = Start Time
 * 	<li>2 = Stop Time
 * 	<li>3 = Mean
 * 	<li>4 = Min
 * 	<li>5 = Max
 * 	<li>6 = Standard Deviation
 * 	<li>7 = Count
 * 	<li>8 = Transaction Per Second
 * </ul>
 * <pre>
 * [["tag",new Date(0000000),new Date(0000000), 0, 0, 0, 0, 0, 0], ...]
 * </pre>
 * This format can be converted to a Google <a href="https://developers.google.com/chart/interactive/docs/reference#DataTable">DataTable</a> using the helper function 
 * <code><a href="https://developers.google.com/chart/interactive/docs/reference#google.visualization.arraytodatatable">google.visualization.arrayToDataTable()</a></code>

 * <h6>List-Objects Format</h6>
 * <pre>
 * [{tag:"name",startTime:new Date(0000000),stopTime:new Date(0000000),mean:0,min:0.max:0,stddev:0,count:0,tps:0}, ...]
 * </pre>
 * 
 * <h6>Google DataTable Format</h6>
 * Google DataTable format is designed for use with Google Visualization API.
 * See <a href="https://developers.google.com/chart/interactive/docs/reference#DataTable">DataTable</a> documentation
 * and <a href="https://developers.google.com/chart/interactive/docs/index">Google Chart Tools</a>.
 * <pre>
 * {
 * cols: [{id: 'tab', label: 'Tag', type: 'string'},
 *        {id: 'startTime', label: 'Start Time', type: 'date'}
 *        {id: 'stopTime', label: 'Stop Time', type: 'date'}
 *        {id: 'mean', label: 'Mean', type: 'number'},
 *        {id: 'min', label: 'Min', type: 'number'},
 *        {id: 'max', label: 'Max', type: 'number'},
 *        {id: 'stddev', label: 'Standard Deviation', type: 'number'},
 *        {id: 'count', label: 'Count', type: 'number'},
 *        {id: 'tps', label: 'Transactions Per Second', type: 'number'}
 *       ],
 * rows: [{c:[{v: 'tag'}, {v: new Date(0000000)},{v: new Date(0000000)},{v: 0},{v: 0},{v: 0},{v: 0},{v: 0},{v: 0}]}
 *      ]
 * }
 * </pre>
 * 
 * @author Kyle Cronin
 */
public class GroupedTimingStatisticsJsonFormatter implements StatisticsFormatter, GroupedTimingStatisticsFormatter {

	private enum JsonFormat {
		GOOGLE_DATA_TABLE("google-datatable"),
		LIST("list"),
		LIST_OBJECTS("list-objects");;

		String id;
		private JsonFormat(String id) {
			this.id = id; 
		}

		public static JsonFormat findById(String id) {
			for (JsonFormat format : JsonFormat.values()) {
				if(format.id.equalsIgnoreCase(id)) {
					return format;
				}
			}
			return null;
		}
	}

	private final JsonFormat format;
	private boolean isFirst = true;
	
	 // --- Constructors ---
	
	/**
	 * Construct {@link JsonFormat#LIST} formatter 
	 */
	public GroupedTimingStatisticsJsonFormatter() {
		this.format = JsonFormat.LIST;
	}

	/**
	 * Construct JSON formatter
	 * @see JsonFormat
	 * @param jsonFormat json format type, not {@code null}
	 * @throws IllegalArgumentException if {@code jsonFormat} is not one of the supported format types
	 */
	public GroupedTimingStatisticsJsonFormatter(String jsonFormat) {
		String[] parts = jsonFormat.split(":");
		String formatId = JsonFormat.LIST.id;
		switch (parts.length) {
		case 2:
			formatId = parts[1];
			break;
		}
		this.format = JsonFormat.findById(formatId);
		if(this.format == null) {
			throw new IllegalArgumentException("Unknown json format " + formatId);
		}
	}
	
	// --- Formatting Methods ---

	public String format(GroupedTimingStatistics stats) {
		switch (format) {
		case GOOGLE_DATA_TABLE:
			return formatGoogleDataTableRow(stats);
		case LIST_OBJECTS:
			formatListObjects(stats);
		default:
			return formatList(stats);
		}
	}

	/**
	 * Format statistics as Google DataTable row
	 * 
	 * @see https://developers.google.com/chart/interactive/docs/reference#DataTable
	 * @param stats timing statistics
	 * @return formatted statistics string
	 */
	private String formatGoogleDataTableRow(GroupedTimingStatistics stats) {
		StringBuilder retVal = new StringBuilder();
		for (Map.Entry<String, TimingStatistics> tagAndStats : stats.getStatisticsByTag().entrySet()) {
			if(!isFirst) {
				retVal.append(",").append(MiscUtils.NEWLINE);
			} else {
				isFirst = false;
			}
			String tag = tagAndStats.getKey();
			TimingStatistics timingStats = tagAndStats.getValue();
			retVal.append("{c:[");
			retVal.append("{v:'").append(jsonize(tag)).append("'},");
			retVal.append("{v:new Date(").append(stats.getStartTime()).append(")},");
			retVal.append("{v:new Date(").append(stats.getStopTime()).append(")},");
			retVal.append("{v: ").append(timingStats.getMean()).append("},");
			retVal.append("{v:").append(timingStats.getMin()).append("},");
			retVal.append("{v:").append(timingStats.getMax()).append("},");
			retVal.append("{v:").append(timingStats.getStandardDeviation()).append("},");
			retVal.append("{v:").append(timingStats.getCount()).append("},");
			retVal.append("{v:").append(StatsValueRetriever.TPS_VALUE_RETRIEVER.getStatsValue(timingStats, stats.getWindowLength())).append("}");
			retVal.append("]}");
		}
		return retVal.toString();
	}

	/**
	 * Format statistics as JSON object
	 * @param stats timing statistics
	 * @return formatted statistics string
	 */
	private String formatListObjects(GroupedTimingStatistics stats) {
		StringBuilder retVal = new StringBuilder();
		for (Map.Entry<String, TimingStatistics> tagAndStats : stats.getStatisticsByTag().entrySet()) {
			if(!isFirst) {
				retVal.append(",").append(MiscUtils.NEWLINE);
			} else {
				isFirst = false;
			}
			String tag = tagAndStats.getKey();
			TimingStatistics timingStats = tagAndStats.getValue();
			retVal.append("{tag:'").append(jsonize(tag)).append("',");
			retVal.append("startTime:new Date(").append(stats.getStartTime()).append("),");
			retVal.append("stopTime:new Date(").append(stats.getStopTime()).append("),");
			retVal.append("mean:").append(timingStats.getMean()).append(",");
			retVal.append("min:").append(timingStats.getMin()).append(",");
			retVal.append("max:").append(timingStats.getMax()).append(",");
			retVal.append("stddev:").append(timingStats.getStandardDeviation()).append(",");
			retVal.append("count:").append(timingStats.getCount()).append(",");
			retVal.append("tps:").append(StatsValueRetriever.TPS_VALUE_RETRIEVER.getStatsValue(timingStats, stats.getWindowLength()));
			retVal.append("}");
		}
		return retVal.toString();
	}
	
	/**
	 * Format statistics as JSON array
	 * @param stats timing statistics
	 * @return formatted statistics string
	 */
	private String formatList(GroupedTimingStatistics stats) {
		StringBuilder retVal = new StringBuilder();
		for (Map.Entry<String, TimingStatistics> tagAndStats : stats.getStatisticsByTag().entrySet()) {
			if(!isFirst) {
				retVal.append(",").append(MiscUtils.NEWLINE);
			} else {
				isFirst = false;
			}
			String tag = tagAndStats.getKey();
			TimingStatistics timingStats = tagAndStats.getValue();
			retVal.append("['").append(jsonize(tag)).append("',");
			retVal.append("new Date(").append(stats.getStartTime()).append("),");
			retVal.append("new Date(").append(stats.getStopTime()).append("),");
			retVal.append(timingStats.getMean()).append(",");
			retVal.append(timingStats.getMin()).append(",");
			retVal.append(timingStats.getMax()).append(",");
			retVal.append(timingStats.getStandardDeviation()).append(",");
			retVal.append(timingStats.getCount()).append(",");
			retVal.append(StatsValueRetriever.TPS_VALUE_RETRIEVER.getStatsValue(timingStats, stats.getWindowLength()));
			retVal.append("]");
		}
		return retVal.toString();
	}

	public String header() {
		isFirst = true;
		StringBuilder retVal = new StringBuilder();
		switch (format) {
		case GOOGLE_DATA_TABLE:
			retVal.append("{").append(MiscUtils.NEWLINE);
			retVal.append("cols:[{id: 'tag', label:'Tag', type: 'string'},").append(MiscUtils.NEWLINE);
			retVal.append("{id: 'startTime', label:'Start Time', type: 'datetime'},").append(MiscUtils.NEWLINE);
			retVal.append("{id: 'stopTime', label:'Stop Time', type: 'datetime'},").append(MiscUtils.NEWLINE);
			retVal.append("{id: 'mean', label:'Mean', type: 'number'},").append(MiscUtils.NEWLINE);
			retVal.append("{id: 'min', label:'Min', type: 'number'},").append(MiscUtils.NEWLINE);
			retVal.append("{id: 'max', label:'Max', type: 'number'},").append(MiscUtils.NEWLINE);
			retVal.append("{id: 'stddev', label:'Standard Deviation', type: 'number'},").append(MiscUtils.NEWLINE);
			retVal.append("{id: 'count', label:'Count', type: 'number'},").append(MiscUtils.NEWLINE);
			retVal.append("{id: 'tps', label:'Transactions Per Second', type: 'number'}],").append(MiscUtils.NEWLINE);
			retVal.append("rows: [").append(MiscUtils.NEWLINE);
			break;
		default:
			retVal.append("[");
			break;
		}
		return retVal.toString();
	}

	public String footer() {
		isFirst = true;
		switch (format) {
		case GOOGLE_DATA_TABLE:
			return "]}";
		default:
			return "]";
		}
	}
	
	// --- Helper Methods ---
	
	/**
	 * JSONize a string value, performing any character escaping as needed.
	 * @param value string value, or {@code null}
	 * @return jsonized string value or {@code null} if input value was {@code null}
	 */
	private static String jsonize(String value) {
		if (value == null) {
			return null;
		}

		// Start with a buffer the size of the input string + a little to avoid
		// resizing for most strings.
		StringBuilder builder = new StringBuilder(value.length() + 5);
		int length = value.length();
		int i = 0;
		while (i < length) {
			char c = value.charAt(i++);
			switch (c) {
			case '"':
				builder.append("\\\"");
				break;
			case '\'':
				builder.append("\\\'");
				break;
			case '\\':
				builder.append("\\\\");
				break;
			case '\n':
				builder.append("\\\n");
				break;
			case '\r':
				builder.append("\\\r");
				break;
			case '\b':
				builder.append("\\\b");
				break;
			case '\f':
				builder.append("\\\f");
				break;
			default:
				builder.append(c);
			}
		}

		return builder.toString();
	}
}
