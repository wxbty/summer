package ink.zfei.summer.core;

import com.sun.istack.internal.Nullable;
import ink.zfei.summer.beans.*;
import ink.zfei.summer.beans.factory.*;
import ink.zfei.summer.beans.factory.config.*;
import ink.zfei.summer.beans.factory.support.*;
import ink.zfei.summer.core.convert.ConversionService;
import ink.zfei.summer.util.*;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.beans.PropertyDescriptor;
import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.security.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static ink.zfei.summer.beans.factory.config.AutowireCapableBeanFactory.AUTOWIRE_BY_NAME;
import static ink.zfei.summer.beans.factory.config.AutowireCapableBeanFactory.AUTOWIRE_BY_TYPE;
import static ink.zfei.summer.beans.factory.support.AbstractBeanDefinition.AUTOWIRE_CONSTRUCTOR;

public abstract class AbstractApplicationContext implements ApplicationContext, BeanDefinitionRegistry {

    protected final Log logger = LogFactory.getLog(getClass());
    /*
     *  容器对外显示名称，默认类似Object的toString
     */
    private String displayName = ObjectUtils.identityToString(this);

    private Map<String, GenericBeanDefinition> beanDefinitionMap = new ConcurrentHashMap<String, GenericBeanDefinition>();
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

    private volatile List<String> configuationNames = new ArrayList<>(256);

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

    public AbstractApplicationContext() {
    }

    public Map<String, GenericBeanDefinition> getBeanDefinitionMap() {
        return beanDefinitionMap;
    }

    public void refresh() {

        prepareBeanFactory(this);

        //1、从外界获取bean定义信息
        try {
            beanDefinitionMap.putAll(loadBeanDefination());
        } catch (IOException ex) {
            throw new RuntimeException("I/O error parsing bean definition source for " + getDisplayName(), ex);
        }
        beanDefinitionNames = new ArrayList<>(beanDefinitionMap.keySet());

        //Invoke factory processors registered as beans in the context.
        invokeBeanFactoryPostProcessors();

        registerBeanPostProcessors();

        finishBeanFactoryInitialization();


        //4、发布refresh事件，遍历listener,分别执行
        RefreshApplicationEvent event = new RefreshApplicationEvent();
        publishEvent(event);

    }

    public void prepareBeanFactory(AbstractApplicationContext applicationContext) {
        String id = "applicationContextAwareProcessor";
//        GenericBeanDefinition beanDefinition = new GenericBeanDefinition();
//        beanDefinition.setId(id);
//        beanDefinition.setBeanClassName("ink.zfei.summer.core.ApplicationContextAwareProcessor");
//        beanDefinitionMap.put(id, beanDefinition);
        applicationContext.addBeanPostProcessor(id, new ApplicationContextAwareProcessor(this));
    }

    private void invokeBeanFactoryPostProcessors() {

        List<BeanFactoryPostProcessor> beanFactoryPostProcessors = new CopyOnWriteArrayList<>();
        List<String> tmpBeanDefinitionNames = new ArrayList<>(beanDefinitionNames);
        //1、扫描beanDefination,找到BeanFactoryPostProcessor实现，实例化
        //1、遍历beanDefination，把BeanPostProcessors类型的bean
        tmpBeanDefinitionNames.stream().filter(id -> {
            String beanClass = beanDefinitionMap.get(id).getBeanClassName();
            if (StringUtils.isEmpty(beanClass)) {
                return false;
            }
            try {
                Class clazz = Class.forName(beanClass);
                return BeanFactoryPostProcessor.class.isAssignableFrom(clazz);
            } catch (ClassNotFoundException e) {
                return false;
            }
        }).forEach(id -> {
            //2、提前注册，实例化
            BeanFactoryPostProcessor beanFactoryPostProcessor = (BeanFactoryPostProcessor) getBean(id);
            if (beanFactoryPostProcessor instanceof BeanDefinitionRegistryPostProcessor) {
                BeanDefinitionRegistryPostProcessor registryPostProcessor = (BeanDefinitionRegistryPostProcessor) beanFactoryPostProcessor;
                if (this instanceof BeanDefinitionRegistry) {
                    registryPostProcessor.postProcessBeanDefinitionRegistry((BeanDefinitionRegistry) this);
                }
            }

            beanFactoryPostProcessor.postProcessBeanFactory(this);

//            this.beanFactoryPostProcessorMap.put(id, beanFactoryPostProcessor);
            beanFactoryPostProcessors.add(beanFactoryPostProcessor);


        });

        //2、把当前的spring容器传入BeanFactoryPostProcessor，执行invoke方法

    }


    private void registerBeanPostProcessors() {

        //1、遍历beanDefination，把BeanPostProcessors类型的bean
        beanDefinitionNames.stream().filter(id -> {
            String beanClass = beanDefinitionMap.get(id).getBeanClassName();
            if (StringUtils.isEmpty(beanClass)) {
                return false;
            }
            try {
                Class clazz = Class.forName(beanClass);
                return BeanPostProcessor.class.isAssignableFrom(clazz);
            } catch (ClassNotFoundException e) {
                return false;
            }
        }).forEach(id -> {
            //2、提前注册，实例化
            BeanPostProcessor beanPostProcessor = (BeanPostProcessor) getBean(id);
            beanPostProcessorMap.put(id, beanPostProcessor);
            this.beanPostProcessors.add(beanPostProcessor);
        });


    }

    private void registerInstantiationAwareBeanPostProcessors() {

        //1、遍历beanDefination，把instantiationAwareBeanPostProcessor类型的bean
        beanDefinitionNames.stream().filter(id -> {
            String beanClass = beanDefinitionMap.get(id).getBeanClassName();
            if (StringUtils.isEmpty(beanClass)) {
                return false;
            }
            try {
                Class clazz = Class.forName(beanClass);
                if (Arrays.asList(ClassUtils.getAllInterfaces(clazz)).contains(InstantiationAwareBeanPostProcessor.class)) {
                    return true;
                }
            } catch (ClassNotFoundException e) {
                return false;
            }
            return false;
        }).forEach(id -> {
            //2、提前注册，实例化
            InstantiationAwareBeanPostProcessor instantiationAwareBeanPostProcessor = (InstantiationAwareBeanPostProcessor) getBean(id);
            instantiationAwareBeanPostProcessorMap.put(id, instantiationAwareBeanPostProcessor);
            this.instantiationAwareBeanPostProcessors.add(instantiationAwareBeanPostProcessor);
        });


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


    private Object applyPostProcessBeforeInstantiation(Class clazz, String beanName) {

        for (InstantiationAwareBeanPostProcessor processor : getinstantiationAwareBeanPostProcessors()) {
            Object current = processor.postProcessBeforeInstantiation(clazz, beanName);
            if (current != null) {
                return processor.postProcessAfterInitialization(current, beanName);
            }
        }
        return null;
    }

    private Object applyPostProcessAfaterInstantiation(Object existingBean, String beanName) {
        Object result = existingBean;
        for (InstantiationAwareBeanPostProcessor processor : getinstantiationAwareBeanPostProcessors()) {
            Object current = processor.postProcessBeforeInstantiation(result.getClass(), beanName);
            if (current == null) {
                return result;
            }
            result = current;
        }
        return result;
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

    protected abstract Map<String, GenericBeanDefinition> loadBeanDefination() throws IOException;

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
        return doGetBean(name, null);
    }

    private final Map<String, Scope> scopes = new LinkedHashMap<>(8);

    public Object doGetBean(String name, final Object[] args) {

        final String beanName = transformedBeanName(name);
        Object bean;

        // 2. 尝试从缓存中获取bean
        Object sharedInstance = getSingleton(beanName);

        //第一次获取FactoryBean，缓存和参数都为空，第二次获取缓存不空（factoryBean实例），arg空，根据fb.getObject获取实例
        if (sharedInstance != null && args == null) {
            bean = getObjectForBeanInstance(sharedInstance, name, beanName, null);
        } else {
            // 如果我们已经在创建此bean实例，则失败：
            // 一个bean依赖自己自己，循环引用时会出现
            if (isPrototypeCurrentlyInCreation(beanName)) {
                throw new BeanCurrentlyInCreationException(beanName);
            }
            // todo 判断是否存在父容器中.

            final GenericBeanDefinition mbd = (GenericBeanDefinition) getBeanDefinition(beanName);
            //检查是否抽象类
            checkMergedBeanDefinition(mbd, beanName, args);

            // todo depends on.

            // Create bean instance.
            if (mbd.isSingleton()) {
                //缓存中没有，创建bean，放入缓存
                sharedInstance = getSingleton(beanName, () -> createBean(beanName, mbd, args));
                //第一次获取FactoryBean，直接返回FactoryBean实例
                bean = getObjectForBeanInstance(sharedInstance, name, beanName, mbd);
            } else if (mbd.isPrototype()) {
                // It's a prototype -> create a new instance.
                Object prototypeInstance = createBean(beanName, mbd, args);
                bean = getObjectForBeanInstance(prototypeInstance, name, beanName, mbd);
            } else {
                String scopeName = mbd.getScope();
                final Scope scope = this.scopes.get(scopeName);
                if (scope == null) {
                    throw new IllegalStateException("No Scope registered for scope name '" + scopeName + "'");
                }
                try {
                    Object scopedInstance = scope.get(beanName, () -> createBean(beanName, mbd, args));
                    bean = getObjectForBeanInstance(scopedInstance, name, beanName, mbd);
                } catch (IllegalStateException ex) {
                    throw new BeanCreationException(beanName,
                            "Scope '" + scopeName + "' is not active for the current thread; consider " +
                                    "defining a scoped proxy for this bean if you intend to refer to it from a singleton"
                    );
                }
            }

        }

//        GenericBeanDefinition beanDefination = beanDefinitionMap.get(name);
//        try {
//            Class clazz = Class.forName(beanDefination.getBeanClassName());
//            return doGetBean(name, clazz, beanDefination);
//        } catch (ClassNotFoundException | IllegalAccessException | InstantiationException e) {
//            e.printStackTrace();
//        } catch (NullPointerException e) {
//            throw e;
//        }
        return bean;
    }

    //真正创建bean第一步，先执行实例化前置处理，再解析beanClass（如果有）
    public Object createBean(String beanName, GenericBeanDefinition mbd, Object[] args) {

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

    private final ConcurrentMap<String, BeanWrapper> factoryBeanInstanceCache = new ConcurrentHashMap<>();

    //真正创建bean第二步，创建实例->融合@AutoWired @Resource的属性->属性依赖注入->初始化
    protected Object doCreateBean(final String beanName, final GenericBeanDefinition mbd, final Object[] args) {

        // Instantiate the bean.
        BeanWrapper instanceWrapper = null;
        if (mbd.isSingleton()) {
            instanceWrapper = this.factoryBeanInstanceCache.remove(beanName);
        }
        if (instanceWrapper == null) {
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

        // Shortcut when re-creating the same bean...
        boolean resolved = false;
        boolean autowireNecessary = false;
        if (args == null) {
            if (mbd.resolvedConstructorOrFactoryMethod != null) {
                resolved = true;
                autowireNecessary = mbd.constructorArgumentsResolved;
            }
        }
        if (resolved) {
            if (autowireNecessary) {
                return autowireConstructor(beanName, mbd, null, null);
            } else {
                return instantiateBean(beanName, mbd);
            }
        }

        // Candidate constructors for autowiring?
        Constructor<?>[] ctors = determineConstructorsFromBeanPostProcessors(beanClass, beanName);
        if (ctors != null || mbd.getResolvedAutowireMode() == AUTOWIRE_CONSTRUCTOR ||
                mbd.hasConstructorArgumentValues() || !ObjectUtils.isEmpty(args)) {
            return autowireConstructor(beanName, mbd, ctors, args);
        }

        // Preferred constructors for default construction?
        ctors = mbd.getPreferredConstructors();
        if (ctors != null) {
            return autowireConstructor(beanName, mbd, ctors, null);
        }

        // No special handling: simply use no-arg constructor.
        return instantiateBean(beanName, mbd);
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

    protected BeanWrapper autowireConstructor(
            String beanName, GenericBeanDefinition mbd, @Nullable Constructor<?>[] ctors, @Nullable Object[] explicitArgs) {

        return new ConstructorResolver(this).autowireConstructor(beanName, mbd, ctors, explicitArgs);
    }

    protected BeanWrapper instantiateUsingFactoryMethod(
            String beanName, GenericBeanDefinition mbd, @Nullable Object[] explicitArgs) {

        return new ConstructorResolver(this).instantiateUsingFactoryMethod(beanName, mbd, explicitArgs);
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

    private void checkMergedBeanDefinition(BeanDefinition mbd, String beanName, Object[] args) {

        if (mbd.isAbstract()) {
            throw new BeanIsAbstractException(beanName);
        }
    }

    private boolean isPrototypeCurrentlyInCreation(String beanName) {
        //todo 判断bean是否创建中
        return false;
    }

    @Override
    public Object getBean(Class clazz) {

        List<Object> list = singletonObjects.values().stream().filter(bean -> bean.getClass() == clazz).collect(Collectors.toList());
        if (list.size() == 0) {
            return null;
        } else if (list.size() == 1) {
            return list.get(0);
        } else
            return list;
    }


    @Override
    public void registerBeanDefinition(String beanName, GenericBeanDefinition beanDefinition) {
        beanDefinitionMap.put(beanName, beanDefinition);
        beanDefinitionNames.add(beanName);
    }

    @Override
    public void registerBeanDefinition(GenericBeanDefinition beanDefinition) {
        beanDefinitionMap.put(beanDefinition.getId(), beanDefinition);
        beanDefinitionNames.add(beanDefinition.getId());
    }

    public void registerConfiguation(String configuationName) {
        configuationNames.add(configuationName);
    }

    @Override
    public void removeBeanDefinition(String beanName) {
        beanDefinitionMap.remove(beanName);
        beanDefinitionNames.remove(beanName);
    }

    @Override
    public BeanDefinition getBeanDefinition(String beanName) {
        BeanDefinition bd = this.beanDefinitionMap.get(beanName);
        if (bd == null) {
            if (logger.isTraceEnabled()) {
                logger.trace("No bean named '" + beanName + "' found in " + this);
            }
            throw new NoSuchBeanDefinitionException(beanName);
        }
        return new GenericBeanDefinition(bd);
    }


    @Override
    public void addBeanPostProcessor(String id, BeanPostProcessor beanPostProcessor) {
        this.beanPostProcessors.add(beanPostProcessor);
        singletonObjects.put(id, beanPostProcessor);
    }

    public List<String> getConfiguationNames() {
        return configuationNames;
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

    public boolean containsBean(String name) {
        String beanName = transformedBeanName(name);
        if (containsSingleton(beanName) || containsBeanDefinition(beanName)) {
            return (!BeanFactoryUtils.isFactoryDereference(name) || isFactoryBean(name));
        }
        return false;
        //todo check parent.
    }

    public boolean containsSingleton(String beanName) {
        return this.singletonObjects.containsKey(beanName);
    }

    public boolean containsBeanDefinition(String beanName) {
        Assert.notNull(beanName, "Bean name must not be null");
        return this.beanDefinitionMap.containsKey(beanName);
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

    public void initBeanWrapper(BeanWrapper bw) {
        bw.setConversionService(getConversionService());
//        registerCustomEditors(bw);
    }

    public ConversionService getConversionService() {
        return this.conversionService;
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

    public NamedBeanHolder<?> resolveNamedBean(Class<?> beanType) {
        return null;
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

    private Optional<?> createOptionalDependency(
            DependencyDescriptor descriptor, @Nullable String beanName, final Object... args) {

        DependencyDescriptor descriptorToUse = new NestedDependencyDescriptor(descriptor) {
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

    public AutowireCandidateResolver getAutowireCandidateResolver() {
        return this.autowireCandidateResolver;
    }

    private static class NestedDependencyDescriptor extends DependencyDescriptor {

        public NestedDependencyDescriptor(DependencyDescriptor original) {
            super(original);
            increaseNestingLevel();
        }
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

    protected Map<String, Object> findAutowireCandidates(
            @Nullable String beanName, Class<?> requiredType, DependencyDescriptor descriptor) {

        String[] candidateNames = BeanFactoryUtils.beanNamesForTypeIncludingAncestors(
                this, requiredType);
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

    private boolean indicatesMultipleBeans(Class<?> type) {
        return (type.isArray() || (type.isInterface() &&
                (Collection.class.isAssignableFrom(type) || Map.class.isAssignableFrom(type))));
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

    private static class MultiElementDescriptor extends NestedDependencyDescriptor {

        public MultiElementDescriptor(DependencyDescriptor original) {
            super(original);
        }
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

    private boolean isSelfReference(@Nullable String beanName, @Nullable String candidateName) {
        return (beanName != null && candidateName != null &&
                (beanName.equals(candidateName) || (containsBeanDefinition(candidateName) &&
                        beanName.equals(getBeanDefinition(candidateName).getFactoryBeanName()))));
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

    private Comparator<Object> adaptDependencyComparator(Map<String, ?> matchingBeans) {
        Comparator<Object> comparator = getDependencyComparator();
        if (comparator instanceof OrderComparator) {
            return ((OrderComparator) comparator).withSourceProvider(
                    createFactoryAwareOrderSourceProvider(matchingBeans));
        } else {
            return comparator;
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

    protected boolean matchesBeanName(String beanName, @Nullable String candidateName) {
        return (candidateName != null &&
                (candidateName.equals(beanName)));
    }
}
