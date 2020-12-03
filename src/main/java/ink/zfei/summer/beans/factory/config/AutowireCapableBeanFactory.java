package ink.zfei.summer.beans.factory.config;

import ink.zfei.summer.beans.factory.BeanFactory;

/**
 * 1、为已经实例化的对象自动装配属性，非@Autowired注解；
 * 2、不在IOC容器的Bean也可以被Spring管理，如web容器中的Filter，listener
 */
public interface AutowireCapableBeanFactory extends BeanFactory {

    int AUTOWIRE_NO = 0;

    int AUTOWIRE_BY_NAME = 1;

    int AUTOWIRE_BY_TYPE = 2;

    /**
     * Constant that indicates autowiring the greediest constructor that
     * can be satisfied (involves resolving the appropriate constructor).
     *
     * @see #createBean
     * @see #autowire
     */
    int AUTOWIRE_CONSTRUCTOR = 3;


    String ORIGINAL_INSTANCE_SUFFIX = ".ORIGINAL";


    <T> T createBean(Class<T> beanClass);


    void autowireBean(Object existingBean);

    Object configureBean(Object existingBean, String beanName);


    Object createBean(Class<?> beanClass, int autowireMode, boolean dependencyCheck);


    Object autowire(Class<?> beanClass, int autowireMode, boolean dependencyCheck);

    <T> NamedBeanHolder<T> resolveNamedBean(Class<T> requiredType);

}
