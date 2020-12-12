package ink.zfei.summer.context;

import ink.zfei.summer.annation.Component;
import ink.zfei.summer.beans.factory.support.GenericBeanDefinition;
import ink.zfei.summer.context.annotation.AnnotatedBeanDefinitionReader;
import ink.zfei.summer.context.annotation.AnnotationConfigRegistry;
import ink.zfei.summer.context.annotation.ClassPathBeanDefinitionScanner;
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
    private final ClassPathBeanDefinitionScanner scanner;

    public AnnotationConfigApplicationContext() {
        this.reader = new AnnotatedBeanDefinitionReader(this);
        this.scanner = new ClassPathBeanDefinitionScanner(this);
    }

    public List<GenericBeanDefinition> getConfigBeanDefinitions() {
        return configBeanDefinitions;
    }

    public List<GenericBeanDefinition> configBeanDefinitions = new ArrayList<>();

    /**
     * Create a new AnnotationConfigApplicationContext, scanning for components
     * in the given packages, registering bean definitions for those components,
     * and automatically refreshing the context.
     * @param basePackages the packages to scan for component classes
     */
    public AnnotationConfigApplicationContext(String... basePackages) {
        this();
        scan(basePackages);
        refresh();

    }


    public AnnotationConfigApplicationContext(String basePackages, Class<?> componentClasses) {
        this();
        AnnotationConfigUtils.registerAnnotationConfigProcessors(this);
        register(componentClasses);
        scan(basePackages);
        refresh();

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

    public void scan(String... basePackages) {
        Assert.notEmpty(basePackages, "At least one base package must be specified");
        this.scanner.scan(basePackages);
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


    @Override
    public <T> T getBean(Class<T> requiredType, Object... args) {
        return null;
    }


    @Override
    public Object getBean(String name, Object... args) {
        return null;
    }
}