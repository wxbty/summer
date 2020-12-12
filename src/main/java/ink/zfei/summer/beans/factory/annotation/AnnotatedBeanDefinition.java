package ink.zfei.summer.beans.factory.annotation;

import ink.zfei.summer.beans.factory.config.BeanDefinition;
import ink.zfei.summer.core.type.AnnotationMetadata;

public interface AnnotatedBeanDefinition extends BeanDefinition {

    AnnotationMetadata getMetadata();
}
