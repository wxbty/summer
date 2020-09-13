package ink.zfei.summer.core.env;

import java.security.AccessControlException;
import java.util.Collections;
import java.util.Map;

public abstract class AbstractEnvironment implements ConfigurableEnvironment {

    private final MutablePropertySources propertySources = new MutablePropertySources();

    public AbstractEnvironment() {
        customizePropertySources(this.propertySources);
    }

    protected void customizePropertySources(MutablePropertySources propertySources) {

    }


    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    public Map<String, Object> getSystemProperties() {
        return (Map) System.getProperties();
    }

    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    public Map<String, Object> getSystemEnvironment() {

        return (Map) System.getenv();
    }

}
