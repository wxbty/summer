package ink.zfei.summer.beans.factory;

import ink.zfei.summer.beans.BeanPostProcessor;

public interface BeanFactory {

      /**
       * factoryBean本身在spring容器中beanName(& + beanName)
       */
      String FACTORY_BEAN_PREFIX = "&";

      Object getBean(String name);

      Object getBean(Class configuationClass);

      <T> T getBean(String name, Class<T> requiredType);

      <T> T getBean(Class<T> requiredType, Object... args);

      Object getBean(String name, Object... args);

      void addBeanPostProcessor(String id, BeanPostProcessor beanPostProcessor);
}
