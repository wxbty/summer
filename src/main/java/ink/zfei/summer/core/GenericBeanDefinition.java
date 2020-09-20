package ink.zfei.summer.core;

import ink.zfei.summer.beans.factory.config.BeanDefinition;
import ink.zfei.summer.beans.factory.support.AbstractBeanDefinition;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 *  1、beanDefinition有父子关系，子可以继承大部分父通用属性，
 *  2、在BeanFactory阶段merge属性
 *  3、是为了代替Root/childBeanDefinition,需要硬编码指定父子关系Generic更灵活
 */
public class GenericBeanDefinition extends AbstractBeanDefinition {

    private String id;
    private String scope = "singleton";
    private String factoryBeanName;
    private String factoryMethodName;
    private String parentName;
    private String construcrorParm;

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

    public String getConstrucrorParm() {
        return construcrorParm;
    }

    public void setConstrucrorParm(String construcrorParm) {
        this.construcrorParm = construcrorParm;
    }
}
