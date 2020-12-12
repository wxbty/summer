package ink.zfei.summer.context.annotation;

import ink.zfei.summer.beans.factory.annotation.AnnotatedBeanDefinition;
import ink.zfei.summer.beans.factory.support.GenericBeanDefinition;
import ink.zfei.summer.core.type.AnnotationMetadata;
import ink.zfei.summer.core.type.classreading.MetadataReader;
import ink.zfei.summer.util.Assert;

public class ScannedGenericBeanDefinition extends GenericBeanDefinition implements AnnotatedBeanDefinition {

    private final AnnotationMetadata metadata;

    public ScannedGenericBeanDefinition(MetadataReader metadataReader) {
        Assert.notNull(metadataReader, "MetadataReader must not be null");
        this.metadata = metadataReader.getAnnotationMetadata();
        setBeanClassName(this.metadata.getClassName());
    }

    @Override
    public AnnotationMetadata getMetadata() {
        return null;
    }
}
