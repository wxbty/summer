package ink.zfei.summer.context.annotation;

import ink.zfei.summer.beans.BeanDefinitionRegistry;
import ink.zfei.summer.beans.factory.annotation.AnnotatedGenericBeanDefinition;
import ink.zfei.summer.beans.factory.support.BeanNameGenerator;
import ink.zfei.summer.core.annotation.AnnotationConfigUtils;
import ink.zfei.summer.core.annotation.Configuration;
import ink.zfei.summer.core.env.Environment;
import ink.zfei.summer.util.AnnationUtil;
import ink.zfei.summer.util.Assert;

import java.lang.annotation.Annotation;

public class AnnotatedBeanDefinitionReader {

    private BeanNameGenerator beanNameGenerator = AnnotationBeanNameGenerator.INSTANCE;
    private final BeanDefinitionRegistry registry;

    public AnnotatedBeanDefinitionReader(BeanDefinitionRegistry registry) {
        this(registry, null);
    }

    public AnnotatedBeanDefinitionReader(BeanDefinitionRegistry registry, Environment environment) {
        Assert.notNull(registry, "BeanDefinitionRegistry must not be null");
//        Assert.notNull(environment, "Environment must not be null");
        this.registry = registry;
//        this.conditionEvaluator = new ConditionEvaluator(registry, environment, null);
        AnnotationConfigUtils.registerAnnotationConfigProcessors(this.registry);

    }

    public void register(Class<?>... componentClasses) {
        for (Class<?> componentClass : componentClasses) {
            registerBean(componentClass);
        }
    }

    public void registerBean(Class<?> beanClass) {
        registerBean(beanClass, null);
    }

    public void registerBean(Class<?> beanClass, String name) {
        Annotation annotations = AnnationUtil.findAnnotation(beanClass, Configuration.class);
        if (annotations == null) {
            throw new RuntimeException("不是配置类");
        }
//        GenericBeanDefinition abd = new GenericBeanDefinition(beanClass);

        AnnotatedGenericBeanDefinition abd = new AnnotatedGenericBeanDefinition(beanClass);
        //todo  条件注解过滤

        String beanName = (name != null ? name : this.beanNameGenerator.generateBeanName(abd, this.registry));

        registry.registerBeanDefinition(beanName, abd);

//        registerBeanDefinition(abd);
//        registerConfiguration(componentClasses.getName());

//        boolean isPorxy = annotations.proxyBeanMethods();
//        Method[] methods = componentClasses.getMethods();
//        for (Method method : methods) {
//            Bean bean = method.getAnnotation(Bean.class);
//            if (bean != null) {
//                GenericBeanDefinition definition = new GenericBeanDefinition();
//                definition.setId(method.getName());
//                definition.setFactoryBeanName(beanName);
//                definition.setFactoryMethodName(method.getName());
//                registerBeanDefinition(definition);
//                configBeanDefinitions.add(definition);
//            }
//        }
    }
}
