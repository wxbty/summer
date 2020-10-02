package context.common;

import bean.Person;
import ink.zfei.summer.beans.BeanPostProcessor;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

public class LogBeanPostProcessor implements BeanPostProcessor {

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) {

        if (bean instanceof Iperson) {
            LogInvocationHandler invocationHandler = new LogInvocationHandler(bean);
            Object proxy = Proxy.newProxyInstance(LogBeanPostProcessor.class.getClassLoader(), new Class[]{Iperson.class}, invocationHandler);
            return proxy;
        }
        return bean;
    }


    public static class LogInvocationHandler implements InvocationHandler {

        private Object target;

        public LogInvocationHandler(Object target) {
            this.target = target;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

            String methodName = method.getName();
            Class<?>[] parameterTypes = method.getParameterTypes();

            // 拦截定义在 Object 类中的方法（未被子类重写），比如 wait/notify
            if (method.getDeclaringClass() == Object.class) {
                return method.invoke(target, args);
            }

            // 如果 toString、hashCode 和 equals 等方法被子类重写了，这里也直接调用
            if ("toString".equals(methodName) && parameterTypes.length == 0) {
                return target.toString();
            }
            if ("hashCode".equals(methodName) && parameterTypes.length == 0) {
                return target.hashCode();
            }
            if ("equals".equals(methodName) && parameterTypes.length == 1) {
                return target.equals(args[0]);
            }

            System.out.println("person invoke befre" + method.getName());
            Object result = method.invoke(target, args);
            System.out.println("person invoke after");
            return result;
        }
    }
}
