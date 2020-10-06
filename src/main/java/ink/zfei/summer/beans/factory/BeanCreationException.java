package ink.zfei.summer.beans.factory;

public class BeanCreationException extends RuntimeException {

    private final String beanName;

    public BeanCreationException(String beanName, String msg) {
        super("Error creating bean with name '" + beanName + "': " + msg);
        this.beanName = beanName;
    }
}
