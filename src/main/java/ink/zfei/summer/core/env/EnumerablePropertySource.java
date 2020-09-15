package ink.zfei.summer.core.env;

import ink.zfei.summer.util.ObjectUtils;


/**
 * 可枚举属性源，能够查询基数数据源所有的属性key/value
 * 这种属性源对containsProperty方法效率更高，因为getPropertyNames方法获取缓存的属性名称，比直接使用getProperty更快
 * spring提供大部分PropertySource都是可枚举的，一个反例是jndi属性源，只能通过getProperty确定是否属性存在
 */
public abstract class EnumerablePropertySource<T> extends PropertySource<T> {


    /**
     * 可枚举属性源主要让containsProperty更快，所以构造方法直接调用父类
     */
    public EnumerablePropertySource(String name, T source) {
        super(name, source);
    }


    protected EnumerablePropertySource(String name) {
        super(name);
    }


    @Override
    public boolean containsProperty(String name) {
        return ObjectUtils.containsElement(getPropertyNames(), name);
    }

    /**
     * Return the names of all properties contained by the
     * {@linkplain #getSource() source} object (never {@code null}).
     */
    public abstract String[] getPropertyNames();

}
