package ink.zfei.core.annation;

import ink.zfei.context.AnnotationConfigApplicationContext;
import ink.zfei.core.AbstractApplicationContext;
import ink.zfei.core.BeanDefinition;

public class AnnotationConfigUtils {
    public static void registerAnnotationConfigProcessors(AbstractApplicationContext annotationConfigApplicationContext) {

        BeanDefinition beanDefinition = new BeanDefinition();
        beanDefinition.setBeanClass(ConfigurationClassPostProcessor.class.getPackage().getName()+".ConfigurationClassPostProcessor");
        beanDefinition.setId("org.springframework.context.annotation.internalConfigurationAnnotationProcessor");

        annotationConfigApplicationContext.registerBeanDefinition(beanDefinition);
    }
}
