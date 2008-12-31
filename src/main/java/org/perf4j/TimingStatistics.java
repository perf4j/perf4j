/* Copyright Homeaway, Inc 2005-2007. All Rights Reserved.
 * No unauthorized use of this software.
 */
package org.perf4j;

import java.util.Collection;
import java.io.Serializable;

/**
 * TimingStatistics represent a set of statistical measures over a set of timing data, such as a collection of
 * StopWatch instances.
 * 
 * @author Alex Devine
 */
public class TimingStatistics implements Serializable, Cloneable {
    private double mean;
    private double standardDeviation;
    private long max;
    private long min;
    private int count;

    // --- Constructors ---

    public TimingStatistics() { }

    public TimingStatistics(double mean, double standardDeviation, long max, long min, int count) {
        this.mean = mean;
        this.standardDeviation = standardDeviation;
        this.max = max;
        this.min = min;
        this.count = count;
    }

    public TimingStatistics(Collection<StopWatch> timeRecords) {
        if (timeRecords.isEmpty()) {
            return;
        }

        this.count = 0;
        this.min = Long.MAX_VALUE;
        this.max = Long.MIN_VALUE;
        long sum = 0L;
        long[] elapsedTimes = new long[timeRecords.size()];

        //calculate min, max and mean
        for (StopWatch timeRecord : timeRecords) {
            long elapsedTime = timeRecord.getElapsedTime();
            sum += elapsedTime;
            this.min = Math.min(this.min, elapsedTime);
            this.max = Math.max(this.max, elapsedTime);
            elapsedTimes[this.count++] = elapsedTime;
        }

        this.mean = ((double)sum) / ((double)this.count);

        //calculate standard deviation
        double sumOfDeviation = 0.0;
        for (long elapsedTime : elapsedTimes) {
            sumOfDeviation += Math.pow(elapsedTime - this.mean, 2.0);
        }
        this.standardDeviation = Math.sqrt(sumOfDeviation / this.count);
    }

    // --- Bean Properties ---

    public double getMean() {
        return mean;
    }

    public void setMean(double mean) {
        this.mean = mean;
    }

    public double getStandardDeviation() {
        return standardDeviation;
    }

    public void setStandardDeviation(double standardDeviation) {
        this.standardDeviation = standardDeviation;
    }

    public long getMax() {
        return max;
    }

    public void setMax(long max) {
        this.max = max;
    }

    public long getMin() {
        return min;
    }

    public void setMin(long min) {
        this.min = min;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    // --- Object Methods ---

    public String toString() {
        return "mean[" + mean +
               "] stddiv[" + standardDeviation +
               "] min[" + standardDeviation +
               "] max[" + standardDeviation +
               "] count[" + standardDeviation + "]";
    }

    public TimingStatistics clone()  {
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
        if (Double.compare(that.standardDeviation, standardDeviation) != 0) {
            return false;
        }

        return true;
    }

    public int hashCode() {
        int result;
        long temp;
        temp = mean != +0.0d ? Double.doubleToLongBits(mean) : 0L;
        result = (int) (temp ^ (temp >>> 32));
        temp = standardDeviation != +0.0d ? Double.doubleToLongBits(standardDeviation) : 0L;
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        result = 31 * result + (int) (max ^ (max >>> 32));
        result = 31 * result + (int) (min ^ (min >>> 32));
        result = 31 * result + count;
        return result;
    }
}
