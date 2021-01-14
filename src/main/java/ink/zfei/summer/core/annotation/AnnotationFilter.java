package ink.zfei.summer.core.annotation;

import java.lang.annotation.Annotation;

/**
 * 查找注解时按需过滤
 * PLAIN：与 java.lang 和 org.springframework.lang 包及其子包中的注解匹配。
 * JAVA：与 java 和 javax 包及其子包中的注解匹配。
 * ALL：始终匹配，可以在根本不存在任何相关注释类型时使用。
 * NONE：永远不匹配，可以在不需要过滤时使用（允许存在任何注释类型）。
 */
@FunctionalInterface
public interface AnnotationFilter {

    /**
     * 在class或method等注解元素中找注解时，忽略一下包的注解
     * org.springframework.lang 包含Nullable
     * java.lang 包含大部分jdk注解
     */
    AnnotationFilter PLAIN = packages("java.lang", "org.springframework.lang");

    /**
     * {@link AnnotationFilter} that matches annotations in the
     * {@code java} and {@code javax} packages and their subpackages.
     */
    AnnotationFilter JAVA = packages("java", "javax");

    /**
     * {@link AnnotationFilter} that always matches and can be used when no
     * relevant annotation types are expected to be present at all.
     */
    AnnotationFilter ALL = new AnnotationFilter() {
        @Override
        public boolean matches(Annotation annotation) {
            return true;
        }

        @Override
        public boolean matches(Class<?> type) {
            return true;
        }

        @Override
        public boolean matches(String typeName) {
            return true;
        }

        @Override
        public String toString() {
            return "All annotations filtered";
        }
    };

    /**
     * {@link AnnotationFilter} that never matches and can be used when no
     * filtering is needed (allowing for any annotation types to be present).
     *
     * @see #PLAIN
     * @deprecated as of 5.2.6 since the {@link MergedAnnotations} model
     * always ignores lang annotations according to the {@link #PLAIN} filter
     * (for efficiency reasons)
     */
    @Deprecated
    AnnotationFilter NONE = new AnnotationFilter() {
        @Override
        public boolean matches(Annotation annotation) {
            return false;
        }

        @Override
        public boolean matches(Class<?> type) {
            return false;
        }

        @Override
        public boolean matches(String typeName) {
            return false;
        }

        @Override
        public String toString() {
            return "No annotation filtering";
        }
    };


    /**
     * Test if the given annotation matches the filter.
     *
     * @param annotation the annotation to test
     * @return {@code true} if the annotation matches
     */
    default boolean matches(Annotation annotation) {
        return matches(annotation.annotationType());
    }

    /**
     * Test if the given type matches the filter.
     *
     * @param type the annotation type to test
     * @return {@code true} if the annotation matches
     */
    default boolean matches(Class<?> type) {
        return matches(type.getName());
    }

    /**
     * Test if the given type name matches the filter.
     *
     * @param typeName the fully qualified class name of the annotation type to test
     * @return {@code true} if the annotation matches
     */
    boolean matches(String typeName);


    /**
     * Create a new {@link AnnotationFilter} that matches annotations in the
     * specified packages.
     *
     * @param packages the annotation packages that should match
     * @return a new {@link AnnotationFilter} instance
     */
    static AnnotationFilter packages(String... packages) {
        return new PackagesAnnotationFilter(packages);
    }

}
