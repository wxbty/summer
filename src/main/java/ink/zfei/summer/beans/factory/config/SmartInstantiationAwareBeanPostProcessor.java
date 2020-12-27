package ink.zfei.summer.beans.factory.config;

import ink.zfei.summer.beans.InstantiationAwareBeanPostProcessor;
import ink.zfei.summer.lang.Nullable;

import java.lang.reflect.Constructor;

/**
 * 实例化后置处理器子类，用于预测已处理bean的最终类型
 * 注意：专用接口，框架内部使用
 */
public interface SmartInstantiationAwareBeanPostProcessor extends InstantiationAwareBeanPostProcessor {


    @Nullable
    default Class<?> predictBeanType(Class<?> beanClass, String beanName) {
        return null;
    }


    @Nullable
    default Constructor<?>[] determineCandidateConstructors(Class<?> beanClass, String beanName) {

        return null;
    }


    default Object getEarlyBeanReference(Object bean, String beanName) {
        return bean;
    }

}
