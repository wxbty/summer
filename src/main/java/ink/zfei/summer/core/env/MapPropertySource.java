package ink.zfei.summer.core.env;

import ink.zfei.summer.util.StringUtils;

import java.util.Map;

/**
 * 最常见的一种属性源，以map形式存储数据，其他PropertiesPropertySource、ResourcePropertySource都是其子类，
 * 因为Properties继承hashtable（Map），resource读取属性存入Properties中，这些子类，只是标明属性来源区别，实际存储形式一样
 */
public class MapPropertySource extends EnumerablePropertySource<Map<String, Object>> {

    /**
     * Create a new {@code MapPropertySource} with the given name and {@code Map}.
     *
     * @param name   the associated name
     * @param source the Map source (without {@code null} values in order to get
     *               consistent {@link #getProperty} and {@link #containsProperty} behavior)
     */
    public MapPropertySource(String name, Map<String, Object> source) {
        super(name, source);
    }


    @Override
    public Object getProperty(String name) {
        return this.source.get(name);
    }

    @Override
    public boolean containsProperty(String name) {
        return this.source.containsKey(name);
    }

    @Override
    public String[] getPropertyNames() {
        return StringUtils.toStringArray(this.source.keySet());
    }

}