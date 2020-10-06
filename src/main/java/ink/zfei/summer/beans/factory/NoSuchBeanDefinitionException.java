package ink.zfei.summer.beans.factory;

public class NoSuchBeanDefinitionException extends RuntimeException {

    private final String beanName;

    public NoSuchBeanDefinitionException(String beanName) {
        this.beanName = beanName;
    }
}
