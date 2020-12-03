package ink.zfei.summer.beans.factory;

import ink.zfei.summer.lang.Nullable;

/**
 * Hierarchical(分层)，提供父容器的访问功能.至于父容器的设置,
 * 需要找ConfigurableBeanFactory的setParentBeanFactory
 * (接口把设置跟获取给拆开了!)
 */
public interface HierarchicalBeanFactory extends BeanFactory {

    /**
     * 返回本Bean工厂的父工厂
     */
    @Nullable
    BeanFactory getParentBeanFactory();

    /**
     * 判断本地工厂是否包含这个Bean（忽略其他所有父工厂）
     */
    boolean containsLocalBean(String name);

}
