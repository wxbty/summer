package ink.zfei.summer.beans.factory;

public interface FactoryBean<T> {

    T getObject() ;

    Class<?> getObjectType();

    default boolean isSingleton() {
        return true;
    }

    String OBJECT_TYPE_ATTRIBUTE = "factoryBeanObjectType";
}
