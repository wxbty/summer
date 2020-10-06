package ink.zfei.summer.beans.factory;

public class BeanIsAbstractException extends RuntimeException {

    private final String beanName;

    public BeanIsAbstractException(String beanName) {
        this.beanName = beanName;
    }
}
