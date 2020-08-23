package ink.zfei.beans;

import ink.zfei.core.AbstractApplicationContext;

@FunctionalInterface
public interface BeanFactoryPostProcessor {


	void postProcessBeanFactory(AbstractApplicationContext abstractApplicationContext);

}