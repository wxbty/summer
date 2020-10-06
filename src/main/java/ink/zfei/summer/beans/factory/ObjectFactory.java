package ink.zfei.summer.beans.factory;

@FunctionalInterface
public interface ObjectFactory<T> {

    T getObject() ;

}
