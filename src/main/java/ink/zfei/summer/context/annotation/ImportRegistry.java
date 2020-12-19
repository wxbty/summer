package ink.zfei.summer.context.annotation;

import ink.zfei.summer.core.type.AnnotationMetadata;

public interface ImportRegistry {

    AnnotationMetadata getImportingClassFor(String importedClass);

    void removeImportingClass(String importingClass);
}
