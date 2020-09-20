package ink.zfei.summer.beans;

/**
 * Return the configuration source {@code Object} for this metadata element
 * (may be {@code null}).
 * bean元信息的来源，实际用到不多，只要和bean元信息相关的，都会实现该接口
 * bd属性来源，比如scan扫到的controller的bd的source，是映射controller.class的Resource对象
 * 自动配置类的bean，source是StandardMethodMetadata，封装了生成bean的配置类@bean方法
 * 自动配置类的bean，source是SimpleMethodMetadata,method信息来自asm解析class文件（不用jvm）
 * 除了bd，另一种实现是构造器参数内部类ValueHolder，source存储和value一样的属性值，实例化时获取使用
 */
public interface BeanMetadataElement {

    default Object getSource() {
        return null;
    }

}
