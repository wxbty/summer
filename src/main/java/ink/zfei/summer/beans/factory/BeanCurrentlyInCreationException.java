package ink.zfei.summer.beans.factory;

public class BeanCurrentlyInCreationException extends RuntimeException {

    private final String beanName;

    public BeanCurrentlyInCreationException(String beanName) {
        this.beanName = beanName;
    }
}
