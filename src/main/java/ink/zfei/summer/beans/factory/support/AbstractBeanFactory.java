package ink.zfei.summer.beans.factory.support;

import ink.zfei.summer.beans.BeanPostProcessor;
import ink.zfei.summer.beans.InstantiationAwareBeanPostProcessor;
import ink.zfei.summer.beans.factory.*;
import ink.zfei.summer.beans.factory.config.BeanDefinition;
import ink.zfei.summer.beans.factory.config.BeanDefinitionHolder;
import ink.zfei.summer.beans.factory.config.ConfigurableBeanFactory;
import ink.zfei.summer.context.support.FactoryBeanRegistrySupport;
import ink.zfei.summer.core.AttributeAccessor;
import ink.zfei.summer.core.ResolvableType;
import ink.zfei.summer.lang.Nullable;
import ink.zfei.summer.util.Assert;
import ink.zfei.summer.util.ClassUtils;
import ink.zfei.summer.util.ObjectUtils;
import ink.zfei.summer.util.StringUtils;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public abstract class AbstractBeanFactory extends FactoryBeanRegistrySupport implements ConfigurableBeanFactory {

    private final Map<String, GenericBeanDefinition> mergedBeanDefinitions = new ConcurrentHashMap<>(256);
    private BeanFactory parentBeanFactory;
    private ClassLoader beanClassLoader = ClassUtils.getDefaultClassLoader();

    /**
     * Indicates whether any InstantiationAwareBeanPostProcessors have been registered.
     */
    private volatile boolean hasInstantiationAwareBeanPostProcessors;

    /**
     * Indicates whether any DestructionAwareBeanPostProcessors have been registered.
     */
    private volatile boolean hasDestructionAwareBeanPostProcessors;


    private final List<BeanPostProcessor> beanPostProcessors = new CopyOnWriteArrayList<>();

    @Override
    public Object getBean(String name) {
        return doGetBean(name, null, null, false);
    }

    public <T> T getBean(String name, @Nullable Class<T> requiredType, @Nullable Object... args) {

        return doGetBean(name, requiredType, args, false);
    }


    public void setParentBeanFactory(@Nullable BeanFactory parentBeanFactory) {
        if (this.parentBeanFactory != null && this.parentBeanFactory != parentBeanFactory) {
            throw new IllegalStateException("Already associated with parent BeanFactory: " + this.parentBeanFactory);
        }
        this.parentBeanFactory = parentBeanFactory;
    }

    @SuppressWarnings("unchecked")
    protected <T> T doGetBean(final String name, @Nullable final Class<T> requiredType,
                              @Nullable final Object[] args, boolean typeCheckOnly) {

        final String beanName = transformedBeanName(name);
        Object bean = null;

        // Eagerly check singleton cache for manually registered singletons.
        Object sharedInstance = getSingleton(beanName);
        if (sharedInstance != null && args == null) {
            bean = getObjectForBeanInstance(sharedInstance, name, beanName, null);
        } else {
            // Fail if we're already creating this bean instance:
            // We're assumably within a circular reference.
            if (isPrototypeCurrentlyInCreation(beanName)) {
                throw new BeanCurrentlyInCreationException(beanName);
            }

            GenericBeanDefinition mbd = (GenericBeanDefinition) getBeanDefinition(beanName);
            if (mbd.isSingleton()) {
                sharedInstance = getSingleton(beanName, () -> createBean(beanName, mbd, args));
                bean = getObjectForBeanInstance(sharedInstance, name, beanName, mbd);
            }
        }
        return (T) bean;

    }

    protected Object getObjectForBeanInstance(
            Object beanInstance, String name, String beanName, @Nullable GenericBeanDefinition mbd) {

        // Don't let calling code try to dereference the factory if the bean isn't a factory.
        //如果是第一次获取FactoryBean，直接返回bean实例
        if (BeanFactoryUtils.isFactoryDereference(name)) {
            if (!(beanInstance instanceof FactoryBean)) {
                throw new RuntimeException("BeanIsNotAFactoryException " + beanName);
            }
            if (mbd != null) {
                mbd.isFactoryBean = true;
            }
            return beanInstance;
        }

        // Now we have the bean instance, which may be a normal bean or a FactoryBean.
        // If it's a FactoryBean, we use it to create a bean instance, unless the
        // caller actually wants a reference to the factory.
        if (!(beanInstance instanceof FactoryBean)) {
            return beanInstance;
        }

        //如果是第二次获取最终bean，根据fb生成bean实例
        Object object = null;
        if (mbd != null) {
            mbd.isFactoryBean = true;
        }
        // Return bean instance from factory.
        FactoryBean<?> factory = (FactoryBean<?>) beanInstance;
        // Caches object obtained from FactoryBean if it is a singleton.
        if (mbd == null && containsBeanDefinition(beanName)) {
            mbd = (GenericBeanDefinition) getBeanDefinition(beanName);
        }
//      todo 判断是否需要后置处理  boolean synthetic = (mbd != null && mbd.isSynthetic());
        object = getObjectFromFactoryBean(factory, beanName);
        return object;
    }

    protected abstract boolean containsBeanDefinition(String beanName);

    protected abstract BeanDefinition getBeanDefinition(String beanName);

    /*
     * 判断我们传递的name是不是以“&”开头的，要是的需要去掉再返回。需要注意的是，我们传递的name可能是bean的别名，
     * 也可能是BeanFactory，所以transformedBeanName的操作的目的就是取到真正的bean的名字，以便开始后续的流程。
     * */
    protected String transformedBeanName(String name) {
        return name;
    }

    private boolean isPrototypeCurrentlyInCreation(String beanName) {
        //todo 判断bean是否创建中
        return false;
    }

    protected abstract Object createBean(String beanName, GenericBeanDefinition mbd, @Nullable Object[] args);

    protected GenericBeanDefinition getMergedLocalBeanDefinition(String beanName) {
        // Quick check on the concurrent map first, with minimal locking.
        GenericBeanDefinition mbd = this.mergedBeanDefinitions.get(beanName);
        if (mbd != null) {
            return mbd;
        }
        return getMergedBeanDefinition(beanName, getBeanDefinition(beanName));
    }

    protected GenericBeanDefinition getMergedBeanDefinition(String beanName, BeanDefinition bd) {

        return getMergedBeanDefinition(beanName, bd, null);
    }

    protected GenericBeanDefinition getMergedBeanDefinition(
            String beanName, BeanDefinition bd, @Nullable BeanDefinition containingBd) {

        synchronized (this.mergedBeanDefinitions) {
            GenericBeanDefinition mbd = null;
            GenericBeanDefinition previous = null;

            // Check with full lock now in order to enforce the same merged instance.
            if (containingBd == null) {
                mbd = this.mergedBeanDefinitions.get(beanName);
            }

            if (mbd == null) {
                previous = mbd;
                if (bd.getParentName() == null) {
                    // Use copy of given root bean definition.
                    if (bd instanceof GenericBeanDefinition) {
                        mbd = ((GenericBeanDefinition) bd).cloneBeanDefinition();
                    } else {
                        mbd = new GenericBeanDefinition(bd);
                    }
                } else {
                    // Child bean definition: needs to be merged with parent.
                    BeanDefinition pbd;
                    try {
                        String parentBeanName = transformedBeanName(bd.getParentName());
                        if (!beanName.equals(parentBeanName)) {
                            pbd = getMergedBeanDefinition(parentBeanName);
                        } else {
                            BeanFactory parent = getParentBeanFactory();
                            if (parent instanceof ConfigurableBeanFactory) {
                                pbd = ((ConfigurableBeanFactory) parent).getMergedBeanDefinition(parentBeanName);
                            } else {
                                throw new NoSuchBeanDefinitionException(parentBeanName);
                            }
                        }
                    } catch (NoSuchBeanDefinitionException ex) {
                        throw new BeanDefinitionStoreException("bd.getResourceDescription()", beanName,
                                "Could not resolve parent bean definition '" + bd.getParentName() + "'", ex);
                    }
                    // Deep copy with overridden values.
                    mbd = new GenericBeanDefinition(pbd);
//                    mbd.overrideFrom(bd);
                }

                // Set default singleton scope, if not configured before.
                if (!StringUtils.hasLength(mbd.getScope())) {
                    mbd.setScope(SCOPE_SINGLETON);
                }

                // A bean contained in a non-singleton bean cannot be a singleton itself.
                // Let's correct this on the fly here, since this might be the result of
                // parent-child merging for the outer bean, in which case the original inner bean
                // definition will not have inherited the merged outer bean's singleton status.
                if (containingBd != null && !containingBd.isSingleton() && mbd.isSingleton()) {
                    mbd.setScope(containingBd.getScope());
                }

                // Cache the merged bean definition for the time being
                // (it might still get re-merged later on in order to pick up metadata changes)
                if (containingBd == null) {
                    this.mergedBeanDefinitions.put(beanName, mbd);
                }
            }
            if (previous != null) {
                copyRelevantMergedBeanDefinitionCaches(previous, mbd);
            }
            return mbd;
        }
    }

    @Override
    public BeanDefinition getMergedBeanDefinition(String name) {
        String beanName = transformedBeanName(name);
        // Efficiently check whether bean definition exists in this factory.
        if (!containsBeanDefinition(beanName) && getParentBeanFactory() instanceof ConfigurableBeanFactory) {
            return ((ConfigurableBeanFactory) getParentBeanFactory()).getMergedBeanDefinition(beanName);
        }
        // Resolve merged bean definition locally.
        return getMergedLocalBeanDefinition(beanName);
    }

    private void copyRelevantMergedBeanDefinitionCaches(GenericBeanDefinition previous, GenericBeanDefinition mbd) {
        if (ObjectUtils.nullSafeEquals(mbd.getBeanClassName(), previous.getBeanClassName()) &&
                ObjectUtils.nullSafeEquals(mbd.getFactoryBeanName(), previous.getFactoryBeanName()) &&
                ObjectUtils.nullSafeEquals(mbd.getFactoryMethodName(), previous.getFactoryMethodName())) {
            ResolvableType targetType = mbd.targetType;
            ResolvableType previousTargetType = previous.targetType;
            if (targetType == null || targetType.equals(previousTargetType)) {
                mbd.targetType = previousTargetType;
                mbd.isFactoryBean = previous.isFactoryBean;
                mbd.resolvedTargetType = previous.resolvedTargetType;
                mbd.factoryMethodReturnType = previous.factoryMethodReturnType;
                mbd.factoryMethodToIntrospect = previous.factoryMethodToIntrospect;
            }
        }
    }

    /**
     * todo 第三个参数不知道干什么用
     */
    protected Class<?> resolveBeanClass(GenericBeanDefinition mbd, String beanName, final Class<?>... typesToMatch) {

        if (mbd.hasBeanClass()) {
            return mbd.getBeanClass();
        } else {
            try {
                return doResolveBeanClass(mbd, typesToMatch);
            } catch (ClassNotFoundException ex) {
                throw new RuntimeException(ex);
            }
        }

    }

    private Class<?> doResolveBeanClass(GenericBeanDefinition mbd, Class<?>... typesToMatch) throws ClassNotFoundException {

        ClassLoader beanClassLoader = getBeanClassLoader();
        boolean freshResolve = false;

        if (!ObjectUtils.isEmpty(typesToMatch)) {
            //不知道干什么用
        }

        String className = mbd.getBeanClassName();
        if (className != null) {
            //className上可能有spel表达式，先去解析，如#{aa}
            Object evaluated = evaluateBeanDefinitionString(className, mbd);
            // A dynamically resolved expression, supported as of 4.2...
            if (evaluated instanceof Class) {
                return (Class<?>) evaluated;
            } else if (evaluated instanceof String) {
                className = (String) evaluated;
                freshResolve = true;
            } else {
                throw new IllegalStateException("Invalid class name expression result: " + evaluated);
            }
            if (freshResolve) {
                //如果有解析出新值，用新值获取class
                return ClassUtils.forName(className, beanClassLoader);
            }
        }
        return mbd.resolveBeanClass(beanClassLoader);

    }

    protected Object evaluateBeanDefinitionString(String className, GenericBeanDefinition mbd) {

        return className;
    }

    @Override
    public boolean isTypeMatch(String name, Class<?> typeToMatch) {
        return isTypeMatch(name, ResolvableType.forRawClass(typeToMatch));
    }

    @Override
    public boolean isTypeMatch(String name, ResolvableType typeToMatch) throws NoSuchBeanDefinitionException {
        return isTypeMatch(name, typeToMatch, true);
    }

    protected boolean isTypeMatch(String name, ResolvableType typeToMatch, boolean allowFactoryBeanInit) {

        String beanName = transformedBeanName(name);
        boolean isFactoryDereference = BeanFactoryUtils.isFactoryDereference(name);

        // Check manually registered singletons.
        Object beanInstance = getSingleton(beanName, false);
        if (beanInstance != null && beanInstance.getClass() != NullBean.class) {
            if (beanInstance instanceof FactoryBean) {
                if (!isFactoryDereference) {
                    Class<?> type = getTypeForFactoryBean((FactoryBean<?>) beanInstance);
                    return (type != null && typeToMatch.isAssignableFrom(type));
                } else {
                    return typeToMatch.isInstance(beanInstance);
                }
            } else if (!isFactoryDereference) {
                if (typeToMatch.isInstance(beanInstance)) {
                    // Direct match for exposed instance?
                    return true;
                } else if (typeToMatch.hasGenerics() && containsBeanDefinition(beanName)) {
                    // Generics potentially only match on the target class, not on the proxy...
                    GenericBeanDefinition mbd = getMergedLocalBeanDefinition(beanName);
                    Class<?> targetType = mbd.getTargetType();
                    if (targetType != null && targetType != ClassUtils.getUserClass(beanInstance)) {
                        // Check raw class match as well, making sure it's exposed on the proxy.
                        Class<?> classToMatch = typeToMatch.resolve();
                        if (classToMatch != null && !classToMatch.isInstance(beanInstance)) {
                            return false;
                        }
                        if (typeToMatch.isAssignableFrom(targetType)) {
                            return true;
                        }
                    }
                    ResolvableType resolvableType = mbd.targetType;
                    if (resolvableType == null) {
                        resolvableType = mbd.factoryMethodReturnType;
                    }
                    return (resolvableType != null && typeToMatch.isAssignableFrom(resolvableType));
                }
            }
            return false;
        } else if (containsSingleton(beanName) && !containsBeanDefinition(beanName)) {
            // null instance registered
            return false;
        }


        // Retrieve corresponding bean definition.
        GenericBeanDefinition mbd = getMergedLocalBeanDefinition(beanName);
        BeanDefinitionHolder dbd = mbd.getDecoratedDefinition();

        // Setup the types that we want to match against
        Class<?> classToMatch = typeToMatch.resolve();
        if (classToMatch == null) {
            classToMatch = FactoryBean.class;
        }
        Class<?>[] typesToMatch = (FactoryBean.class == classToMatch ?
                new Class<?>[]{classToMatch} : new Class<?>[]{FactoryBean.class, classToMatch});


        // Attempt to predict the bean type
        Class<?> predictedType = null;

        // We're looking for a regular reference but we're a factory bean that has
        // a decorated bean definition. The target bean should be the same type
        // as FactoryBean would ultimately return.
        if (!isFactoryDereference && dbd != null && isFactoryBean(beanName, mbd)) {
            // We should only attempt if the user explicitly set lazy-init to true
            // and we know the merged bean definition is for a factory bean.
            if (!mbd.isLazyInit() || allowFactoryBeanInit) {
                GenericBeanDefinition tbd = getMergedBeanDefinition(dbd.getBeanName(), dbd.getBeanDefinition(), mbd);
                Class<?> targetType = predictBeanType(dbd.getBeanName(), tbd, typesToMatch);
                if (targetType != null && !FactoryBean.class.isAssignableFrom(targetType)) {
                    predictedType = targetType;
                }
            }
        }

        // If we couldn't use the target type, try regular prediction.
        if (predictedType == null) {
            predictedType = predictBeanType(beanName, mbd, typesToMatch);
            if (predictedType == null) {
                return false;
            }
        }

        // Attempt to get the actual ResolvableType for the bean.
        ResolvableType beanType = null;

        // If it's a FactoryBean, we want to look at what it creates, not the factory class.
        if (FactoryBean.class.isAssignableFrom(predictedType)) {
            if (beanInstance == null && !isFactoryDereference) {
                beanType = getTypeForFactoryBean(beanName, mbd, allowFactoryBeanInit);
                predictedType = beanType.resolve();
                if (predictedType == null) {
                    return false;
                }
            }
        } else if (isFactoryDereference) {
            // Special case: A SmartInstantiationAwareBeanPostProcessor returned a non-FactoryBean
            // type but we nevertheless are being asked to dereference a FactoryBean...
            // Let's check the original bean class and proceed with it if it is a FactoryBean.
            predictedType = predictBeanType(beanName, mbd, FactoryBean.class);
            if (predictedType == null || !FactoryBean.class.isAssignableFrom(predictedType)) {
                return false;
            }
        }

        // We don't have an exact type but if bean definition target type or the factory
        // method return type matches the predicted type then we can use that.
        if (beanType == null) {
            ResolvableType definedType = mbd.targetType;
            if (definedType == null) {
                definedType = mbd.factoryMethodReturnType;
            }
            if (definedType != null && definedType.resolve() == predictedType) {
                beanType = definedType;
            }
        }

        // If we have a bean type use it so that generics are considered
        if (beanType != null) {
            return typeToMatch.isAssignableFrom(beanType);
        }

        // If we don't have a bean type, fallback to the predicted type
        return typeToMatch.isAssignableFrom(predictedType);
    }


    @Override
    public boolean isFactoryBean(String name) throws NoSuchBeanDefinitionException {
        String beanName = transformedBeanName(name);
        Object beanInstance = getSingleton(beanName, false);
        if (beanInstance != null) {
            return (beanInstance instanceof FactoryBean);
        }
        // No singleton instance found -> check bean definition.
        if (!containsBeanDefinition(beanName) && getParentBeanFactory() instanceof ConfigurableBeanFactory) {
            // No bean definition found in this factory -> delegate to parent.
            return ((ConfigurableBeanFactory) getParentBeanFactory()).isFactoryBean(name);
        }
        return isFactoryBean(beanName, getMergedLocalBeanDefinition(beanName));


    }

    protected boolean isFactoryBean(String beanName, GenericBeanDefinition mbd) {
        Boolean result = mbd.isFactoryBean;
        if (result == null) {
            Class<?> beanType = predictBeanType(beanName, mbd, FactoryBean.class);
            result = (beanType != null && FactoryBean.class.isAssignableFrom(beanType));
            mbd.isFactoryBean = result;
        }
        return result;
    }

    protected Class<?> predictBeanType(String beanName, GenericBeanDefinition mbd, Class<?>... typesToMatch) {
        Class<?> targetType = mbd.getTargetType();
        if (targetType != null) {
            return targetType;
        }
        if (mbd.getFactoryMethodName() != null) {
            return null;
        }
        return resolveBeanClass(mbd, beanName, typesToMatch);
    }

    @Override
    public void addBeanPostProcessor(BeanPostProcessor beanPostProcessor) {
        Assert.notNull(beanPostProcessor, "BeanPostProcessor must not be null");
        // Remove from old position, if any
        this.beanPostProcessors.remove(beanPostProcessor);
        // Track whether it is instantiation/destruction aware
        if (beanPostProcessor instanceof InstantiationAwareBeanPostProcessor) {
            this.hasInstantiationAwareBeanPostProcessors = true;
        }
//        if (beanPostProcessor instanceof DestructionAwareBeanPostProcessor) {
//            this.hasDestructionAwareBeanPostProcessors = true;
//        }
        // Add to end of list
        this.beanPostProcessors.add(beanPostProcessor);
    }

    protected Class<?> getTypeForFactoryBean(final FactoryBean<?> factoryBean) {
        return factoryBean.getObjectType();
    }

    protected ResolvableType getTypeForFactoryBean(String beanName, GenericBeanDefinition mbd, boolean allowInit) {
        ResolvableType result = getTypeForFactoryBeanFromAttributes(mbd);
        if (result != ResolvableType.NONE) {
            return result;
        }

        if (allowInit && mbd.isSingleton()) {
            FactoryBean<?> factoryBean = doGetBean(FACTORY_BEAN_PREFIX + beanName, FactoryBean.class, null, true);
            Class<?> objectType = getTypeForFactoryBean(factoryBean);
            return (objectType != null) ? ResolvableType.forClass(objectType) : ResolvableType.NONE;
        }
        return ResolvableType.NONE;
    }

    @Override
    public int getBeanPostProcessorCount() {
        return this.beanPostProcessors.size();
    }

    ResolvableType getTypeForFactoryBeanFromAttributes(AttributeAccessor attributes) {
        Object attribute = attributes.getAttribute(FactoryBean.OBJECT_TYPE_ATTRIBUTE);
        if (attribute instanceof ResolvableType) {
            return (ResolvableType) attribute;
        }
        if (attribute instanceof Class) {
            return ResolvableType.forClass((Class<?>) attribute);
        }
        return ResolvableType.NONE;
    }

    @Override
    @Nullable
    public ClassLoader getBeanClassLoader() {
        return this.beanClassLoader;
    }

    @Override
    public boolean containsLocalBean(String name) {
        String beanName = transformedBeanName(name);
        return ((containsSingleton(beanName) || containsBeanDefinition(beanName)) &&
                (!BeanFactoryUtils.isFactoryDereference(name) || isFactoryBean(beanName)));
    }
}
