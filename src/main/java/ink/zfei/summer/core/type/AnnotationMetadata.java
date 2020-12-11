package ink.zfei.summer.core.type;

public interface AnnotationMetadata extends ClassMetadata, AnnotatedTypeMetadata {


    static AnnotationMetadata introspect(Class<?> type) {
        return StandardAnnotationMetadata.from(type);
    }
}
