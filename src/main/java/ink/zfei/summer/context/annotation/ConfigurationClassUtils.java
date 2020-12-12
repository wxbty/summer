package ink.zfei.summer.context.annotation;

import ink.zfei.summer.annation.Component;
import ink.zfei.summer.annation.Import;
import ink.zfei.summer.beans.BeanFactoryPostProcessor;
import ink.zfei.summer.beans.BeanPostProcessor;
import ink.zfei.summer.beans.factory.annotation.AnnotatedBeanDefinition;
import ink.zfei.summer.beans.factory.config.BeanDefinition;
import ink.zfei.summer.beans.factory.support.AbstractBeanDefinition;
import ink.zfei.summer.core.Conventions;
import ink.zfei.summer.core.annotation.Bean;
import ink.zfei.summer.core.annotation.Configuration;
import ink.zfei.summer.core.type.AnnotationMetadata;
import ink.zfei.summer.core.type.classreading.MetadataReader;
import ink.zfei.summer.core.type.classreading.MetadataReaderFactory;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

abstract class ConfigurationClassUtils {

    public static final String CONFIGURATION_CLASS_FULL = "full";

    public static final String CONFIGURATION_CLASS_LITE = "lite";

    public static final String CONFIGURATION_CLASS_ATTRIBUTE =
            Conventions.getQualifiedAttributeName(ConfigurationClassPostProcessor.class, "configurationClass");


    private static final Log logger = LogFactory.getLog(ConfigurationClassUtils.class);

    private static final Set<String> candidateIndicators = new HashSet<>(8);

    static {
        candidateIndicators.add(Component.class.getName());
//        candidateIndicators.add(ComponentScan.class.getName());
        candidateIndicators.add(Import.class.getName());
//        candidateIndicators.add(ImportResource.class.getName());
    }


    /**
     * Check whether the given bean definition is a candidate for a configuration class
     * (or a nested component class declared within a configuration/component class,
     * to be auto-registered as well), and mark it accordingly.
     *
     * @param beanDef               the bean definition to check
     * @param metadataReaderFactory the current factory in use by the caller
     * @return whether the candidate qualifies as (any kind of) configuration class
     */
    public static boolean checkConfigurationClassCandidate(
            BeanDefinition beanDef, MetadataReaderFactory metadataReaderFactory) {

        String className = beanDef.getBeanClassName();
        if (className == null || beanDef.getFactoryMethodName() != null) {
            return false;
        }

        //获取bean的类元信息，beanDef可能处于各种状态（未解析class、已解析class、已生产metadata）
        //根据不同状态获取元信息
        AnnotationMetadata metadata;
        //AnnotatedBeanDefinition分三种，scan扫描|@Configuration类|配置类注册的bean，AnnotatedBeanDefinition生成时已解析metadata
        if (beanDef instanceof AnnotatedBeanDefinition &&
                className.equals(((AnnotatedBeanDefinition) beanDef).getMetadata().getClassName())) {
            // Can reuse the pre-parsed metadata from the given BeanDefinition...
            metadata = ((AnnotatedBeanDefinition) beanDef).getMetadata();
        } else if (beanDef instanceof AbstractBeanDefinition && ((AbstractBeanDefinition) beanDef).hasBeanClass()) {
            // Check already loaded Class if present...
            // since we possibly can't even load the class file for this Class.
            Class<?> beanClass = ((AbstractBeanDefinition) beanDef).getBeanClass();
            if (BeanFactoryPostProcessor.class.isAssignableFrom(beanClass) ||
                    BeanPostProcessor.class.isAssignableFrom(beanClass)) {
                return false;
            }
            //如果class已加载，直接用反射封装metadata
            metadata = AnnotationMetadata.introspect(beanClass);
        } else {
            //class未加载，用asm解析class文件，这样不会引发class加载，从而提高性能
            MetadataReader metadataReader = metadataReaderFactory.getMetadataReader(className);
            metadata = metadataReader.getAnnotationMetadata();
        }
        //根据Configuration注解判断是哪种bean
        Map<String, Object> config = metadata.getAnnotationAttributes(Configuration.class.getName());
        if (config != null && !Boolean.FALSE.equals(config.get("proxyBeanMethods"))) {
            beanDef.setAttribute(CONFIGURATION_CLASS_ATTRIBUTE, CONFIGURATION_CLASS_FULL);
        } else if (config != null || isConfigurationCandidate(metadata)) {
            beanDef.setAttribute(CONFIGURATION_CLASS_ATTRIBUTE, CONFIGURATION_CLASS_LITE);
        } else {
            return false;
        }

        // It's a full or lite configuration candidate... Let's determine the order value, if any.
//        Integer order = getOrder(metadata);
//        if (order != null) {
//            beanDef.setAttribute(ORDER_ATTRIBUTE, order);
//        }

        return true;
    }

    /**
     * Check the given metadata for a configuration class candidate
     * (or nested component class declared within a configuration/component class).
     *
     * @param metadata the metadata of the annotated class
     * @return {@code true} if the given class is to be registered for
     * configuration class processing; {@code false} otherwise
     */
    public static boolean isConfigurationCandidate(AnnotationMetadata metadata) {
        // Do not consider an interface or an annotation...
        if (metadata.isInterface()) {
            return false;
        }

        // Any of the typical annotations found?
        for (String indicator : candidateIndicators) {
            if (metadata.isAnnotated(indicator)) {
                return true;
            }
        }

        // Finally, let's look for @Bean methods...
        try {
            return metadata.hasAnnotatedMethods(Bean.class.getName());
        } catch (Throwable ex) {
            if (logger.isDebugEnabled()) {
                logger.debug("Failed to introspect @Bean methods on class [" + metadata.getClassName() + "]: " + ex);
            }
            return false;
        }
    }


}
