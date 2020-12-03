package ink.zfei.summer.context.support;

import ink.zfei.summer.beans.BeanDefinitionRegistry;
import ink.zfei.summer.beans.factory.BeanDefinitionStoreException;
import ink.zfei.summer.beans.factory.BeanFactory;
import ink.zfei.summer.beans.factory.config.BeanDefinition;
import ink.zfei.summer.beans.factory.config.ConfigurableListableBeanFactory;
import ink.zfei.summer.beans.factory.support.GenericBeanDefinition;
import ink.zfei.summer.core.AbstractApplicationContext;
import ink.zfei.summer.core.ApplicationContext;
import ink.zfei.summer.core.ResolvableType;
import ink.zfei.summer.lang.Nullable;
import ink.zfei.summer.util.Assert;

import java.lang.annotation.Annotation;
import java.util.Map;

public class GenericApplicationContext extends AbstractApplicationContext implements BeanDefinitionRegistry {

    private final DefaultListableBeanFactory beanFactory;

    public GenericApplicationContext() {
        this.beanFactory = new DefaultListableBeanFactory();
    }

    public GenericApplicationContext(DefaultListableBeanFactory beanFactory) {
        Assert.notNull(beanFactory, "BeanFactory must not be null");
        this.beanFactory = beanFactory;
    }

    @Override
    protected final void refreshBeanFactory() throws IllegalStateException {

//        this.beanFactory.setSerializationId(getId());
    }

    @Override
    public final ConfigurableListableBeanFactory getBeanFactory() {
        return this.beanFactory;
    }

    @Override
    public void registerBeanDefinition(String beanName, BeanDefinition beanDefinition)
            throws BeanDefinitionStoreException {

        this.beanFactory.registerBeanDefinition(beanName, beanDefinition);
    }

    @Override
    public void removeBeanDefinition(String beanName) {
        this.beanFactory.removeBeanDefinition(beanName);
    }

    @Override
    public BeanDefinition getBeanDefinition(String beanName) {
        return this.beanFactory.getBeanDefinition(beanName);
    }

    @Override
    protected final void closeBeanFactory() {
//        this.beanFactory.setSerializationId(null);
    }

    @Override
    public void setParent(@Nullable ApplicationContext parent) {
//        super.setParent(parent);
//        this.beanFactory.setParentBeanFactory(getInternalParentBeanFactory());
    }


    @Override
    public String[] getBeanNamesForType(ResolvableType type, boolean includeNonSingletons, boolean allowEagerInit) {
        return new String[0];
    }

    @Override
    public <T> Map<String, T> getBeansOfType(@Nullable Class<T> type) {
        return getBeanFactory().getBeansOfType(type);
    }

    @Override
    public <T> Map<String, T> getBeansOfType(Class<T> type, boolean includeNonSingletons, boolean allowEagerInit) {
        return getBeanFactory().getBeansOfType(type, includeNonSingletons, allowEagerInit);
    }

    @Override
    public String[] getBeanNamesForAnnotation(Class<? extends Annotation> annotationType) {
//        assertBeanFactoryActive();
        return getBeanFactory().getBeanNamesForAnnotation(annotationType);
    }

    @Override
    public Map<String, Object> getBeansWithAnnotation(Class<? extends Annotation> annotationType) {
        return getBeanFactory().getBeansWithAnnotation(annotationType);
    }

    @Override
    public <T> T getBean(Class<T> requiredType, Object... args) {
        return getBeanFactory().getBean(requiredType, args);
    }

    @Override
    public Object getBean(String name, Object... args) {
        return getBeanFactory().getBean(name, args);
    }
}
