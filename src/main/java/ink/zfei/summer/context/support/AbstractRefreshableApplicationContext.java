package ink.zfei.summer.context.support;

import ink.zfei.summer.beans.factory.BeanFactory;
import ink.zfei.summer.beans.factory.config.ConfigurableListableBeanFactory;
import ink.zfei.summer.context.ConfigurableApplicationContext;
import ink.zfei.summer.core.AbstractApplicationContext;

public abstract class AbstractRefreshableApplicationContext extends AbstractApplicationContext {

    private DefaultListableBeanFactory beanFactory;

    private final Object beanFactoryMonitor = new Object();

    @Override
    public final ConfigurableListableBeanFactory getBeanFactory() {
        synchronized (this.beanFactoryMonitor) {
            if (this.beanFactory == null) {
                throw new IllegalStateException("BeanFactory not initialized or already closed - " +
                        "call 'refresh' before accessing beans via the ApplicationContext");
            }
            return this.beanFactory;
        }
    }

    protected final boolean hasBeanFactory() {
        synchronized (this.beanFactoryMonitor) {
            return (this.beanFactory != null);
        }
    }

    @Override
    protected final void refreshBeanFactory() {
        if (hasBeanFactory()) {
//            destroyBeans();
            closeBeanFactory();
        }
        DefaultListableBeanFactory beanFactory = createBeanFactory();
//            beanFactory.setSerializationId(getId());
//            customizeBeanFactory(beanFactory);
        loadBeanDefinitions(beanFactory);
        synchronized (this.beanFactoryMonitor) {
            this.beanFactory = beanFactory;
        }
    }

    @Override
    protected final void closeBeanFactory() {
        synchronized (this.beanFactoryMonitor) {
            if (this.beanFactory != null) {
//                this.beanFactory.setSerializationId(null);
                this.beanFactory = null;
            }
        }
    }

    protected DefaultListableBeanFactory createBeanFactory() {
        return new DefaultListableBeanFactory(getInternalParentBeanFactory());
    }

    protected BeanFactory getInternalParentBeanFactory() {
        return (getParent() instanceof ConfigurableApplicationContext ?
                ((ConfigurableApplicationContext) getParent()).getBeanFactory() : getParent());
    }

    protected abstract void loadBeanDefinitions(DefaultListableBeanFactory beanFactory);

}
