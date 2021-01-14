package ink.zfei.summer.core.type.classreading;

import ink.zfei.summer.core.io.Resource;
import ink.zfei.summer.core.io.ResourceLoader;
import ink.zfei.summer.util.ClassUtils;

import java.io.FileNotFoundException;

public class CachingMetadataReaderFactory extends SimpleMetadataReaderFactory {

    @Override
    public MetadataReader getMetadataReader(Resource resource) {
        return super.getMetadataReader(resource);
    }
}
