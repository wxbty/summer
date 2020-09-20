package ink.zfei.summer.beans;

import ink.zfei.summer.core.AttributeAccessorSupport;

/**
 * BeanMetadataElement：有来源
 * 重写了父类AttributeAccessorSupport，只能操作BeanMetadataAttribute对象属性的存储。
 * 在额外自己增加了两个方法
 */

@SuppressWarnings("serial")
public class BeanMetadataAttributeAccessor extends AttributeAccessorSupport implements BeanMetadataElement {


    private Object source;


    /**
     * Set the configuration source {@code Object} for this metadata element.
     * <p>The exact type of the object will depend on the configuration mechanism used.
     */
    public void setSource(Object source) {
        this.source = source;
    }

    @Override

    public Object getSource() {
        return this.source;
    }


    /**
     * Add the given BeanMetadataAttribute to this accessor's set of attributes.
     *
     * @param attribute the BeanMetadataAttribute object to register
     */
    public void addMetadataAttribute(BeanMetadataAttribute attribute) {
        super.setAttribute(attribute.getName(), attribute);
    }

    /**
     * Look up the given BeanMetadataAttribute in this accessor's set of attributes.
     *
     * @param name the name of the attribute
     * @return the corresponding BeanMetadataAttribute object,
     * or {@code null} if no such attribute defined
     */

    public BeanMetadataAttribute getMetadataAttribute(String name) {
        return (BeanMetadataAttribute) super.getAttribute(name);
    }

    @Override
    public void setAttribute(String name, Object value) {
        super.setAttribute(name, new BeanMetadataAttribute(name, value));
    }

    @Override
    public Object getAttribute(String name) {
        BeanMetadataAttribute attribute = (BeanMetadataAttribute) super.getAttribute(name);
        return (attribute != null ? attribute.getValue() : null);
    }

    @Override
    public Object removeAttribute(String name) {
        BeanMetadataAttribute attribute = (BeanMetadataAttribute) super.removeAttribute(name);
        return (attribute != null ? attribute.getValue() : null);
    }

}
