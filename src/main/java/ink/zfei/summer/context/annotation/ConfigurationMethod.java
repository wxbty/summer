package ink.zfei.summer.context.annotation;

import ink.zfei.summer.core.type.MethodMetadata;

/**
 * @Configuration 配置类的方法
 */
public class ConfigurationMethod {

    protected final ConfigurationClass configurationClass;

    protected final MethodMetadata metadata;

    public ConfigurationMethod(MethodMetadata metadata, ConfigurationClass configurationClass) {
        this.configurationClass = configurationClass;
        this.metadata = metadata;
    }


    public ConfigurationClass getConfigurationClass() {
        return this.configurationClass;
    }

    public MethodMetadata getMetadata() {
        return this.metadata;
    }
}
