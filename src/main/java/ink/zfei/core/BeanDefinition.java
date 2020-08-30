package ink.zfei.core;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class BeanDefinition {


    private String id;
    private String beanClass;
    private String initMethod;
    private String scope = "singleton";
    private String factoryBeanName;
    private String factoryMethodName;

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

    public String getBeanClass() {
        return beanClass;
    }

    public void setBeanClass(String beanClass) {
        this.beanClass = beanClass;
    }

    public String getInitMethod() {
        return initMethod;
    }

    public void setInitMethod(String initMethod) {
        this.initMethod = initMethod;
    }

    public void putDep(String fieldName, String beanName) {
        this.propertyValues.put(fieldName, beanName);
    }

    public boolean hasPropertyValues() {
        return (this.propertyValues != null && !this.propertyValues.isEmpty());
    }
}
