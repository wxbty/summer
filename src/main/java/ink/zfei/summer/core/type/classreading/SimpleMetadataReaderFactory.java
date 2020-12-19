package ink.zfei.summer.core.type.classreading;

import ink.zfei.summer.core.io.DefaultResourceLoader;
import ink.zfei.summer.core.io.Resource;
import ink.zfei.summer.core.io.ResourceLoader;
import ink.zfei.summer.util.ClassUtils;

import java.io.FileNotFoundException;

public class SimpleMetadataReaderFactory implements MetadataReaderFactory {

    private final ResourceLoader resourceLoader;

    public SimpleMetadataReaderFactory() {
        this.resourceLoader = new DefaultResourceLoader();
    }

    @Override
    public MetadataReader getMetadataReader(String className) {
        String resourcePath = ResourceLoader.CLASSPATH_URL_PREFIX +
                ClassUtils.convertClassNameToResourcePath(className) + ClassUtils.CLASS_FILE_SUFFIX;
        Resource resource = this.resourceLoader.getResource(resourcePath);
        return getMetadataReader(resource);
    }

    @Override
    public MetadataReader getMetadataReader(Resource resource) {
        return null;
    }
}
