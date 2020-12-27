package ink.zfei.summer.beans.factory.annotation;

import ink.zfei.summer.beans.PropertyValues;
import ink.zfei.summer.beans.TypeConverter;
import ink.zfei.summer.beans.factory.BeanCreationException;
import ink.zfei.summer.beans.factory.BeanFactory;
import ink.zfei.summer.beans.factory.BeanFactoryAware;
import ink.zfei.summer.beans.factory.config.ConfigurableListableBeanFactory;
import ink.zfei.summer.beans.factory.config.DependencyDescriptor;
import ink.zfei.summer.beans.factory.config.SmartInstantiationAwareBeanPostProcessor;
import ink.zfei.summer.beans.factory.support.GenericBeanDefinition;
import ink.zfei.summer.beans.factory.support.MergedBeanDefinitionPostProcessor;
import ink.zfei.summer.core.Ordered;
import ink.zfei.summer.core.PriorityOrdered;
import ink.zfei.summer.core.annotation.AnnotationAttributes;
import ink.zfei.summer.core.annotation.AnnotationUtils;
import ink.zfei.summer.core.annotation.MergedAnnotation;
import ink.zfei.summer.core.annotation.MergedAnnotations;
import ink.zfei.summer.lang.Nullable;
import ink.zfei.summer.util.Assert;
import ink.zfei.summer.util.ClassUtils;
import ink.zfei.summer.util.ReflectionUtils;
import ink.zfei.summer.util.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.beans.PropertyDescriptor;
import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class AutowiredAnnotationBeanPostProcessor implements SmartInstantiationAwareBeanPostProcessor, MergedBeanDefinitionPostProcessor, PriorityOrdered, BeanFactoryAware {

    protected final Log logger = LogFactory.getLog(getClass());

    @Nullable
    private ConfigurableListableBeanFactory beanFactory;

    private final Set<Class<? extends Annotation>> autowiredAnnotationTypes = new LinkedHashSet<>(4);
    private String requiredParameterName = "required";
    private boolean requiredParameterValue = true;
    private int order = Ordered.LOWEST_PRECEDENCE - 2;

    public AutowiredAnnotationBeanPostProcessor() {
        this.autowiredAnnotationTypes.add(Autowired.class);
        this.autowiredAnnotationTypes.add(Value.class);
    }

    @Override
    public void postProcessMergedBeanDefinition(GenericBeanDefinition beanDefinition, Class<?> beanType, String beanName) {
//        InjectionMetadata metadata = findAutowiringMetadata(beanName, beanType, null);
//        metadata.checkConfigMembers(beanDefinition);
    }

    @Override
    public int getOrder() {
        return this.order;
    }

    @Override
    public Constructor<?>[] determineCandidateConstructors(Class<?> beanClass, final String beanName) {
        Constructor<?>[] rawCandidates = beanClass.getDeclaredConstructors();

        Constructor<?> requiredConstructor = null;
        Constructor<?> defaultConstructor = null;
        List<Constructor<?>> candidates = new ArrayList<>(rawCandidates.length);

        for (Constructor<?> candidate : rawCandidates) {
            MergedAnnotation<?> ann = findAutowiredAnnotation(candidate);
            if (ann == null) {
                //父类有没有@Autowired注解
                Class<?> userClass = ClassUtils.getUserClass(beanClass);
                if (userClass != beanClass) {
                    try {
                        Constructor<?> superCtor =
                                userClass.getDeclaredConstructor(candidate.getParameterTypes());
                        ann = findAutowiredAnnotation(superCtor);
                    } catch (NoSuchMethodException ex) {
                        // Simply proceed, no equivalent superclass constructor found...
                    }
                }
            }
            if (ann != null) {
                //@Autowired的 required =true只能有一个（默认true）
                if (requiredConstructor != null) {
                    throw new BeanCreationException(beanName,
                            "Invalid autowire-marked constructor: " + candidate +
                                    ". Found constructor with 'required' Autowired annotation already: " +
                                    requiredConstructor);
                }
                boolean required = determineRequiredStatus(ann);
                if (required) {
                    //再次判断
                    if (!candidates.isEmpty()) {
                        throw new BeanCreationException(beanName,
                                "Invalid autowire-marked constructors: " + candidate +
                                        ". Found constructor with 'required' Autowired annotation: " +
                                        candidate);
                    }
                    requiredConstructor = candidate;
                }
                candidates.add(candidate);
            } else if (candidate.getParameterCount() == 0) {
                defaultConstructor = candidate;
            }
        }

        Constructor<?>[] candidateConstructors;

        if (!candidates.isEmpty()) {
            // 没有指定required=true，使用默认无参构造器
            if (requiredConstructor == null && defaultConstructor != null) {
                candidates.add(defaultConstructor);
            }
            candidateConstructors = candidates.toArray(new Constructor<?>[0]);
        } else if (rawCandidates.length == 1 && rawCandidates[0].getParameterCount() > 0) {
            //只有一个没得选
            candidateConstructors = new Constructor<?>[]{rawCandidates[0]};
        } else {
            candidateConstructors = new Constructor<?>[0];
        }

        return (candidateConstructors.length > 0 ? candidateConstructors : null);
    }

    /**
     * AccessibleObject是field、method、class等带注解元素的基类，提供了Accessible能力
     *
     * @param ao
     * @return
     */
    private MergedAnnotation<?> findAutowiredAnnotation(AccessibleObject ao) {
        //获取注解元素的所有注解信息（封装成方便获取注解属性的MergedAnnotations对象）
        MergedAnnotations annotations = MergedAnnotations.from(ao);
        for (Class<? extends Annotation> type : this.autowiredAnnotationTypes) {
            MergedAnnotation<?> annotation = annotations.get(type);
            if (annotation.isPresent()) {
                return annotation;
            }
        }
        return null;
    }

    protected boolean determineRequiredStatus(MergedAnnotation<?> ann) {
        // The following (AnnotationAttributes) cast is required on JDK 9+.
        return determineRequiredStatus((AnnotationAttributes)
                ann.asMap(mergedAnnotation -> new AnnotationAttributes(mergedAnnotation.getType())));
    }

    /**
     * @Autowired注解的属性key是否带了required，且value=true
     */
    protected boolean determineRequiredStatus(AnnotationAttributes ann) {
        return (!ann.containsKey(this.requiredParameterName) ||
                this.requiredParameterValue == ann.getBoolean(this.requiredParameterName));
    }

    private InjectionMetadata findAutowiringMetadata(String beanName, Class<?> clazz, @Nullable PropertyValues pvs) {
        //忽略缓存
        return buildAutowiringMetadata(clazz);
    }

    /**
     * 遍历clazz所有field，获取所有注解，匹配@Autowired，如匹配封装成InjectedElement集合
     * 最后分装成InjectionMetadata对象返回
     *
     * @param clazz
     * @return
     */
    private InjectionMetadata buildAutowiringMetadata(final Class<?> clazz) {
        if (!AnnotationUtils.isCandidateClass(clazz, this.autowiredAnnotationTypes)) {
            return InjectionMetadata.EMPTY;
        }

        List<InjectionMetadata.InjectedElement> elements = new ArrayList<>();
        Class<?> targetClass = clazz;

        do {
            final List<InjectionMetadata.InjectedElement> currElements = new ArrayList<>();

            /**
             * 循环class所有field
             */
            ReflectionUtils.doWithLocalFields(targetClass, field -> {
                //MergedAnnotation是field对应Autowired注解的封装对象，可以方便获取注解属性，比如下面的required
                MergedAnnotation<?> ann = findAutowiredAnnotation(field);
                if (ann != null) {
                    if (Modifier.isStatic(field.getModifiers())) {
                        if (logger.isInfoEnabled()) {
                            logger.info("Autowired annotation is not supported on static fields: " + field);
                        }
                        return;
                    }
                    //获取Autowired注解required属性值
                    boolean required = determineRequiredStatus(ann);
                    currElements.add(new AutowiredFieldElement(field, required));
                }
            });


            elements.addAll(0, currElements);
            targetClass = targetClass.getSuperclass();
        }
        while (targetClass != null && targetClass != Object.class);

        return InjectionMetadata.forElements(elements, clazz);
    }


    private class AutowiredFieldElement extends InjectionMetadata.InjectedElement {

        private final boolean required;

        private volatile boolean cached = false;

        /**
         * 缓存有两种，注入的对象或依赖封装对象
         */
        @Nullable
        private volatile Object cachedFieldValue;

        public AutowiredFieldElement(Field field, boolean required) {
            super(field, null);
            this.required = required;
        }

        @Override
        protected void inject(Object bean, @Nullable String beanName, @Nullable PropertyValues pvs) throws Throwable {
            Field field = (Field) this.member;
            Object value;

            if (this.cached) {
                //如果有缓存（已经解析过），根据缓存使用beanName映射注入对象值
                value = resolvedCachedArgument(beanName, this.cachedFieldValue);
            } else {
                DependencyDescriptor desc = new DependencyDescriptor(field, this.required);
                desc.setContainingClass(bean.getClass());
                Set<String> autowiredBeanNames = new LinkedHashSet<>(1);
                Assert.state(beanFactory != null, "No BeanFactory available");
                TypeConverter typeConverter = beanFactory.getTypeConverter();
                value = beanFactory.resolveDependency(desc, beanName, autowiredBeanNames, typeConverter);

                synchronized (this) {
                    if (value != null || this.required) {
                        this.cachedFieldValue = desc;
                        // registerDependentBeans(beanName, autowiredBeanNames); 忽略注册依赖关系
                        if (autowiredBeanNames.size() == 1) {
                            String autowiredBeanName = autowiredBeanNames.iterator().next();
                            if (beanFactory.containsBean(autowiredBeanName) &&
                                    beanFactory.isTypeMatch(autowiredBeanName, field.getType())) {
                                this.cachedFieldValue = new ShortcutDependencyDescriptor(
                                        desc, autowiredBeanName, field.getType());
                            }
                        }
                    } else {
                        this.cachedFieldValue = null;
                    }
                }
            }
            if (value != null) {
                ReflectionUtils.makeAccessible(field);
                field.set(bean, value);
            }
        }
    }

    @Nullable
    private Object resolvedCachedArgument(@Nullable String beanName, @Nullable Object cachedArgument) {
        if (cachedArgument instanceof DependencyDescriptor) {
            DependencyDescriptor descriptor = (DependencyDescriptor) cachedArgument;
            Assert.state(this.beanFactory != null, "No BeanFactory available");
            return this.beanFactory.resolveDependency(descriptor, beanName, null, null);
        } else {
            return cachedArgument;
        }
    }

    @Override
    public void setBeanFactory(BeanFactory beanFactory) {
        if (!(beanFactory instanceof ConfigurableListableBeanFactory)) {
            throw new IllegalArgumentException(
                    "AutowiredAnnotationBeanPostProcessor requires a ConfigurableListableBeanFactory: " + beanFactory);
        }
        this.beanFactory = (ConfigurableListableBeanFactory) beanFactory;
    }


    @SuppressWarnings("serial")
    private static class ShortcutDependencyDescriptor extends DependencyDescriptor {

        private final String shortcut;

        private final Class<?> requiredType;

        public ShortcutDependencyDescriptor(DependencyDescriptor original, String shortcut, Class<?> requiredType) {
            super(original);
            this.shortcut = shortcut;
            this.requiredType = requiredType;
        }

        @Override
        public Object resolveShortcut(BeanFactory beanFactory) {
            return beanFactory.getBean(this.shortcut, this.requiredType);
        }
    }

    @Override
    public PropertyValues postProcessProperties(PropertyValues pvs, Object bean, String beanName) {
        InjectionMetadata metadata = findAutowiringMetadata(beanName, bean.getClass(), pvs);
        try {
            metadata.inject(bean, beanName, pvs);
        } catch (BeanCreationException ex) {
            throw ex;
        } catch (Throwable ex) {
            throw new BeanCreationException(beanName, "Injection of autowired dependencies failed", ex);
        }
        return pvs;
    }
}
