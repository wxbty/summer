package ink.zfei.summer.beans;

import ink.zfei.summer.core.convert.ConversionService;
import ink.zfei.summer.lang.Nullable;

public interface ConfigurablePropertyAccessor extends PropertyAccessor,TypeConverter {

    /**
     * Specify a Spring 3.0 ConversionService to use for converting
     * property values, as an alternative to JavaBeans PropertyEditors.
     */
    void setConversionService(@Nullable ConversionService conversionService);
    /**
     * Set whether to extract the old property value when applying a
     * property editor to a new value for a property.
     */
    void setExtractOldValueForEditor(boolean extractOldValueForEditor);

    /**
     * Return whether to extract the old property value when applying a
     * property editor to a new value for a property.
     */
    boolean isExtractOldValueForEditor();

    /**
     * Set whether this instance should attempt to "auto-grow" a
     * nested path that contains a {@code null} value.
     * <p>If {@code true}, a {@code null} path location will be populated
     * with a default object value and traversed instead of resulting in a
     * <p>Default is {@code false} on a plain PropertyAccessor instance.
     */
    void setAutoGrowNestedPaths(boolean autoGrowNestedPaths);

    /**
     * Return whether "auto-growing" of nested paths has been activated.
     */
    boolean isAutoGrowNestedPaths();

}