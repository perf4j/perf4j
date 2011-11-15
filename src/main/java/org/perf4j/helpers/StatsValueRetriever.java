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

import org.perf4j.TimingStatistics;

import java.util.Map;
import java.util.Collections;
import java.util.LinkedHashMap;

/**
 * The StatsValueRetriever is used to enable retrieval of any of the statistics on the TimingStatistics object
 * by name. In addition, retrieval of a transactions per second statistic is supported.
 *
 * @author Alex Devine
 */
public abstract class StatsValueRetriever {
    public static final StatsValueRetriever MEAN_VALUE_RETRIEVER = new StatsValueRetriever() {
        public Number getStatsValue(TimingStatistics timingStats, long windowLength) {
            return (timingStats == null) ? 0.0 : timingStats.getMean();
        }

        public Class<Double> getValueClass() { return Double.class; }

        public String getValueName() { return "Mean"; }
    };

    public static final StatsValueRetriever STD_DEV_VALUE_RETRIEVER = new StatsValueRetriever() {
        public Number getStatsValue(TimingStatistics timingStats, long windowLength) {
            return (timingStats == null) ? 0.0 : timingStats.getStandardDeviation();
        }

        public Class<Double> getValueClass() { return Double.class; }

        public String getValueName() { return "StdDev"; }
    };

    public static final StatsValueRetriever MIN_VALUE_RETRIEVER = new StatsValueRetriever() {
        public Number getStatsValue(TimingStatistics timingStats, long windowLength) {
            return (timingStats == null) ? 0L : timingStats.getMin();
        }

        public Class<Long> getValueClass() { return Long.class; }

        public String getValueName() { return "Min"; }
    };

    public static final StatsValueRetriever MAX_VALUE_RETRIEVER = new StatsValueRetriever() {
        public Number getStatsValue(TimingStatistics timingStats, long windowLength) {
            return (timingStats == null) ? 0L : timingStats.getMax();
        }

        public Class<Long> getValueClass() { return Long.class; }

        public String getValueName() { return "Max"; }
    };

    public static final StatsValueRetriever COUNT_VALUE_RETRIEVER = new StatsValueRetriever() {
        public Number getStatsValue(TimingStatistics timingStats, long windowLength) {
            return (timingStats == null) ? 0 : timingStats.getCount();
        }

        public Class<Integer> getValueClass() { return Integer.class; }

        public String getValueName() { return "Count"; }
    };

    public static final StatsValueRetriever TPS_VALUE_RETRIEVER = new StatsValueRetriever() {
        public Number getStatsValue(TimingStatistics timingStats, long windowLength) {
            return (timingStats == null || windowLength == 0) ?
                   0.0 :
                   ((double) timingStats.getCount()) / (((double) windowLength) / 1000.0);
        }

        public Class<Double> getValueClass() { return Double.class; }

        public String getValueName() { return "TPS"; }
    };

    /**
     * Default unmodifiable Map of statistic name to the corresponding StatsValueRetriever object that retrieves that
     * statistic. Statistic names are Mean, StdDev, Min, Max, Count and TPS.
     */
    public static final Map<String, StatsValueRetriever> DEFAULT_RETRIEVERS;
    static {
        Map<String, StatsValueRetriever> defaultRetrievers = new LinkedHashMap<String, StatsValueRetriever>();
        defaultRetrievers.put(MEAN_VALUE_RETRIEVER.getValueName(), MEAN_VALUE_RETRIEVER);
        defaultRetrievers.put(STD_DEV_VALUE_RETRIEVER.getValueName(), STD_DEV_VALUE_RETRIEVER);
        defaultRetrievers.put(MIN_VALUE_RETRIEVER.getValueName(), MIN_VALUE_RETRIEVER);
        defaultRetrievers.put(MAX_VALUE_RETRIEVER.getValueName(), MAX_VALUE_RETRIEVER);
        defaultRetrievers.put(COUNT_VALUE_RETRIEVER.getValueName(), COUNT_VALUE_RETRIEVER);
        defaultRetrievers.put(TPS_VALUE_RETRIEVER.getValueName(), TPS_VALUE_RETRIEVER);
        DEFAULT_RETRIEVERS = Collections.unmodifiableMap(defaultRetrievers);
    }

    /**
     * Retrieves a single statistic value from the specified TimingStatistics object.
     *
     * @param timingStats  The TimingStatistics object containing the data to be retrieved.
     *                     May be null, if so 0 is returned.
     * @param windowLength The length of time, in milliseconds, of the data window represented by the TimingStatistics.
     * @return The value requested.
     */
    public abstract Number getStatsValue(TimingStatistics timingStats, long windowLength);

    /**
     * Gets the class of the object returned by {@link #getStatsValue(org.perf4j.TimingStatistics, long)}.
     *
     * @return The value class.
     */
    public abstract Class<? extends Number> getValueClass();

    /**
     * Returns the name of the value, such as "Mean" or "Max".
     *
     * @return The name of the value retrieved.
     */
    public abstract String getValueName();
}
