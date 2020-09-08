package ink.zfei.summer.core;

import ink.zfei.summer.beans.factory.BeanFactory;

public interface ApplicationContext extends BeanFactory {

    void publishEvent(ApplicationEvent event);
}
