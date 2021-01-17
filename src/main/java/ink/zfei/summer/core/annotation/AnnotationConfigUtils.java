package ink.zfei.summer.core.annotation;

import ink.zfei.summer.beans.BeanDefinitionRegistry;
import ink.zfei.summer.beans.factory.annotation.AnnotatedBeanDefinition;
import ink.zfei.summer.beans.factory.annotation.AutowiredAnnotationBeanPostProcessor;
import ink.zfei.summer.beans.factory.config.BeanDefinition;
import ink.zfei.summer.context.annotation.CommonAnnotationBeanPostProcessor;
import ink.zfei.summer.context.annotation.ConfigurationClassPostProcessor;
import ink.zfei.summer.beans.factory.support.GenericBeanDefinition;
import ink.zfei.summer.context.annotation.Role;
import ink.zfei.summer.core.type.AnnotatedTypeMetadata;
import ink.zfei.summer.lang.Nullable;

public class AnnotationConfigUtils {


    public static final String CONFIGURATION_ANNOTATION_PROCESSOR_BEAN_NAME =
            "org.springframework.context.annotation.internalConfigurationAnnotationProcessor";

    public static final String COMMON_ANNOTATION_PROCESSOR_BEAN_NAME =
            "org.springframework.context.annotation.internalCommonAnnotationProcessor";

    public static final String AUTOWIRED_ANNOTATION_PROCESSOR_BEAN_NAME =
            "org.springframework.context.annotation.internalAutowiredAnnotationProcessor";

    public static void registerAnnotationConfigProcessors(BeanDefinitionRegistry registry) {

        GenericBeanDefinition def = new GenericBeanDefinition(ConfigurationClassPostProcessor.class);
        def.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
        registry.registerBeanDefinition(CONFIGURATION_ANNOTATION_PROCESSOR_BEAN_NAME, def);

        def = new GenericBeanDefinition(CommonAnnotationBeanPostProcessor.class);
        def.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
        registry.registerBeanDefinition(COMMON_ANNOTATION_PROCESSOR_BEAN_NAME,def);

        def = new GenericBeanDefinition(AutowiredAnnotationBeanPostProcessor.class);
        def.setSource(null);
        def.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
        registry.registerBeanDefinition(AUTOWIRED_ANNOTATION_PROCESSOR_BEAN_NAME,def);
    }

    @Nullable
    public static AnnotationAttributes attributesFor(AnnotatedTypeMetadata metadata, Class<?> annotationClass) {
        return attributesFor(metadata, annotationClass.getName());
    }

    @Nullable
    static AnnotationAttributes attributesFor(AnnotatedTypeMetadata metadata, String annotationClassName) {
        return AnnotationAttributes.fromMap(metadata.getAnnotationAttributes(annotationClassName, false));
    }

    public static void processCommonDefinitionAnnotations(AnnotatedBeanDefinition abd, AnnotatedTypeMetadata metadata) {

        AnnotationAttributes role = attributesFor(metadata, Role.class);
        if (role != null) {
            abd.setRole(role.getNumber("value").intValue());
        }
        AnnotationAttributes description = attributesFor(metadata, Description.class);
        if (description != null) {
            abd.setDescription(description.getString("value"));
        }
    }
}