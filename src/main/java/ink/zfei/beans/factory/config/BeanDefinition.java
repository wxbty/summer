package ink.zfei.beans.factory.config;

public interface BeanDefinition {

    /**
     * 单例
     */
    String SCOPE_SINGLETON = ConfigurableBeanFactory.SCOPE_SINGLETON;
    /**
     * 多例
     */
    String SCOPE_PROTOTYPE = ConfigurableBeanFactory.SCOPE_PROTOTYPE;

    void setParentName(String parentName);

    String getParentName();

    void setBeanClassName(String beanClassName);

    String getBeanClassName();

    void setFactoryBeanName(String factoryBeanName);


    String getFactoryBeanName();

    void setFactoryMethodName(String factoryMethodName);


    String getFactoryMethodName();

    void setInitMethodName(String initMethodName);

    String getInitMethodName();
}
