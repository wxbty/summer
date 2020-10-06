package ink.zfei.summer.beans;

import ink.zfei.summer.lang.Nullable;

import java.util.Arrays;
import java.util.Iterator;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public interface PropertyValues extends Iterable<PropertyValue> {

    /**
     * Return an {@link Iterator} over the property values.
     * @since 5.1
     */
    @Override
    default Iterator<PropertyValue> iterator() {
        return Arrays.asList(getPropertyValues()).iterator();
    }

    /**
     * Return a {@link Spliterator} over the property values.
     * @since 5.1
     */
    @Override
    default Spliterator<PropertyValue> spliterator() {
        return Spliterators.spliterator(getPropertyValues(), 0);
    }

    /**
     * Return a sequential {@link Stream} containing the property values.
     * @since 5.1
     */
    default Stream<PropertyValue> stream() {
        return StreamSupport.stream(spliterator(), false);
    }

    /**
     * Return an array of the PropertyValue objects held in this object.
     */
    PropertyValue[] getPropertyValues();

    /**
     * Return the property value with the given name, if any.
     * @param propertyName the name to search for
     * @return the property value, or {@code null} if none
     */
    @Nullable
    PropertyValue getPropertyValue(String propertyName);

    /**
     * Return the changes since the previous PropertyValues.
     * Subclasses should also override {@code equals}.
     * @param old the old property values
     * @return the updated or new properties.
     * Return empty PropertyValues if there are no changes.
     * @see Object#equals
     */
    PropertyValues changesSince(PropertyValues old);

    /**
     * Is there a property value (or other processing entry) for this property?
     * @param propertyName the name of the property we're interested in
     * @return whether there is a property value for this property
     */
    boolean contains(String propertyName);

    /**
     * Does this holder not contain any PropertyValue objects at all?
     */
    boolean isEmpty();

}
