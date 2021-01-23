package ink.zfei.summer.context.annotation;

import ink.zfei.summer.annation.Import;
import ink.zfei.summer.beans.BeanDefinitionRegistry;
import ink.zfei.summer.beans.factory.annotation.AnnotatedBeanDefinition;
import ink.zfei.summer.beans.factory.config.BeanDefinition;
import ink.zfei.summer.beans.factory.config.BeanDefinitionHolder;
import ink.zfei.summer.beans.factory.support.AbstractBeanDefinition;
import ink.zfei.summer.core.ImportBeanDefinitionRegistrar;
import ink.zfei.summer.core.ImportSelector;
import ink.zfei.summer.core.Ordered;
import ink.zfei.summer.core.annotation.Bean;
import ink.zfei.summer.core.io.ResourceLoader;
import ink.zfei.summer.core.type.AnnotationMetadata;
import ink.zfei.summer.core.type.MethodMetadata;
import ink.zfei.summer.core.type.StandardAnnotationMetadata;
import ink.zfei.summer.core.type.classreading.MetadataReader;
import ink.zfei.summer.core.type.classreading.MetadataReaderFactory;
import ink.zfei.summer.core.type.filter.AssignableTypeFilter;
import ink.zfei.summer.lang.Nullable;
import ink.zfei.summer.util.*;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.*;
import java.util.function.Predicate;

public class ConfigurationClassParser {

    private final ResourceLoader resourceLoader;
    private final ImportStack importStack = new ImportStack();
    private final BeanDefinitionRegistry registry;
    private final MetadataReaderFactory metadataReaderFactory;
    private final Map<ConfigurationClass, ConfigurationClass> configurationClasses = new LinkedHashMap<>();
    private final Map<String, ConfigurationClass> knownSuperclasses = new HashMap<>();

    private static final Predicate<String> DEFAULT_EXCLUSION_FILTER = className ->
            (className.startsWith("java.lang.annotation.") || className.startsWith("org.springframework.stereotype."));

    public ConfigurationClassParser(MetadataReaderFactory metadataReaderFactory, BeanDefinitionRegistry registry, ResourceLoader resourceLoader) {
        this.registry = registry;
        this.resourceLoader = resourceLoader;
        this.metadataReaderFactory = metadataReaderFactory;
    }

    protected final void parse(AnnotationMetadata metadata, String beanName) {
        processConfigurationClass(new ConfigurationClass(metadata, beanName), DEFAULT_EXCLUSION_FILTER);
    }

    protected void processConfigurationClass(ConfigurationClass configClass, Predicate<String> filter) {

        //有条件注解，先过滤

        //
        ConfigurationClass existingClass = this.configurationClasses.get(configClass);
        if (existingClass != null) {
            if (configClass.isImported()) {
                if (existingClass.isImported()) {
                    existingClass.mergeImportedBy(configClass);
                }
                // Otherwise ignore new imported config class; existing non-imported class overrides it.
                return;
            } else {
                // Explicit bean definition found, probably replacing an import.
                // Let's remove the old one and go with the new one.
                this.configurationClasses.remove(configClass);
                this.knownSuperclasses.values().removeIf(configClass::equals);
            }
        }


        // Recursively process the configuration class and its superclass hierarchy.
        SourceClass sourceClass = asSourceClass(configClass, filter);
        do {
            sourceClass = doProcessConfigurationClass(configClass, sourceClass, filter);
        }
        while (sourceClass != null);

        this.configurationClasses.put(configClass, configClass);
    }

    protected final void parse(Class<?> clazz, String beanName) {
        processConfigurationClass(new ConfigurationClass(clazz, beanName), DEFAULT_EXCLUSION_FILTER);
    }

    public void parse(Set<BeanDefinitionHolder> configCandidates) {
        for (BeanDefinitionHolder holder : configCandidates) {
            BeanDefinition bd = holder.getBeanDefinition();
            if (bd instanceof AnnotatedBeanDefinition) {
                parse(((AnnotatedBeanDefinition) bd).getMetadata(), holder.getBeanName());
            } else if (bd instanceof AbstractBeanDefinition && ((AbstractBeanDefinition) bd).hasBeanClass()) {
                parse(((AbstractBeanDefinition) bd).getBeanClass(), holder.getBeanName());
            } else {
                parse(bd.getBeanClassName(), holder.getBeanName());
            }
        }

    }

    public Set<ConfigurationClass> getConfigurationClasses() {
        return this.configurationClasses.keySet();
    }

    protected final void parse(@Nullable String className, String beanName) {
        Assert.notNull(className, "No bean class name for configuration class bean definition");
        MetadataReader reader = this.metadataReaderFactory.getMetadataReader(className);
        processConfigurationClass(new ConfigurationClass(reader, beanName), DEFAULT_EXCLUSION_FILTER);
    }

    /**
     * 简单包装类，统一的方式去处理带注解的类，无论它是如何加载的
     */
    private class SourceClass implements Ordered {

        //注解类,class或者MetadataReader
        private final Object source;
        //注解元信息
        private final AnnotationMetadata metadata;

        public SourceClass(Object source) {
            this.source = source;
            if (source instanceof Class) {
                this.metadata = AnnotationMetadata.introspect((Class<?>) source);
            } else {
                this.metadata = ((MetadataReader) source).getAnnotationMetadata();
            }
        }

        @Override
        public boolean equals(@Nullable Object other) {
            return (this == other || (other instanceof SourceClass &&
                    this.metadata.getClassName().equals(((SourceClass) other).metadata.getClassName())));
        }

        @Override
        public int hashCode() {
            return this.metadata.getClassName().hashCode();
        }

        @Override
        public int getOrder() {
            return 0;
        }

        public ConfigurationClass asConfigClass(ConfigurationClass importedBy) {
            if (this.source instanceof Class) {
                return new ConfigurationClass((Class<?>) this.source, importedBy);
            }
            return new ConfigurationClass((MetadataReader) this.source, importedBy);
        }

        public Class<?> loadClass() throws ClassNotFoundException {
            if (this.source instanceof Class) {
                return (Class<?>) this.source;
            }
            String className = ((MetadataReader) this.source).getClassMetadata().getClassName();
            return ClassUtils.forName(className, resourceLoader.getClassLoader());
        }

        public final AnnotationMetadata getMetadata() {
            return this.metadata;
        }

        public boolean isAssignable(Class<?> clazz) {
            if (this.source instanceof Class) {
                return clazz.isAssignableFrom((Class<?>) this.source);
            }
            return new AssignableTypeFilter(clazz).match((MetadataReader) this.source, metadataReaderFactory);
        }

        public Set<SourceClass> getAnnotations() {
            Set<SourceClass> result = new LinkedHashSet<>();
            if (this.source instanceof Class) {
                Class<?> sourceClass = (Class<?>) this.source;
                for (Annotation ann : sourceClass.getDeclaredAnnotations()) {
                    Class<?> annType = ann.annotationType();
                    if (!annType.getName().startsWith("java")) {
                        result.add(asSourceClass(annType, DEFAULT_EXCLUSION_FILTER));
                    }
                }
            } else {
                for (String className : this.metadata.getAnnotationTypes()) {
                    if (!className.startsWith("java")) {
                        try {
                            result.add(getRelated(className));
                        } catch (Throwable ex) {
                            // An annotation not present on the classpath is being ignored
                            // by the JVM's class loading -> ignore here as well.
                        }
                    }
                }
            }


            return result;
        }

        private SourceClass getRelated(String className) {
            if (this.source instanceof Class) {
                try {
                    Class<?> clazz = ClassUtils.forName(className, ((Class<?>) this.source).getClassLoader());
                    return asSourceClass(clazz, DEFAULT_EXCLUSION_FILTER);
                } catch (ClassNotFoundException ex) {
                    // Ignore -> fall back to ASM next, except for core java types.
                    if (className.startsWith("java")) {
                        throw new RuntimeException("Failed to load class [" + className + "]", ex);
                    }
                    return new SourceClass(metadataReaderFactory.getMetadataReader(className));
                }
            }
            return asSourceClass(className, DEFAULT_EXCLUSION_FILTER);
        }

        public Collection<SourceClass> getAnnotationAttributes(String annType, String attribute) {
            Map<String, Object> annotationAttributes = this.metadata.getAnnotationAttributes(annType, true);
            if (annotationAttributes == null || !annotationAttributes.containsKey(attribute)) {
                return Collections.emptySet();
            }
            String[] classNames = (String[]) annotationAttributes.get(attribute);
            Set<SourceClass> result = new LinkedHashSet<>();
            for (String className : classNames) {
                result.add(getRelated(className));
            }
            return result;
        }

    }

    private SourceClass asSourceClass(ConfigurationClass configurationClass, Predicate<String> filter) {
        AnnotationMetadata metadata = configurationClass.getMetadata();
        if (metadata instanceof StandardAnnotationMetadata) {
            return asSourceClass(((StandardAnnotationMetadata) metadata).getIntrospectedClass(), filter);
        }
        return asSourceClass(metadata.getClassName(), filter);
    }

    SourceClass asSourceClass(@Nullable Class<?> classType, Predicate<String> filter) {
        return new SourceClass(classType);
    }

    SourceClass asSourceClass(@Nullable String className, Predicate<String> filter) {
        return new SourceClass(this.metadataReaderFactory.getMetadataReader(className));
    }

    protected final SourceClass doProcessConfigurationClass(
            ConfigurationClass configClass, SourceClass sourceClass, Predicate<String> filter) {

        // Recursively process any member (nested) classes first

        // Process any @PropertySource annotations

        // Process any @ComponentScan annotations

        // Process any @Import annotations
        processImports(configClass, sourceClass, getImports(sourceClass), filter, true);

        // Process any @ImportResource annotations

        // Process individual @Bean methods
        Set<MethodMetadata> beanMethods = retrieveBeanMethodMetadata(sourceClass);
        for (MethodMetadata methodMetadata : beanMethods) {
            configClass.addBeanMethod(new BeanMethod(methodMetadata, configClass));
        }
        // Process default methods on interfaces

        // Process superclass, if any

        return null;
    }

    private Set<MethodMetadata> retrieveBeanMethodMetadata(SourceClass sourceClass) {
        AnnotationMetadata original = sourceClass.getMetadata();
        Set<MethodMetadata> beanMethods = original.getAnnotatedMethods(Bean.class.getName());
        if (beanMethods.size() > 1 && original instanceof StandardAnnotationMetadata) {
            // Try reading the class file via ASM for deterministic declaration order...
            // Unfortunately, the JVM's standard reflection returns methods in arbitrary
            // order, even between different runs of the same application on the same JVM.
            AnnotationMetadata asm =
                    this.metadataReaderFactory.getMetadataReader(original.getClassName()).getAnnotationMetadata();
            Set<MethodMetadata> asmMethods = asm.getAnnotatedMethods(Bean.class.getName());
            if (asmMethods.size() >= beanMethods.size()) {
                Set<MethodMetadata> selectedMethods = new LinkedHashSet<>(asmMethods.size());
                for (MethodMetadata asmMethod : asmMethods) {
                    for (MethodMetadata beanMethod : beanMethods) {
                        if (beanMethod.getMethodName().equals(asmMethod.getMethodName())) {
                            selectedMethods.add(beanMethod);
                            break;
                        }
                    }
                }
                if (selectedMethods.size() == beanMethods.size()) {
                    // All reflection-detected methods found in ASM method set -> proceed
                    beanMethods = selectedMethods;
                }
            }
        }
        return beanMethods;
    }


    /**
     * 从配置类（遍历所有层级并去重）获取@Import的value，把value封装成class的包装对象SourceClass
     * Returns {@code @Import} class, considering all meta-annotations.
     */
    private Set<SourceClass> getImports(SourceClass sourceClass) {
        Set<SourceClass> imports = new LinkedHashSet<>();
        Set<SourceClass> visited = new LinkedHashSet<>();
        collectImports(sourceClass, imports, visited);
        return imports;
    }

    private void collectImports(SourceClass sourceClass, Set<SourceClass> imports, Set<SourceClass> visited) {

//        利用Set.add返回结果可以去重
        if (visited.add(sourceClass)) {
            for (SourceClass annotation : sourceClass.getAnnotations()) {
                String annName = annotation.getMetadata().getClassName();
                if (!annName.equals(Import.class.getName())) {
                    collectImports(annotation, imports, visited);
                }
            }
            //这里子注解也能获取到父注解的value，存在重复，需要重写SourceClass的hashcode和equals，addAll可以去重
            imports.addAll(sourceClass.getAnnotationAttributes(Import.class.getName(), "value"));
        }
    }


    private void processImports(ConfigurationClass configClass, SourceClass currentSourceClass,
                                Collection<SourceClass> importCandidates, Predicate<String> exclusionFilter,
                                boolean checkForCircularImports) {

        if (importCandidates.isEmpty()) {
            return;
        }

        this.importStack.push(configClass);
        try {
            for (SourceClass candidate : importCandidates) {
                if (candidate.isAssignable(ImportSelector.class)) {
                    // Candidate class is an ImportSelector -> delegate to it to determine imports
                    Class<?> candidateClass = candidate.loadClass();
                    //实例化ImportSelector，如有需要的化，注入resourceLoader、beanFactory（构造函数或者aware接口）
                    ImportSelector selector = ParserStrategyUtils.instantiateClass(candidateClass, ImportSelector.class,
                            this.resourceLoader, this.registry);
                    Predicate<String> selectorFilter = selector.getExclusionFilter();
                    if (selectorFilter != null) {
                        //忽略基础注解+ImportSelector显式指定忽略的配置类
                        exclusionFilter = exclusionFilter.or(selectorFilter);
                    }
                    if (selector instanceof DeferredImportSelector) {
//                        this.deferredImportSelectorHandler.handle(configClass, (DeferredImportSelector) selector);
                    } else {
                        String[] importClassNames = selector.selectImports(currentSourceClass.getMetadata());
                        Collection<SourceClass> importSourceClasses = asSourceClasses(importClassNames, exclusionFilter);
                        //递归解析，当configClass不是import时，下次循环走最下面else，重新解析配置类所有bean信息
                        processImports(configClass, currentSourceClass, importSourceClasses, exclusionFilter, false);
                    }
                } else if (candidate.isAssignable(ImportBeanDefinitionRegistrar.class)) {
                    // Candidate class is an ImportBeanDefinitionRegistrar ->
                    // delegate to it to register additional bean definitions
                    Class<?> candidateClass = candidate.loadClass();
                    ImportBeanDefinitionRegistrar registrar =
                            ParserStrategyUtils.instantiateClass(candidateClass, ImportBeanDefinitionRegistrar.class,
                                    this.resourceLoader, this.registry);
                    configClass.addImportBeanDefinitionRegistrar(registrar, currentSourceClass.getMetadata());
                } else {
                    // Candidate class not an ImportSelector or ImportBeanDefinitionRegistrar ->
                    // process it as an @Configuration class
                    this.importStack.registerImport(
                            currentSourceClass.getMetadata(), candidate.getMetadata().getClassName());
                    processConfigurationClass(candidate.asConfigClass(configClass), exclusionFilter);
                }
            }
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } finally {
            this.importStack.pop();
        }
    }


    private static class ImportStack extends ArrayDeque<ConfigurationClass> implements ImportRegistry {

        private final MultiValueMap<String, AnnotationMetadata> imports = new LinkedMultiValueMap<>();

        public void registerImport(AnnotationMetadata importingClass, String importedClass) {
            this.imports.add(importedClass, importingClass);
        }

        @Override
        @Nullable
        public AnnotationMetadata getImportingClassFor(String importedClass) {
            return CollectionUtils.lastElement(this.imports.get(importedClass));
        }

        @Override
        public void removeImportingClass(String importingClass) {
            for (List<AnnotationMetadata> list : this.imports.values()) {
                for (Iterator<AnnotationMetadata> iterator = list.iterator(); iterator.hasNext(); ) {
                    if (iterator.next().getClassName().equals(importingClass)) {
                        iterator.remove();
                        break;
                    }
                }
            }
        }

        /**
         * Given a stack containing (in order)
         * <ul>
         * <li>com.acme.Foo</li>
         * <li>com.acme.Bar</li>
         * <li>com.acme.Baz</li>
         * </ul>
         * return "[Foo->Bar->Baz]".
         */
        @Override
        public String toString() {
            StringJoiner joiner = new StringJoiner("->", "[", "]");
            for (ConfigurationClass configurationClass : this) {
                joiner.add(configurationClass.getSimpleName());
            }
            return joiner.toString();
        }
    }

    private Collection<SourceClass> asSourceClasses(String[] classNames, Predicate<String> filter) {
        List<SourceClass> annotatedClasses = new ArrayList<>(classNames.length);
        for (String className : classNames) {
            annotatedClasses.add(asSourceClass(className, filter));
        }
        return annotatedClasses;
    }
}
