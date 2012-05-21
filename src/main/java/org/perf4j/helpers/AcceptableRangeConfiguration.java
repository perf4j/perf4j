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

import java.io.Serializable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Instances of this class are used by the StatisticsExposingMBean to determine if JMX notifications should be sent if
 * timing statistics fall outside a specified acceptable range.
 *
 * @author Alex Devine
 */
public class AcceptableRangeConfiguration implements Serializable, Cloneable {
    private static final long serialVersionUID = -1386668939617425680L;
    private String attributeName;
    private double minValue = Double.NEGATIVE_INFINITY;
    private double maxValue = Double.POSITIVE_INFINITY;

    protected static final Pattern CONFIG_STRING_PATTERN = Pattern.compile("(.+?)\\((<(.+?)|>(.+?)|(.+?)-(.+?))\\)");

    // --- Constructors ---

    /**
     * Default constructor allows the attributeName, minValue and maxValue properties to be set later with
     * the setter methods.
     */
    public AcceptableRangeConfiguration() { }

    /**
     * Parses a configuration string to get the attributeName, minValue and maxValue properties. The format of the
     * config string should be one of:
     * <ul>
     * <li>attributeName(&lt;maxValue), where maxValue is the maximum possible acceptable value.
     * <li>attributeName(&gt;minValue), where minValue is the maximum possible acceptable value.
     * <li>attributeName(minValue-maxValue), to specify a range for acceptable values.
     * </ul>
     *
     * @param configString The configString to parse
     * @throws IllegalArgumentException Thrown if the configString did not use the acceptable format.
     */
    public AcceptableRangeConfiguration(String configString) {
        Matcher matcher = CONFIG_STRING_PATTERN.matcher(configString);
        if (matcher.matches()) {
            attributeName = matcher.group(1).trim();

            try {
                String rangeString = matcher.group(2).trim();
                if (rangeString.startsWith("<")) {
                    maxValue = Double.parseDouble(matcher.group(3).trim());
                } else if (rangeString.startsWith(">")) {
                    minValue = Double.parseDouble(matcher.group(4).trim());
                } else {
                    minValue = Double.parseDouble(matcher.group(5).trim());
                    maxValue = Double.parseDouble(matcher.group(6).trim());
                }
            } catch (Exception e) {
                throw new IllegalArgumentException("Invalid acceptable range config string: " + configString);
            }
        } else {
            throw new IllegalArgumentException("Invalid acceptable range config string: " + configString);
        }
    }

    /**
     * Creates a new AcceptableRangeConfiguration with the specified attributeName, min and max values.
     *
     * @param attributeName The name of the MBean attribute that is being constrained.
     * @param minValue      The minimum acceptable value
     * @param maxValue      The maximum acceptable value
     */
    public AcceptableRangeConfiguration(String attributeName, double minValue, double maxValue) {
        this.attributeName = attributeName;
        this.minValue = minValue;
        this.maxValue = maxValue;
    }

    // --- Bean Methods ---

    public String getAttributeName() { return attributeName; }

    public void setAttributeName(String attributeName) { this.attributeName = attributeName; }

    public double getMinValue() { return minValue; }

    public void setMinValue(double minValue) { this.minValue = minValue; }

    public double getMaxValue() { return maxValue; }

    public void setMaxValue(double maxValue) { this.maxValue = maxValue; }

    // --- Utility Methods ---

    /**
     * Determines whether or not the specified value is within the acceptable range.
     *
     * @param value The value to check
     * @return returns true if value >= minValue && value <= maxValue, false otherwise
     */
    public boolean isInRange(double value) {
        return value >= minValue && value <= maxValue;
    }

    // --- Object Methods ---

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof AcceptableRangeConfiguration)) {
            return false;
        }

        AcceptableRangeConfiguration that = (AcceptableRangeConfiguration) o;

        return Double.compare(that.maxValue, maxValue) == 0 && 
               Double.compare(that.minValue, minValue) == 0 &&
               (attributeName == null ? that.attributeName == null : attributeName.equals(that.attributeName));
    }

    public int hashCode() {
        int result;
        long temp;
        result = (attributeName != null ? attributeName.hashCode() : 0);
        temp = minValue != +0.0d ? Double.doubleToLongBits(minValue) : 0L;
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        temp = maxValue != +0.0d ? Double.doubleToLongBits(maxValue) : 0L;
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        return result;
    }

    public String toString() {
        if (minValue == Double.NEGATIVE_INFINITY) {
            return attributeName + "(<" + maxValue + ")";
        } else if (maxValue == Double.POSITIVE_INFINITY) {
            return attributeName + "(>" + minValue + ")";
        } else {
            return attributeName + "(" + minValue + "-" + maxValue + ")";
        }
    }

    public AcceptableRangeConfiguration clone() {
        try {
            return (AcceptableRangeConfiguration) super.clone();
        } catch (Exception e) {
            throw new Error("Unexpected CloneNotSupportedException");
        }
    }
}
