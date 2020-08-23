package ink.zfei.beans;

import ink.zfei.core.AbstractApplicationContext;

public interface BeanDefinitionRegistryPostProcessor extends BeanFactoryPostProcessor {


    void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry);

}