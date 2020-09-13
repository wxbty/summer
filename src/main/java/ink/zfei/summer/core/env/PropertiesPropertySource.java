package ink.zfei.summer.core.env;

import java.util.Map;

public class PropertiesPropertySource extends PropertySource<Map<String, Object>> {


    public PropertiesPropertySource(String name, Map<String, Object> source) {
        super(name, source);
    }

    @Override
    public Object getProperty(String name) {
        return null;
    }
}
