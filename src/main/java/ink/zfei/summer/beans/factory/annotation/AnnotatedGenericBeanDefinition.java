package ink.zfei.summer.beans.factory.annotation;

import ink.zfei.summer.beans.factory.support.GenericBeanDefinition;
import ink.zfei.summer.core.type.AnnotationMetadata;

public class AnnotatedGenericBeanDefinition extends GenericBeanDefinition implements AnnotatedBeanDefinition{

    private final AnnotationMetadata metadata;


    public AnnotatedGenericBeanDefinition(Class<?> beanClass) {
        setBeanClass(beanClass);
        this.metadata = AnnotationMetadata.introspect(beanClass);
    }

    @Override
    public AnnotationMetadata getMetadata() {
        return metadata;
    }
}
