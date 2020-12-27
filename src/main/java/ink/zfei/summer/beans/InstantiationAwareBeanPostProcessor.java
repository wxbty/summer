package ink.zfei.summer.beans;

import java.lang.reflect.Constructor;

public interface InstantiationAwareBeanPostProcessor extends BeanPostProcessor {


    /**
     * 实例化之前，一次返回代理的机会。
     * 如果返回不是null，会中断bean后续流程，直接到postProcessAfterInitialization的after初始化流程
     *
     * @param beanClass
     * @param beanName
     * @return
     */
    default Object postProcessBeforeInstantiation(Class<?> beanClass, String beanName) {
        return null;
    }

    /**
     * 在实例化之后、属性填充前执行
     * 如果想自定义属性注入，这里是一个理性的方式
     * 返回false，会直接return populateBean，跳过spring的属性注入
     */
    default boolean postProcessAfterInstantiation(Object bean, String beanName) {
        return true;
    }

    default PropertyValues postProcessProperties(PropertyValues pvs, Object bean, String beanName) {

        return null;
    }


    default Constructor<?>[] determineCandidateConstructors(Class<?> beanClass, String beanName) {

        return null;
    }
}
