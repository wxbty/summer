package ink.zfei.summer.beans.factory.config;

import ink.zfei.summer.beans.BeanMetadataElement;
import ink.zfei.summer.beans.MutablePropertyValues;
import ink.zfei.summer.core.AttributeAccessor;

/**
 * AttributeAccessor：额外属性访问能力
 * BeanMetadataElement：标记属性哪里来
 */
public interface BeanDefinition extends AttributeAccessor, BeanMetadataElement {

    /**
     * 单例
     */
    String SCOPE_SINGLETON = ConfigurableBeanFactory.SCOPE_SINGLETON;
    /**
     * 多例
     */
    String SCOPE_PROTOTYPE = ConfigurableBeanFactory.SCOPE_PROTOTYPE;

    /**
     * 用户定义的bean
     */
    int ROLE_APPLICATION = 0;

    /**
     * 辅助角色
     */
    int ROLE_SUPPORT = 1;

    /**
     * 系统定义角色，用户无感，如处理@Resource和@Autowire注解的beanPostProcessor
     */
    int ROLE_INFRASTRUCTURE = 2;


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

    boolean isAbstract();

    boolean isSingleton();

    boolean isPrototype();

    MutablePropertyValues getPropertyValues();

    ConstructorArgumentValues getConstructorArgumentValues();

    default boolean hasPropertyValues() {
        return !getPropertyValues().isEmpty();
    }

    default boolean hasConstructorArgumentValues() {
        return !getConstructorArgumentValues().isEmpty();
    }

    void setAutowireCandidate(boolean autowireCandidate);

    boolean isAutowireCandidate();

    boolean isPrimary();

    void setRole(int role);

    int getRole();

    void setScope(String scope);

    String getScope();
}