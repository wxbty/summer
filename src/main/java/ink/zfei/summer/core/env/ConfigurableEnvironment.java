package ink.zfei.summer.core.env;

import java.util.Map;

public interface ConfigurableEnvironment extends Environment{

    void setProfile(String profile);

    Map<String, Object> getSystemEnvironment();

    Map<String, Object> getSystemProperties();

    MutablePropertySources getPropertySources();
}
