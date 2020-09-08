package ink.zfei.summer.demo;

import ink.zfei.summer.beans.BeanDefinitionRegistry;
import ink.zfei.summer.beans.BeanDefinitionRegistryPostProcessor;
import ink.zfei.summer.core.AbstractApplicationContext;
import ink.zfei.summer.core.GenericBeanDefinition;

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
