package ink.zfei.summer.core;

import ink.zfei.summer.beans.factory.HierarchicalBeanFactory;
import ink.zfei.summer.beans.factory.ListableBeanFactory;

/**
 * spring容器核心接口，也被称为应用上下文。
 * 程序运行时接口方法只读，但可以重新加载
 * 它不仅提供了 BeanFactory 的所有功能，还添加了对 i18n（国际化）、
 * 资源访问、事件传播等方面的良好支持
 */
public interface ApplicationContext extends ListableBeanFactory, HierarchicalBeanFactory {

    /**
     * 返回容器唯一id
     */
    String getId();

    String getApplicationName();

    ApplicationContext getParent();

    void publishEvent(ApplicationEvent event);
}
