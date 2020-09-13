package ink.zfei.summer.core;

import ink.zfei.summer.context.AnnotationConfigApplicationContext;

public interface ImportBeanDefinitionRegistrar {
   void registerBeanDefinitions(AnnotationConfigApplicationContext registry, Class configClass);
}