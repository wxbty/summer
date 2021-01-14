package ink.zfei.summer.core.type.classreading;

import ink.zfei.summer.core.io.Resource;
import ink.zfei.summer.core.type.AnnotationMetadata;
import ink.zfei.summer.core.type.ClassMetadata;

public class StandardMetadataReader implements MetadataReader {

    private final AnnotationMetadata annotationMetadata;


    public StandardMetadataReader(AnnotationMetadata annotationMetadata) {
        this.annotationMetadata = annotationMetadata;
    }

    @Override
    public AnnotationMetadata getAnnotationMetadata() {
        return this.annotationMetadata;
    }

    @Override
    public Resource getResource() {
        return null;
    }

    @Override
    public ClassMetadata getClassMetadata() {
        return null;
    }
}
