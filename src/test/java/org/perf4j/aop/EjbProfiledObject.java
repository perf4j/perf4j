package org.perf4j.aop;

import javax.interceptor.Interceptors;

/**
 * This class is used to test the AbstractEjbTimingAspect
 */
public class EjbProfiledObject implements EjbProfiledObjectInterface {
    @Interceptors(EjbInMemoryTimingAspect.class)
    public long simpleTest(long sleepTime) throws Exception {
        Thread.sleep(sleepTime);
        return sleepTime;
    }

    @Interceptors(EjbInMemoryTimingAspect.class)
    @Profiled(tag = "usingProfiled")
    public long simpleTestWithProfiled(long sleepTime) throws Exception {
        Thread.sleep(sleepTime);
        return sleepTime;
    }

    /**
     * See perf4j.properties for expected tag and message
     * @param sleepTime
     * @return
     * @throws Exception
     */
    @Interceptors(EjbInMemoryTimingAspect.class)
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
    @Interceptors(EjbInMemoryTimingAspect.class)
    @Profiled
    public long simpleTestDefaultTagMessageFromPropertiesJexl(long sleepTime) throws Exception {
        Thread.sleep(sleepTime);
        return sleepTime;
    }

}
