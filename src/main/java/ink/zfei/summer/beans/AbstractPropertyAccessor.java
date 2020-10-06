package ink.zfei.summer.beans;

import ink.zfei.summer.lang.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public abstract class AbstractPropertyAccessor extends TypeConverterSupport implements ConfigurablePropertyAccessor {

    private boolean extractOldValueForEditor = false;

    private boolean autoGrowNestedPaths = false;


    @Override
    public void setExtractOldValueForEditor(boolean extractOldValueForEditor) {
        this.extractOldValueForEditor = extractOldValueForEditor;
    }

    @Override
    public boolean isExtractOldValueForEditor() {
        return this.extractOldValueForEditor;
    }

    @Override
    public void setAutoGrowNestedPaths(boolean autoGrowNestedPaths) {
        this.autoGrowNestedPaths = autoGrowNestedPaths;
    }

    @Override
    public boolean isAutoGrowNestedPaths() {
        return this.autoGrowNestedPaths;
    }


    @Override
    public void setPropertyValue(PropertyValue pv) {
        setPropertyValue(pv.getName(), pv.getValue());
    }

    @Override
    public void setPropertyValues(Map<?, ?> map) {
        setPropertyValues(new MutablePropertyValues(map));
    }

    @Override
    public void setPropertyValues(PropertyValues pvs) {
        setPropertyValues(pvs, false, false);
    }

    @Override
    public void setPropertyValues(PropertyValues pvs, boolean ignoreUnknown) {
        setPropertyValues(pvs, ignoreUnknown, false);
    }

    @Override
    public void setPropertyValues(PropertyValues pvs, boolean ignoreUnknown, boolean ignoreInvalid) {

        List<PropertyValue> propertyValues = (pvs instanceof MutablePropertyValues ?
                ((MutablePropertyValues) pvs).getPropertyValueList() : Arrays.asList(pvs.getPropertyValues()));
        for (PropertyValue pv : propertyValues) {
            // This method may throw any BeansException, which won't be caught
            // here, if there is a critical failure such as no matching field.
            // We can attempt to deal only with less serious exceptions.
            setPropertyValue(pv);

        }

    }


    // Redefined with public visibility.
    @Override
    @Nullable
    public Class<?> getPropertyType(String propertyPath) {
        return null;
    }


    @Override
    @Nullable
    public abstract Object getPropertyValue(String propertyName);


    @Override
    public abstract void setPropertyValue(String propertyName, @Nullable Object value);

}
