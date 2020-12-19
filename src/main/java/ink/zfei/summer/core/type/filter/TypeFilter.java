package ink.zfei.summer.core.type.filter;

import ink.zfei.summer.core.type.classreading.MetadataReader;
import ink.zfei.summer.core.type.classreading.MetadataReaderFactory;

public interface TypeFilter {

    boolean match(MetadataReader metadataReader, MetadataReaderFactory metadataReaderFactory);
}
