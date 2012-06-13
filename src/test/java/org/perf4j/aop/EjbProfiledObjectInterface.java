package org.perf4j.aop;

/**
 * This is the "session bean interface" used for the EJB interceptors test.
 */
public interface EjbProfiledObjectInterface {
    long simpleTest(long sleepTime) throws Exception;

    long simpleTestWithProfiled(long sleepTime) throws Exception;

    long simpleTestDefaultTagMessageFromProperties(long sleepTime) throws Exception;

    long simpleTestDefaultTagMessageFromPropertiesJexl(long sleepTime) throws Exception;

}
