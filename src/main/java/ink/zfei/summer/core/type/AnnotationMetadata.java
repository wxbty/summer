package ink.zfei.summer.core.type;

import ink.zfei.summer.core.annotation.MergedAnnotation;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 *  对某个类上的注解提供了抽象的访问
 *  不需要提前类加载
 */
public interface AnnotationMetadata extends ClassMetadata, AnnotatedTypeMetadata {


    static AnnotationMetadata introspect(Class<?> type) {
        return StandardAnnotationMetadata.from(type);
    }

    default boolean hasAnnotatedMethods(String annotationName) {
        return !getAnnotatedMethods(annotationName).isEmpty();
    }

    Set<MethodMetadata> getAnnotatedMethods(String annotationName);

    default Set<String> getAnnotationTypes() {
        return getAnnotations().stream()
                .filter(MergedAnnotation::isDirectlyPresent)
                .map(annotation -> annotation.getType().getName())
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }
}
