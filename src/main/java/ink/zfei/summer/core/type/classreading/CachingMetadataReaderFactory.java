package ink.zfei.summer.core.type.classreading;

import ink.zfei.summer.core.io.Resource;

public class CachingMetadataReaderFactory extends SimpleMetadataReaderFactory {
    @Override
    public MetadataReader getMetadataReader(String className) {
        return null;
    }

    @Override
    public MetadataReader getMetadataReader(Resource resource) {
        return super.getMetadataReader(resource);
    }
}
