package ink.zfei.summer.core.annotation;

import ink.zfei.summer.beans.factory.annotation.AutowiredAnnotationBeanPostProcessor;
import ink.zfei.summer.beans.factory.config.BeanDefinition;
import ink.zfei.summer.context.annotation.CommonAnnotationBeanPostProcessor;
import ink.zfei.summer.core.AbstractApplicationContext;
import ink.zfei.summer.beans.factory.support.GenericBeanDefinition;

public class AnnotationConfigUtils {


    public static final String CONFIGURATION_ANNOTATION_PROCESSOR_BEAN_NAME =
            "org.springframework.context.annotation.internalConfigurationAnnotationProcessor";

    public static final String COMMON_ANNOTATION_PROCESSOR_BEAN_NAME =
            "org.springframework.context.annotation.internalCommonAnnotationProcessor";

    public static final String AUTOWIRED_ANNOTATION_PROCESSOR_BEAN_NAME =
            "org.springframework.context.annotation.internalAutowiredAnnotationProcessor";

    public static void registerAnnotationConfigProcessors(AbstractApplicationContext registry) {

        GenericBeanDefinition def = new GenericBeanDefinition(ConfigurationClassPostProcessor.class);
        def.setId(CONFIGURATION_ANNOTATION_PROCESSOR_BEAN_NAME);
        def.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
        registry.registerBeanDefinition(def);

        def = new GenericBeanDefinition(CommonAnnotationBeanPostProcessor.class);
        def.setId(COMMON_ANNOTATION_PROCESSOR_BEAN_NAME);
        def.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
        registry.registerBeanDefinition(def);

        def = new GenericBeanDefinition(AutowiredAnnotationBeanPostProcessor.class);
        def.setId(AUTOWIRED_ANNOTATION_PROCESSOR_BEAN_NAME);
        def.setSource(null);
        def.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
        registry.registerBeanDefinition(def);
    }
}