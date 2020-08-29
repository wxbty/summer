package ink.zfei.core;

import ink.zfei.beans.BeanPostProcessor;

public class ApplicationContextAwareProcessor  implements BeanPostProcessor {

    private ApplicationContext applicationContext;

    public ApplicationContextAwareProcessor(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    public Object postProcessBeforeInitialization(Object bean, String beanName) {

        if (bean instanceof ApplicationContextAware)
        {
            ApplicationContextAware beanAware =  (ApplicationContextAware) bean;
            beanAware.setApplicationContext(applicationContext);
        }
        return bean;
    }

}
