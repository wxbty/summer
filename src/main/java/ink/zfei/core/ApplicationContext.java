package ink.zfei.core;

public interface ApplicationContext extends BeanFactory {

    void publishEvent(ApplicationEvent event);
}
