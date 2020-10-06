package ink.zfei.summer.context;

import ink.zfei.summer.annation.Component;
import ink.zfei.summer.core.AbstractApplicationContext;
import ink.zfei.summer.beans.factory.support.GenericBeanDefinition;
import ink.zfei.summer.core.annotation.AnnotationConfigUtils;
import ink.zfei.summer.core.annotation.Bean;
import ink.zfei.summer.core.annotation.Configuration;
import ink.zfei.summer.util.AnnationUtil;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;


public class AnnotationConfigApplicationContext extends AbstractApplicationContext {

    Map<String, String> maps = new ConcurrentHashMap<>();

    public List<GenericBeanDefinition> getConfigBeanDefinitions() {
        return configBeanDefinitions;
    }

    public List<GenericBeanDefinition> configBeanDefinitions = new ArrayList<>();

    public AnnotationConfigApplicationContext(String basePackages)  {
        super();
        try {
            scan(basePackages);
        } catch (IOException | URISyntaxException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        refresh();

    }

    public AnnotationConfigApplicationContext(String basePackages, Class<?> componentClasses) {
        super();
        try {
            AnnotationConfigUtils.registerAnnotationConfigProcessors(this);
            register(componentClasses);
            scan(basePackages);
            refresh();
        } catch (IOException | ClassNotFoundException | URISyntaxException e) {
            e.printStackTrace();
        }
    }

    public AnnotationConfigApplicationContext(Class<?> componentClasses) {

        //1、注册一个bean工厂后置处理器，负责注册配置类里拿到的bean信息
        AnnotationConfigUtils.registerAnnotationConfigProcessors(this);

        //2、解析传入的配置类
        register(componentClasses);

        refresh();

    }

    public void register(Class<?> componentClasses) {

        Annotation annotations = AnnationUtil.findAnnotation(componentClasses, Configuration.class);
        if (annotations == null) {
            throw new RuntimeException("不是配置类");
        }
        GenericBeanDefinition abd = new GenericBeanDefinition();
        String beanName = componentClasses.getSimpleName();
        beanName = beanName.substring(0, 1).toLowerCase() + beanName.substring(1);
        abd.setId(beanName);
        abd.setBeanClassName(componentClasses.getName());
        registerBeanDefinition(abd);
        registerConfiguation(componentClasses.getName());

//        boolean isPorxy = annotations.proxyBeanMethods();
        Method[] methods = componentClasses.getMethods();
        for (Method method : methods) {
            Bean bean = method.getAnnotation(Bean.class);
            if (bean != null) {
                GenericBeanDefinition definition = new GenericBeanDefinition();
                definition.setId(method.getName());
                definition.setFactoryBeanName(beanName);
                definition.setFactoryMethodName(method.getName());
                registerBeanDefinition(definition);
                configBeanDefinitions.add(definition);
            }
        }

    }

    @Override
    protected Map<String, GenericBeanDefinition> loadBeanDefination() {

        return maps.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, entry -> {
            GenericBeanDefinition beanDefinition = new GenericBeanDefinition();
            beanDefinition.setId(entry.getKey());
            beanDefinition.setBeanClassName(entry.getValue());
            beanDefinition.setScope("singleton");
            return beanDefinition;
        }));
    }


    public void scan(String basePackages) throws IOException, URISyntaxException, ClassNotFoundException {
        if (basePackages == null) {
            throw new RuntimeException("At least one base package must be specified");
        }
        String path = AnnationUtil.resolveBasePackage(basePackages);

        ClassLoader cl = Thread.currentThread().getContextClassLoader();

        List<URL> list = new ArrayList<>();
        Enumeration<URL> resourceUrls = (cl != null ? cl.getResources(path) : ClassLoader.getSystemResources(path));
        while (resourceUrls.hasMoreElements()) {
            URL url = resourceUrls.nextElement();
            list.add(url);
        }

        URL url = list.get(0);
        File dir = new File(AnnationUtil.toURI(url.toString()).getSchemeSpecificPart());
        for (File content : AnnationUtil.listDirectory(dir)) {

            String className = content.getAbsolutePath();
            className = className.replace(File.separatorChar, '.');
            className = className.substring(className.indexOf(basePackages));

            className = className.substring(0, className.length() - 6);
//            //将/替换成. 得到全路径类名


            //className = ink.zfei.annation.Component
            // 加载Class类
            Class<?> aClass = Class.forName(className);
            Component component = aClass.getAnnotation(Component.class);
            if (component != null) {
                String beanName = component.value();
                if (StringUtils.isBlank(beanName)) {
                    beanName = aClass.getSimpleName();
                    beanName = beanName.substring(0, 1).toLowerCase() + beanName.substring(1);
                }
                maps.put(beanName, className);
            }

        }

    }


    @Override
    public <T> T getBean(String name, Class<T> requiredType) {
        return null;
    }

    @Override
    public <T> T getBean(Class<T> requiredType, Object... args) {
        return null;
    }

    @Override
    public Object getBean(String name, Object... args) {
        return null;
    }
}
