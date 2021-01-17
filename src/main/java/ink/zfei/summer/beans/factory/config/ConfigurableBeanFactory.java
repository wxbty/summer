package ink.zfei.summer.beans.factory.config;

import ink.zfei.summer.beans.BeanPostProcessor;
import ink.zfei.summer.beans.TypeConverter;
import ink.zfei.summer.beans.factory.HierarchicalBeanFactory;

/**
 * Configurable（可配置的），定义BeanFactory的属性配置
 * 继承自HierarchicalBeanFactory 和 SingletonBeanRegistry 这两个接口，并额外独有37个方法
 * 这37个方法包含了工厂创建、注册一个Bean的众多细节
 */
public interface ConfigurableBeanFactory extends HierarchicalBeanFactory,SingletonBeanRegistry {

    String SCOPE_SINGLETON = "singleton";

    String SCOPE_PROTOTYPE = "prototype";

    ClassLoader getBeanClassLoader();

    BeanDefinition getMergedBeanDefinition(String beanName);

    boolean isFactoryBean(String name);

    void addBeanPostProcessor(BeanPostProcessor beanPostProcessor);

    int getBeanPostProcessorCount();

    /**
     * 将该依赖进行注册，便于在销毁 bean 之前对其进行销毁。
     * 其实将就是该映射关系保存到两个集合中：dependentBeanMap、dependenciesForBeanMap
     */
    void registerDependentBean(String beanName, String dependentBeanName);

    TypeConverter getTypeConverter();
}
