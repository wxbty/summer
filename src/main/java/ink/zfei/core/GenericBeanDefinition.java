package ink.zfei.core;

import ink.zfei.beans.factory.config.BeanDefinition;
import ink.zfei.beans.factory.config.ConfigurableBeanFactory;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class GenericBeanDefinition implements BeanDefinition {

    private String id;
    private String beanClass;
    private String initMethod;
    private String scope = "singleton";
    private String factoryBeanName;
    private String factoryMethodName;
    private String parentName;

    public Map<String, String> getPropertyValues() {
        return propertyValues;
    }

    private Map<String, String> propertyValues = new ConcurrentHashMap<>();

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

    public void setScope(String scope) {
        this.scope = scope;
    }


    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @Override
    public String getBeanClassName() {
        return beanClass;
    }

    @Override
    public void setBeanClassName(String beanClass) {
        this.beanClass = beanClass;
    }

    @Override
    public String getInitMethodName() {
        return initMethod;
    }

    @Override
    public void setInitMethodName(String initMethodName) {
        this.initMethod = initMethod;
    }

    public void putDep(String fieldName, String beanName) {
        this.propertyValues.put(fieldName, beanName);
    }

    public boolean hasPropertyValues() {
        return (this.propertyValues != null && !this.propertyValues.isEmpty());
    }

    @Override
    public void setParentName(String parentName) {
        this.parentName = parentName;
    }

    @Override
    public String getParentName() {
        return this.parentName;
    }
}
