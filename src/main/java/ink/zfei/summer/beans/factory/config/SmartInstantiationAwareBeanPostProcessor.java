package ink.zfei.summer.beans.factory.config;

import ink.zfei.summer.beans.InstantiationAwareBeanPostProcessor;
import ink.zfei.summer.lang.Nullable;

import java.lang.reflect.Constructor;

public interface SmartInstantiationAwareBeanPostProcessor extends InstantiationAwareBeanPostProcessor {


    @Nullable
    default Class<?> predictBeanType(Class<?> beanClass, String beanName) {
        return null;
    }


    @Nullable
    default Constructor<?>[] determineCandidateConstructors(Class<?> beanClass, String beanName)
            {

        return null;
    }


    default Object getEarlyBeanReference(Object bean, String beanName) {
        return bean;
    }

}
