package ink.zfei.summer.beans.factory;

import ink.zfei.summer.beans.BeanPostProcessor;

public interface BeanFactory {

    /**
     * factoryBean本身在spring容器中beanName(& + beanName)
     */
    String FACTORY_BEAN_PREFIX = "&";

    Object getBean(String name);

    Object getBean(Class configuationClass);

    <T> T getBean(String name, Class<T> requiredType);

    <T> T getBean(Class<T> requiredType, Object... args);

    Object getBean(String name, Object... args);

    void addBeanPostProcessor(String id, BeanPostProcessor beanPostProcessor);

    /**
     * 通过别名获取的bean和id获取的是同一个，别名通常用在引入三方依赖时，
     * 内置配置通过别名场景化命名，在自己系统中使用
     */
    String[] getAliases(String name);

    boolean containsBeanDefinition(String beanName);

    Class<?> getType(String name);
}