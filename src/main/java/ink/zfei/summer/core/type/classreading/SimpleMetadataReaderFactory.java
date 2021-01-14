package ink.zfei.summer.core.type.classreading;

import ink.zfei.summer.core.io.DefaultResourceLoader;
import ink.zfei.summer.core.io.Resource;
import ink.zfei.summer.core.io.ResourceLoader;
import ink.zfei.summer.core.type.AnnotationMetadata;
import ink.zfei.summer.core.type.StandardAnnotationMetadata;
import ink.zfei.summer.util.ClassUtils;

import java.io.FileNotFoundException;

/**
 * simple在spring中是asm实现，这样获取bean的class元信息时，
 * 不会提早触发class加载，提高启动速度
 * asm解析较为复杂，这里仍用标准反射实现
 *
 * @see #getMetadataReader(String)
 */
public class SimpleMetadataReaderFactory implements MetadataReaderFactory {

    public SimpleMetadataReaderFactory() {
    }

    @Override
    public MetadataReader getMetadataReader(String className) {
        Class clazz = null;
        try {
            clazz = Class.forName(className);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        //SimpleMetadataReader是asm实现，自定义StandardMetadataReader用反射实现
        return new StandardMetadataReader(AnnotationMetadata.introspect(clazz));

    }

    @Override
    public MetadataReader getMetadataReader(Resource resource) {
        return null;
    }
}
