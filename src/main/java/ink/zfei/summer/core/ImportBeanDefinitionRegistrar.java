package ink.zfei.summer.core;

import ink.zfei.summer.beans.BeanDefinitionRegistry;

public interface ImportBeanDefinitionRegistrar {
   void registerBeanDefinitions(BeanDefinitionRegistry registry, Class configClass);
}