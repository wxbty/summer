package ink.zfei.summer.beans.factory.support;

import ink.zfei.summer.beans.BeanDefinitionRegistry;
import ink.zfei.summer.beans.factory.config.BeanDefinition;

public interface BeanNameGenerator {

    String generateBeanName(BeanDefinition definition, BeanDefinitionRegistry registry);
}
