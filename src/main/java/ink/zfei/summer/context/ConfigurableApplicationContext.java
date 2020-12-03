package ink.zfei.summer.context;

import ink.zfei.summer.beans.BeanFactoryPostProcessor;
import ink.zfei.summer.beans.factory.config.ConfigurableListableBeanFactory;
import ink.zfei.summer.core.ApplicationContext;
import ink.zfei.summer.core.ApplicationListener;
import ink.zfei.summer.core.env.ConfigurableEnvironment;
import ink.zfei.summer.lang.Nullable;

public interface ConfigurableApplicationContext extends ApplicationContext {


    String CONFIG_LOCATION_DELIMITERS = ",; \t\n";

    String CONVERSION_SERVICE_BEAN_NAME = "conversionService";


    String LOAD_TIME_WEAVER_BEAN_NAME = "loadTimeWeaver";


    String ENVIRONMENT_BEAN_NAME = "environment";


    String SYSTEM_PROPERTIES_BEAN_NAME = "systemProperties";


    String SYSTEM_ENVIRONMENT_BEAN_NAME = "systemEnvironment";

    String SHUTDOWN_HOOK_THREAD_NAME = "SpringContextShutdownHook";


    /**
     * Set the unique id of this application context.
     * @since 3.0
     */
    void setId(String id);


    void setParent(@Nullable ApplicationContext parent);

    /**
     * Set the {@code Environment} for this application context.
     * @param environment the new environment
     * @since 3.1
     */
    void setEnvironment(ConfigurableEnvironment environment);

//    @Override
//    ConfigurableEnvironment getEnvironment();

    void addBeanFactoryPostProcessor(BeanFactoryPostProcessor postProcessor);


    void addApplicationListener(ApplicationListener listener);

//    void addProtocolResolver(ProtocolResolver resolver);

    void refresh();


    ConfigurableListableBeanFactory getBeanFactory() throws IllegalStateException;

}
