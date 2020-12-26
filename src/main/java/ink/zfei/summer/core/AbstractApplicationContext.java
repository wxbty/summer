package ink.zfei.summer.core;

import ink.zfei.summer.beans.*;
import ink.zfei.summer.beans.factory.*;
import ink.zfei.summer.beans.factory.config.*;
import ink.zfei.summer.beans.factory.support.*;
import ink.zfei.summer.context.ConfigurableApplicationContext;
import ink.zfei.summer.context.support.PostProcessorRegistrationDelegate;
import ink.zfei.summer.core.convert.ConversionService;
import ink.zfei.summer.core.env.ConfigurableEnvironment;
import ink.zfei.summer.lang.Nullable;
import ink.zfei.summer.util.*;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.beans.PropertyDescriptor;
import java.io.IOException;
import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static ink.zfei.summer.beans.factory.config.AutowireCapableBeanFactory.AUTOWIRE_BY_NAME;
import static ink.zfei.summer.beans.factory.config.AutowireCapableBeanFactory.AUTOWIRE_BY_TYPE;
import static ink.zfei.summer.beans.factory.support.AbstractBeanDefinition.AUTOWIRE_CONSTRUCTOR;

public abstract class AbstractApplicationContext implements ConfigurableApplicationContext, BeanDefinitionRegistry {

    protected final Log logger = LogFactory.getLog(getClass());

    private String id = ObjectUtils.identityToString(this);

    @Nullable
    private ApplicationContext parent;
    /*
     *  容器对外显示名称，默认类似Object的toString
     */
    private String displayName = ObjectUtils.identityToString(this);

    //    private Map<String, GenericBeanDefinition> beanDefinitionMap = new ConcurrentHashMap<String, GenericBeanDefinition>();
    private final Map<String, Object> singletonObjects = new ConcurrentHashMap<String, Object>();
    /* todo 实例化半成品集合 */
    private final Map<String, Object> earlySingletonObjects = new ConcurrentHashMap<String, Object>();
    private Map<String, BeanPostProcessor> beanPostProcessorMap = new ConcurrentHashMap<String, BeanPostProcessor>();
    protected Map<String, BeanFactoryPostProcessor> beanFactoryPostProcessorMap = new ConcurrentHashMap<String, BeanFactoryPostProcessor>();
    private final List<BeanPostProcessor> beanPostProcessors = new CopyOnWriteArrayList<>();
    private final List<InstantiationAwareBeanPostProcessor> instantiationAwareBeanPostProcessors = new CopyOnWriteArrayList<>();
    private Map<String, InstantiationAwareBeanPostProcessor> instantiationAwareBeanPostProcessorMap = new ConcurrentHashMap<String, InstantiationAwareBeanPostProcessor>();
    private InstantiationStrategy instantiationStrategy = new CglibSubclassingInstantiationStrategy();

    //@bean类型class对应的method集合，缓存下来不需要每次都解析
    private final ConcurrentMap<Class<?>, Method[]> factoryMethodCandidateCache = new ConcurrentHashMap<>();


    private volatile List<String> beanDefinitionNames = new ArrayList<>(256);

    private volatile List<String> configurationNames = new ArrayList<>(256);

    private ParameterNameDiscoverer parameterNameDiscoverer = new DefaultParameterNameDiscoverer();

    private TypeConverter typeConverter;

    private ConversionService conversionService;
    private final List<StringValueResolver> embeddedValueResolvers = new CopyOnWriteArrayList<>();
    private AutowireCandidateResolver autowireCandidateResolver = SimpleAutowireCandidateResolver.INSTANCE;
    private final Map<String, Set<String>> dependentBeanMap = new ConcurrentHashMap<>(64);
    private final Map<String, Set<String>> dependenciesForBeanMap = new ConcurrentHashMap<>(64);
    private final Map<Class<?>, Object> resolvableDependencies = new ConcurrentHashMap<>(16);
    private volatile boolean hasInstantiationAwareBeanPostProcessors;
    private Comparator<Object> dependencyComparator;
    private final List<BeanFactoryPostProcessor> beanFactoryPostProcessors = new ArrayList<>();
    private final Set<ApplicationListener> applicationListeners = new LinkedHashSet<>();
    private ConfigurableEnvironment environment;

    public AbstractApplicationContext() {
    }

    public void refresh() {

        ConfigurableListableBeanFactory beanFactory = obtainFreshBeanFactory();

        prepareBeanFactory(beanFactory);

        //1、从外界获取bean定义信息
//        try {
//            beanDefinitionMap.putAll(loadBeanDefination());
//        } catch (IOException ex) {
//            throw new RuntimeException("I/O error parsing bean definition source for " + getDisplayName(), ex);
//        }
//        beanDefinitionNames = new ArrayList<>(beanDefinitionMap.keySet());

        //Invoke factory processors registered as beans in the context.
        invokeBeanFactoryPostProcessors(beanFactory);

        registerBeanPostProcessors(beanFactory);

        finishBeanFactoryInitialization();


        //4、发布refresh事件，遍历listener,分别执行
        RefreshApplicationEvent event = new RefreshApplicationEvent();
        publishEvent(event);

    }

    public String[] getAliases(String name) {
        return new String[]{name};
    }

    public void prepareBeanFactory(ConfigurableListableBeanFactory beanFactory) {
        String id = "applicationContextAwareProcessor";
//        GenericBeanDefinition beanDefinition = new GenericBeanDefinition();
//        beanDefinition.setId(id);
//        beanDefinition.setBeanClassName("ink.zfei.summer.core.ApplicationContextAwareProcessor");
//        beanDefinitionMap.put(id, beanDefinition);
        beanFactory.addBeanPostProcessor(id, new ApplicationContextAwareProcessor(this));
    }

    public List<BeanFactoryPostProcessor> getBeanFactoryPostProcessors() {
        return this.beanFactoryPostProcessors;
    }

    private void invokeBeanFactoryPostProcessors(ConfigurableListableBeanFactory beanFactory) {
        PostProcessorRegistrationDelegate.invokeBeanFactoryPostProcessors(beanFactory, getBeanFactoryPostProcessors());

    }

    protected void registerBeanPostProcessors(ConfigurableListableBeanFactory beanFactory) {
        PostProcessorRegistrationDelegate.registerBeanPostProcessors(beanFactory, this);
    }


    protected ConfigurableListableBeanFactory obtainFreshBeanFactory() {
        refreshBeanFactory();
        return getBeanFactory();
    }

    private void finishBeanFactoryInitialization() {

        //todo ConversionService 类型转换
        //todo 内置属性解析器

        preInstantiateSingletons();

    }

    private void preInstantiateSingletons() {

        List<String> beanNames = new ArrayList<>(this.beanDefinitionNames);
        //2、遍历bean定义信息，实例化bean

        for (String beanName : beanNames) {
            if (beanPostProcessors.contains(beanName)) {
                continue;
            }
            //todo 过滤 if (!bd.isAbstract() && bd.isSingleton() && !bd.isLazyInit())

            if (isFactoryBean(beanName)) {
                Object bean = getBean(FACTORY_BEAN_PREFIX + beanName);
                if (bean instanceof FactoryBean) {
                    //todo SmartFactoryBean
                    getBean(beanName);
                }
            } else {
                getBean(beanName);
            }

//            GenericBeanDefinition mbd = beanDefinitionMap.get(beanName);
//            GenericBeanDefinition mbdToUse = mbd;
//            Class<?> resolvedClass = resolveBeanClass(mbd, beanName);
//            if (resolvedClass != null && !mbd.hasBeanClass() && mbd.getBeanClassName() != null) {
//                mbdToUse = new GenericBeanDefinition(mbd);
//                mbd.setBeanClass(resolvedClass);
//            }
//
//
//            //postProcessBeforeInstantiation
//            Object bean = applyPostProcessBeforeInstantiation(resolvedClass, beanName);
//            if (bean != null) {
//                singletonObjects.put(beanName, bean);
//            } else {
//                Object wrappedBean = getBean(beanName, resolvedClass, mbd);
//                //postProcessafterInstantiation
//                applyPostProcessAfaterInstantiation(resolvedClass, beanName);
//
//
//                populateBean(beanName, mbd, wrappedBean);
//                initializeBean(beanName, mbd, resolvedClass, wrappedBean);
//
//            }
        }

    }

    /**
     * todo 第三个参数不知道干什么用
     */
    private Class<?> resolveBeanClass(GenericBeanDefinition mbd, String beanName, final Class<?>... typesToMatch) {

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

    public Object evaluateBeanDefinitionString(String className, GenericBeanDefinition mbd) {

        return className;
    }

    public ClassLoader getBeanClassLoader() {
        return Thread.currentThread().getContextClassLoader();
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

    private Object applyBeanPostProcessorsAfterInitialization(Object existingBean, String beanName) {


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

    private List<BeanPostProcessor> getBeanPostProcessors() {
        return beanPostProcessors;
    }

    private List<InstantiationAwareBeanPostProcessor> getinstantiationAwareBeanPostProcessors() {
        return instantiationAwareBeanPostProcessors;
    }

    ;

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

//    private Object getBean(String id) throws InstantiationException, IllegalAccessException {
//        Object bean;
//
//        GenericBeanDefinition beanDefination = beanDefinitionMap.get(id);
//
//
//        if ("prototype".equals(beanDefination.getScope())) {
//            bean = doGetBean(id, clazz, beanDefination);
//        } else if ("singleton".equals(beanDefination.getScope())) {
//            if (singletonObjects.containsKey(id)) {
//                bean = singletonObjects.get(id);
//            } else {
//                bean = doGetBean(id, clazz, beanDefination);
//                singletonObjects.put(beanDefination.getId(), bean);
//            }
//        } else {
//            throw new RuntimeException("当前只支持singleton和prototype！");
//        }
//
//        return bean;
//    }

    //真正实例化生成bean
    private Object doGetBean(String id, Class clazz, GenericBeanDefinition mbd) throws InstantiationException, IllegalAccessException {
        Object bean;
        if (isFactoryBean(mbd.getId())) {
            FactoryBean factoryBean;
            if (singletonObjects.containsKey(BeanFactory.FACTORY_BEAN_PREFIX + id)) {
                factoryBean = (FactoryBean) singletonObjects.get(BeanFactory.FACTORY_BEAN_PREFIX + id);
            } else {
                if (mbd.getConstrucrorParm() != null) {
                    try {
                        Constructor c = clazz.getConstructor(String.class);
                        factoryBean = (FactoryBean) c.newInstance(mbd.getConstrucrorParm());
                    } catch (NoSuchMethodException | InvocationTargetException e) {
                        throw new RuntimeException(e);
                    }

                } else {
                    factoryBean = (FactoryBean) clazz.newInstance();
                }
                singletonObjects.put(BeanFactory.FACTORY_BEAN_PREFIX + id, factoryBean);
            }
            return factoryBean.getObject();
        }

        if (mbd.getFactoryMethodName() != null) {
            String factoryBeanName = mbd.getFactoryBeanName();
            String methodName = mbd.getFactoryMethodName();
            Object factoryBean = getBean(factoryBeanName);
            try {
                Method factoryMethod = factoryBean.getClass().getDeclaredMethod(methodName);
                Object result = factoryMethod.invoke(factoryBean);
                return result;
            } catch (NoSuchMethodException | InvocationTargetException e) {
                e.printStackTrace();
            }

        }


        return clazz.newInstance();
    }

    /*
     *  如果已经实例化，用bean到instanceOf判断，否则bd判断
     * */
    private boolean isFactoryBean(String name) {
        String beanName = transformedBeanName(name);
        Object beanInstance = getSingleton(beanName);
        if (beanInstance != null) {
            return (beanInstance instanceof FactoryBean);
        }
        //todo 判断是否在父容器中


        return isFactoryBean(beanName, (GenericBeanDefinition) getBeanDefinition(beanName));
//        return FactoryBean.class.isAssignableFrom(clazz);
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

    /*
     * 判断我们传递的name是不是以“&”开头的，要是的需要去掉再返回。需要注意的是，我们传递的name可能是bean的别名，
     * 也可能是BeanFactory，所以transformedBeanName的操作的目的就是取到真正的bean的名字，以便开始后续的流程。
     * */
    private String transformedBeanName(String name) {
        return name;
    }

//    protected abstract Map<String, GenericBeanDefinition> loadBeanDefination() throws IOException;

    @Override
    public void publishEvent(ApplicationEvent event) {

        for (ApplicationListener applicationListener : getApplicationListeners()) {
            applicationListener.onApplicationEvent(event);
        }

    }

    protected Collection<ApplicationListener> getApplicationListeners() {
        List<ApplicationListener> listeners = singletonObjects.entrySet().stream().filter(entry -> (entry.getValue() instanceof ApplicationListener)).map(entry -> (ApplicationListener) entry.getValue()).collect(Collectors.toList());
        return listeners;
    }

    @Override
    public Object getBean(String name) {
        //多个参数
//        return doGetBean(name, null);
        return getBeanFactory().getBean(name);
    }

    private final Map<String, Scope> scopes = new LinkedHashMap<>(8);

    public InstantiationStrategy getInstantiationStrategy() {
        return this.instantiationStrategy;
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

    @Override
    public Class<?> getType(String name) throws NoSuchBeanDefinitionException {
        String beanName = transformedBeanName(name);

        // Check manually registered singletons.
        Object beanInstance = getSingleton(beanName);
        if (beanInstance != null) {
            if (beanInstance instanceof FactoryBean) {
                return getTypeForFactoryBean((FactoryBean<?>) beanInstance);
            } else {
                return beanInstance.getClass();
            }
        }

        // todo 查找父容器.


        GenericBeanDefinition mbd = (GenericBeanDefinition) getBeanDefinition(beanName);

        Class<?> beanClass = predictBeanType(beanName, mbd);

        return beanClass;
    }

    protected Class<?> getTypeForFactoryBean(final FactoryBean<?> factoryBean) {

        return factoryBean.getObjectType();

    }


    @Override
    public void addBeanPostProcessor(String id, BeanPostProcessor beanPostProcessor) {
        this.beanPostProcessors.remove(beanPostProcessor);
        if (beanPostProcessor instanceof InstantiationAwareBeanPostProcessor) {
            this.hasInstantiationAwareBeanPostProcessors = true;
        }
        this.beanPostProcessors.add(beanPostProcessor);
        singletonObjects.put(id, beanPostProcessor);
    }


    public List<String> getConfigurationNames() {
        return configurationNames;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return this.displayName;
    }

    protected Object getSingleton(String beanName) {
        // 从一级缓存获取，key=beanName value=bean
        Object singletonObject = this.singletonObjects.get(beanName);
        // singletonObject为空，且该bean正在创建中（假设不在创建中那么肯定是还没被实例化以及提前曝光的，继续查找没有意义）
        if (singletonObject == null) { //todo 加判断条件 创建中
            synchronized (this.singletonObjects) {
                // 从二级缓存获取，key=beanName value=bean
                singletonObject = this.earlySingletonObjects.get(beanName);
                //todo 循环依赖，三级缓存
            }
        }
        return singletonObject;
    }

    public Object getSingleton(String beanName, ObjectFactory<?> singletonFactory) {
        Assert.notNull(beanName, "Bean name must not be null");
        synchronized (this.singletonObjects) {
            Object singletonObject = this.singletonObjects.get(beanName);
            if (singletonObject == null) {
                //todo 判断是否在创建中
                if (logger.isDebugEnabled()) {
                    logger.debug("Creating shared instance of singleton bean '" + beanName + "'");
                }
                beforeSingletonCreation(beanName);
                boolean newSingleton = false;
                try {
                    singletonObject = singletonFactory.getObject();
                    newSingleton = true;
                } catch (IllegalStateException ex) {
                    // Has the singleton object implicitly appeared in the meantime ->
                    // if yes, proceed with it since the exception indicates that state.
                    singletonObject = this.singletonObjects.get(beanName);
                    if (singletonObject == null) {
                        throw ex;
                    }
                }
                if (newSingleton) {
                    addSingleton(beanName, singletonObject);
                }
            }
            return singletonObject;
        }
    }

    protected void addSingleton(String beanName, Object singletonObject) {
        synchronized (this.singletonObjects) {
            this.singletonObjects.put(beanName, singletonObject);
            this.earlySingletonObjects.remove(beanName);
        }
    }

    protected void beforeSingletonCreation(String beanName) {
        //todo add bean创建状态解决循环依赖
    }

    public ParameterNameDiscoverer getParameterNameDiscoverer() {
        return this.parameterNameDiscoverer;
    }


    public Log getLogger() {
        return logger;
    }

    protected void applyMergedBeanDefinitionPostProcessors(GenericBeanDefinition mbd, Class<?> beanType, String beanName) {
        for (BeanPostProcessor bp : getBeanPostProcessors()) {
            if (bp instanceof MergedBeanDefinitionPostProcessor) {
                MergedBeanDefinitionPostProcessor bdp = (MergedBeanDefinitionPostProcessor) bp;
                bdp.postProcessMergedBeanDefinition(mbd, beanType, beanName);
            }
        }
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

    @Override
    public boolean containsBean(String name) {
        return getBeanFactory().containsBean(name);
    }

    public boolean containsSingleton(String beanName) {
        return this.singletonObjects.containsKey(beanName);
    }

    @Override
    public boolean containsBeanDefinition(String beanName) {
        return getBeanFactory().containsBeanDefinition(beanName);
    }

    public TypeConverter getCustomTypeConverter() {
        return this.typeConverter;
    }

    public void initBeanWrapper(BeanWrapper bw) {
        bw.setConversionService(getConversionService());
//        registerCustomEditors(bw);
    }

    public ConversionService getConversionService() {
        return this.conversionService;
    }

    public AutowireCandidateResolver getAutowireCandidateResolver() {
        return this.autowireCandidateResolver;
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

    private final Map<String, Object> factoryBeanObjectCache = new ConcurrentHashMap<>(16);

    public Object getObjectFromFactoryBean(FactoryBean<?> factory, String beanName) {
        if (factory.isSingleton() && containsSingleton(beanName)) {
            Object object = this.factoryBeanObjectCache.get(beanName);
            if (object == null) {
                object = doGetObjectFromFactoryBean(factory, beanName);
                // Only post-process and store if not put there already during getObject() call above
                // (e.g. because of circular reference processing triggered by custom getBean calls)
                Object alreadyThere = this.factoryBeanObjectCache.get(beanName);
                if (alreadyThere != null) {
                    object = alreadyThere;
                } else {
                    if (containsSingleton(beanName)) {
                        this.factoryBeanObjectCache.put(beanName, object);
                    }
                }
            }
            return object;
        } else {
            return doGetObjectFromFactoryBean(factory, beanName);
        }
    }

    private Object doGetObjectFromFactoryBean(final FactoryBean<?> factory, final String beanName)
            throws BeanCreationException {

        Object object = factory.getObject();
        // Do not accept a null value for a FactoryBean that's not fully
        // initialized yet: Many FactoryBeans just return null then.
        if (object == null) {

            object = new NullBean();
        }
        return object;
    }

    protected boolean hasInstantiationAwareBeanPostProcessors() {
        return this.hasInstantiationAwareBeanPostProcessors;
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

    private static class AutowireByTypeDependencyDescriptor extends DependencyDescriptor {

        public AutowireByTypeDependencyDescriptor(MethodParameter methodParameter, boolean eager) {
            super(methodParameter, false, eager);
        }

        @Override
        public String getDependencyName() {
            return null;
        }
    }


    private boolean indicatesMultipleBeans(Class<?> type) {
        return (type.isArray() || (type.isInterface() &&
                (Collection.class.isAssignableFrom(type) || Map.class.isAssignableFrom(type))));
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

    private boolean isRequired(DependencyDescriptor descriptor) {
        return getAutowireCandidateResolver().isRequired(descriptor);
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


    public String[] getBeanNamesForType(@Nullable Class<?> type) {
        return getBeanNamesForType(type, true, true);
    }


    public String[] getBeanNamesForType(@Nullable Class<?> type, boolean includeNonSingletons, boolean allowEagerInit) {
        Map<Class<?>, String[]> cache =
                new HashMap<>();
        String[] resolvedBeanNames = cache.get(type);
        if (resolvedBeanNames != null) {
            return resolvedBeanNames;
        }
        resolvedBeanNames = doGetBeanNamesForType(ResolvableType.forRawClass(type), includeNonSingletons, true);
        if (ClassUtils.isCacheSafe(type, getBeanClassLoader())) {
            cache.put(type, resolvedBeanNames);
        }
        return resolvedBeanNames;
    }

    private String[] doGetBeanNamesForType(ResolvableType type, boolean includeNonSingletons, boolean allowEagerInit) {
        List<String> result = new ArrayList<>();

        // Check all bean definitions.
        for (String beanName : this.beanDefinitionNames) {
            // Only consider bean as eligible if the bean name
            // is not defined as alias for some other bean.
            GenericBeanDefinition mbd = (GenericBeanDefinition) getBeanDefinition(beanName);
            // Only check bean definition if it is complete.
            if (!mbd.isAbstract()) {
                boolean isFactoryBean = isFactoryBean(beanName, mbd);
                BeanDefinitionHolder dbd = mbd.getDecoratedDefinition();
                boolean matchFound = false;
                boolean allowFactoryBeanInit = allowEagerInit || containsSingleton(beanName);
                if (!isFactoryBean) {
                    if (includeNonSingletons || isSingleton(beanName)) {
                        matchFound = isTypeMatch(beanName, type, allowFactoryBeanInit);
                    }
                } else {
                    if (includeNonSingletons ||
                            (allowFactoryBeanInit && isSingleton(beanName))) {
                        matchFound = isTypeMatch(beanName, type, allowFactoryBeanInit);
                    }
                    if (!matchFound) {
                        // In case of FactoryBean, try to match FactoryBean instance itself next.
                        beanName = FACTORY_BEAN_PREFIX + beanName;
                        matchFound = isTypeMatch(beanName, type, allowFactoryBeanInit);
                    }
                }
                if (matchFound) {
                    result.add(beanName);
                }
            }
        }


        return StringUtils.toStringArray(result);
    }

    public boolean isSingleton(String name) throws NoSuchBeanDefinitionException {
        String beanName = transformedBeanName(name);

        Object beanInstance = getSingleton(beanName);
        if (beanInstance != null) {
            if (beanInstance instanceof FactoryBean) {
                return (BeanFactoryUtils.isFactoryDereference(name) || ((FactoryBean<?>) beanInstance).isSingleton());
            } else {
                return !BeanFactoryUtils.isFactoryDereference(name);
            }
        }


        GenericBeanDefinition mbd = (GenericBeanDefinition) getBeanDefinition(beanName);

        // In case of FactoryBean, return singleton status of created object if not a dereference.
        if (mbd.isSingleton()) {
            if (isFactoryBean(beanName, mbd)) {
                if (BeanFactoryUtils.isFactoryDereference(name)) {
                    return true;
                }
                FactoryBean<?> factoryBean = (FactoryBean<?>) getBean(FACTORY_BEAN_PREFIX + beanName);
                return factoryBean.isSingleton();
            } else {
                return !BeanFactoryUtils.isFactoryDereference(name);
            }
        } else {
            return false;
        }
    }

    public boolean isTypeMatch(String name, Class<?> typeToMatch) throws NoSuchBeanDefinitionException {
        return isTypeMatch(name, ResolvableType.forRawClass(typeToMatch));
    }

    public boolean isTypeMatch(String name, ResolvableType typeToMatch) throws NoSuchBeanDefinitionException {
        return isTypeMatch(name, typeToMatch, true);
    }

    protected boolean isTypeMatch(String name, ResolvableType typeToMatch, boolean allowFactoryBeanInit)
            throws NoSuchBeanDefinitionException {

        String beanName = transformedBeanName(name);
        boolean isFactoryDereference = BeanFactoryUtils.isFactoryDereference(name);

        // Check manually registered singletons.
        Object beanInstance = getSingleton(beanName);
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
                    GenericBeanDefinition mbd = (GenericBeanDefinition) getBeanDefinition(beanName);
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
        GenericBeanDefinition mbd = (GenericBeanDefinition) getBeanDefinition(beanName);
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
        if (!isFactoryDereference && dbd != null && isFactoryBean(beanName)) {
            // We should only attempt if the user explicitly set lazy-init to true
            // and we know the merged bean definition is for a factory bean.
            if (allowFactoryBeanInit) {
                GenericBeanDefinition tbd = (GenericBeanDefinition) getBeanDefinition(dbd.getBeanName());
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

    protected ResolvableType getTypeForFactoryBean(String beanName, GenericBeanDefinition mbd, boolean allowInit) {
        ResolvableType result = getTypeForFactoryBeanFromAttributes(mbd);
        if (result != ResolvableType.NONE) {
            return result;
        }

        if (allowInit && mbd.isSingleton()) {
            try {
                FactoryBean<?> factoryBean = (FactoryBean) doGetBean(FACTORY_BEAN_PREFIX + beanName, FactoryBean.class, null);
                Class<?> objectType = getTypeForFactoryBean(factoryBean);
                return (objectType != null) ? ResolvableType.forClass(objectType) : ResolvableType.NONE;
            } catch (IllegalAccessException | InstantiationException e) {
                e.printStackTrace();
            }
        }
        return ResolvableType.NONE;
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

    protected boolean matchesBeanName(String beanName, @Nullable String candidateName) {
        return (candidateName != null &&
                (candidateName.equals(beanName)));
    }

    @Override
    public <T> T getBean(String name, Class<T> requiredType) {
        return getBeanFactory().getBean(name, requiredType);
    }

    @Override
    public Object getBean(String name, Object... args) {
        return getBeanFactory().getBean(name, args);
    }

    @Override
    public <T> T getBean(Class<T> requiredType) {
        return getBeanFactory().getBean(requiredType);
    }


    public abstract ConfigurableListableBeanFactory getBeanFactory() throws IllegalStateException;

    protected abstract void refreshBeanFactory();

    protected abstract void closeBeanFactory();

    @Override
    public void setId(String id) {
        this.id = id;
    }

    @Override
    public String getId() {
        return this.id;
    }

    @Override
    public void addBeanFactoryPostProcessor(BeanFactoryPostProcessor postProcessor) {
        Assert.notNull(postProcessor, "BeanFactoryPostProcessor must not be null");
        this.beanFactoryPostProcessors.add(postProcessor);
    }

    @Override
    public void addApplicationListener(ApplicationListener listener) {
        Assert.notNull(listener, "ApplicationListener must not be null");
//        if (this.applicationEventMulticaster != null) {
//            this.applicationEventMulticaster.addApplicationListener(listener);
//        }
        this.applicationListeners.add(listener);
    }

    @Override
    public String getApplicationName() {
        return "";
    }

    @Override
    public int getBeanDefinitionCount() {
        return getBeanFactory().getBeanDefinitionCount();
    }

    @Override
    public void setEnvironment(ConfigurableEnvironment environment) {
        this.environment = environment;
    }

    @Override
    @Nullable
    public ApplicationContext getParent() {
        return this.parent;
    }

    public BeanFactory getParentBeanFactory() {
        return getParent();
    }

    @Override
    public String[] getBeanDefinitionNames() {
        return getBeanFactory().getBeanDefinitionNames();
    }

    @Override
    public String[] getBeanNamesForType(ResolvableType type) {
//        assertBeanFactoryActive();
        return getBeanFactory().getBeanNamesForType(type);
    }

    @Override
    public boolean containsLocalBean(String name) {
        return getBeanFactory().containsLocalBean(name);
    }

    @Override
    @Nullable
    public <A extends Annotation> A findAnnotationOnBean(String beanName, Class<A> annotationType)
            throws NoSuchBeanDefinitionException {

        return getBeanFactory().findAnnotationOnBean(beanName, annotationType);
    }

    @Override
    @Nullable
    public Class<?> getType(String name, boolean allowFactoryBeanInit) {
        return getBeanFactory().getType(name, allowFactoryBeanInit);
    }

}