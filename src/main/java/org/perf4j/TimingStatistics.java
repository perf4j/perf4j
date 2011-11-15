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

import java.io.Serializable;

/**
 * TimingStatistics represent a set of statistical measures over a set of timing data, such as a collection of
 * StopWatch instances.
 *
 * @author Alex Devine
 */
public class TimingStatistics implements Serializable, Cloneable {
    private static final long serialVersionUID = 2854670870560621993L;
    private double mean;
    private double runningQ; //for keeping running standard deviation
    private long max;
    private long min;
    private int count;

    // --- Constructors ---
    /**
     * Default constructor allows you to set performance statistics later using the setter methods.
     */
    public TimingStatistics() { }

    /**
     * Creates a TimingStatistics object with the specified data.
     *
     * @param mean              The mean execution time, in ms, of the underlying time records.
     * @param standardDeviation The standard deviation, in ms, of the underlying time records.
     * @param max               The maximum value in ms of the logged execution times.
     * @param min               The minimum value in ms of the logged execution times.
     * @param count             The total number of executions that were timed.
     */
    public TimingStatistics(double mean, double standardDeviation, long max, long min, int count) {
        this.mean = mean;
        this.runningQ = Math.pow(standardDeviation, 2.0) * count;
        this.max = max;
        this.min = min;
        this.count = count;
    }

    // --- Utility Methods ---
    /**
     * This method updates the calculated statistics with a new logged execution time.
     *
     * @param elapsedTime The elapsed time being used to update the statistics.
     * @return this TimingStatistics instance
     */
    public TimingStatistics addSampleTime(long elapsedTime) {
        count++;

        double diffFromMean = elapsedTime - mean;
        mean = mean + (diffFromMean / count);

        runningQ = runningQ + (((count - 1) * Math.pow(diffFromMean, 2.0)) / count);

        //special case initial stopWatch when finding max and min
        if (count == 1) {
            min = elapsedTime;
            max = elapsedTime;
        } else {
            if (elapsedTime < min) {
                min = elapsedTime;
            }
            if (elapsedTime > max) {
                max = elapsedTime;
            }
        }

        return this;
    }

    // --- Bean Properties ---

    public double getMean() {
        return mean;
    }

    public double getStandardDeviation() {
        return Math.sqrt(runningQ / count);
    }

    public long getMax() {
        return max;
    }

    public long getMin() {
        return min;
    }

    public int getCount() {
        return count;
    }

    // --- Object Methods ---

    public String toString() {
        return "mean[" + getMean() +
               "] stddev[" + getStandardDeviation() +
               "] min[" + getMin() +
               "] max[" + getMax() +
               "] count[" + getCount() + "]";
    }

    public TimingStatistics clone() {
        try {
            return (TimingStatistics) super.clone();
        } catch (CloneNotSupportedException cnse) {
            throw new Error("Unexpected CloneNotSupportedException");
        }
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof TimingStatistics)) {
            return false;
        }

        TimingStatistics that = (TimingStatistics) o;

        if (count != that.count) {
            return false;
        }
        if (max != that.max) {
            return false;
        }
        if (Double.compare(that.mean, mean) != 0) {
            return false;
        }
        if (min != that.min) {
            return false;
        }
        if (Double.compare(that.runningQ, runningQ) != 0) {
            return false;
        }

        return true;
    }

    public int hashCode() {
        int result;
        long temp;
        temp = mean != +0.0d ? Double.doubleToLongBits(mean) : 0L;
        result = (int) (temp ^ (temp >>> 32));
        temp = runningQ != +0.0d ? Double.doubleToLongBits(runningQ) : 0L;
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        result = 31 * result + (int) (max ^ (max >>> 32));
        result = 31 * result + (int) (min ^ (min >>> 32));
        result = 31 * result + count;
        return result;
    }
}
