package ink.zfei.summer.beans.factory;

import ink.zfei.summer.beans.BeanPostProcessor;
import ink.zfei.summer.core.ResolvableType;

public interface BeanFactory {

    /**
     * factoryBean本身在spring容器中beanName(& + beanName)
     */
    String FACTORY_BEAN_PREFIX = "&";

    Object getBean(String name);

    <T> T getBean(Class<T> requiredType);

    <T> T getBean(String name, Class<T> requiredType);

    <T> T getBean(Class<T> requiredType, Object... args);

    Object getBean(String name, Object... args);

    void addBeanPostProcessor(String id, BeanPostProcessor beanPostProcessor);

    /**
     * 通过别名获取的bean和id获取的是同一个，别名通常用在引入三方依赖时，
     * 内置配置通过别名场景化命名，在自己系统中使用
     */
    String[] getAliases(String name);

    Class<?> getType(String name);

    /**
     * 判断名称是name的bean是否是匹配指定类型
     * @param name
     * @param typeToMatch
     * @return
     */
    boolean isTypeMatch(String name, Class<?> typeToMatch);

    boolean isTypeMatch(String name, ResolvableType typeToMatch);

    boolean isSingleton(String name);
}