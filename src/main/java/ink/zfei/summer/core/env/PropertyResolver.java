package ink.zfei.summer.core.env;

/*
 * 属性读写接口，主要用于环境
 * */
public interface PropertyResolver {

    boolean containsProperty(String key);

    String getProperty(String key);

    String resolvePlaceholders(String text);

}
