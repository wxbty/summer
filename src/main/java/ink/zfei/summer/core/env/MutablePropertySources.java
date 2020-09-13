package ink.zfei.summer.core.env;

import java.util.Iterator;
import java.util.List;
import java.util.Spliterator;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

public class MutablePropertySources implements PropertySources  {

    private final List<PropertySource<?>> propertySourceList = new CopyOnWriteArrayList<>();

    public MutablePropertySources() {
    }

    public MutablePropertySources(PropertySources propertySources) {
        this();
        for (PropertySource<?> propertySource : propertySources) {
            addLast(propertySource);
        }
    }

    @Override
    public boolean contains(String name) {
        return false;
    }

    @Override
    public PropertySource<?> get(String name) {
        return null;
    }


    /**
     * Add the given property source object with lowest precedence.
     */
    public void addLast(PropertySource<?> propertySource) {
        synchronized (this.propertySourceList) {
            removeIfPresent(propertySource);
            this.propertySourceList.add(propertySource);
        }
    }

    protected void removeIfPresent(PropertySource<?> propertySource) {
        this.propertySourceList.remove(propertySource);
    }

    @Override
    public Iterator<PropertySource<?>> iterator() {
        return null;
    }

    @Override
    public void forEach(Consumer<? super PropertySource<?>> action) {

    }

    @Override
    public Spliterator<PropertySource<?>> spliterator() {
        return null;
    }
}
