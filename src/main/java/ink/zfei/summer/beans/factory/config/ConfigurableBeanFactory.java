package ink.zfei.summer.beans.factory.config;

import ink.zfei.summer.beans.BeanPostProcessor;
import ink.zfei.summer.beans.factory.HierarchicalBeanFactory;

/**
 * Configurable（可配置的），定义BeanFactory的属性配置
 * 继承自HierarchicalBeanFactory 和 SingletonBeanRegistry 这两个接口，并额外独有37个方法
 * 这37个方法包含了工厂创建、注册一个Bean的众多细节
 */
public interface ConfigurableBeanFactory extends HierarchicalBeanFactory {

    String SCOPE_SINGLETON = "singleton";

    String SCOPE_PROTOTYPE = "prototype";

    ClassLoader getBeanClassLoader();

    BeanDefinition getMergedBeanDefinition(String beanName);

    boolean isFactoryBean(String name);

    void addBeanPostProcessor(BeanPostProcessor beanPostProcessor);

    int getBeanPostProcessorCount();
}
