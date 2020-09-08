package ink.zfei.summer.beans;

import ink.zfei.summer.core.AbstractApplicationContext;

@FunctionalInterface
public interface BeanFactoryPostProcessor {


	void postProcessBeanFactory(AbstractApplicationContext abstractApplicationContext);

}