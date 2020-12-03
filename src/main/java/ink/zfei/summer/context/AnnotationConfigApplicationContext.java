package ink.zfei.summer.context;

import ink.zfei.summer.annation.Component;
import ink.zfei.summer.beans.factory.support.GenericBeanDefinition;
import ink.zfei.summer.context.annotation.AnnotatedBeanDefinitionReader;
import ink.zfei.summer.context.annotation.AnnotationConfigRegistry;
import ink.zfei.summer.context.support.GenericApplicationContext;
import ink.zfei.summer.core.annotation.AnnotationConfigUtils;
import ink.zfei.summer.util.AnnationUtil;
import ink.zfei.summer.util.Assert;
import ink.zfei.summer.util.StringUtils;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;


public class AnnotationConfigApplicationContext extends GenericApplicationContext implements AnnotationConfigRegistry {

    Map<String, String> maps = new ConcurrentHashMap<>();
    private final AnnotatedBeanDefinitionReader reader;

    public AnnotationConfigApplicationContext() {
        this.reader = new AnnotatedBeanDefinitionReader(this);
        AnnotationConfigUtils.registerAnnotationConfigProcessors(this);
    }

    public List<GenericBeanDefinition> getConfigBeanDefinitions() {
        return configBeanDefinitions;
    }

    public List<GenericBeanDefinition> configBeanDefinitions = new ArrayList<>();

    public AnnotationConfigApplicationContext(String basePackages) {
        this();
        try {
            scan(basePackages);
        } catch (IOException | URISyntaxException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        refresh();

    }

    public AnnotationConfigApplicationContext(String basePackages, Class<?> componentClasses) {
        this();
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
        this();
        register(componentClasses);
        refresh();

    }

    @Override
    public void register(Class<?>... componentClasses) {
        Assert.notEmpty(componentClasses, "At least one component class must be specified");
        this.reader.register(componentClasses);
    }

//    public void register(Class<?>... componentClasses) {
//
//        Assert.notEmpty(componentClasses, "At least one component class must be specified");
//        Annotation annotations = AnnationUtil.findAnnotation(componentClasses, Configuration.class);
//        if (annotations == null) {
//            throw new RuntimeException("不是配置类");
//        }
//        GenericBeanDefinition abd = new GenericBeanDefinition();
//        String beanName = componentClasses.getSimpleName();
//        beanName = beanName.substring(0, 1).toLowerCase() + beanName.substring(1);
//        abd.setId(beanName);
//        abd.setBeanClassName(componentClasses.getName());
//        registerBeanDefinition(abd);
//        registerConfiguration(componentClasses.getName());
//
////        boolean isPorxy = annotations.proxyBeanMethods();
//        Method[] methods = componentClasses.getMethods();
//        for (Method method : methods) {
//            Bean bean = method.getAnnotation(Bean.class);
//            if (bean != null) {
//                GenericBeanDefinition definition = new GenericBeanDefinition();
//                definition.setId(method.getName());
//                definition.setFactoryBeanName(beanName);
//                definition.setFactoryMethodName(method.getName());
//                registerBeanDefinition(definition);
//                configBeanDefinitions.add(definition);
//            }
//        }
//
//    }

//    @Override
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
        loadScanBean(basePackages, dir);

    }

    private void loadScanBean(String basePackages, File dir) throws ClassNotFoundException {
        for (File content : AnnationUtil.listDirectory(dir)) {
            if (content.isDirectory()) {
                loadScanBean(basePackages + "." + content.getName(), content);
                return;
            }
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
                if (StringUtils.isEmpty(beanName)) {
                    beanName = aClass.getSimpleName();
                    beanName = beanName.substring(0, 1).toLowerCase() + beanName.substring(1);
                }
                maps.put(beanName, className);
            }

        }
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