package ink.zfei.summer.beans.factory.config;

import ink.zfei.summer.context.LifecycleProcessor;
import ink.zfei.summer.lang.Nullable;

/**
 * 单例Bean的注册中心
 */
public interface SingletonBeanRegistry {

//    void registerSingleton(String beanName, Object singletonObject);

    /**
     */
    @Nullable
    Object getSingleton(String beanName);

    /**
     * Check if this registry contains a singleton instance with the given name.
     * <p>Only checks already instantiated singletons; does not return {@code true}
     * for singleton bean definitions which have not been instantiated yet.
     * <p>The main purpose of this method is to check manually registered singletons
     * (see). Can also be used to check whether a
     * singleton defined by a bean definition has already been created.
     * <p>To check whether a bean factory contains a bean definition with a given name,
     * use ListableBeanFactory's {@code containsBeanDefinition}. Calling both
     * {@code containsBeanDefinition} and {@code containsSingleton} answers
     * whether a specific bean factory contains a local bean instance with the given name.
     * <p>Use BeanFactory's {@code containsBean} for general checks whether the
     * factory knows about a bean with a given name (whether manually registered singleton
     * instance or created by bean definition), also checking ancestor factories.
     * <p><b>NOTE:</b> This lookup method is not aware of FactoryBean prefixes or aliases.
     * You need to resolve the canonical bean name first before checking the singleton status.
     */
    boolean containsSingleton(String beanName);

    /**
     * Return the names of singleton beans registered in this registry.
     * <p>Only checks already instantiated singletons; does not return names
     * for singleton bean definitions which have not been instantiated yet.
     * <p>The main purpose of this method is to check manually registered singletons
     * (see ). Can also be used to check which singletons
     * defined by a bean definition have already been created.
     */
    String[] getSingletonNames();

    /**
     * Return the number of singleton beans registered in this registry.
     * <p>Only checks already instantiated singletons; does not count
     * singleton bean definitions which have not been instantiated yet.
     * <p>The main purpose of this method is to check manually registered singletons
     * (see). Can also be used to count the number of
     * singletons defined by a bean definition that have already been created.
     */
    int getSingletonCount();

    /**
     * Return the singleton mutex used by this registry (for external collaborators).
     * @return the mutex object (never {@code null})
     * @since 4.2
     */
    Object getSingletonMutex();

    void registerSingleton(String beanName, Object singletonObject);

//    void registerBeanDefinition(String beanName, BeanDefinition beanDefinition);
}
