package ink.zfei.summer.core;

/**
 * 属性访问接口，可以使一些元数据类有额外扩展属性的能力，有一个默认基础实现AttributeAccessorSupport供各子类访问
 */
public interface AttributeAccessor {

    void setAttribute(String name, Object value);

    Object getAttribute(String name);

    Object removeAttribute(String name);

    boolean hasAttribute(String name);

    String[] attributeNames();
}
