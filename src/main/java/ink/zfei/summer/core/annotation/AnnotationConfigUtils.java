package ink.zfei.summer.core.annotation;

import ink.zfei.summer.core.AbstractApplicationContext;
import ink.zfei.summer.beans.factory.support.GenericBeanDefinition;

public class AnnotationConfigUtils {
    public static void registerAnnotationConfigProcessors(AbstractApplicationContext annotationConfigApplicationContext) {

        GenericBeanDefinition beanDefinition = new GenericBeanDefinition();
        beanDefinition.setBeanClassName(ConfigurationClassPostProcessor.class.getPackage().getName()+".ConfigurationClassPostProcessor");
        beanDefinition.setId("org.springframework.context.annotation.internalConfigurationAnnotationProcessor");

        annotationConfigApplicationContext.registerBeanDefinition(beanDefinition);
    }
}
