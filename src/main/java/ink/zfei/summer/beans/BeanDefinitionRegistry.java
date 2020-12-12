package ink.zfei.summer.beans;

import ink.zfei.summer.beans.factory.config.BeanDefinition;
import ink.zfei.summer.beans.factory.support.GenericBeanDefinition;

public interface BeanDefinitionRegistry {

    void registerBeanDefinition(String beanName, BeanDefinition beanDefinition);

//    void registerBeanDefinition(GenericBeanDefinition beanDefinition);

    void removeBeanDefinition(String beanName);


    BeanDefinition getBeanDefinition(String beanName);

    int getBeanDefinitionCount();

    String[] getBeanDefinitionNames();
}
