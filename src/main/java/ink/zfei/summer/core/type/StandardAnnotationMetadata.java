package ink.zfei.summer.core.type;

import ink.zfei.summer.core.annotation.AnnotationFilter;
import ink.zfei.summer.core.annotation.MergedAnnotations;
import ink.zfei.summer.core.annotation.MergedAnnotations.SearchStrategy;
import ink.zfei.summer.core.annotation.RepeatableContainers;

import java.util.Set;

public class StandardAnnotationMetadata extends StandardClassMetadata implements AnnotationMetadata {

    private final MergedAnnotations mergedAnnotations;
    private final boolean nestedAnnotationsAsMap;

    public StandardAnnotationMetadata(Class<?> introspectedClass, boolean nestedAnnotationsAsMap) {
        super(introspectedClass);
        //缓存类上的注解，方便访问
        this.mergedAnnotations = MergedAnnotations.from(introspectedClass,
                SearchStrategy.INHERITED_ANNOTATIONS, RepeatableContainers.none(),
                AnnotationFilter.NONE);
        this.nestedAnnotationsAsMap = nestedAnnotationsAsMap;
    }

    @Override
    public MergedAnnotations getAnnotations() {
        return this.mergedAnnotations;
    }

    static AnnotationMetadata from(Class<?> introspectedClass) {
        return new StandardAnnotationMetadata(introspectedClass, true);
    }

    @Override
    public Set<MethodMetadata> getAnnotatedMethods(String annotationName) {
        return null;
    }
}
