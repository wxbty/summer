package ink.zfei.summer.core.type;

import java.util.Set;

public interface AnnotationMetadata extends ClassMetadata, AnnotatedTypeMetadata {


    static AnnotationMetadata introspect(Class<?> type) {
        return StandardAnnotationMetadata.from(type);
    }

    default boolean hasAnnotatedMethods(String annotationName) {
        return !getAnnotatedMethods(annotationName).isEmpty();
    }

    Set<MethodMetadata> getAnnotatedMethods(String annotationName);
}
