package ink.zfei.summer.beans.factory;

import ink.zfei.summer.core.ResolvableType;
import ink.zfei.summer.lang.Nullable;

import java.lang.annotation.Annotation;
import java.util.Map;

/**
 * 这个接口扩展了BeanFactory,然后提供了对Bean批量获取的能力,
 * 不包含其他层级容器（父子容器）和非beanDefinition形式定义的bean.
 */
public interface ListableBeanFactory extends BeanFactory {

    /**
     * 判断当前bean工厂是否包含指定beanName的bean定义.
     * @param beanName the name of the bean to look for
     * @return if this bean factory contains a bean definition with the given name
     */
    boolean containsBeanDefinition(String beanName);

    /**
     * 返回当前bean工厂中的Bean的数目
     * @return the number of beans defined in the factory
     */
    int getBeanDefinitionCount();

    /**
     * 返回当前bean工厂中所有Bean的名称
     * @return the names of all beans defined in this factory,
     * or an empty array if none defined
     */
    String[] getBeanDefinitionNames();

    /**
     * 返回与给定类型（包括子类）匹配的bean的名称
     * 从bean定义判读，如果是FactoryBean，用{@code getObjectType}的值判断
     * 注意：该方法只检查顶级bean（不包含嵌套bean）
     *  嵌套Bean定义：在Spring中，如果某个Bean所依赖的Bean不想被Spring容器直接访问，
     *  -可以使用嵌套Bean。和普通的Bean一样，使用<bean>元素来定义嵌套的Bean，
     *  -嵌套Bean只对它的外部的Bean有效，Spring容器无法直接访问嵌套的Bean，
     *  -因此定义嵌套Bean也无需指定id属性。
     *  方法也考虑由FactoryBeans创建的对象，这意味着FactoryBean将被【提早】初始化。
     *  如果FactoryBean创建的对象不匹配，原始FactoryBean本身也将与类型匹配
     *  不包含父子容器
     *  注意：<i>不</i>会忽略已注册的单例bean
     * @since 4.2
     * @see FactoryBean#getObjectType
     */
    String[] getBeanNamesForType(ResolvableType type);

    /**
     * 比上面的多了参数，是否包含原型bean，是否包含<懒加载单例>
     * summer不实现
     */
    default String[] getBeanNamesForType(ResolvableType type, boolean includeNonSingletons, boolean allowEagerInit){
        throw new RuntimeException("UnImplemented");
    }

    /**
     * 匹配所有类型的bean，
     * 不管是单例，原型还是FactoryBean
     * @see FactoryBean#getObjectType
     */
    String[] getBeanNamesForType(@Nullable Class<?> type);


    default String[] getBeanNamesForType(@Nullable Class<?> type, boolean includeNonSingletons, boolean allowEagerInit){
        throw new RuntimeException("UnImplemented");
    }

    default <T> Map<String, T> getBeansOfType(@Nullable Class<T> type) {
        throw new RuntimeException("UnImplemented");
    }


    default <T> Map<String, T> getBeansOfType(@Nullable Class<T> type, boolean includeNonSingletons, boolean allowEagerInit){
        throw new RuntimeException("UnImplemented");
    }

    /**
     * 查找使用提供的{@link Annotation}注解修饰的beanName，
     * 并不会提早实例化bean，除了factoryBean
     */
    String[] getBeanNamesForAnnotation(Class<? extends Annotation> annotationType);


   default Map<String, Object> getBeansWithAnnotation(Class<? extends Annotation> annotationType) {
        throw new RuntimeException("UnImplemented");
    }

    /**
     * 查找bean上指定的注解，包括父类和接口
     */
    @Nullable
    <A extends Annotation> A findAnnotationOnBean(String beanName, Class<A> annotationType)
            throws NoSuchBeanDefinitionException;

}
