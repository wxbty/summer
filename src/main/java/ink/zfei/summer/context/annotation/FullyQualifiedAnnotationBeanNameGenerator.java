package ink.zfei.summer.context.annotation;

import ink.zfei.summer.beans.factory.config.BeanDefinition;
import ink.zfei.summer.util.Assert;

public class FullyQualifiedAnnotationBeanNameGenerator extends AnnotationBeanNameGenerator {

    @Override
    protected String buildDefaultBeanName(BeanDefinition definition) {
        String beanClassName = definition.getBeanClassName();
        Assert.state(beanClassName != null, "No bean class name set");
        return beanClassName;
    }

}
