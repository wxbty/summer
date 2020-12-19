package ink.zfei.summer.context.annotation;

import ink.zfei.summer.beans.BeanDefinitionRegistry;

import java.util.Set;

public class ConfigurationClassBeanDefinitionReader {
    private final BeanDefinitionRegistry registry;

    public ConfigurationClassBeanDefinitionReader(BeanDefinitionRegistry registry) {
        this.registry = registry;
    }


    public void loadBeanDefinitions(Set<ConfigurationClass> configurationModel) {

    }
}
