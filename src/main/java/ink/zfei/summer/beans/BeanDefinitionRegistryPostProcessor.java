package ink.zfei.summer.beans;

public interface BeanDefinitionRegistryPostProcessor extends BeanFactoryPostProcessor {


    void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry);

}