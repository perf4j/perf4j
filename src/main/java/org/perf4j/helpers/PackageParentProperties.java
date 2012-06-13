/* Copyright (c) 2011 Brett Randall.
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

import java.util.Properties;

public class PackageParentProperties extends Properties {

    private static final long serialVersionUID = 4732002255463533934L;


    public PackageParentProperties() {
        super();
    }

    public PackageParentProperties(Properties defaults) {
        super(defaults);
    }

    @Override
    public synchronized Object get(Object key) {

        if (key == null) {
            throw new NullPointerException();
        }

        if (!(key instanceof String)) {
            return super.get(key);
        }

        String keyString = (String) key;

        Object o = super.get(keyString);
        if (o != null) {
            // found at current position
            return o;
        }

        // search parent if exists
        if (keyString.contains(".")) {
            // com.some.package.SomeClass -> com.some.package
            // com.some.package -> com.some
            // com.some -> com
            keyString = keyString.substring(0, keyString.lastIndexOf('.'));
            return get(keyString);
        } else {
            // all parent keys exhausted, look no further
            return null;
        }
    }

    @Override
    public String getProperty(String key) {
        Object oval = get(key);
        String sval = (oval instanceof String) ? (String)oval : null;
        return ((sval == null) && (defaults != null)) ? defaults.getProperty(key) : sval;
    }

    @Override
    public String getProperty(String key, String defaultValue) {
        String val = getProperty(key);
        return (val == null) ? defaultValue : val;
    }
}
