package ink.zfei.summer.beans.factory.annotation;

import ink.zfei.summer.beans.factory.support.GenericBeanDefinition;
import ink.zfei.summer.core.type.AnnotationMetadata;
import ink.zfei.summer.core.type.StandardAnnotationMetadata;
import ink.zfei.summer.util.Assert;

public class AnnotatedGenericBeanDefinition extends GenericBeanDefinition implements AnnotatedBeanDefinition{

    private final AnnotationMetadata metadata;


    public AnnotatedGenericBeanDefinition(Class<?> beanClass) {
        setBeanClass(beanClass);
        this.metadata = AnnotationMetadata.introspect(beanClass);
    }

    public AnnotatedGenericBeanDefinition(AnnotationMetadata metadata) {
        Assert.notNull(metadata, "AnnotationMetadata must not be null");
        if (metadata instanceof StandardAnnotationMetadata) {
            setBeanClass(((StandardAnnotationMetadata) metadata).getIntrospectedClass());
        }
        else {
            setBeanClassName(metadata.getClassName());
        }
        this.metadata = metadata;
    }

    @Override
    public AnnotationMetadata getMetadata() {
        return metadata;
    }
}
