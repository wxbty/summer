package ink.zfei.summer.context.annotation;

import ink.zfei.summer.beans.InstantiationAwareBeanPostProcessor;
import ink.zfei.summer.beans.PropertyValues;
import ink.zfei.summer.beans.factory.BeanCreationException;
import ink.zfei.summer.beans.factory.BeanFactory;
import ink.zfei.summer.beans.factory.BeanFactoryAware;
import ink.zfei.summer.beans.factory.NoSuchBeanDefinitionException;
import ink.zfei.summer.beans.factory.annotation.InjectionMetadata;
import ink.zfei.summer.beans.factory.config.AutowireCapableBeanFactory;
import ink.zfei.summer.beans.factory.config.ConfigurableBeanFactory;
import ink.zfei.summer.beans.factory.config.DependencyDescriptor;
import ink.zfei.summer.beans.factory.support.GenericBeanDefinition;
import ink.zfei.summer.beans.factory.support.MergedBeanDefinitionPostProcessor;
import ink.zfei.summer.core.MethodParameter;
import ink.zfei.summer.core.Ordered;
import ink.zfei.summer.core.PriorityOrdered;
import ink.zfei.summer.core.annotation.AnnotationUtils;
import ink.zfei.summer.lang.Nullable;
import ink.zfei.summer.util.Assert;
import ink.zfei.summer.util.ReflectionUtils;
import ink.zfei.summer.util.StringUtils;

import javax.annotation.Resource;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.*;

/**
 * 通用注解后置处理器，主要是@Resource解析与注入
 * 也包含 @PostConstruct、@PreDestroy等bean生命周期注解(summer没有)
 */
public class CommonAnnotationBeanPostProcessor implements MergedBeanDefinitionPostProcessor, InstantiationAwareBeanPostProcessor, PriorityOrdered, BeanFactoryAware {

    private int order = Ordered.LOWEST_PRECEDENCE;
    private static Set<Class<? extends Annotation>> resourceAnnotationTypes = new LinkedHashSet<>(4);
    @Nullable
    private transient BeanFactory resourceFactory;
    @Nullable
    private transient BeanFactory beanFactory;

    /**
     * 当@Resource没有显式提供名字的时候，如果根据默认名字找不到对应的Spring管理对象，注入机制会回滚至类型匹配（type-match）。
     * 如果刚好只有一个Spring管理对象符合该依赖的类型，那么它会被注入
     * 设置false禁用这一特性
     */
    private boolean fallbackToDefaultTypeMatch = true;

    static {
        resourceAnnotationTypes.add(Resource.class);
    }

    @Override
    public int getOrder() {
        return this.order;
    }

    @Override
    public void postProcessMergedBeanDefinition(GenericBeanDefinition beanDefinition, Class<?> beanType, String beanName) {

        // 解析bean属性上@Resource的注解，将其封装为metadata对象并缓存
        InjectionMetadata metadata = findResourceMetadata(beanName, beanType, null);
        // 对注入的metadata对象进行检查，没有注册的bd需要进行注册。最后添加到metadata对象的checkedElements集合中。
        System.out.println(111);
    }

    @Override
    public PropertyValues postProcessProperties(PropertyValues pvs, Object bean, String beanName) {
        InjectionMetadata metadata = findResourceMetadata(beanName, bean.getClass(), pvs);
        try {
            metadata.inject(bean, beanName, pvs);
        } catch (Throwable throwable) {
            throw new BeanCreationException(beanName, "Injection of resource dependencies failed");
        }
        return pvs;
    }

    private InjectionMetadata findResourceMetadata(String beanName, final Class<?> clazz, @Nullable PropertyValues pvs) {
        //省略缓存
        return buildResourceMetadata(clazz);
    }


    private InjectionMetadata buildResourceMetadata(final Class<?> clazz) {
        //如果是jdk自带类，直接返回空对象
        if (!AnnotationUtils.isCandidateClass(clazz, resourceAnnotationTypes)) {
            return InjectionMetadata.EMPTY;
        }

        List<InjectionMetadata.InjectedElement> elements = new ArrayList<>();
        Class<?> targetClass = clazz;

        do {
            final List<InjectionMetadata.InjectedElement> currElements = new ArrayList<>();

            //遍历targetClass中所有属性，检查是否带指定注解
            ReflectionUtils.doWithLocalFields(targetClass, field -> {
                if (field.isAnnotationPresent(Resource.class)) {
                    if (Modifier.isStatic(field.getModifiers())) {
                        throw new IllegalStateException("@Resource annotation is not supported on static fields");
                    }
                    currElements.add(new ResourceElement(field, field, null));
                }
            });
            elements.addAll(0, currElements);
            targetClass = targetClass.getSuperclass();
        }
        while (targetClass != null && targetClass != Object.class);

        return InjectionMetadata.forElements(elements, clazz);
    }

    private class ResourceElement extends InjectionMetadata.InjectedElement {

        //@Resource(name="xxx")
        private String name;
        protected boolean isDefaultName = false;
        protected Class<?> lookupType = Object.class;

        //member是Field或者Method对象，ae是可带注解的元素
        public ResourceElement(Member member, AnnotatedElement ae, @Nullable PropertyDescriptor pd) {
            super(member, pd);
            Resource resource = ae.getAnnotation(Resource.class);
            String resourceName = resource.name();
            this.isDefaultName = !StringUtils.hasLength(resourceName);
            if (this.isDefaultName) {
                //默认名字，如果是属性，获取属性名字，如果是set方法，截取set后小写首字母
                resourceName = this.member.getName();
                if (this.member instanceof Method && resourceName.startsWith("set") && resourceName.length() > 3) {
                    resourceName = Introspector.decapitalize(resourceName.substring(3));
                }
            }

            this.name = resourceName;

        }

        public final Class<?> getLookupType() {
            return this.lookupType;
        }

        @Override
        protected Object getResourceToInject(Object target, @Nullable String requestingBeanName) {
            //省略lazy加载
            return getResource(this, requestingBeanName);
        }

        public final DependencyDescriptor getDependencyDescriptor() {
            if (this.isField) {
                return new LookupDependencyDescriptor((Field) this.member, this.lookupType);
            } else {
                return new LookupDependencyDescriptor((Method) this.member, this.lookupType);
            }
        }
    }

    protected Object getResource(ResourceElement element, @Nullable String requestingBeanName) {
        //省略jndi注入
        if (this.resourceFactory == null) {
            throw new NoSuchBeanDefinitionException(element.lookupType,
                    "No resource factory configured - specify the 'resourceFactory' property");
        }
        return autowireResource(this.resourceFactory, element, requestingBeanName);
    }


    @Override
    public void setBeanFactory(BeanFactory beanFactory) {
        Assert.notNull(beanFactory, "BeanFactory must not be null");
        this.beanFactory = beanFactory;
        if (this.resourceFactory == null) {
            this.resourceFactory = beanFactory;
        }
    }


    protected Object autowireResource(BeanFactory factory, ResourceElement element, @Nullable String requestingBeanName)
            throws NoSuchBeanDefinitionException {

        Object resource;
        Set<String> autowiredBeanNames;
        String name = element.name;

        if (factory instanceof AutowireCapableBeanFactory) {
            AutowireCapableBeanFactory beanFactory = (AutowireCapableBeanFactory) factory;
            DependencyDescriptor descriptor = element.getDependencyDescriptor();
            if (this.fallbackToDefaultTypeMatch && element.isDefaultName && !factory.containsBean(name)) {
                //根据name可以找不到bd，按类型查找
                autowiredBeanNames = new LinkedHashSet<>();
                resource = beanFactory.resolveDependency(descriptor, requestingBeanName, autowiredBeanNames, null);
                if (resource == null) {
                    throw new NoSuchBeanDefinitionException(element.getLookupType(), "No resolvable resource object");
                }
            } else {
                //根据name可以找到bd
                resource = beanFactory.resolveBeanByName(name, descriptor);
                autowiredBeanNames = Collections.singleton(name);
            }
        } else {
            resource = factory.getBean(name, element.lookupType);
            autowiredBeanNames = Collections.singleton(name);
        }

        //注册依赖关系，要销毁一起销毁
        if (factory instanceof ConfigurableBeanFactory) {
            ConfigurableBeanFactory beanFactory = (ConfigurableBeanFactory) factory;
            for (String autowiredBeanName : autowiredBeanNames) {
                if (requestingBeanName != null && beanFactory.containsBean(autowiredBeanName)) {
                    beanFactory.registerDependentBean(autowiredBeanName, requestingBeanName);
                }
            }
        }

        return resource;
    }

    private static class LookupDependencyDescriptor extends DependencyDescriptor {

        private final Class<?> lookupType;

        public LookupDependencyDescriptor(Field field, Class<?> lookupType) {
            super(field, true);
            this.lookupType = lookupType;
        }

        public LookupDependencyDescriptor(Method method, Class<?> lookupType) {
            super(new MethodParameter(method, 0), true);
            this.lookupType = lookupType;
        }

    }
}
