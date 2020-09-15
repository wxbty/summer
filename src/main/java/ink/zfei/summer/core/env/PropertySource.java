package ink.zfei.summer.core.env;


import ink.zfei.summer.util.ObjectUtils;

/**
 * 属性源，对获取键值对资源的抽象；
 * 一般情况下不会单独使用PropertySource，而是通过PropertySources来获取相关属性;
 */
public abstract class PropertySource<T> {

    protected final String name;

    /**
     * 任何封装了属性集的类型，如map对象，Properties、redis客户端
     * 一般是最终属性数据集合，看getProperty具体实现，比如composite，保存了无意义到object，实际获取数据是子类的一个list
     */
    protected final T source;

    /**
     * 使用该构造器，且source不是占位obj，表面source存储了有效属性集合
     */
    public PropertySource(String name, T source) {
        this.name = name;
        this.source = source;
    }

    /**
     * 如果source不是用于保存数据，source用一个obj对象占位
     */
    @SuppressWarnings("unchecked")
    public PropertySource(String name) {
        this(name, (T) new Object());
    }

    public String getName() {
        return this.name;
    }


    public T getSource() {
        return this.source;
    }

    /**
     * 判断是否包含属性，springboot中大部分是可枚举source，使用缓存都name集合判断即可，少部门使用该方法判断
     * 小细节：若对应的key存在但是值为null，此处也是返回false的  表示不包含
     */
    public boolean containsProperty(String name) {
        return (getProperty(name) != null);
    }

    /**
     * PropertySource经常作为map、list等元素，如果不重写hashcode（默认近似内存地址）
     * 使用contains、get等方法会找不到，这里用name来区分
     */
    @Override
    public int hashCode() {
        return ObjectUtils.nullSafeHashCode(getName());
    }

    /**
     * 同hashcode，equals使用更频繁
     * 只要name相等  就代表着是同一个对象
     */
    @Override
    public boolean equals(Object other) {
        return (this == other || (other instanceof PropertySource &&
                ObjectUtils.nullSafeEquals(getName(), ((PropertySource<?>) other).getName())));
    }

    public abstract Object getProperty(String name);

}
