package ink.zfei.summer.core;

import ink.zfei.summer.core.type.AnnotationMetadata;

import java.util.function.Predicate;

public interface ImportSelector {
    String[] selectImports(Class var1);

    default Predicate<String> getExclusionFilter() {
        return null;
    }

    String[] selectImports(AnnotationMetadata importingClassMetadata);
}