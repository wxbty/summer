package ink.zfei.summer.core.env;

public interface PropertySources extends Iterable<PropertySource<?>>{


    boolean contains(String name);


    PropertySource<?> get(String name);


}
