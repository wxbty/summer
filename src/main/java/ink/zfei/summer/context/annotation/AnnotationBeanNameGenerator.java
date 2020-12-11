package ink.zfei.summer.context.annotation;

import ink.zfei.summer.beans.BeanDefinitionRegistry;
import ink.zfei.summer.beans.factory.annotation.AnnotatedBeanDefinition;
import ink.zfei.summer.beans.factory.config.BeanDefinition;
import ink.zfei.summer.beans.factory.support.BeanNameGenerator;
import ink.zfei.summer.util.Assert;
import ink.zfei.summer.util.ClassUtils;
import ink.zfei.summer.util.StringUtils;

import java.beans.Introspector;

public class AnnotationBeanNameGenerator implements BeanNameGenerator {

    public static final AnnotationBeanNameGenerator INSTANCE = new AnnotationBeanNameGenerator();

    @Override
    public String generateBeanName(BeanDefinition definition, BeanDefinitionRegistry registry) {

        if (definition instanceof AnnotatedBeanDefinition) {
            String beanName = determineBeanNameFromAnnotation((AnnotatedBeanDefinition) definition);
            if (StringUtils.hasText(beanName)) {
                // Explicit bean name found.
                return beanName;
            }
        }
        return buildDefaultBeanName(definition, registry);
    }

    private String determineBeanNameFromAnnotation(AnnotatedBeanDefinition definition) {

        Class beanClass = null;
        try {
            beanClass = Class.forName(definition.getBeanClassName());
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        String beanName = beanClass.getSimpleName();
        beanName = beanName.substring(0, 1).toLowerCase() + beanName.substring(1);

        return beanName;
    }

    protected String buildDefaultBeanName(BeanDefinition definition, BeanDefinitionRegistry registry) {
        return buildDefaultBeanName(definition);
    }

    protected String buildDefaultBeanName(BeanDefinition definition) {
        String beanClassName = definition.getBeanClassName();
        Assert.state(beanClassName != null, "No bean class name set");
        String shortClassName = ClassUtils.getShortName(beanClassName);
        return Introspector.decapitalize(shortClassName);
    }
}
