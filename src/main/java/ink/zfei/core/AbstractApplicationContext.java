package ink.zfei.core;

import ink.zfei.beans.*;
import org.apache.commons.lang3.ClassUtils;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

public abstract class AbstractApplicationContext implements ApplicationContext, BeanDefinitionRegistry {


    protected Map<String, BeanDefinition> beanDefinationMap = new ConcurrentHashMap<String, BeanDefinition>();
    protected Map<String, Object> beanSingleMap = new ConcurrentHashMap<String, Object>();
    protected Map<String, BeanPostProcessor> beanPostProcessorMap = new ConcurrentHashMap<String, BeanPostProcessor>();
    protected Map<String, BeanFactoryPostProcessor> beanFactoryPostProcessorMap = new ConcurrentHashMap<String, BeanFactoryPostProcessor>();
    private final List<BeanPostProcessor> beanPostProcessors = new CopyOnWriteArrayList<>();
    private final List<InstantiationAwareBeanPostProcessor> instantiationAwareBeanPostProcessors = new CopyOnWriteArrayList<>();
    protected Map<String, InstantiationAwareBeanPostProcessor> instantiationAwareBeanPostProcessorMap = new ConcurrentHashMap<String, InstantiationAwareBeanPostProcessor>();


    private volatile List<String> beanDefinitionNames = new ArrayList<>(256);

    final String FACTORY_BEAN_PREFIX = "&";

    public AbstractApplicationContext() {
    }


    public void refresh() throws IOException {
        //1、从外界获取bean定义信息
        beanDefinationMap = loadBeanDefination();
        beanDefinitionNames = beanDefinationMap.keySet().stream().collect(Collectors.toList());

        //Invoke factory processors registered as beans in the context.
        invokeBeanFactoryPostProcessors();

        registerBeanPostProcessors();

        finishBeanFactoryInitialization();


        //4、发布refresh事件，遍历listener,分别执行
        RefreshApplicationEvent event = new RefreshApplicationEvent();
        publishEvent(event);

    }

    private void invokeBeanFactoryPostProcessors() {

        List<BeanFactoryPostProcessor> beanFactoryPostProcessors = new CopyOnWriteArrayList<>();
        List<String> tmpBeanDefinitionNames = new ArrayList<>(beanDefinitionNames);
        //1、扫描beanDefination,找到BeanFactoryPostProcessor实现，实例化
        //1、遍历beanDefination，把BeanPostProcessors类型的bean
        tmpBeanDefinitionNames.stream().filter(id -> {
            String beanClass = beanDefinationMap.get(id).getBeanClass();
            try {
                Class clazz = Class.forName(beanClass);
                if (ClassUtils.getAllInterfaces(clazz).contains(BeanFactoryPostProcessor.class)) {
                    return true;
                }
            } catch (ClassNotFoundException e) {
                return false;
            }
            return false;
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
            String beanClass = beanDefinationMap.get(id).getBeanClass();
            try {
                Class clazz = Class.forName(beanClass);
                if (ClassUtils.getAllInterfaces(clazz).contains(BeanPostProcessor.class)) {
                    return true;
                }
            } catch (ClassNotFoundException e) {
                return false;
            }
            return false;
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
            String beanClass = beanDefinationMap.get(id).getBeanClass();
            try {
                Class clazz = Class.forName(beanClass);
                if (ClassUtils.getAllInterfaces(clazz).contains(InstantiationAwareBeanPostProcessor.class)) {
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
            BeanDefinition beanDefination = beanDefinationMap.get(beanName);
            try {
                Class clazz = Class.forName(beanDefination.getBeanClass());

                //postProcessBeforeInstantiation
                Object bean = applyPostProcessBeforeInstantiation(clazz, beanName);
                if (bean != null) {
                    beanSingleMap.put(beanName, bean);
                } else {
                    Object wrappedBean = getBean(beanName, clazz, beanDefination);
                    //postProcessafterInstantiation
                    applyPostProcessAfaterInstantiation(clazz, beanName);

                    //3、遍历BeanPostProcessor实现，调用before方法，返回bean
                    wrappedBean = applyBeanPostProcessorsBeforeInitialization(wrappedBean, beanName);

                    //4、初始化bean
                    invokeInitMethods(beanDefination, clazz, wrappedBean);

                    //5、遍历BeanPostProcessor实现，调用after方法，返回bean
                    wrappedBean = applyBeanPostProcessorsAfterInitialization(wrappedBean, beanName);
                    if (wrappedBean != null) {
                        beanSingleMap.put(beanName, wrappedBean);
                    }
                }


            } catch (ClassNotFoundException | IllegalAccessException | InstantiationException | NoSuchMethodException | InvocationTargetException e) {
                e.printStackTrace();
            }
        });
    }


    private Object applyPostProcessBeforeInstantiation(Class clazz, String beanName) {

        for (InstantiationAwareBeanPostProcessor processor : getinstantiationAwareBeanPostProcessors()) {
            Object current = processor.postProcessBeforeInstantiation(clazz, beanName);
            if (current != null) {
                return  processor.postProcessAfterInitialization(current, beanName);
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

    private void invokeInitMethods(BeanDefinition beanDefination, Class clazz, Object bean) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {


        if (bean instanceof InitializingBean) {
            ((InitializingBean) bean).afterPropertiesSet();
        }

        String initMethodName = beanDefination.getInitMethod();
        if (initMethodName != null && initMethodName.length() > 0) {
            Method method = clazz.getDeclaredMethod(initMethodName);
            method.invoke(bean);
        }

    }

    private Object getBean(String id, Class clazz, BeanDefinition beanDefination) throws InstantiationException, IllegalAccessException {
        Object bean;

        if ("prototype".equals(beanDefination.getScope())) {
            bean = doGetBean(id, clazz);
        } else if ("singleton".equals(beanDefination.getScope())) {
            if (beanSingleMap.containsKey(id)) {
                bean = beanSingleMap.get(id);
            } else {
                bean = doGetBean(id, clazz);
                beanSingleMap.put(beanDefination.getId(), bean);
            }
        } else {
            throw new RuntimeException("当前只支持singleton和prototype！");
        }

        return bean;
    }

    //真正实例化生成bean
    private Object doGetBean(String id, Class clazz) throws InstantiationException, IllegalAccessException {
        Object bean;
        if (Arrays.asList(clazz.getInterfaces()).contains(FactoryBean.class)) {
            FactoryBean factoryBean;
            if (beanSingleMap.containsKey(FACTORY_BEAN_PREFIX + id)) {
                factoryBean = (FactoryBean) beanSingleMap.get(FACTORY_BEAN_PREFIX + id);
            } else {
                factoryBean = (FactoryBean) clazz.newInstance();
                beanSingleMap.put(FACTORY_BEAN_PREFIX + id, factoryBean);
            }
            bean = factoryBean.getObject();
        } else {
            bean = clazz.newInstance();
        }
        return bean;
    }

    protected abstract Map<String, BeanDefinition> loadBeanDefination() throws IOException;

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
            BeanDefinition beanDefination = beanDefinationMap.get(id);
            try {
                Class clazz = Class.forName(beanDefination.getBeanClass());
                return doGetBean(id, clazz);
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InstantiationException e) {
                e.printStackTrace();
            }
            return null;
        }
    }


    @Override
    public void registerBeanDefinition(String beanName, BeanDefinition beanDefinition) {
        beanDefinationMap.put(beanName, beanDefinition);
        beanDefinitionNames.add(beanName);
    }

    @Override
    public void removeBeanDefinition(String beanName) {
        beanDefinationMap.remove(beanName);
        beanDefinitionNames.remove(beanName);
    }

    @Override
    public BeanDefinition getBeanDefinition(String beanName) {
        return beanDefinationMap.get(beanName);
    }

}
