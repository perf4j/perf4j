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
package org.perf4j.aop;

/**
 * Dummy class used to test aspects.
 */
public class ProfiledObject {
    @Profiled
    public static long simpleTestDefaultTagStatic(long sleepTime) throws Exception {
        Thread.sleep(sleepTime);
        return sleepTime;
    }

    @Profiled
    public long simpleTestDefaultTag(long sleepTime) throws Exception {
        Thread.sleep(sleepTime);
        return sleepTime;
    }

    /**
     * See perf4j.properties for expected tag and message
     * @param sleepTime
     * @return
     * @throws Exception
     */
    @Profiled
    public long simpleTestDefaultTagMessageFromProperties(long sleepTime) throws Exception {
        Thread.sleep(sleepTime);
        return sleepTime;
    }

    /**
     * See perf4j.properties for expected tag and message
     * @param sleepTime
     * @return
     * @throws Exception
     */
    @Profiled
    public long simpleTestDefaultTagMessageFromPropertiesJexl(long sleepTime) throws Exception {
        Thread.sleep(sleepTime);
        return sleepTime;
    }

    @Profiled(tag = "simple")
    public long simpleTest(long sleepTime) throws Exception {
        Thread.sleep(sleepTime);
        return sleepTime;
    }

    @Profiled(tag = "simpleWithMessage", message = "message")
    public long simpleTestWithMessage(long sleepTime) throws Exception {
        Thread.sleep(sleepTime);
        return sleepTime;
    }

    @Profiled(tag = "simpleWithFails", message = "messageWithFails", logFailuresSeparately = true)
    public long simpleTestWithFailuresSeparate(long sleepTime, boolean shouldFail) throws Exception {
        Thread.sleep(sleepTime);
        if (shouldFail) {
            throw new Exception("shouldFail was true");
        }
        return sleepTime;
    }

    @Profiled(tag = "expressionTest_{$0}_{$1.name}_{$1.age}")
    public long simpleTestWithJexlTag(long sleepTime, SimpleBean bean) throws Exception {
        Thread.sleep(sleepTime);
        return sleepTime;
    }

    @Profiled(tag = "expressionTest_{$0}_{$1.name}_{$1.age}", message = "message_{$0}_{$1.name}_{$1.age}")
    public long simpleTestWithJexlTagAndMessage(long sleepTime, SimpleBean bean) throws Exception {
        Thread.sleep(sleepTime);
        return sleepTime;
    }

    @Profiled(tag = "expressionTest_{$class.name}#{$methodName}", message = "message_{$methodName}({$0},{$1.name}_{$1.age})")
    public long simpleTestWithJexlTagAndMessageClassMethod(long sleepTime, SimpleBean bean) throws Exception {
        Thread.sleep(sleepTime);
        return sleepTime;
    }
    
    @Profiled(tag = "expressionTest", message = "message_{$0}_{$1.name}_{$1.age}")
    public long simpleTestWithJexlMessageOnly(long sleepTime, SimpleBean bean) throws Exception {
        Thread.sleep(sleepTime);
        return sleepTime;
    }

    @Profiled(tag = "simpleTestWithLevel", level = "DEBUG")
    public long simpleTestWithLevel(long sleepTime) throws Exception {
        Thread.sleep(sleepTime);
        return sleepTime;
    }

    @Profiled(tag = "expressionTest_{$return}", message = "message: {$this.beanProp}, exception: {$exception}")
    public int simpleTestWithJexlThisAndReturn(long sleepTime) throws Exception {
        Thread.sleep(sleepTime);
        return 0;
    }

    @Profiled(tag = "expressionTest_{$return}", message = "message: {$this.beanProp}, exception: {$exception}")
    public int simpleTestWithJexlException(long sleepTime) throws Exception {
        throw new Exception("failure");
    }

    @Profiled(tag = "simpleWithThreshold", timeThreshold = 50)
    public long simpleTestWithTimeThreshold(long sleepTime) throws Exception {
        Thread.sleep(sleepTime);
        return sleepTime;
    }

    @Profiled(tag = "simpleWithSuffixesNoThreshold", normalAndSlowSuffixesEnabled = true)
    public long simpleTestWithSuffixesNoThreshold(long sleepTime) throws Exception {
        Thread.sleep(sleepTime);
        return sleepTime;
    }

    @Profiled(tag = "simpleWithSuffixes", timeThreshold = 50, normalAndSlowSuffixesEnabled = true)
    public long simpleTestWithSuffixes(long sleepTime) throws Exception {
        Thread.sleep(sleepTime);
        return sleepTime;
    }

    public long simpleTestUnprofiled(long sleepTime) throws Exception {
	    Thread.sleep(sleepTime);
        return sleepTime;
	}

    public long simpleTestUnprofiledNotAdvised(long sleepTime) throws Exception {
	    Thread.sleep(sleepTime);
        return sleepTime;
	}

    //this method is called using JEXL in the @Profiled tags above
    public int getBeanProp() {
        return 5;
    }

    /**
     * A simple wrapper method that will expose a call joinpoint on a Profiled method.
     * If the AbstractTimingAspect isn't correctly configured, this will result in a log
     * line in addition to the line generated by the execution of the simpleTest method.
     */
    public long simpleMethodCallExample(long sleepTime) throws Exception {
        return simpleTest(sleepTime);
    }

    public static class SimpleBean {
        private String name;
        private int age;

        public SimpleBean() { }

        public SimpleBean(String name, int age) {
            this.name = name;
            this.age = age;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public int getAge() {
            return age;
        }

        public void setAge(int age) {
            this.age = age;
        }
    }
}
