package ink.zfei.summer.beans.factory.config;

import ink.zfei.summer.beans.factory.BeanFactory;

public interface AutowireCapableBeanFactory extends BeanFactory {

    int AUTOWIRE_NO = 0;


    int AUTOWIRE_BY_NAME = 1;

    int AUTOWIRE_BY_TYPE = 2;

    /**
     * Constant that indicates autowiring the greediest constructor that
     * can be satisfied (involves resolving the appropriate constructor).
     * @see #createBean
     * @see #autowire
     */
    int AUTOWIRE_CONSTRUCTOR = 3;

    /**
     * Constant that indicates determining an appropriate autowire strategy
     * through introspection of the bean class.
     * @see #createBean
     * @see #autowire
     * @deprecated as of Spring 3.0: If you are using mixed autowiring strategies,
     * prefer annotation-based autowiring for clearer demarcation of autowiring needs.
     */
    @Deprecated
    int AUTOWIRE_AUTODETECT = 4;

    String ORIGINAL_INSTANCE_SUFFIX = ".ORIGINAL";


    //-------------------------------------------------------------------------
    // Typical methods for creating and populating external bean instances
    //-------------------------------------------------------------------------


    <T> T createBean(Class<T> beanClass);


    void autowireBean(Object existingBean);

    Object configureBean(Object existingBean, String beanName) ;


    Object createBean(Class<?> beanClass, int autowireMode, boolean dependencyCheck) ;


    Object autowire(Class<?> beanClass, int autowireMode, boolean dependencyCheck);


}
