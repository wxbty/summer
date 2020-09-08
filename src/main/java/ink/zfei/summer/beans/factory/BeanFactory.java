package ink.zfei.summer.beans.factory;

import ink.zfei.summer.beans.BeanPostProcessor;

public interface BeanFactory {

      /**
       * factoryBean本身在spring容器中beanName(& + beanName)
       */
      String FACTORY_BEAN_PREFIX = "&";

      Object getBean(String name);

      Object getBean(Class configuationClass);

      void addBeanPostProcessor(String id, BeanPostProcessor beanPostProcessor);
}
