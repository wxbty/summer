package ink.zfei.summer.beans;

import ink.zfei.summer.util.Assert;
import ink.zfei.summer.util.ObjectUtils;
/**
 * Holder for a key-value style attribute that is part of a bean definition.
 * Keeps track of the definition source in addition to the key-value pair.
 * beanDefinition中，key-value形式存储的元信息，也有source
 */
public class BeanMetadataAttribute implements BeanMetadataElement {

    private final String name;

    
    private final Object value;

    
    private Object source;


    /**
     * Create a new AttributeValue instance.
     * @param name the name of the attribute (never {@code null})
     * @param value the value of the attribute (possibly before type conversion)
     */
    public BeanMetadataAttribute(String name,  Object value) {
        Assert.notNull(name, "Name must not be null");
        this.name = name;
        this.value = value;
    }


    /**
     * Return the name of the attribute.
     */
    public String getName() {
        return this.name;
    }

    /**
     * Return the value of the attribute.
     */
    
    public Object getValue() {
        return this.value;
    }

    /**
     * Set the configuration source {@code Object} for this metadata element.
     * <p>The exact type of the object will depend on the configuration mechanism used.
     */
    public void setSource( Object source) {
        this.source = source;
    }

    @Override
    
    public Object getSource() {
        return this.source;
    }


    @Override
    public boolean equals( Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof BeanMetadataAttribute)) {
            return false;
        }
        BeanMetadataAttribute otherMa = (BeanMetadataAttribute) other;
        return (this.name.equals(otherMa.name) &&
                ObjectUtils.nullSafeEquals(this.value, otherMa.value) &&
                ObjectUtils.nullSafeEquals(this.source, otherMa.source));
    }

    @Override
    public int hashCode() {
        return this.name.hashCode() * 29 + ObjectUtils.nullSafeHashCode(this.value);
    }

    @Override
    public String toString() {
        return "metadata attribute '" + this.name + "'";
    }

}