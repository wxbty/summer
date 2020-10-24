package ink.zfei.summer.beans.factory.support;

import ink.zfei.summer.beans.MutablePropertyValues;
import ink.zfei.summer.beans.factory.config.BeanDefinition;
import ink.zfei.summer.beans.factory.config.BeanDefinitionHolder;
import ink.zfei.summer.beans.factory.support.AbstractBeanDefinition;
import ink.zfei.summer.core.ResolvableType;

import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 1、beanDefinition有父子关系，子可以继承大部分父通用属性，
 * 2、在BeanFactory阶段merge属性
 * 3、是为了代替Root/childBeanDefinition,需要硬编码指定父子关系Generic更灵活
 */
public class GenericBeanDefinition extends AbstractBeanDefinition {

    private BeanDefinitionHolder decoratedDefinition;

    public boolean isFactoryMethodUnique = false;

    public GenericBeanDefinition() {
        super();

    }

    public GenericBeanDefinition(BeanDefinition original) {
        super(original);
    }

    public GenericBeanDefinition(Class<?> beanClass) {
        super();
        setBeanClass(beanClass);
    }

    private String id;
    private String scope = "singleton";
    private String factoryBeanName;
    private String factoryMethodName;
    private String parentName;
    private String construcrorParm;

    public volatile Boolean isFactoryBean;
    public volatile Class<?> resolvedTargetType;
    public volatile Method factoryMethodToIntrospect;
    public Class<?> resolved;
    public boolean postProcessed = false;

    public volatile ResolvableType factoryMethodReturnType;
    public Executable resolvedConstructorOrFactoryMethod;
    public boolean constructorArgumentsResolved = false;
    Object[] resolvedConstructorArguments;
    public Object[] preparedConstructorArguments;

    public volatile ResolvableType targetType;

    public Method getResolvedFactoryMethod() {
        return this.factoryMethodToIntrospect;
    }

    public Constructor<?>[] getPreferredConstructors() {
        return null;
    }

    public String getFactoryMethodName() {
        return factoryMethodName;
    }


    public void setFactoryMethodName(String factoryMethodName) {
        this.factoryMethodName = factoryMethodName;
    }

    public String getFactoryBeanName() {
        return factoryBeanName;
    }

    public void setFactoryBeanName(String factoryBeanName) {
        this.factoryBeanName = factoryBeanName;
    }


    public String getScope() {
        return scope;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }


    @Override
    public void setParentName(String parentName) {
        this.parentName = parentName;
    }

    @Override
    public String getParentName() {
        return this.parentName;
    }

    public String getConstrucrorParm() {
        return construcrorParm;
    }

    public void setConstrucrorParm(String construcrorParm) {
        this.construcrorParm = construcrorParm;
    }

    public boolean isFactoryMethod(Method candidate) {
        return candidate.getName().equals(getFactoryMethodName());
    }

    public BeanDefinitionHolder getDecoratedDefinition() {
        return this.decoratedDefinition;
    }

    public Class<?> getTargetType() {
        if (this.resolvedTargetType != null) {
            return this.resolvedTargetType;
        }
        ResolvableType targetType = this.targetType;
        return (targetType != null ? targetType.resolve() : null);
    }
}