package ink.zfei.summer.beans.factory.support;

import java.lang.reflect.Method;

public interface MethodReplacer {

    /**
     * Reimplement the given method.
     * @param obj the instance we're reimplementing the method for
     * @param method the method to reimplement
     * @param args arguments to the method
     * @return return value for the method
     */
    Object reimplement(Object obj, Method method, Object[] args) throws Throwable;

}
