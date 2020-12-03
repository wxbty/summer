package ink.zfei.summer.beans.factory.config;

import ink.zfei.summer.beans.factory.ListableBeanFactory;
import ink.zfei.summer.core.ResolvableType;

/**
 *  比ConfigurableBeanFactory多的功能：
 *  分析和修改bean定义，并预先实例化单例。
 */
public interface ConfigurableListableBeanFactory extends ListableBeanFactory, AutowireCapableBeanFactory, ConfigurableBeanFactory{

    BeanDefinition getBeanDefinition(String beanName);

    boolean isTypeMatch(String name, Class<?> typeToMatch);

    boolean isTypeMatch(String name, ResolvableType typeToMatch);

    int getBeanPostProcessorCount();

    boolean isConfigurationFrozen();
}
