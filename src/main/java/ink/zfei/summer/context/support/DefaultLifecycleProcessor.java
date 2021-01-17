package ink.zfei.summer.context.support;

import ink.zfei.summer.beans.factory.BeanFactoryUtils;
import ink.zfei.summer.beans.factory.config.ConfigurableListableBeanFactory;
import ink.zfei.summer.context.Lifecycle;
import ink.zfei.summer.context.LifecycleProcessor;
import ink.zfei.summer.beans.factory.BeanFactoryAware;
import ink.zfei.summer.beans.factory.BeanFactory;
import ink.zfei.summer.context.SmartLifecycle;
import ink.zfei.summer.lang.Nullable;
import ink.zfei.summer.util.Assert;

import java.util.*;

public class DefaultLifecycleProcessor implements LifecycleProcessor, BeanFactoryAware {

    private volatile boolean running;

    @Nullable
    private volatile ConfigurableListableBeanFactory beanFactory;

    @Override
    public void start() {
        startBeans(false);
        this.running = true;
    }

    @Override
    public void stop() {

    }

    @Override
    public boolean isRunning() {
        return false;
    }

    @Override
    public void onRefresh() {
        startBeans(true);
        this.running = true;
    }

    @Override
    public void onClose() {

    }

    public void setBeanFactory(BeanFactory beanFactory) {
        if (!(beanFactory instanceof ConfigurableListableBeanFactory)) {
            throw new IllegalArgumentException(
                    "DefaultLifecycleProcessor requires a ConfigurableListableBeanFactory: " + beanFactory);
        }
        this.beanFactory = (ConfigurableListableBeanFactory) beanFactory;
    }

    private ConfigurableListableBeanFactory getBeanFactory() {
        ConfigurableListableBeanFactory beanFactory = this.beanFactory;
        Assert.state(beanFactory != null, "No BeanFactory available");
        return beanFactory;
    }

    private void startBeans(boolean autoStartupOnly) {
        Map<String, Lifecycle> lifecycleBeans = getLifecycleBeans();
        if (!lifecycleBeans.isEmpty()) {
            for (String key : lifecycleBeans.keySet()) {
                Lifecycle bean = lifecycleBeans.get(key);
                //SmartLifecycle属性的AutoStartup返回true则执行start方法
                if (!autoStartupOnly || (bean instanceof SmartLifecycle && ((SmartLifecycle) bean).isAutoStartup())) {
                    bean.start();
                }
            }
        }


    }

    protected Map<String, Lifecycle> getLifecycleBeans() {
        ConfigurableListableBeanFactory beanFactory = getBeanFactory();
        Map<String, Lifecycle> beans = new LinkedHashMap<>();
        String[] beanNames = beanFactory.getBeanNamesForType(Lifecycle.class, false, false);
        for (String beanName : beanNames) {
            String beanNameToRegister = BeanFactoryUtils.transformedBeanName(beanName);
            boolean isFactoryBean = beanFactory.isFactoryBean(beanNameToRegister);
            String beanNameToCheck = (isFactoryBean ? BeanFactory.FACTORY_BEAN_PREFIX + beanName : beanName);
            if ((beanFactory.containsSingleton(beanNameToRegister) &&
                    (!isFactoryBean || matchesBeanType(Lifecycle.class, beanNameToCheck, beanFactory))) ||
                    matchesBeanType(SmartLifecycle.class, beanNameToCheck, beanFactory)) {
                Object bean = beanFactory.getBean(beanNameToCheck);
                if (bean != this && bean instanceof Lifecycle) {
                    beans.put(beanNameToRegister, (Lifecycle) bean);
                }
            }
        }
        return beans;
    }

    private boolean matchesBeanType(Class<?> targetType, String beanName, BeanFactory beanFactory) {
        Class<?> beanType = beanFactory.getType(beanName);
        return (beanType != null && targetType.isAssignableFrom(beanType));
    }
}
