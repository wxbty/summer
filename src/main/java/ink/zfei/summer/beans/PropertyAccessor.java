package ink.zfei.summer.beans;

import ink.zfei.summer.core.convert.TypeDescriptor;
import ink.zfei.summer.lang.Nullable;

import java.util.Map;

public interface PropertyAccessor {

    /**
     * Path separator for nested properties.
     * Follows normal Java conventions: getFoo().getBar() would be "foo.bar".
     */
    String NESTED_PROPERTY_SEPARATOR = ".";

    /**
     * Path separator for nested properties.
     * Follows normal Java conventions: getFoo().getBar() would be "foo.bar".
     */
    char NESTED_PROPERTY_SEPARATOR_CHAR = '.';

    /**
     * Marker that indicates the start of a property key for an
     * indexed or mapped property like "person.addresses[0]".
     */
    String PROPERTY_KEY_PREFIX = "[";

    /**
     * Marker that indicates the start of a property key for an
     * indexed or mapped property like "person.addresses[0]".
     */
    char PROPERTY_KEY_PREFIX_CHAR = '[';

    /**
     * Marker that indicates the end of a property key for an
     * indexed or mapped property like "person.addresses[0]".
     */
    String PROPERTY_KEY_SUFFIX = "]";

    /**
     * Marker that indicates the end of a property key for an
     * indexed or mapped property like "person.addresses[0]".
     */
    char PROPERTY_KEY_SUFFIX_CHAR = ']';


    /**
     * Determine whether the specified property is readable.
     * <p>Returns {@code false} if the property doesn't exist.
     *
     * @param propertyName the property to check
     *                     (may be a nested path and/or an indexed/mapped property)
     * @return whether the property is readable
     */
    boolean isReadableProperty(String propertyName);


    boolean isWritableProperty(String propertyName);


    @Nullable
    Class<?> getPropertyType(String propertyName);


    @Nullable
    TypeDescriptor getPropertyTypeDescriptor(String propertyName);


    @Nullable
    Object getPropertyValue(String propertyName);

    void setPropertyValue(String propertyName, @Nullable Object value);


    void setPropertyValue(PropertyValue pv);


    void setPropertyValues(Map<?, ?> map);

    void setPropertyValues(PropertyValues pvs);


    void setPropertyValues(PropertyValues pvs, boolean ignoreUnknown);


    void setPropertyValues(PropertyValues pvs, boolean ignoreUnknown, boolean ignoreInvalid);

}
