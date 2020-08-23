package ink.zfei.demo;

import ink.zfei.beans.BeanDefinitionRegistry;
import ink.zfei.beans.BeanDefinitionRegistryPostProcessor;
import ink.zfei.core.AbstractApplicationContext;
import ink.zfei.core.BeanDefinition;

public class StarterBeanDefinitionPostProcessor implements BeanDefinitionRegistryPostProcessor {
    @Override
    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) {

        String beanName = "starter";
        BeanDefinition beanDefinition = new BeanDefinition();
        beanDefinition.setId(beanName);
        beanDefinition.setBeanClass("ink.zfei.demo.Starter");
        beanDefinition.setScope("singleton");

        registry.registerBeanDefinition(beanName, beanDefinition);
    }

    @Override
    public void postProcessBeanFactory(AbstractApplicationContext abstractApplicationContext) {

    }
}
