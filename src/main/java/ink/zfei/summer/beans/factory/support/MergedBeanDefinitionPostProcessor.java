package ink.zfei.summer.beans.factory.support;

import ink.zfei.summer.beans.BeanPostProcessor;

public interface MergedBeanDefinitionPostProcessor extends BeanPostProcessor {


    void postProcessMergedBeanDefinition(GenericBeanDefinition beanDefinition, Class<?> beanType, String beanName);

    default void resetBeanDefinition(String beanName) {
    }

}
