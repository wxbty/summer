package ink.zfei.summer.beans.factory.support;

import ink.zfei.summer.beans.factory.config.BeanDefinitionHolder;
import ink.zfei.summer.beans.factory.config.DependencyDescriptor;
import ink.zfei.summer.lang.Nullable;
import ink.zfei.summer.util.BeanUtils;

public interface AutowireCandidateResolver {


    default boolean isAutowireCandidate(BeanDefinitionHolder bdHolder, DependencyDescriptor descriptor) {
        return bdHolder.getBeanDefinition().isAutowireCandidate();
    }

    /**
     * Determine whether the given descriptor is effectively required.
     * <p>The default implementation checks {@link DependencyDescriptor#isRequired()}.
     *
     * @param descriptor the descriptor for the target method parameter or field
     * @return whether the descriptor is marked as required or possibly indicating
     * non-required status some other way (e.g. through a parameter annotation)
     * @see DependencyDescriptor#isRequired()
     * @since 5.0
     */
    default boolean isRequired(DependencyDescriptor descriptor) {
        return descriptor.isRequired();
    }


    default boolean hasQualifier(DependencyDescriptor descriptor) {
        return false;
    }

    /**
     * Determine whether a default value is suggested for the given dependency.
     * <p>The default implementation simply returns {@code null}.
     *
     * @param descriptor the descriptor for the target method parameter or field
     * @return the value suggested (typically an expression String),
     * or {@code null} if none found
     * @since 3.0
     */
    @Nullable
    default Object getSuggestedValue(DependencyDescriptor descriptor) {
        return null;
    }

    /**
     * Build a proxy for lazy resolution of the actual dependency target,
     * if demanded by the injection point.
     * <p>The default implementation simply returns {@code null}.
     *
     * @param descriptor the descriptor for the target method parameter or field
     * @param beanName   the name of the bean that contains the injection point
     * @return the lazy resolution proxy for the actual dependency target,
     * or {@code null} if straight resolution is to be performed
     * @since 4.0
     */
    @Nullable
    default Object getLazyResolutionProxyIfNecessary(DependencyDescriptor descriptor, @Nullable String beanName) {
        return null;
    }


    default AutowireCandidateResolver cloneIfNecessary() {
        return null;
    }

}
