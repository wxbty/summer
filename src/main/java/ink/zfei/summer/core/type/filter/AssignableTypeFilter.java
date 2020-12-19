package ink.zfei.summer.core.type.filter;

import ink.zfei.summer.core.type.classreading.MetadataReader;
import ink.zfei.summer.core.type.classreading.MetadataReaderFactory;

public class AssignableTypeFilter extends AbstractTypeHierarchyTraversingFilter{

    private final Class<?> targetType;

    public AssignableTypeFilter(Class<?> targetType) {
        super(true, true);
        this.targetType = targetType;
    }

    @Override
    public boolean match(MetadataReader metadataReader, MetadataReaderFactory metadataReaderFactory) {
        return false;
    }
}
