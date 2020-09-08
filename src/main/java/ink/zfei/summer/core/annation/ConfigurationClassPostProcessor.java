package ink.zfei.summer.core.annation;

import ink.zfei.summer.beans.BeanDefinitionRegistry;
import ink.zfei.summer.beans.BeanDefinitionRegistryPostProcessor;
import ink.zfei.summer.context.AnnotationConfigApplicationContext;
import ink.zfei.summer.core.AbstractApplicationContext;

import java.util.HashMap;
import java.util.Map;

public class ConfigurationClassPostProcessor implements BeanDefinitionRegistryPostProcessor {


    public static ThreadLocal<Map<String, RootBeanDefination>> configBeanInfos = ThreadLocal.withInitial(() -> new HashMap<>());

    @Override
    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) {

        AnnotationConfigApplicationContext context = (AnnotationConfigApplicationContext) registry;
        context.getConfigBeanDefinitions().forEach(registry::registerBeanDefinition);
    }

    @Override
    public void postProcessBeanFactory(AbstractApplicationContext abstractApplicationContext) {

    }
}
