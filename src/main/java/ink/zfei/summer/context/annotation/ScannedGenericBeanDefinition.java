package ink.zfei.summer.context.annotation;

import ink.zfei.summer.beans.factory.annotation.AnnotatedBeanDefinition;
import ink.zfei.summer.beans.factory.support.GenericBeanDefinition;
import ink.zfei.summer.core.type.AnnotationMetadata;
import ink.zfei.summer.core.type.classreading.MetadataReader;
import ink.zfei.summer.util.Assert;


public class ScannedGenericBeanDefinition extends GenericBeanDefinition implements AnnotatedBeanDefinition {

    private final AnnotationMetadata metadata;


    public ScannedGenericBeanDefinition(Class<?> beanClass) {
        setBeanClass(beanClass);
        this.metadata = AnnotationMetadata.introspect(beanClass);
    }

    @Override
    public AnnotationMetadata getMetadata() {
        return metadata;
    }
}
