package ink.zfei.summer.beans;

import ink.zfei.summer.core.GenericBeanDefinition;

public interface BeanDefinitionRegistry {

    void registerBeanDefinition(String beanName, GenericBeanDefinition beanDefinition);

    void registerBeanDefinition(GenericBeanDefinition beanDefinition);

    void removeBeanDefinition(String beanName);

    GenericBeanDefinition getBeanDefinition(String beanName);

}
