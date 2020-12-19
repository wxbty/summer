package ink.zfei.summer.core.type.classreading;

import ink.zfei.summer.core.io.Resource;
import ink.zfei.summer.core.type.AnnotationMetadata;
import ink.zfei.summer.core.type.ClassMetadata;
import ink.zfei.summer.lang.Nullable;

import java.io.IOException;

public class SimpleMetadataReader implements MetadataReader {

    private static final int PARSING_OPTIONS = 2 | 1 | 4;


    SimpleMetadataReader(Resource resource, @Nullable ClassLoader classLoader) throws IOException {
    }

    @Override
    public AnnotationMetadata getAnnotationMetadata() {
        return null;
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
