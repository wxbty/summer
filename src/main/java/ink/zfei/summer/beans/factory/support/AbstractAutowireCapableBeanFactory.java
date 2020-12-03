package ink.zfei.summer.beans.factory.support;

import ink.zfei.summer.beans.*;
import ink.zfei.summer.beans.factory.*;
import ink.zfei.summer.beans.factory.config.*;
import ink.zfei.summer.core.*;
import ink.zfei.summer.core.convert.ConversionService;
import ink.zfei.summer.lang.Nullable;
import ink.zfei.summer.util.*;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.beans.PropertyDescriptor;
import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Stream;

import static ink.zfei.summer.beans.factory.support.AbstractBeanDefinition.AUTOWIRE_CONSTRUCTOR;

public abstract class AbstractAutowireCapableBeanFactory extends AbstractBeanFactory
        implements AutowireCapableBeanFactory {

    public AbstractAutowireCapableBeanFactory(@Nullable BeanFactory parentBeanFactory) {
        this();
        setParentBeanFactory(parentBeanFactory);
    }


    public AbstractAutowireCapableBeanFactory() {
        super();
//        ignoreDependencyInterface(BeanNameAware.class);
//        ignoreDependencyInterface(BeanFactoryAware.class);
//        ignoreDependencyInterface(BeanClassLoaderAware.class);
    }

    protected final Log logger = LogFactory.getLog(this.getClass());
    private ParameterNameDiscoverer parameterNameDiscoverer = new DefaultParameterNameDiscoverer();
    private final List<BeanPostProcessor> beanPostProcessors = new CopyOnWriteArrayList<>();
    //@bean类型class对应的method集合，缓存下来不需要每次都解析
    private final ConcurrentMap<Class<?>, Method[]> factoryMethodCandidateCache = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, BeanWrapper> factoryBeanInstanceCache = new ConcurrentHashMap<>();
    private InstantiationStrategy instantiationStrategy = new CglibSubclassingInstantiationStrategy();
    private ConversionService conversionService;
    private volatile boolean hasInstantiationAwareBeanPostProcessors;
    private TypeConverter typeConverter;
    private AutowireCandidateResolver autowireCandidateResolver = SimpleAutowireCandidateResolver.INSTANCE;
    private final Map<String, Object> singletonObjects = new ConcurrentHashMap<String, Object>();
    private final Map<String, Set<String>> dependentBeanMap = new ConcurrentHashMap<>(64);
    private final List<StringValueResolver> embeddedValueResolvers = new CopyOnWriteArrayList<>();
    private final Map<Class<?>, Object> resolvableDependencies = new ConcurrentHashMap<>(16);
    private final Map<String, Set<String>> dependenciesForBeanMap = new ConcurrentHashMap<>(64);
    private Comparator<Object> dependencyComparator;


    @Override
    @SuppressWarnings("unchecked")
    public <T> T createBean(Class<T> beanClass) {
        // Use prototype bean definition, to avoid registering bean as dependent bean.
        GenericBeanDefinition bd = new GenericBeanDefinition(beanClass);
        bd.setScope(SCOPE_PROTOTYPE);
//        bd.allowCaching = ClassUtils.isCacheSafe(beanClass, getBeanClassLoader());
        return (T) createBean(beanClass.getName(), bd, null);
    }

    @Override
    protected Object createBean(String beanName, GenericBeanDefinition mbd, Object[] args) {
        if (logger.isTraceEnabled()) {
            logger.trace("Creating instance of bean '" + beanName + "'");
        }
        GenericBeanDefinition mbdToUse = mbd;

        // Make sure bean class is actually resolved at this point, and
        // clone the bean definition in case of a dynamically resolved Class
        // which cannot be stored in the shared merged bean definition.
        //这一步必须要获取bd的beanClass，给动态获取bean信息的bd克隆属性
        Class<?> resolvedClass = resolveBeanClass(mbdToUse, beanName);
        if (resolvedClass != null && !mbd.hasBeanClass() && mbd.getBeanClassName() != null) {
            mbdToUse = new GenericBeanDefinition(mbd);
            mbdToUse.setBeanClass(resolvedClass);
        }

        // Prepare method overrides.
        mbdToUse.prepareMethodOverrides();

        // Give BeanPostProcessors a chance to return a proxy instead of the target bean instance.
        //bean实例化前置处理
        Object bean = resolveBeforeInstantiation(beanName, mbdToUse);
        if (bean != null) {
            return bean;
        }

        try {
            Object beanInstance = doCreateBean(beanName, mbdToUse, args);
            if (logger.isTraceEnabled()) {
                logger.trace("Finished creating instance of bean '" + beanName + "'");
            }
            return beanInstance;
        } catch (BeanCreationException ex) {
            // A previously detected exception with proper bean creation context already,
            // or illegal singleton state to be communicated up to DefaultSingletonBeanRegistry.
            throw ex;
        } catch (Throwable ex) {
            throw new BeanCreationException(
                    beanName, "Unexpected exception during bean creation");
        }
    }

    protected Object resolveBeforeInstantiation(String beanName, GenericBeanDefinition mbd) {
        Object bean = null;
        // Make sure bean class is actually resolved at this point.
        Class<?> targetType = determineTargetType(beanName, mbd);
        if (targetType != null) {
            bean = applyBeanPostProcessorsBeforeInstantiation(targetType, beanName);
            if (bean != null) {
                bean = applyBeanPostProcessorsAfterInitialization(bean, beanName);
            }
        }
        return bean;
    }

    protected Class<?> determineTargetType(String beanName, GenericBeanDefinition mbd, Class<?>... typesToMatch) {
        Class<?> targetType = mbd.getTargetType();
        if (targetType == null) {
            targetType = (mbd.getFactoryMethodName() != null ?
                    getTypeForFactoryMethod(beanName, mbd, typesToMatch) :
                    resolveBeanClass(mbd, beanName, typesToMatch));
            if (ObjectUtils.isEmpty(typesToMatch)) {
                mbd.resolvedTargetType = targetType;
            }
        }
        return targetType;
    }

    protected Class<?> getTypeForFactoryMethod(String beanName, GenericBeanDefinition mbd, Class<?>... typesToMatch) {

        ResolvableType cachedReturnType = mbd.factoryMethodReturnType;
        if (cachedReturnType != null) {
            return cachedReturnType.resolve();
        }

        Class<?> commonType = null;
        Method uniqueCandidate = mbd.factoryMethodToIntrospect;

        if (uniqueCandidate == null) {
            Class<?> factoryClass;
            boolean isStatic = true;

            String factoryBeanName = mbd.getFactoryBeanName();
            if (factoryBeanName != null) {
                if (factoryBeanName.equals(beanName)) {
                    throw new RuntimeException();
                }
                // Check declared factory method return type on factory class.
                factoryClass = getType(factoryBeanName);
                isStatic = false;
            } else {
                // Check declared factory method return type on bean class.
                factoryClass = resolveBeanClass(mbd, beanName, typesToMatch);
            }

            if (factoryClass == null) {
                return null;
            }
            factoryClass = ClassUtils.getUserClass(factoryClass);

            // If all factory methods have the same return type, return that type.
            // Can't clearly figure out exact method due to type converting / autowiring!
            int minNrOfArgs =
                    (mbd.hasConstructorArgumentValues() ? mbd.getConstructorArgumentValues().getArgumentCount() : 0);
            Method[] candidates = this.factoryMethodCandidateCache.computeIfAbsent(factoryClass,
                    clazz -> ReflectionUtils.getUniqueDeclaredMethods(clazz, ReflectionUtils.USER_DECLARED_METHODS));

            for (Method candidate : candidates) {
                if (Modifier.isStatic(candidate.getModifiers()) == isStatic && mbd.isFactoryMethod(candidate) &&
                        candidate.getParameterCount() >= minNrOfArgs) {
                    // Declared type variables to inspect?
                    if (candidate.getTypeParameters().length > 0) {
                        try {
                            // Fully resolve parameter names and argument values.
                            Class<?>[] paramTypes = candidate.getParameterTypes();
                            String[] paramNames = null;
                            ParameterNameDiscoverer pnd = getParameterNameDiscoverer();
                            if (pnd != null) {
                                paramNames = pnd.getParameterNames(candidate);
                            }
                            ConstructorArgumentValues cav = mbd.getConstructorArgumentValues();
                            Set<ConstructorArgumentValues.ValueHolder> usedValueHolders = new HashSet<>(paramTypes.length);
                            Object[] args = new Object[paramTypes.length];
                            for (int i = 0; i < args.length; i++) {
                                ConstructorArgumentValues.ValueHolder valueHolder = cav.getArgumentValue(
                                        i, paramTypes[i], (paramNames != null ? paramNames[i] : null), usedValueHolders);
                                if (valueHolder == null) {
                                    valueHolder = cav.getGenericArgumentValue(null, null, usedValueHolders);
                                }
                                if (valueHolder != null) {
                                    args[i] = valueHolder.getValue();
                                    usedValueHolders.add(valueHolder);
                                }
                            }
                            Class<?> returnType = AutowireUtils.resolveReturnTypeForFactoryMethod(
                                    candidate, args, getBeanClassLoader());
                            uniqueCandidate = (commonType == null && returnType == candidate.getReturnType() ?
                                    candidate : null);
                            commonType = ClassUtils.determineCommonAncestor(returnType, commonType);
                            if (commonType == null) {
                                // Ambiguous return types found: return null to indicate "not determinable".
                                return null;
                            }
                        } catch (Throwable ex) {
                            if (logger.isDebugEnabled()) {
                                logger.debug("Failed to resolve generic return type for factory method: " + ex);
                            }
                        }
                    } else {
                        uniqueCandidate = (commonType == null ? candidate : null);
                        commonType = ClassUtils.determineCommonAncestor(candidate.getReturnType(), commonType);
                        if (commonType == null) {
                            // Ambiguous return types found: return null to indicate "not determinable".
                            return null;
                        }
                    }
                }
            }

            mbd.factoryMethodToIntrospect = uniqueCandidate;
            if (commonType == null) {
                return null;
            }
        }

        // Common return type found: all factory methods return same type. For a non-parameterized
        // unique candidate, cache the full type declaration context of the target factory method.
        cachedReturnType = (uniqueCandidate != null ?
                ResolvableType.forMethodReturnType(uniqueCandidate) : ResolvableType.forClass(commonType));
        mbd.factoryMethodReturnType = cachedReturnType;
        return cachedReturnType.resolve();
    }

    public ParameterNameDiscoverer getParameterNameDiscoverer() {
        return this.parameterNameDiscoverer;
    }

    protected Object applyBeanPostProcessorsBeforeInstantiation(Class<?> beanClass, String beanName) {
        for (BeanPostProcessor bp : getBeanPostProcessors()) {
            if (bp instanceof InstantiationAwareBeanPostProcessor) {
                InstantiationAwareBeanPostProcessor ibp = (InstantiationAwareBeanPostProcessor) bp;
                Object result = ibp.postProcessBeforeInstantiation(beanClass, beanName);
                if (result != null) {
                    return result;
                }
            }
        }
        return null;
    }

    private List<BeanPostProcessor> getBeanPostProcessors() {
        return beanPostProcessors;
    }

    public Object applyBeanPostProcessorsAfterInitialization(Object existingBean, String beanName) {


        Object result = existingBean;
        for (BeanPostProcessor processor : getBeanPostProcessors()) {
            Object current = processor.postProcessAfterInitialization(result, beanName);
            if (current == null) {
                return result;
            }
            result = current;
        }
        return result;
    }

    //真正创建bean第二步，创建实例->融合@AutoWired @Resource的属性->属性依赖注入->初始化
    protected Object doCreateBean(final String beanName, final GenericBeanDefinition mbd, final Object[] args) {

        // Instantiate the bean.
        BeanWrapper instanceWrapper = null;
        if (mbd.isSingleton()) {
            instanceWrapper = this.factoryBeanInstanceCache.remove(beanName);
        }
        if (instanceWrapper == null) {
            /*
             * createBeanInstance中包含三种创建 bean 实例的方式：
             * 1. 通过工厂方法创建 bean 实例
             * 2. 通过构造方法自动注入（autowire by constructor）的方式创建 bean 实例
             * 3. 通过无参构造方法方法创建 bean 实例
             */
            instanceWrapper = createBeanInstance(beanName, mbd, args);
        }
        final Object bean = instanceWrapper.getWrappedInstance();
        Class<?> beanType = instanceWrapper.getWrappedClass();
        mbd.resolvedTargetType = beanType;

        // Allow post-processors to modify the merged bean definition.
        if (!mbd.postProcessed) {
            try {
                applyMergedBeanDefinitionPostProcessors(mbd, beanType, beanName);
            } catch (Throwable ex) {
                throw new BeanCreationException(beanName,
                        "Post-processing of merged bean definition failed");
            }
            mbd.postProcessed = true;
        }

        // todo 允许循环依赖，add到early

        // Initialize the bean instance.
        Object exposedObject = bean;
        try {
            populateBean(beanName, mbd, instanceWrapper);
            exposedObject = initializeBean(beanName, exposedObject, mbd);
        } catch (Throwable ex) {
            if (ex instanceof BeanCreationException) {
                throw (BeanCreationException) ex;
            } else {
                ex.printStackTrace();
                throw new BeanCreationException(beanName, "Initialization of bean failed");
            }
        }

        //todo earlySingle

        //todo Register bean as disposable.
        return exposedObject;
    }


    //真正创建bean第三步，
    protected BeanWrapper createBeanInstance(String beanName, GenericBeanDefinition mbd, Object[] args) {
        // Make sure bean class is actually resolved at this point.
        Class<?> beanClass = resolveBeanClass(mbd, beanName);

        //如果beanClass存在（@bean中方法产生的bean没有beanClass），必须是public的
        if (beanClass != null && !Modifier.isPublic(beanClass.getModifiers())) {
            throw new BeanCreationException(beanName,
                    "Bean class isn't public, and non-public access not allowed: " + beanClass.getName());
        }

//      todo 用bd的实例回调生成实例  Supplier<?> instanceSupplier = mbd.getInstanceSupplier();
//        if (instanceSupplier != null) {
//            return obtainFromSupplier(instanceSupplier, beanName);
//        }

        //使用@bean中方法反射生成实例
        if (mbd.getFactoryMethodName() != null) {
            return instantiateUsingFactoryMethod(beanName, mbd, args);
        }

        // 创建同样的bean时，如果之前已经解析过（最终使用哪个构造器构造），直接使用之前的构造器
        boolean resolved = false;
        // autowireNecessary: 是否需要自动注入（即是否需要解析构造函数参数）
        boolean autowireNecessary = false;
        if (args == null) {
            // 如果resolvedConstructorOrFactoryMethod缓存不为空，则将resolved标记为已解析
            if (mbd.resolvedConstructorOrFactoryMethod != null) {
                resolved = true;
                autowireNecessary = mbd.constructorArgumentsResolved;
            }
        }

        if (resolved) {
            // 3.如果已经解析过，则使用
            // resolvedConstructorOrFactoryMethod缓存里解析好的构造函数方法
            if (autowireNecessary) {
                // 3.1 需要自动注入，则执行构造函数自动注入
                // 通过构造方法自动装备的方式构造Bean对象
                return autowireConstructor(beanName, mbd, null, null);
            } else {
                // 3.2 否则使用默认的构造函数进行bean的实例化
                // 如果已经解析了构造方法的参数，则必须要通过一个带参构造方法来实例
                return instantiateBean(beanName, mbd);
            }
        }

        // 后置处理器决定使用哪个构造器
        Constructor<?>[] ctors = determineConstructorsFromBeanPostProcessors(beanClass, beanName);
        if (ctors != null || mbd.getResolvedAutowireMode() == AUTOWIRE_CONSTRUCTOR ||
                mbd.hasConstructorArgumentValues() || !ObjectUtils.isEmpty(args)) {
            return autowireConstructor(beanName, mbd, ctors, args);
        }

        // 首选构造器
        ctors = mbd.getPreferredConstructors();
        if (ctors != null) {
            return autowireConstructor(beanName, mbd, ctors, null);
        }

        // No special handling: simply use no-arg constructor.
        return instantiateBean(beanName, mbd);
    }

    protected BeanWrapper autowireConstructor(
            String beanName, GenericBeanDefinition mbd, @Nullable Constructor<?>[] ctors, @Nullable Object[] explicitArgs) {

        return new ConstructorResolver(this).autowireConstructor(beanName, mbd, ctors, explicitArgs);
    }

    protected BeanWrapper instantiateUsingFactoryMethod(
            String beanName, GenericBeanDefinition mbd, @Nullable Object[] explicitArgs) {

        return new ConstructorResolver(this).instantiateUsingFactoryMethod(beanName, mbd, explicitArgs);
    }

    protected void applyMergedBeanDefinitionPostProcessors(GenericBeanDefinition mbd, Class<?> beanType, String beanName) {
        for (BeanPostProcessor bp : getBeanPostProcessors()) {
            if (bp instanceof MergedBeanDefinitionPostProcessor) {
                MergedBeanDefinitionPostProcessor bdp = (MergedBeanDefinitionPostProcessor) bp;
                bdp.postProcessMergedBeanDefinition(mbd, beanType, beanName);
            }
        }
    }

    protected void populateBean(String beanName, GenericBeanDefinition mbd, BeanWrapper bw) {

        if (bw == null) {
            if (mbd.hasPropertyValues()) {
                throw new BeanCreationException(
                        beanName, "Cannot apply property values to null instance");
            } else {
                // Skip property population phase for null instance.
                return;
            }
        }

        for (BeanPostProcessor bp : getBeanPostProcessors()) {
            if (bp instanceof InstantiationAwareBeanPostProcessor) {
                InstantiationAwareBeanPostProcessor ibp = (InstantiationAwareBeanPostProcessor) bp;
                //bean实例化后置处理
                if (!ibp.postProcessAfterInstantiation(bw.getWrappedInstance(), beanName)) {
                    return;
                }
            }
        }

        PropertyValues pvs = (mbd.hasPropertyValues() ? mbd.getPropertyValues() : null);

        int resolvedAutowireMode = mbd.getResolvedAutowireMode();
        if (resolvedAutowireMode == AUTOWIRE_BY_NAME || resolvedAutowireMode == AUTOWIRE_BY_TYPE) {
            MutablePropertyValues newPvs = new MutablePropertyValues(pvs);
            // Add property values based on autowire by name if applicable.
            if (resolvedAutowireMode == AUTOWIRE_BY_NAME) {
                autowireByName(beanName, mbd, bw, newPvs);
            }
            // Add property values based on autowire by type if applicable.
            if (resolvedAutowireMode == AUTOWIRE_BY_TYPE) {
                autowireByType(beanName, mbd, bw, newPvs);
            }
            pvs = newPvs;
        }

        boolean hasInstAwareBpps = hasInstantiationAwareBeanPostProcessors();
//        boolean needsDepCheck = (mbd.getDependencyCheck() != AbstractBeanDefinition.DEPENDENCY_CHECK_NONE);

        PropertyDescriptor[] filteredPds = null;
        if (hasInstAwareBpps) {
            if (pvs == null) {
                pvs = mbd.getPropertyValues();
            }
            for (BeanPostProcessor bp : getBeanPostProcessors()) {
                if (bp instanceof InstantiationAwareBeanPostProcessor) {
                    InstantiationAwareBeanPostProcessor ibp = (InstantiationAwareBeanPostProcessor) bp;
                    PropertyValues pvsToUse = ibp.postProcessProperties(pvs, bw.getWrappedInstance(), beanName);
                    if (pvsToUse == null) {
                        return;
                    }
                    pvs = pvsToUse;
                }
            }
        }
//        if (needsDepCheck) {
//            if (filteredPds == null) {
//                filteredPds = filterPropertyDescriptorsForDependencyCheck(bw, mbd.allowCaching);
//            }
//            checkDependencies(beanName, mbd, filteredPds, pvs);
//        }

        if (pvs != null) {
            applyPropertyValues(beanName, mbd, bw, pvs);
        }

//        if (mbd.hasPropertyValues()) {
//            MutablePropertyValues vals = mbd.getPropertyValues();
//            vals.getPropertyValueList().forEach(fieldName -> {
//
//                String depBeanName = vals.get(fieldName);
//                String methodName = "set" + fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1);
//                try {
//                    Object depBean = getBean(depBeanName, null, null);
//                    Method method = bw.getClass().getMethod(methodName, depBean.getClass());
//
//                    method.invoke(bw, depBean);
//                } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | InstantiationException e) {
//                    e.printStackTrace();
//                }
//
//
//            });
//        }


    }

    protected BeanWrapper instantiateBean(final String beanName, final GenericBeanDefinition mbd) {
        try {
            Object beanInstance;
            final BeanFactory parent = this;

            beanInstance = getInstantiationStrategy().instantiate(mbd, beanName, parent);
            BeanWrapper bw = new BeanWrapperImpl(beanInstance);
            initBeanWrapper(bw);
            return bw;
        } catch (Throwable ex) {
            throw new BeanCreationException(
                    beanName, "Instantiation of bean failed");
        }
    }

    public InstantiationStrategy getInstantiationStrategy() {
        return this.instantiationStrategy;
    }

    public void initBeanWrapper(BeanWrapper bw) {
        bw.setConversionService(getConversionService());
//        registerCustomEditors(bw);
    }

    public ConversionService getConversionService() {
        return this.conversionService;
    }

    private Object initializeBean(String beanName, Object wrappedBean, GenericBeanDefinition mbd) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {

        invokeAwareMethods(beanName, wrappedBean);

        //3、遍历BeanPostProcessor实现，调用before方法，返回bean
        wrappedBean = applyBeanPostProcessorsBeforeInitialization(wrappedBean, beanName);

        //4、初始化bean
        try {
            invokeInitMethods(beanName, wrappedBean, mbd);
        } catch (Throwable ex) {
            ex.printStackTrace();
            throw new BeanCreationException(
                    beanName, "Invocation of init method failed");
        }

        //5、遍历BeanPostProcessor实现，调用after方法，返回bean
        wrappedBean = applyBeanPostProcessorsAfterInitialization(wrappedBean, beanName);
        if (wrappedBean != null) {
            singletonObjects.put(beanName, wrappedBean);
        }
        return wrappedBean;
    }

    private void invokeAwareMethods(String beanName, Object bean) {

        if (bean instanceof BeanNameAware) {
            BeanNameAware beanNameAware = (BeanNameAware) bean;
            beanNameAware.setBeanName(beanName);
        }

    }

    private Object applyBeanPostProcessorsBeforeInitialization(Object existingBean, String beanName) {
        Object result = existingBean;
        for (BeanPostProcessor processor : getBeanPostProcessors()) {
            Object current = processor.postProcessBeforeInitialization(result, beanName);
            if (current == null) {
                return result;
            }
            result = current;
        }
        return result;
    }

    private void invokeInitMethods(String beanName, Object bean, GenericBeanDefinition mbd) throws Throwable {


        boolean isInitializingBean = (bean instanceof InitializingBean);
        if (isInitializingBean && (mbd == null)) {
            ((InitializingBean) bean).afterPropertiesSet();
        }

        if (mbd != null && bean.getClass() != NullBean.class) {
            String initMethodName = mbd.getInitMethodName();
            if (StringUtils.hasLength(initMethodName) &&
                    !(isInitializingBean && "afterPropertiesSet".equals(initMethodName))) {
                invokeCustomInitMethod(beanName, bean, mbd);
            }
        }


    }

    protected void invokeCustomInitMethod(String beanName, final Object bean, GenericBeanDefinition mbd)
            throws Throwable {

        String initMethodName = mbd.getInitMethodName();
        Assert.state(initMethodName != null, "No init method set");
        Method initMethod = (mbd.isNonPublicAccessAllowed() ?
                BeanUtils.findMethod(bean.getClass(), initMethodName) :
                ClassUtils.getMethodIfAvailable(bean.getClass(), initMethodName));

        if (initMethod == null) {
            return;
        }

        if (logger.isTraceEnabled()) {
            logger.trace("Invoking init method  '" + initMethodName + "' on bean with name '" + beanName + "'");
        }
        Method methodToInvoke = ClassUtils.getInterfaceMethodIfPossible(initMethod);

        try {
            ReflectionUtils.makeAccessible(methodToInvoke);
            methodToInvoke.invoke(bean);
        } catch (InvocationTargetException ex) {
            throw ex.getTargetException();
        }
    }

    protected Constructor<?>[] determineConstructorsFromBeanPostProcessors(@Nullable Class<?> beanClass, String beanName) {

        if (beanClass != null && hasInstantiationAwareBeanPostProcessors()) {
            for (BeanPostProcessor bp : getBeanPostProcessors()) {
                if (bp instanceof SmartInstantiationAwareBeanPostProcessor) {
                    SmartInstantiationAwareBeanPostProcessor ibp = (SmartInstantiationAwareBeanPostProcessor) bp;
                    Constructor<?>[] ctors = ibp.determineCandidateConstructors(beanClass, beanName);
                    if (ctors != null) {
                        return ctors;
                    }
                }
            }
        }
        return null;
    }

    protected boolean hasInstantiationAwareBeanPostProcessors() {
        return this.hasInstantiationAwareBeanPostProcessors;
    }

    protected void autowireByName(
            String beanName, AbstractBeanDefinition mbd, BeanWrapper bw, MutablePropertyValues pvs) {

        String[] propertyNames = unsatisfiedNonSimpleProperties(mbd, bw);
        for (String propertyName : propertyNames) {
            if (containsBean(propertyName)) {
                Object bean = getBean(propertyName);
                pvs.add(propertyName, bean);
                registerDependentBean(propertyName, beanName);
                if (logger.isTraceEnabled()) {
                    logger.trace("Added autowiring by name from bean name '" + beanName +
                            "' via property '" + propertyName + "' to bean named '" + propertyName + "'");
                }
            } else {
                if (logger.isTraceEnabled()) {
                    logger.trace("Not autowiring property '" + propertyName + "' of bean '" + beanName +
                            "' by name: no matching bean found");
                }
            }
        }
    }

    protected String[] unsatisfiedNonSimpleProperties(AbstractBeanDefinition mbd, BeanWrapper bw) {
        Set<String> result = new TreeSet<>();
        PropertyValues pvs = mbd.getPropertyValues();
        PropertyDescriptor[] pds = bw.getPropertyDescriptors();
        for (PropertyDescriptor pd : pds) {
            if (pd.getWriteMethod() != null && !pvs.contains(pd.getName()) &&
                    !BeanUtils.isSimpleProperty(pd.getPropertyType())) {
                result.add(pd.getName());
            }
        }
        return StringUtils.toStringArray(result);
    }

    protected void autowireByType(
            String beanName, AbstractBeanDefinition mbd, BeanWrapper bw, MutablePropertyValues pvs) {

        TypeConverter converter = getCustomTypeConverter();
        if (converter == null) {
            converter = bw;
        }

        Set<String> autowiredBeanNames = new LinkedHashSet<>(4);
        String[] propertyNames = unsatisfiedNonSimpleProperties(mbd, bw);
        for (String propertyName : propertyNames) {
            PropertyDescriptor pd = bw.getPropertyDescriptor(propertyName);
            // Don't try autowiring by type for type Object: never makes sense,
            // even if it technically is a unsatisfied, non-simple property.
            if (Object.class != pd.getPropertyType()) {
                MethodParameter methodParam = BeanUtils.getWriteMethodParameter(pd);
                // Do not allow eager init for type matching in case of a prioritized post-processor.
                boolean eager = !(bw.getWrappedInstance() instanceof PriorityOrdered);
                DependencyDescriptor desc = new AutowireByTypeDependencyDescriptor(methodParam, eager);
                Object autowiredArgument = resolveDependency(desc, beanName, autowiredBeanNames, converter);
                if (autowiredArgument != null) {
                    pvs.add(propertyName, autowiredArgument);
                }
                for (String autowiredBeanName : autowiredBeanNames) {
                    registerDependentBean(autowiredBeanName, beanName);
                    if (logger.isTraceEnabled()) {
                        logger.trace("Autowiring by type from bean name '" + beanName + "' via property '" +
                                propertyName + "' to bean named '" + autowiredBeanName + "'");
                    }
                }
                autowiredBeanNames.clear();
            }

        }


    }

    public TypeConverter getCustomTypeConverter() {
        return this.typeConverter;
    }

    public Object resolveDependency(DependencyDescriptor descriptor, @Nullable String requestingBeanName,
                                    @Nullable Set<String> autowiredBeanNames, @Nullable TypeConverter typeConverter) {

        descriptor.initParameterNameDiscovery(getParameterNameDiscoverer());
        if (Optional.class == descriptor.getDependencyType()) {
            return createOptionalDependency(descriptor, requestingBeanName);
        } else if (ObjectFactory.class == descriptor.getDependencyType() ||
                ObjectProvider.class == descriptor.getDependencyType()) {
            return new DependencyObjectProvider(descriptor, requestingBeanName);
        } else {
            Object result = getAutowireCandidateResolver().getLazyResolutionProxyIfNecessary(
                    descriptor, requestingBeanName);
            if (result == null) {
                result = doResolveDependency(descriptor, requestingBeanName, autowiredBeanNames, typeConverter);
            }
            return result;
        }
    }

    private Optional<?> createOptionalDependency(
            DependencyDescriptor descriptor, @Nullable String beanName, final Object... args) {

        DependencyDescriptor descriptorToUse =  new NestedDependencyDescriptor(descriptor) {
            @Override
            public boolean isRequired() {
                return false;
            }

            @Override
            public Object resolveCandidate(String beanName, Class<?> requiredType, BeanFactory beanFactory) {
                return (!ObjectUtils.isEmpty(args) ? beanFactory.getBean(beanName) :
                        super.resolveCandidate(beanName, requiredType, beanFactory));
            }
        };
        Object result = doResolveDependency(descriptorToUse, beanName, null, null);
        return (result instanceof Optional ? (Optional<?>) result : Optional.ofNullable(result));
    }

    private static class NestedDependencyDescriptor extends DependencyDescriptor {

        public NestedDependencyDescriptor(DependencyDescriptor original) {
            super(original);
            increaseNestingLevel();
        }
    }

    public Object doResolveDependency(DependencyDescriptor descriptor, @Nullable String beanName,
                                      @Nullable Set<String> autowiredBeanNames, @Nullable TypeConverter typeConverter) {

        InjectionPoint previousInjectionPoint = ConstructorResolver.setCurrentInjectionPoint(descriptor);
        try {
            Object shortcut = descriptor.resolveShortcut(this);
            if (shortcut != null) {
                return shortcut;
            }

            Class<?> type = descriptor.getDependencyType();
            Object value = getAutowireCandidateResolver().getSuggestedValue(descriptor);
            if (value != null) {
                if (value instanceof String) {
                    String strVal = resolveEmbeddedValue((String) value);
                    BeanDefinition bd = (beanName != null && containsBean(beanName) ?
                            getBeanDefinition(beanName) : null);
                    value = evaluateBeanDefinitionString(strVal, (GenericBeanDefinition) bd);
                }
                TypeConverter converter = (typeConverter != null ? typeConverter : getTypeConverter());
                try {
                    return converter.convertIfNecessary(value, type, descriptor.getTypeDescriptor());
                } catch (UnsupportedOperationException ex) {
                    // A custom TypeConverter which does not support TypeDescriptor resolution...
                    return (descriptor.getField() != null ?
                            converter.convertIfNecessary(value, type, descriptor.getField()) :
                            converter.convertIfNecessary(value, type, descriptor.getMethodParameter()));
                }
            }

            Object multipleBeans = resolveMultipleBeans(descriptor, beanName, autowiredBeanNames, typeConverter);
            if (multipleBeans != null) {
                return multipleBeans;
            }

            Map<String, Object> matchingBeans = findAutowireCandidates(beanName, type, descriptor);
            if (matchingBeans.isEmpty()) {
//                if (isRequired(descriptor)) {
//                    raiseNoMatchingBeanFound(type, descriptor.getResolvableType(), descriptor);
//                }
                return null;
            }

            String autowiredBeanName;
            Object instanceCandidate;

            if (matchingBeans.size() > 1) {
                autowiredBeanName = determineAutowireCandidate(matchingBeans, descriptor);
                if (autowiredBeanName == null) {
//                    if (isRequired(descriptor) || !indicatesMultipleBeans(type)) {
//                        return descriptor.resolveNotUnique(descriptor.getResolvableType(), matchingBeans);
//                    } else {
                    // In case of an optional Collection/Map, silently ignore a non-unique case:
                    // possibly it was meant to be an empty collection of multiple regular beans
                    // (before 4.3 in particular when we didn't even look for collection beans).
                    return null;
//                    }
                }
                instanceCandidate = matchingBeans.get(autowiredBeanName);
            } else {
                // We have exactly one match.
                Map.Entry<String, Object> entry = matchingBeans.entrySet().iterator().next();
                autowiredBeanName = entry.getKey();
                instanceCandidate = entry.getValue();
            }

            if (autowiredBeanNames != null) {
                autowiredBeanNames.add(autowiredBeanName);
            }
            if (instanceCandidate instanceof Class) {
                instanceCandidate = descriptor.resolveCandidate(autowiredBeanName, type, this);
            }
            Object result = instanceCandidate;
            if (result instanceof NullBean) {
                result = null;
            }
            if (!ClassUtils.isAssignableValue(type, result)) {
                throw new RuntimeException();
            }
            return result;
        } finally {
            ConstructorResolver.setCurrentInjectionPoint(previousInjectionPoint);
        }
    }

    public AutowireCandidateResolver getAutowireCandidateResolver() {
        return this.autowireCandidateResolver;
    }

    protected String determineAutowireCandidate(Map<String, Object> candidates, DependencyDescriptor descriptor) {
        Class<?> requiredType = descriptor.getDependencyType();
        String primaryCandidate = determinePrimaryCandidate(candidates, requiredType);
        if (primaryCandidate != null) {
            return primaryCandidate;
        }
        // Fallback
        for (Map.Entry<String, Object> entry : candidates.entrySet()) {
            String candidateName = entry.getKey();
            Object beanInstance = entry.getValue();
            if ((beanInstance != null && this.resolvableDependencies.containsValue(beanInstance)) ||
                    matchesBeanName(candidateName, descriptor.getDependencyName())) {
                return candidateName;
            }
        }
        return null;
    }

    protected String determinePrimaryCandidate(Map<String, Object> candidates, Class<?> requiredType) {
        String primaryBeanName = null;
        for (Map.Entry<String, Object> entry : candidates.entrySet()) {
            String candidateBeanName = entry.getKey();
            Object beanInstance = entry.getValue();
            if (isPrimary(candidateBeanName, beanInstance)) {
                if (primaryBeanName != null) {
                    boolean candidateLocal = containsBeanDefinition(candidateBeanName);
                    boolean primaryLocal = containsBeanDefinition(primaryBeanName);
                    if (candidateLocal && primaryLocal) {
                        throw new RuntimeException(
                                "more than one 'primary' bean found among candidates: " + candidates.keySet());
                    } else if (candidateLocal) {
                        primaryBeanName = candidateBeanName;
                    }
                } else {
                    primaryBeanName = candidateBeanName;
                }
            }
        }
        return primaryBeanName;
    }

    protected boolean isPrimary(String beanName, Object beanInstance) {
        String transformedBeanName = transformedBeanName(beanName);
        if (containsBeanDefinition(transformedBeanName)) {
            return getBeanDefinition(transformedBeanName).isPrimary();
        }
        return false;
    }

    protected void applyPropertyValues(String beanName, BeanDefinition mbd, BeanWrapper bw, PropertyValues pvs) {
        if (pvs.isEmpty()) {
            return;
        }

        MutablePropertyValues mpvs = null;
        List<PropertyValue> original;

        if (pvs instanceof MutablePropertyValues) {
            mpvs = (MutablePropertyValues) pvs;
            if (mpvs.isConverted()) {
                // Shortcut: use the pre-converted values as-is.
                bw.setPropertyValues(mpvs);
                return;
            }
            original = mpvs.getPropertyValueList();
        } else {
            original = Arrays.asList(pvs.getPropertyValues());
        }

        TypeConverter converter = getCustomTypeConverter();
        if (converter == null) {
            converter = bw;
        }
        BeanDefinitionValueResolver valueResolver = new BeanDefinitionValueResolver(this, beanName, mbd, converter);

        // Create a deep copy, resolving any references for values.
        List<PropertyValue> deepCopy = new ArrayList<>(original.size());
        boolean resolveNecessary = false;
        for (PropertyValue pv : original) {
            if (pv.isConverted()) {
                deepCopy.add(pv);
            } else {
                String propertyName = pv.getName();
                Object originalValue = pv.getValue();
                if (originalValue == AutowiredPropertyMarker.INSTANCE) {
                    Method writeMethod = bw.getPropertyDescriptor(propertyName).getWriteMethod();
                    if (writeMethod == null) {
                        throw new IllegalArgumentException("Autowire marker for property without write method: " + pv);
                    }
                    originalValue = new DependencyDescriptor(new MethodParameter(writeMethod, 0), true);
                }
                Object resolvedValue = valueResolver.resolveValueIfNecessary(pv, originalValue);
                Object convertedValue = resolvedValue;
                boolean convertible = bw.isWritableProperty(propertyName) &&
                        !PropertyAccessorUtils.isNestedOrIndexedProperty(propertyName);
                if (convertible) {
                    convertedValue = convertForProperty(resolvedValue, propertyName, bw, converter);
                }
                // Possibly store converted value in merged bean definition,
                // in order to avoid re-conversion for every created bean instance.
                if (resolvedValue == originalValue) {
                    if (convertible) {
                        pv.setConvertedValue(convertedValue);
                    }
                    deepCopy.add(pv);
                } else if (convertible && originalValue instanceof TypedStringValue &&
                        !((TypedStringValue) originalValue).isDynamic() &&
                        !(convertedValue instanceof Collection || ObjectUtils.isArray(convertedValue))) {
                    pv.setConvertedValue(convertedValue);
                    deepCopy.add(pv);
                } else {
                    resolveNecessary = true;
                    deepCopy.add(new PropertyValue(pv, convertedValue));
                }
            }
        }
        if (mpvs != null && !resolveNecessary) {
            mpvs.setConverted();
        }

        // Set our (possibly massaged) deep copy.
        bw.setPropertyValues(new MutablePropertyValues(deepCopy));

    }

    private Object convertForProperty(
            @Nullable Object value, String propertyName, BeanWrapper bw, TypeConverter converter) {

        if (converter instanceof BeanWrapperImpl) {
            return ((BeanWrapperImpl) converter).convertForProperty(value, propertyName);
        } else {
            PropertyDescriptor pd = bw.getPropertyDescriptor(propertyName);
            MethodParameter methodParam = BeanUtils.getWriteMethodParameter(pd);
            return converter.convertIfNecessary(value, pd.getPropertyType(), methodParam);
        }
    }

    public boolean containsBean(String name) {
        String beanName = transformedBeanName(name);
        if (containsSingleton(beanName) || containsBeanDefinition(beanName)) {
            return (!BeanFactoryUtils.isFactoryDereference(name) || isFactoryBean(name));
        }
        return false;
        //todo check parent.
    }

    public void registerDependentBean(String beanName, String dependentBeanName) {
        String canonicalName = beanName;

        Set<String> dependentBeans =
                this.dependentBeanMap.computeIfAbsent(canonicalName, k -> new LinkedHashSet<>(8));
        if (!dependentBeans.add(dependentBeanName)) {
            return;
        }

        Set<String> dependenciesForBean =
                this.dependenciesForBeanMap.computeIfAbsent(dependentBeanName, k -> new LinkedHashSet<>(8));
        dependenciesForBean.add(canonicalName);
    }

    private static class AutowireByTypeDependencyDescriptor extends DependencyDescriptor {

        public AutowireByTypeDependencyDescriptor(MethodParameter methodParameter, boolean eager) {
            super(methodParameter, false, eager);
        }

        @Override
        public String getDependencyName() {
            return null;
        }
    }

    private class DependencyObjectProvider implements BeanObjectProvider<Object> {

        private final DependencyDescriptor descriptor;

        private final boolean optional;

        @Nullable
        private final String beanName;

        public DependencyObjectProvider(DependencyDescriptor descriptor, @Nullable String beanName) {
            this.descriptor = new NestedDependencyDescriptor(descriptor);
            this.optional = (this.descriptor.getDependencyType() == Optional.class);
            this.beanName = beanName;
        }

        @Override
        public Object getObject() {
            if (this.optional) {
                return createOptionalDependency(this.descriptor, this.beanName);
            } else {
                Object result = doResolveDependency(this.descriptor, this.beanName, null, null);
                if (result == null) {
                    throw new NoSuchBeanDefinitionException(this.beanName);
                }
                return result;
            }
        }

        @Override
        public Object getObject(final Object... args) {
            if (this.optional) {
                return createOptionalDependency(this.descriptor, this.beanName, args);
            } else {
                DependencyDescriptor descriptorToUse = new DependencyDescriptor(this.descriptor) {
                    @Override
                    public Object resolveCandidate(String beanName, Class<?> requiredType, BeanFactory beanFactory) {
                        return beanFactory.getBean(beanName, args);
                    }
                };
                Object result = doResolveDependency(descriptorToUse, this.beanName, null, null);
                if (result == null) {
                    throw new NoSuchBeanDefinitionException(this.beanName);
                }
                return result;
            }
        }

        @Override
        @Nullable
        public Object getIfAvailable() {
            if (this.optional) {
                return createOptionalDependency(this.descriptor, this.beanName);
            } else {
                DependencyDescriptor descriptorToUse = new DependencyDescriptor(this.descriptor) {
                    @Override
                    public boolean isRequired() {
                        return false;
                    }
                };
                return doResolveDependency(descriptorToUse, this.beanName, null, null);
            }
        }

        @Override
        @Nullable
        public Object getIfUnique() {
            DependencyDescriptor descriptorToUse = new DependencyDescriptor(this.descriptor) {
            };
            if (this.optional) {
                return createOptionalDependency(descriptorToUse, this.beanName);
            } else {
                return doResolveDependency(descriptorToUse, this.beanName, null, null);
            }
        }

        @Nullable
        protected Object getValue() {
            if (this.optional) {
                return createOptionalDependency(this.descriptor, this.beanName);
            } else {
                return doResolveDependency(this.descriptor, this.beanName, null, null);
            }
        }

        @Override
        public Stream<Object> stream() {
            return resolveStream(false);
        }

        @Override
        public Stream<Object> orderedStream() {
            return resolveStream(true);
        }

        @SuppressWarnings("unchecked")
        private Stream<Object> resolveStream(boolean ordered) {
            DependencyDescriptor descriptorToUse = new StreamDependencyDescriptor(this.descriptor, ordered);
            Object result = doResolveDependency(descriptorToUse, this.beanName, null, null);
            return (result instanceof Stream ? (Stream<Object>) result : Stream.of(result));
        }
    }

    private interface BeanObjectProvider<T> extends ObjectProvider<T>, Serializable {
    }

    @Nullable
    public String resolveEmbeddedValue(@Nullable String value) {
        if (value == null) {
            return null;
        }
        String result = value;
        for (StringValueResolver resolver : this.embeddedValueResolvers) {
            result = resolver.resolveStringValue(result);
            if (result == null) {
                return null;
            }
        }
        return result;
    }

    private Object resolveMultipleBeans(DependencyDescriptor descriptor, @Nullable String beanName,
                                        @Nullable Set<String> autowiredBeanNames, @Nullable TypeConverter typeConverter) {

        final Class<?> type = descriptor.getDependencyType();

        if (descriptor instanceof StreamDependencyDescriptor) {
            Map<String, Object> matchingBeans = findAutowireCandidates(beanName, type, descriptor);
            if (autowiredBeanNames != null) {
                autowiredBeanNames.addAll(matchingBeans.keySet());
            }
            Stream<Object> stream = matchingBeans.keySet().stream()
                    .map(name -> descriptor.resolveCandidate(name, type, this))
                    .filter(bean -> !(bean instanceof NullBean));
            if (((StreamDependencyDescriptor) descriptor).isOrdered()) {
                stream = stream.sorted(adaptOrderComparator(matchingBeans));
            }
            return stream;
        } else if (type.isArray()) {
            Class<?> componentType = type.getComponentType();
            ResolvableType resolvableType = descriptor.getResolvableType();
            Class<?> resolvedArrayType = resolvableType.resolve(type);
            if (resolvedArrayType != type) {
                componentType = resolvableType.getComponentType().resolve();
            }
            if (componentType == null) {
                return null;
            }
            Map<String, Object> matchingBeans = findAutowireCandidates(beanName, componentType,
                    new MultiElementDescriptor(descriptor));
            if (matchingBeans.isEmpty()) {
                return null;
            }
            if (autowiredBeanNames != null) {
                autowiredBeanNames.addAll(matchingBeans.keySet());
            }
            TypeConverter converter = (typeConverter != null ? typeConverter : getTypeConverter());
            Object result = converter.convertIfNecessary(matchingBeans.values(), resolvedArrayType);
            if (result instanceof Object[]) {
                Comparator<Object> comparator = adaptDependencyComparator(matchingBeans);
                if (comparator != null) {
                    Arrays.sort((Object[]) result, comparator);
                }
            }
            return result;
        } else if (Collection.class.isAssignableFrom(type) && type.isInterface()) {
            Class<?> elementType = descriptor.getResolvableType().asCollection().resolveGeneric();
            if (elementType == null) {
                return null;
            }
            Map<String, Object> matchingBeans = findAutowireCandidates(beanName, elementType,
                    new MultiElementDescriptor(descriptor));
            if (matchingBeans.isEmpty()) {
                return null;
            }
            if (autowiredBeanNames != null) {
                autowiredBeanNames.addAll(matchingBeans.keySet());
            }
            TypeConverter converter = (typeConverter != null ? typeConverter : getTypeConverter());
            Object result = converter.convertIfNecessary(matchingBeans.values(), type);
            if (result instanceof List) {
                if (((List<?>) result).size() > 1) {
                    Comparator<Object> comparator = adaptDependencyComparator(matchingBeans);
                    if (comparator != null) {
                        ((List<?>) result).sort(comparator);
                    }
                }
            }
            return result;
        } else if (Map.class == type) {
            ResolvableType mapType = descriptor.getResolvableType().asMap();
            Class<?> keyType = mapType.resolveGeneric(0);
            if (String.class != keyType) {
                return null;
            }
            Class<?> valueType = mapType.resolveGeneric(1);
            if (valueType == null) {
                return null;
            }
            Map<String, Object> matchingBeans = findAutowireCandidates(beanName, valueType,
                    new MultiElementDescriptor(descriptor));
            if (matchingBeans.isEmpty()) {
                return null;
            }
            if (autowiredBeanNames != null) {
                autowiredBeanNames.addAll(matchingBeans.keySet());
            }
            return matchingBeans;
        } else {
            return null;
        }
    }

    private static class StreamDependencyDescriptor extends DependencyDescriptor {

        private final boolean ordered;

        public StreamDependencyDescriptor(DependencyDescriptor original, boolean ordered) {
            super(original);
            this.ordered = ordered;
        }

        public boolean isOrdered() {
            return this.ordered;
        }
    }

    public TypeConverter getTypeConverter() {
        TypeConverter customConverter = getCustomTypeConverter();
        if (customConverter != null) {
            return customConverter;
        } else {
            // Build default TypeConverter, registering custom editors.
            SimpleTypeConverter typeConverter = new SimpleTypeConverter();
            typeConverter.setConversionService(getConversionService());
//            registerCustomEditors(typeConverter);
            return typeConverter;
        }
    }

    protected Map<String, Object> findAutowireCandidates(
            @Nullable String beanName, Class<?> requiredType, DependencyDescriptor descriptor) {

        String[] candidateNames = BeanFactoryUtils.beanNamesForTypeIncludingAncestors(
                (ListableBeanFactory)this, requiredType);
        Map<String, Object> result = new LinkedHashMap<>(candidateNames.length);
        for (Map.Entry<Class<?>, Object> classObjectEntry : this.resolvableDependencies.entrySet()) {
            Class<?> autowiringType = classObjectEntry.getKey();
            if (autowiringType.isAssignableFrom(requiredType)) {
                Object autowiringValue = classObjectEntry.getValue();
                autowiringValue = AutowireUtils.resolveAutowiringValue(autowiringValue, requiredType);
                if (requiredType.isInstance(autowiringValue)) {
                    result.put(ObjectUtils.identityToString(autowiringValue), autowiringValue);
                    break;
                }
            }
        }
        for (String candidate : candidateNames) {
            if (!isSelfReference(beanName, candidate) && isAutowireCandidate(candidate, descriptor)) {
                addCandidateEntry(result, candidate, descriptor, requiredType);
            }
        }
        if (result.isEmpty()) {
            boolean multiple = indicatesMultipleBeans(requiredType);
            // Consider fallback matches if the first pass failed to find anything...
            DependencyDescriptor fallbackDescriptor = descriptor.forFallbackMatch();
            for (String candidate : candidateNames) {
                if (!isSelfReference(beanName, candidate) && isAutowireCandidate(candidate, fallbackDescriptor) &&
                        (!multiple || getAutowireCandidateResolver().hasQualifier(descriptor))) {
                    addCandidateEntry(result, candidate, descriptor, requiredType);
                }
            }
            if (result.isEmpty() && !multiple) {
                // Consider self references as a final pass...
                // but in the case of a dependency collection, not the very same bean itself.
                for (String candidate : candidateNames) {
                    if (isSelfReference(beanName, candidate) &&
                            (!(descriptor instanceof MultiElementDescriptor) || !beanName.equals(candidate)) &&
                            isAutowireCandidate(candidate, fallbackDescriptor)) {
                        addCandidateEntry(result, candidate, descriptor, requiredType);
                    }
                }
            }
        }
        return result;
    }

    public boolean isAutowireCandidate(String beanName, DependencyDescriptor descriptor)
            throws NoSuchBeanDefinitionException {

        return isAutowireCandidate(beanName, descriptor, getAutowireCandidateResolver());
    }

    protected boolean isAutowireCandidate(String beanName, DependencyDescriptor descriptor, AutowireCandidateResolver resolver)
            throws NoSuchBeanDefinitionException {

        String beanDefinitionName = BeanFactoryUtils.transformedBeanName(beanName);
        if (containsBeanDefinition(beanDefinitionName)) {
            return isAutowireCandidate(beanName, (GenericBeanDefinition) getBeanDefinition(beanDefinitionName), descriptor, resolver);
        } else if (containsSingleton(beanName)) {
            return isAutowireCandidate(beanName, new GenericBeanDefinition(getType(beanName)), descriptor, resolver);
        }

        return true;
    }

    protected boolean isAutowireCandidate(String beanName, GenericBeanDefinition mbd,
                                          DependencyDescriptor descriptor, AutowireCandidateResolver resolver) {

        String beanDefinitionName = BeanFactoryUtils.transformedBeanName(beanName);
        resolveBeanClass(mbd, beanDefinitionName);
        if (mbd.isFactoryMethodUnique && mbd.factoryMethodToIntrospect == null) {
            new ConstructorResolver(this).resolveFactoryMethodIfPossible(mbd);
        }
        BeanDefinitionHolder holder = new BeanDefinitionHolder(mbd, beanName);
        return resolver.isAutowireCandidate(holder, descriptor);
    }

    protected boolean matchesBeanName(String beanName, @Nullable String candidateName) {
        return (candidateName != null &&
                (candidateName.equals(beanName)));
    }

    private Comparator<Object> adaptOrderComparator(Map<String, ?> matchingBeans) {
        Comparator<Object> dependencyComparator = getDependencyComparator();
        OrderComparator comparator = (dependencyComparator instanceof OrderComparator ?
                (OrderComparator) dependencyComparator : OrderComparator.INSTANCE);
        return comparator.withSourceProvider(createFactoryAwareOrderSourceProvider(matchingBeans));
    }

    public Comparator<Object> getDependencyComparator() {
        return this.dependencyComparator;
    }

    private static class MultiElementDescriptor extends NestedDependencyDescriptor {

        public MultiElementDescriptor(DependencyDescriptor original) {
            super(original);
        }
    }

    private OrderComparator.OrderSourceProvider createFactoryAwareOrderSourceProvider(Map<String, ?> beans) {
        IdentityHashMap<Object, String> instancesToBeanNames = new IdentityHashMap<>();
        beans.forEach((beanName, instance) -> instancesToBeanNames.put(instance, beanName));
        return new FactoryAwareOrderSourceProvider(instancesToBeanNames);
    }

    private class FactoryAwareOrderSourceProvider implements OrderComparator.OrderSourceProvider {

        private final Map<Object, String> instancesToBeanNames;

        public FactoryAwareOrderSourceProvider(Map<Object, String> instancesToBeanNames) {
            this.instancesToBeanNames = instancesToBeanNames;
        }

        @Override
        @Nullable
        public Object getOrderSource(Object obj) {
            String beanName = this.instancesToBeanNames.get(obj);
            if (beanName == null || !containsBeanDefinition(beanName)) {
                return null;
            }
            GenericBeanDefinition beanDefinition = (GenericBeanDefinition) getBeanDefinition(beanName);
            List<Object> sources = new ArrayList<>(2);
            Method factoryMethod = beanDefinition.getResolvedFactoryMethod();
            if (factoryMethod != null) {
                sources.add(factoryMethod);
            }
            Class<?> targetType = beanDefinition.getTargetType();
            if (targetType != null && targetType != obj.getClass()) {
                sources.add(targetType);
            }
            return sources.toArray();
        }
    }

    private boolean isSelfReference(@Nullable String beanName, @Nullable String candidateName) {
        return (beanName != null && candidateName != null &&
                (beanName.equals(candidateName) || (containsBeanDefinition(candidateName) &&
                        beanName.equals(getBeanDefinition(candidateName).getFactoryBeanName()))));
    }

    private Comparator<Object> adaptDependencyComparator(Map<String, ?> matchingBeans) {
        Comparator<Object> comparator = getDependencyComparator();
        if (comparator instanceof OrderComparator) {
            return ((OrderComparator) comparator).withSourceProvider(
                    createFactoryAwareOrderSourceProvider(matchingBeans));
        } else {
            return comparator;
        }
    }

    private void addCandidateEntry(Map<String, Object> candidates, String candidateName,
                                   DependencyDescriptor descriptor, Class<?> requiredType) {

        if (descriptor instanceof MultiElementDescriptor) {
            Object beanInstance = descriptor.resolveCandidate(candidateName, requiredType, this);
            if (!(beanInstance instanceof NullBean)) {
                candidates.put(candidateName, beanInstance);
            }
        } else if (containsSingleton(candidateName) || (descriptor instanceof StreamDependencyDescriptor &&
                ((StreamDependencyDescriptor) descriptor).isOrdered())) {
            Object beanInstance = descriptor.resolveCandidate(candidateName, requiredType, this);
            candidates.put(candidateName, (beanInstance instanceof NullBean ? null : beanInstance));
        } else {
            candidates.put(candidateName, getType(candidateName));
        }
    }

    private boolean indicatesMultipleBeans(Class<?> type) {
        return (type.isArray() || (type.isInterface() &&
                (Collection.class.isAssignableFrom(type) || Map.class.isAssignableFrom(type))));
    }

    Log getLogger() {
        return logger;
    }
}
