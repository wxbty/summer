package ink.zfei.summer.core.type;

import ink.zfei.summer.core.annotation.*;
import ink.zfei.summer.core.annotation.MergedAnnotations.SearchStrategy;
import ink.zfei.summer.util.ReflectionUtils;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.LinkedHashSet;
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
    @SuppressWarnings("deprecation")
    public Set<MethodMetadata> getAnnotatedMethods(String annotationName) {
        Set<MethodMetadata> annotatedMethods = null;
        if (AnnotationUtils.isCandidateClass(getIntrospectedClass(), annotationName)) {
            try {
                Method[] methods = ReflectionUtils.getDeclaredMethods(getIntrospectedClass());
                for (Method method : methods) {
                    if (isAnnotatedMethod(method, annotationName)) {
                        if (annotatedMethods == null) {
                            annotatedMethods = new LinkedHashSet<>(4);
                        }
                        annotatedMethods.add(new StandardMethodMetadata(method, this.nestedAnnotationsAsMap));
                    }
                }
            }
            catch (Throwable ex) {
                throw new IllegalStateException("Failed to introspect annotated methods on " + getIntrospectedClass(), ex);
            }
        }
        return annotatedMethods != null ? annotatedMethods : Collections.emptySet();
    }

    private boolean isAnnotatedMethod(Method method, String annotationName) {
        return !method.isBridge() && method.getAnnotations().length > 0 &&
                AnnotatedElementUtils.isAnnotated(method, annotationName);
    }
}
