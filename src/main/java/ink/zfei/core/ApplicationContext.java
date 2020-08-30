package ink.zfei.core;

import ink.zfei.beans.factory.BeanFactory;

public interface ApplicationContext extends BeanFactory {

    void publishEvent(ApplicationEvent event);
}
