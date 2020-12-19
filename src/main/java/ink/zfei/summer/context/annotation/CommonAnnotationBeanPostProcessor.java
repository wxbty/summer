package ink.zfei.summer.context.annotation;

import ink.zfei.summer.beans.InstantiationAwareBeanPostProcessor;
import ink.zfei.summer.beans.PropertyValues;
import ink.zfei.summer.beans.factory.support.GenericBeanDefinition;
import ink.zfei.summer.beans.factory.support.MergedBeanDefinitionPostProcessor;
import ink.zfei.summer.core.Ordered;
import ink.zfei.summer.core.PriorityOrdered;

public class CommonAnnotationBeanPostProcessor implements MergedBeanDefinitionPostProcessor, InstantiationAwareBeanPostProcessor, PriorityOrdered {

    private int order = Ordered.LOWEST_PRECEDENCE;

    @Override
    public int getOrder() {
        return this.order;
    }

    @Override
    public void postProcessMergedBeanDefinition(GenericBeanDefinition beanDefinition, Class<?> beanType, String beanName) {
        System.out.println(111);
    }

    @Override
    public PropertyValues postProcessProperties(PropertyValues pvs, Object bean, String beanName) {
//        InjectionMetadata metadata = findResourceMetadata(beanName, bean.getClass(), pvs);
//        metadata.inject(bean, beanName, pvs);
        return pvs;
    }
}
