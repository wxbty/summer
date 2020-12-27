package ink.zfei.summer.beans.factory.annotation;

import ink.zfei.summer.beans.PropertyValues;
import ink.zfei.summer.beans.factory.support.GenericBeanDefinition;
import ink.zfei.summer.context.annotation.CommonAnnotationBeanPostProcessor;
import ink.zfei.summer.lang.Nullable;
import ink.zfei.summer.util.ReflectionUtils;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Collections;

/**
 * 注入元数据，包含了目标Bean的Class对象，和注入元素（InjectionElement）集合
 * 注入的注解包含CommonAnnotationBeanPostProcessor处理的@Resource
 * 也包含AutowiredAnnotationBeanPostProcessor处理的@Autowired
 */
public class InjectionMetadata {

    public static final InjectionMetadata EMPTY = new InjectionMetadata(Object.class, Collections.emptyList()) {
        @Override
        public void checkConfigMembers(GenericBeanDefinition beanDefinition) {
        }

        @Override
        public void inject(Object target, @Nullable String beanName, @Nullable PropertyValues pvs) {
        }

    };

    private final Class<?> targetClass;

    private final Collection<InjectedElement> injectedElements;

    public InjectionMetadata(Class<?> targetClass, Collection<InjectedElement> elements) {
        this.targetClass = targetClass;
        this.injectedElements = elements;
    }

    /**
     * 依赖注入的filed或者set方法的封装。
     * 不同注解修饰即不同子类。
     * filed或者method是成员变量。
     */
    public abstract static class InjectedElement {

        protected final Member member;

        protected final boolean isField;

        /**
         * java.beans包下的属性描述器,例如下面这个例子
         * PropertyDescriptor CatPropertyOfName = new PropertyDescriptor("name", Cat.class);
         * System.out.println(CatPropertyOfName.getPropertyType());  String 属性类型
         * System.out.println(CatPropertyOfName.getReadMethod()); --getName 读方法名称
         * System.out.println(CatPropertyOfName.getWriteMethod());  --setName 写方法名称
         */
        @Nullable
        protected final PropertyDescriptor pd;


        protected InjectedElement(Member member, @Nullable PropertyDescriptor pd) {
            this.member = member;
            this.isField = (member instanceof Field);
            this.pd = pd;
        }

        protected void inject(Object target, @Nullable String requestingBeanName, @Nullable PropertyValues pvs)
                throws Throwable {

            if (this.isField) {
                Field field = (Field) this.member;
                ReflectionUtils.makeAccessible(field);
                field.set(target, getResourceToInject(target, requestingBeanName));
            } else {
                Method method = (Method) this.member;
                ReflectionUtils.makeAccessible(method);
                method.invoke(target, getResourceToInject(target, requestingBeanName));

            }
        }

        public final Member getMember() {
            return this.member;
        }

        @Nullable
        protected Object getResourceToInject(Object target, @Nullable String requestingBeanName) {
            return null;
        }


    }

    public void checkConfigMembers(GenericBeanDefinition beanDefinition) {
    }

    public void inject(Object target, @Nullable String beanName, @Nullable PropertyValues pvs) throws Throwable {
        Collection<InjectedElement> elementsToIterate = this.injectedElements;
        if (!elementsToIterate.isEmpty()) {
            for (InjectedElement element : elementsToIterate) {
                //target：要执行注入的bean对象
                element.inject(target, beanName, pvs);
            }
        }
    }


    public static InjectionMetadata forElements(Collection<InjectedElement> elements, Class<?> clazz) {
        return (elements.isEmpty() ? InjectionMetadata.EMPTY : new InjectionMetadata(clazz, elements));
    }


}
