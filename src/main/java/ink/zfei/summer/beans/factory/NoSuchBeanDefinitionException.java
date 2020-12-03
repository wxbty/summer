package ink.zfei.summer.beans.factory;

import ink.zfei.summer.core.ResolvableType;
import ink.zfei.summer.lang.Nullable;

public class NoSuchBeanDefinitionException extends RuntimeException {

    private final String beanName;

    @Nullable
    private final ResolvableType resolvableType;

    public NoSuchBeanDefinitionException(String name) {
        super("No bean named '" + name + "' available");
        this.beanName = name;
        this.resolvableType = null;
    }

    public NoSuchBeanDefinitionException(ResolvableType type, String message) {
        super("No qualifying bean of type '" + type + "' available: " + message);
        this.beanName = null;
        this.resolvableType = type;
    }

    public NoSuchBeanDefinitionException(Class<?> type) {
        this(ResolvableType.forClass(type));
    }

    public NoSuchBeanDefinitionException(ResolvableType type) {
        super("No qualifying bean of type '" + type + "' available");
        this.beanName = null;
        this.resolvableType = type;
    }

    public NoSuchBeanDefinitionException(Class<?> type, String message) {
        this(ResolvableType.forClass(type), message);
    }

    public int getNumberOfBeansFound() {
        return 0;
    }
}
