package org.perf4j.aop;

import org.perf4j.StopWatch;

import java.lang.annotation.Annotation;

/**
 * This unusual concrete implementation of this Profiled annotation interface is used for cases where some
 * interception frameworks may want to profile methods that DON'T have a profiled annotation (for example, EJB 3.0
 * interceptors). See the code for {@link org.perf4j.aop.AbstractEjbTimingAspect} for an example of how this is
 * used.
 */
@SuppressWarnings("all")
public class DefaultProfiled implements Profiled {
    public static final DefaultProfiled INSTANCE = new DefaultProfiled();

    private DefaultProfiled() { }

    public String tag() { return DEFAULT_TAG_NAME; }

    public String message() { return ""; }

    public String logger() { return StopWatch.DEFAULT_LOGGER_NAME; }

    public String level() { return "INFO"; }

    public boolean el() { return true; }

    public boolean logFailuresSeparately() { return false; }

    public long timeThreshold() { return 0; }
    
    public boolean normalAndSlowSuffixesEnabled() { return false; }
    
    public Class<? extends Annotation> annotationType() { return getClass(); }
}
