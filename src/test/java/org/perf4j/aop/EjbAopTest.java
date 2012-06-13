package org.perf4j.aop;

import junit.framework.TestCase;

import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptors;
import javax.interceptor.InvocationContext;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;

/**
 * This class tests the AbstractEjbTimingAspect interceptor. We don't really use a J2EE server for this test, instead
 * we create our own dynamic proxy, similar to what a J2EE server would do.
 */
public class EjbAopTest extends TestCase {
    EjbProfiledObjectInterface profiledObject;

    protected void setUp() throws Exception {
        super.setUp();
        //create the profiled object
        profiledObject = wrapBean(EjbProfiledObjectInterface.class, EjbProfiledObject.class);
    }

    @SuppressWarnings("unchecked")
    protected <T> T wrapBean(Class<T> beanInterface, final Class<? extends T> beanClass) throws Exception {
        //intercept the methods on the bean class as necessary
        final Map<String, Object> methodNameToInterceptorMap = new HashMap<String, Object>();

        for (Method method : beanClass.getMethods()) {
            Interceptors interceptorsAnnotation = method.getAnnotation(Interceptors.class);
            if (interceptorsAnnotation != null) {
                //for the simplicity of this test case we only get the first interceptor
                Object interceptor = interceptorsAnnotation.value()[0].newInstance();
                methodNameToInterceptorMap.put(method.getName(), interceptor);
            }
        }

        //create the dynamic proxy
        final Object beanInstance = beanClass.newInstance();
        return (T) Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class[] {beanInterface},
                new InvocationHandler() {
                    public Object invoke(final Object proxy, final Method method, final Object[] args) throws Throwable {
                        Method interceptorMethod = null;

                        Object interceptor = methodNameToInterceptorMap.get(method.getName());
                        if (interceptor != null) {
                            //for simplicity, we only get the first around invoke method
                            for (Method anInterceptorMethod : interceptor.getClass().getMethods()) {
                                if (anInterceptorMethod.getAnnotation(AroundInvoke.class) != null) {
                                    interceptorMethod = anInterceptorMethod;
                                    break;
                                }
                            }
                        }

                        //find the corresponding INSTANCE method on the bean, DON'T USE THE INTERFACE METHOD
                        final Method instanceMethod =
                                beanClass.getDeclaredMethod(method.getName(), method.getParameterTypes());

                        if (interceptorMethod != null) {
                            InvocationContext ctx = new InvocationContext() {
                                public Object getTarget() {  return beanInstance; }

                                public Method getMethod() { return instanceMethod; }

                                public Object[] getParameters() { return args; }

                                public void setParameters(Object[] objects) { /* not supported */ }

                                public Map<String, Object> getContextData() { return new HashMap<String, Object>(); }

                                public Object proceed() throws Exception { return method.invoke(beanInstance, args); }
                            };

                            return interceptorMethod.invoke(interceptor, ctx);
                        } else {
                            return instanceMethod.invoke(beanInstance, args);
                        }
                    }
                });
    }

    public void testEjbAspects() throws Exception {
        assertEquals(50, profiledObject.simpleTest(50));
        assertTrue(EjbInMemoryTimingAspect.getLastLoggedString().contains("tag[simpleTest]"));

        assertEquals(100, profiledObject.simpleTestWithProfiled(100));
        assertTrue(EjbInMemoryTimingAspect.getLastLoggedString().contains("tag[usingProfiled]"));

        profiledObject.simpleTestDefaultTagMessageFromProperties(5);
        assertTrue("Expected tag not found in " + EjbInMemoryTimingAspect.getLastLoggedString(),
                EjbInMemoryTimingAspect.getLastLoggedString().indexOf("tag[customTag]") >= 0);
        assertTrue("Expected tag not found in " + EjbInMemoryTimingAspect.getLastLoggedString(),
                EjbInMemoryTimingAspect.getLastLoggedString().indexOf("message[customMessage]") >= 0);

        profiledObject.simpleTestDefaultTagMessageFromPropertiesJexl(5);
        assertTrue("Expected tag not found in " + EjbInMemoryTimingAspect.getLastLoggedString(),
                EjbInMemoryTimingAspect.getLastLoggedString().indexOf("tag[org.perf4j.aop.EjbProfiledObject#simpleTestDefaultTagMessageFromPropertiesJexl]") >= 0);
        assertTrue("Expected tag not found in " + EjbInMemoryTimingAspect.getLastLoggedString(),
                EjbInMemoryTimingAspect.getLastLoggedString().indexOf("message[simpleTestDefaultTagMessageFromPropertiesJexl(5)]") >= 0);
    }
}
