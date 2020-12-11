package ink.zfei.summer.core.type;

import ink.zfei.summer.core.annotation.AnnotationFilter;
import ink.zfei.summer.core.annotation.MergedAnnotations;
import ink.zfei.summer.core.annotation.MergedAnnotations.SearchStrategy;
import ink.zfei.summer.core.annotation.RepeatableContainers;

public class StandardAnnotationMetadata extends StandardClassMetadata implements AnnotationMetadata {

    private final MergedAnnotations mergedAnnotations;
    private final boolean nestedAnnotationsAsMap;

    public StandardAnnotationMetadata(Class<?> introspectedClass, boolean nestedAnnotationsAsMap) {
        super(introspectedClass);
        this.mergedAnnotations = MergedAnnotations.from(introspectedClass,
                SearchStrategy.INHERITED_ANNOTATIONS, RepeatableContainers.none(),
                AnnotationFilter.NONE);
        this.nestedAnnotationsAsMap = nestedAnnotationsAsMap;
    }

    @Override
    public MergedAnnotations getAnnotations() {
        return null;
    }

    static AnnotationMetadata from(Class<?> introspectedClass) {
        return new StandardAnnotationMetadata(introspectedClass, true);
    }
}
