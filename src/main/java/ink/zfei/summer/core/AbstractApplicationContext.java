package ink.zfei.summer.core;

import ink.zfei.summer.beans.*;
import ink.zfei.summer.beans.factory.BeanFactory;
import ink.zfei.summer.beans.factory.BeanNameAware;
import ink.zfei.summer.beans.factory.FactoryBean;
import ink.zfei.summer.beans.factory.InitializingBean;
import ink.zfei.summer.util.ClassUtils;
import ink.zfei.summer.util.ObjectUtils;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

public abstract class AbstractApplicationContext implements ApplicationContext, BeanDefinitionRegistry {


    /*
     *  容器对外显示名称，默认类似Object的toString
     */
    private String displayName = ObjectUtils.identityToString(this);

    protected Map<String, GenericBeanDefinition> beanDefinationMap = new ConcurrentHashMap<String, GenericBeanDefinition>();
    protected Map<String, Object> beanSingleMap = new ConcurrentHashMap<String, Object>();
    protected Map<String, BeanPostProcessor> beanPostProcessorMap = new ConcurrentHashMap<String, BeanPostProcessor>();
    protected Map<String, BeanFactoryPostProcessor> beanFactoryPostProcessorMap = new ConcurrentHashMap<String, BeanFactoryPostProcessor>();
    private final List<BeanPostProcessor> beanPostProcessors = new CopyOnWriteArrayList<>();
    private final List<InstantiationAwareBeanPostProcessor> instantiationAwareBeanPostProcessors = new CopyOnWriteArrayList<>();
    protected Map<String, InstantiationAwareBeanPostProcessor> instantiationAwareBeanPostProcessorMap = new ConcurrentHashMap<String, InstantiationAwareBeanPostProcessor>();


    private volatile List<String> beanDefinitionNames = new ArrayList<>(256);

    private volatile List<String> configuationNames = new ArrayList<>(256);


    public AbstractApplicationContext() {
    }

    public Map<String, GenericBeanDefinition> getBeanDefinationMap() {
        return beanDefinationMap;
    }

    public void refresh() {

        prepareBeanFactory(this);

        //1、从外界获取bean定义信息
        try {
            beanDefinationMap.putAll(loadBeanDefination());
        } catch (IOException ex) {
            throw new RuntimeException("I/O error parsing bean definition source for " + getDisplayName(), ex);
        }
        beanDefinitionNames = new ArrayList<>(beanDefinationMap.keySet());

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
        GenericBeanDefinition beanDefinition = new GenericBeanDefinition();
        beanDefinition.setId(id);
        beanDefinition.setBeanClassName("ink.zfei.summer.core.ApplicationContextAwareProcessor");
        beanDefinationMap.put(id, beanDefinition);
        applicationContext.addBeanPostProcessor(id, new ApplicationContextAwareProcessor(this));
    }

    private void invokeBeanFactoryPostProcessors() {

        List<BeanFactoryPostProcessor> beanFactoryPostProcessors = new CopyOnWriteArrayList<>();
        List<String> tmpBeanDefinitionNames = new ArrayList<>(beanDefinitionNames);
        //1、扫描beanDefination,找到BeanFactoryPostProcessor实现，实例化
        //1、遍历beanDefination，把BeanPostProcessors类型的bean
        tmpBeanDefinitionNames.stream().filter(id -> {
            String beanClass = beanDefinationMap.get(id).getBeanClassName();
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
            String beanClass = beanDefinationMap.get(id).getBeanClassName();
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
            String beanClass = beanDefinationMap.get(id).getBeanClassName();
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

        //2、遍历bean定义信息，实例化bean
        beanDefinitionNames.stream().filter(beanName -> !beanPostProcessors.contains(beanName)).forEach(beanName -> {
            GenericBeanDefinition mbd = beanDefinationMap.get(beanName);
            try {

//                GenericBeanDefinition mbdToUse = mbd;

                Class<?> resolvedClass = resolveBeanClass(mbd, beanName);
                if (resolvedClass != null && !mbd.hasBeanClass() && mbd.getBeanClassName() != null) {
//                    mbdToUse = new GenericBeanDefinition(mbd);
                    mbd.setBeanClass(resolvedClass);
                }


                //postProcessBeforeInstantiation
                Object bean = applyPostProcessBeforeInstantiation(resolvedClass, beanName);
                if (bean != null) {
                    beanSingleMap.put(beanName, bean);
                } else {
                    Object wrappedBean = getBean(beanName, resolvedClass, mbd);
                    //postProcessafterInstantiation
                    applyPostProcessAfaterInstantiation(resolvedClass, beanName);


                    populateBean(beanName, mbd, wrappedBean);
                    initializeBean(beanName, mbd, resolvedClass, wrappedBean);

                }


            } catch (IllegalAccessException | InstantiationException | NoSuchMethodException | InvocationTargetException e) {
                e.printStackTrace();
            }
        });
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

    private Object evaluateBeanDefinitionString(String className, GenericBeanDefinition mbd) {

        return className;
    }

    private ClassLoader getBeanClassLoader() {
        return Thread.currentThread().getContextClassLoader();
    }

    protected void populateBean(String beanName, GenericBeanDefinition beanDefination, Object wrappedBean) {
        if (beanDefination.hasPropertyValues()) {
            Map<String, String> vals = beanDefination.getPropertyValues();
            vals.keySet().forEach(fieldName -> {

                String depBeanName = vals.get(fieldName);
                String methodName = "set" + fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1);
                try {
                    Object depBean = getBean(depBeanName, null, null);
                    Method method = wrappedBean.getClass().getMethod(methodName, depBean.getClass());

                    method.invoke(wrappedBean, depBean);
                } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | InstantiationException e) {
                    e.printStackTrace();
                }


            });
        }


    }

    private void initializeBean(String beanName, GenericBeanDefinition beanDefination, Class clazz, Object bean) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {

        invokeAwareMethods(beanName, bean);

        //3、遍历BeanPostProcessor实现，调用before方法，返回bean
        bean = applyBeanPostProcessorsBeforeInitialization(bean, beanName);

        //4、初始化bean
        invokeInitMethods(beanDefination, clazz, bean);

        //5、遍历BeanPostProcessor实现，调用after方法，返回bean
        bean = applyBeanPostProcessorsAfterInitialization(bean, beanName);
        if (bean != null) {
            beanSingleMap.put(beanName, bean);
        }
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

    private void invokeInitMethods(GenericBeanDefinition beanDefination, Class clazz, Object bean) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {


        if (bean instanceof InitializingBean) {
            ((InitializingBean) bean).afterPropertiesSet();
        }

        String initMethodName = beanDefination.getInitMethodName();
        if (initMethodName != null && initMethodName.length() > 0) {
            Method method = clazz.getDeclaredMethod(initMethodName);
            method.invoke(bean);
        }

    }

    private Object getBean(String id, Class clazz1, GenericBeanDefinition beanDefination1) throws InstantiationException, IllegalAccessException {
        Object bean;

        GenericBeanDefinition beanDefination = beanDefinationMap.get(id);
        Class clazz = null;
        try {
            clazz = Class.forName(beanDefination.getBeanClassName());
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

        if ("prototype".equals(beanDefination.getScope())) {
            bean = doGetBean(id, clazz, beanDefination);
        } else if ("singleton".equals(beanDefination.getScope())) {
            if (beanSingleMap.containsKey(id)) {
                bean = beanSingleMap.get(id);
            } else {
                bean = doGetBean(id, clazz, beanDefination);
                beanSingleMap.put(beanDefination.getId(), bean);
            }
        } else {
            throw new RuntimeException("当前只支持singleton和prototype！");
        }

        return bean;
    }

    //真正实例化生成bean
    private Object doGetBean(String id, Class clazz, GenericBeanDefinition mbd) throws InstantiationException, IllegalAccessException {
        Object bean;
        if (Arrays.asList(clazz.getInterfaces()).contains(FactoryBean.class)) {
            FactoryBean factoryBean;
            if (beanSingleMap.containsKey(BeanFactory.FACTORY_BEAN_PREFIX + id)) {
                factoryBean = (FactoryBean) beanSingleMap.get(BeanFactory.FACTORY_BEAN_PREFIX + id);
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
                beanSingleMap.put(BeanFactory.FACTORY_BEAN_PREFIX + id, factoryBean);
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

    protected abstract Map<String, GenericBeanDefinition> loadBeanDefination() throws IOException;

    @Override
    public void publishEvent(ApplicationEvent event) {

        for (ApplicationListener applicationListener : getApplicationListeners()) {
            applicationListener.onApplicationEvent(event);
        }

    }

    protected Collection<ApplicationListener> getApplicationListeners() {
        List<ApplicationListener> listeners = beanSingleMap.entrySet().stream().filter(entry -> (entry.getValue() instanceof ApplicationListener)).map(entry -> (ApplicationListener) entry.getValue()).collect(Collectors.toList());
        return listeners;
    }


    @Override
    public Object getBean(String id) {
        if (beanSingleMap.containsKey(id)) {
            return beanSingleMap.get(id);
        } else {
            if (!beanDefinationMap.containsKey(id)) {
                return null;
            }
            GenericBeanDefinition beanDefination = beanDefinationMap.get(id);
            try {
                Class clazz = Class.forName(beanDefination.getBeanClassName());
                return doGetBean(id, clazz, beanDefination);
            } catch (ClassNotFoundException | IllegalAccessException | InstantiationException e) {
                e.printStackTrace();
            } catch (NullPointerException e) {
                throw e;
            }
            return null;
        }
    }

    @Override
    public Object getBean(Class clazz) {

        List<Object> list = beanSingleMap.values().stream().filter(bean -> clazz.isAssignableFrom(bean.getClass())).collect(Collectors.toList());
        if (list.size() == 0) {
            return null;
        } else if (list.size() == 1) {
            return list.get(0);
        } else
            return list;
    }


    @Override
    public void registerBeanDefinition(String beanName, GenericBeanDefinition beanDefinition) {
        beanDefinationMap.put(beanName, beanDefinition);
        beanDefinitionNames.add(beanName);
    }

    @Override
    public void registerBeanDefinition(GenericBeanDefinition beanDefinition) {
        beanDefinationMap.put(beanDefinition.getId(), beanDefinition);
        beanDefinitionNames.add(beanDefinition.getId());
    }

    public void registerConfiguation(String configuationName) {
        configuationNames.add(configuationName);
    }

    @Override
    public void removeBeanDefinition(String beanName) {
        beanDefinationMap.remove(beanName);
        beanDefinitionNames.remove(beanName);
    }

    @Override
    public GenericBeanDefinition getBeanDefinition(String beanName) {
        return beanDefinationMap.get(beanName);
    }


    @Override
    public void addBeanPostProcessor(String id, BeanPostProcessor beanPostProcessor) {
        this.beanPostProcessors.add(beanPostProcessor);
        beanSingleMap.put(id, beanPostProcessor);
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
}
