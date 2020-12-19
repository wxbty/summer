package ink.zfei.summer.context;

import ink.zfei.summer.context.annotation.AnnotatedBeanDefinitionReader;
import ink.zfei.summer.context.annotation.AnnotationConfigRegistry;
import ink.zfei.summer.context.annotation.ClassPathBeanDefinitionScanner;
import ink.zfei.summer.context.support.GenericApplicationContext;
import ink.zfei.summer.core.annotation.AnnotationConfigUtils;
import ink.zfei.summer.util.Assert;


public class AnnotationConfigApplicationContext extends GenericApplicationContext implements AnnotationConfigRegistry {

    private final AnnotatedBeanDefinitionReader reader;
    private final ClassPathBeanDefinitionScanner scanner;

    public AnnotationConfigApplicationContext() {
        this.reader = new AnnotatedBeanDefinitionReader(this);
        this.scanner = new ClassPathBeanDefinitionScanner(this);
    }

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


    @Override
    public <T> T getBean(Class<T> requiredType, Object... args) {
        return null;
    }


    @Override
    public Object getBean(String name, Object... args) {
        return null;
    }
}