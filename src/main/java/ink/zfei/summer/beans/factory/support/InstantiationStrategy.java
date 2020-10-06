package ink.zfei.summer.beans.factory.support;

import ink.zfei.summer.beans.factory.BeanFactory;
import ink.zfei.summer.lang.Nullable;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

public interface InstantiationStrategy {


    Object instantiate(GenericBeanDefinition bd, @Nullable String beanName, BeanFactory owner);

    Object instantiate(GenericBeanDefinition bd, @Nullable String beanName, BeanFactory owner,
                       Constructor<?> ctor, Object... args);


    Object instantiate(GenericBeanDefinition bd, @Nullable String beanName, BeanFactory owner,
                       @Nullable Object factoryBean, Method factoryMethod, Object... args);


}
