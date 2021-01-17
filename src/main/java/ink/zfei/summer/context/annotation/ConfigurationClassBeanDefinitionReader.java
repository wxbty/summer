package ink.zfei.summer.context.annotation;

import ink.zfei.summer.beans.BeanDefinitionRegistry;
import ink.zfei.summer.beans.factory.annotation.AnnotatedBeanDefinition;
import ink.zfei.summer.beans.factory.annotation.Autowire;
import ink.zfei.summer.beans.factory.config.BeanDefinition;
import ink.zfei.summer.beans.factory.support.AbstractBeanDefinition;
import ink.zfei.summer.beans.factory.support.GenericBeanDefinition;
import ink.zfei.summer.core.annotation.AnnotationAttributes;
import ink.zfei.summer.core.annotation.AnnotationConfigUtils;
import ink.zfei.summer.core.annotation.Bean;
import ink.zfei.summer.core.type.AnnotationMetadata;
import ink.zfei.summer.core.type.MethodMetadata;
import ink.zfei.summer.core.type.StandardAnnotationMetadata;
import ink.zfei.summer.core.type.StandardMethodMetadata;
import ink.zfei.summer.util.Assert;
import ink.zfei.summer.util.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

public class ConfigurationClassBeanDefinitionReader {
    private final BeanDefinitionRegistry registry;

    public ConfigurationClassBeanDefinitionReader(BeanDefinitionRegistry registry) {
        this.registry = registry;
    }


    public void loadBeanDefinitions(Set<ConfigurationClass> configurationModel) {
        for (ConfigurationClass configClass : configurationModel) {
            loadBeanDefinitionsForConfigurationClass(configClass);
        }
    }

    private void loadBeanDefinitionsForConfigurationClass(
            ConfigurationClass configClass) {

        for (BeanMethod beanMethod : configClass.getBeanMethods()) {
            loadBeanDefinitionsForBeanMethod(beanMethod);
        }

//        loadBeanDefinitionsFromImportedResources(configClass.getImportedResources());
//        loadBeanDefinitionsFromRegistrars(configClass.getImportBeanDefinitionRegistrars());
    }

    /**
     * 注册@Configutation的@Bean的实例
     */
    private void loadBeanDefinitionsForBeanMethod(BeanMethod beanMethod) {
        ConfigurationClass configClass = beanMethod.getConfigurationClass();
        MethodMetadata metadata = beanMethod.getMetadata();
        String methodName = metadata.getMethodName();

        //忽略condition
        AnnotationAttributes bean = AnnotationConfigUtils.attributesFor(metadata, Bean.class);
        Assert.state(bean != null, "No @Bean annotation attributes");

        // Consider name and any aliases
        List<String> names = new ArrayList<>(Arrays.asList(bean.getStringArray("name")));
        String beanName = (!names.isEmpty() ? names.remove(0) : methodName);

        //忽略alis

        ConfigurationClassBeanDefinition beanDef = new ConfigurationClassBeanDefinition(configClass, metadata);
        beanDef.setResource(configClass.getResource());

        if (metadata.isStatic()) {
            // static @Bean method
            if (configClass.getMetadata() instanceof StandardAnnotationMetadata) {
                beanDef.setBeanClass(((StandardAnnotationMetadata) configClass.getMetadata()).getIntrospectedClass());
            } else {
                beanDef.setBeanClassName(configClass.getMetadata().getClassName());
            }
            beanDef.setUniqueFactoryMethodName(methodName);
        } else {
            // instance @Bean method
            beanDef.setFactoryBeanName(configClass.getBeanName());
            beanDef.setUniqueFactoryMethodName(methodName);
        }

        if (metadata instanceof StandardMethodMetadata) {
            beanDef.setResolvedFactoryMethod(((StandardMethodMetadata) metadata).getIntrospectedMethod());
        }

        beanDef.setAutowireMode(AbstractBeanDefinition.AUTOWIRE_CONSTRUCTOR);
//        beanDef.setAttribute(RequiredAnnotationBeanPostProcessor.
//                SKIP_REQUIRED_CHECK_ATTRIBUTE, Boolean.TRUE);

        AnnotationConfigUtils.processCommonDefinitionAnnotations(beanDef, metadata);

        Autowire autowire = bean.getEnum("autowire");
        if (autowire.isAutowire()) {
            beanDef.setAutowireMode(autowire.value());
        }

        boolean autowireCandidate = bean.getBoolean("autowireCandidate");
        if (!autowireCandidate) {
            beanDef.setAutowireCandidate(false);
        }

        String initMethodName = bean.getString("initMethod");
        if (StringUtils.hasText(initMethodName)) {
            beanDef.setInitMethodName(initMethodName);
        }


        // Replace the original bean definition with the target one, if necessary
        BeanDefinition beanDefToRegister = beanDef;

        this.registry.registerBeanDefinition(beanName, beanDefToRegister);
    }

    private static class ConfigurationClassBeanDefinition extends GenericBeanDefinition implements AnnotatedBeanDefinition {

        private final AnnotationMetadata annotationMetadata;

        private final MethodMetadata factoryMethodMetadata;

        public ConfigurationClassBeanDefinition(ConfigurationClass configClass, MethodMetadata beanMethodMetadata) {
            this.annotationMetadata = configClass.getMetadata();
            this.factoryMethodMetadata = beanMethodMetadata;
        }

        @Override
        public AnnotationMetadata getMetadata() {
            return null;
        }
    }
}
