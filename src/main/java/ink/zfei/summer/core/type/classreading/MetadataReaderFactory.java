package ink.zfei.summer.core.type.classreading;


import ink.zfei.summer.core.io.Resource;

public interface MetadataReaderFactory {

    MetadataReader getMetadataReader(String className);

    MetadataReader getMetadataReader(Resource resource);
}
