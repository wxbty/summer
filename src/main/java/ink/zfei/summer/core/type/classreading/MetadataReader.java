package ink.zfei.summer.core.type.classreading;

import ink.zfei.summer.core.io.Resource;
import ink.zfei.summer.core.type.AnnotationMetadata;
import ink.zfei.summer.core.type.ClassMetadata;

public interface MetadataReader {

    AnnotationMetadata getAnnotationMetadata();

    Resource getResource();

    ClassMetadata getClassMetadata();
}
