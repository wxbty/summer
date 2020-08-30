package ink.zfei.beans;

import ink.zfei.core.GenericBeanDefinition;

public interface BeanDefinitionRegistry {

    void registerBeanDefinition(String beanName, GenericBeanDefinition beanDefinition);

    void registerBeanDefinition(GenericBeanDefinition beanDefinition);

    void removeBeanDefinition(String beanName);

    GenericBeanDefinition getBeanDefinition(String beanName);

}
