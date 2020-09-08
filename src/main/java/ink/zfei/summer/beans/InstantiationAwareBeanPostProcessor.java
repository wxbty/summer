package ink.zfei.summer.beans;

public interface InstantiationAwareBeanPostProcessor extends BeanPostProcessor {


    default Object postProcessBeforeInstantiation(Class<?> beanClass, String beanName) {
        return null;
    }


    default boolean postProcessAfterInstantiation(Object bean, String beanName) {
        return true;
    }
}
