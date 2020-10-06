package ink.zfei.summer.beans.factory.config;

public interface ConfigurableBeanFactory {

    String SCOPE_SINGLETON = "singleton";

    String SCOPE_PROTOTYPE = "prototype";

    ClassLoader getBeanClassLoader();
}
