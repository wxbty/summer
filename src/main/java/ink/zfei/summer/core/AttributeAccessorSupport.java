package ink.zfei.summer.core;

import ink.zfei.summer.util.Assert;
import ink.zfei.summer.util.StringUtils;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * AttributeAccessor实现，提供基础额外属性访问能力
 */
public abstract class AttributeAccessorSupport implements AttributeAccessor, Serializable {

    private final Map<String, Object> attributes = new LinkedHashMap<>();


    @Override
    public void setAttribute(String name, Object value) {
        Assert.notNull(name, "Name must not be null");
        if (value != null) {
            this.attributes.put(name, value);
        } else {
            removeAttribute(name);
        }
    }

    @Override
    public Object getAttribute(String name) {
        Assert.notNull(name, "Name must not be null");
        return this.attributes.get(name);
    }

    @Override
    public Object removeAttribute(String name) {
        Assert.notNull(name, "Name must not be null");
        return this.attributes.remove(name);
    }

    @Override
    public boolean hasAttribute(String name) {
        Assert.notNull(name, "Name must not be null");
        return this.attributes.containsKey(name);
    }

    @Override
    public String[] attributeNames() {
        return StringUtils.toStringArray(this.attributes.keySet());
    }


    /**
     * Copy the attributes from the supplied AttributeAccessor to this accessor.
     *
     * @param source the AttributeAccessor to copy from
     */
    protected void copyAttributesFrom(AttributeAccessor source) {
        Assert.notNull(source, "Source must not be null");
        String[] attributeNames = source.attributeNames();
        for (String attributeName : attributeNames) {
            setAttribute(attributeName, source.getAttribute(attributeName));
        }
    }


    @Override
    public boolean equals(Object other) {
        return (this == other || (other instanceof AttributeAccessorSupport &&
                this.attributes.equals(((AttributeAccessorSupport) other).attributes)));
    }

    @Override
    public int hashCode() {
        return this.attributes.hashCode();
    }

}

