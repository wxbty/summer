package ink.zfei.summer.core.annotation;

import java.lang.annotation.Annotation;

/**
 * 从多个结果中 pk 出一个最佳的结果返回，有 Nearest 和 FirstDirectlyDeclared 二种实现。
 * isBestCandidate 方法返回 true 则说明是最佳结果，直接返回即可；
 * select 方法则将两个结果进行 pk
 */
@FunctionalInterface
public interface MergedAnnotationSelector<A extends Annotation> {

    /**
     * Determine if the existing annotation is known to be the best
     * candidate and any subsequent selections may be skipped.
     * @param annotation the annotation to check
     * @return {@code true} if the annotation is known to be the best candidate
     */
    default boolean isBestCandidate(MergedAnnotation<A> annotation) {
        return false;
    }

    /**
     * Select the annotation that should be used.
     * @param existing an existing annotation returned from an earlier result
     * @param candidate a candidate annotation that may be better suited
     * @return the most appropriate annotation from the {@code existing} or
     * {@code candidate}
     */
    MergedAnnotation<A> select(MergedAnnotation<A> existing, MergedAnnotation<A> candidate);

}