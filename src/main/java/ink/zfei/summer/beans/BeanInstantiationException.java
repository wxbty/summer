package ink.zfei.summer.beans;

public class BeanInstantiationException extends RuntimeException{

    private final Class<?> beanClass;

    public BeanInstantiationException(Class<?> beanClass,String message,Throwable cause) {
        super(message,cause);
        this.beanClass = beanClass;
    }

    public BeanInstantiationException(Class<?> beanClass,String message) {
        super(message);
        this.beanClass = beanClass;
    }

    public BeanInstantiationException(String message) {
        super(message);
        this.beanClass = null;
    }
}
