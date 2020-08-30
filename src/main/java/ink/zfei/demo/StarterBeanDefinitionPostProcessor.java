package ink.zfei.demo;

import ink.zfei.beans.BeanDefinitionRegistry;
import ink.zfei.beans.BeanDefinitionRegistryPostProcessor;
import ink.zfei.core.AbstractApplicationContext;
import ink.zfei.core.GenericBeanDefinition;

public class StarterBeanDefinitionPostProcessor implements BeanDefinitionRegistryPostProcessor {
    @Override
    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) {

        String beanName = "starter";
        GenericBeanDefinition beanDefinition = new GenericBeanDefinition();
        beanDefinition.setId(beanName);
        beanDefinition.setBeanClassName("ink.zfei.demo.Starter");
        beanDefinition.setScope("singleton");

        registry.registerBeanDefinition(beanName, beanDefinition);
    }

    @Override
    public void postProcessBeanFactory(AbstractApplicationContext abstractApplicationContext) {

    }
}
