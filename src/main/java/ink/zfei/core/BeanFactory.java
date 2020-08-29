package ink.zfei.core;

import ink.zfei.beans.BeanPostProcessor;

public interface BeanFactory {

      Object getBean(String id);

      Object getBean(Class configuationClass);

      void addBeanPostProcessor(String id,BeanPostProcessor beanPostProcessor);
}
