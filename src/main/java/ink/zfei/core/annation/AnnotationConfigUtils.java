package ink.zfei.core.annation;

import ink.zfei.core.AbstractApplicationContext;
import ink.zfei.core.GenericBeanDefinition;

public class AnnotationConfigUtils {
    public static void registerAnnotationConfigProcessors(AbstractApplicationContext annotationConfigApplicationContext) {

        GenericBeanDefinition beanDefinition = new GenericBeanDefinition();
        beanDefinition.setBeanClassName(ConfigurationClassPostProcessor.class.getPackage().getName()+".ConfigurationClassPostProcessor");
        beanDefinition.setId("org.springframework.context.annotation.internalConfigurationAnnotationProcessor");

        annotationConfigApplicationContext.registerBeanDefinition(beanDefinition);
    }
}
