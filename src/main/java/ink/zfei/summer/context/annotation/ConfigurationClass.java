package ink.zfei.summer.context.annotation;

import ink.zfei.summer.core.ImportBeanDefinitionRegistrar;
import ink.zfei.summer.core.io.DescriptiveResource;
import ink.zfei.summer.core.io.Resource;
import ink.zfei.summer.core.type.AnnotationMetadata;
import ink.zfei.summer.core.type.classreading.MetadataReader;
import ink.zfei.summer.lang.Nullable;
import ink.zfei.summer.util.Assert;
import ink.zfei.summer.util.ClassUtils;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public class ConfigurationClass {

    private final AnnotationMetadata metadata;

    private final Set<ConfigurationClass> importedBy = new LinkedHashSet<>(1);

    private final Resource resource;

    private String beanName;

    private final Set<BeanMethod> beanMethods = new LinkedHashSet<>();


    private final Map<ImportBeanDefinitionRegistrar, AnnotationMetadata> importBeanDefinitionRegistrars =
            new LinkedHashMap<>();

    public ConfigurationClass(AnnotationMetadata metadata, String beanName) {
        Assert.notNull(beanName, "Bean name must not be null");
        this.metadata = metadata;
        this.resource = new DescriptiveResource(metadata.getClassName());
        this.beanName = beanName;
    }

    public ConfigurationClass(Class<?> clazz, String beanName) {
        Assert.notNull(beanName, "Bean name must not be null");
        this.metadata = AnnotationMetadata.introspect(clazz);
        this.resource = new DescriptiveResource(clazz.getName());
        this.beanName = beanName;
    }

    public ConfigurationClass(MetadataReader metadataReader, String beanName) {
        Assert.notNull(beanName, "Bean name must not be null");
        this.metadata = metadataReader.getAnnotationMetadata();
        this.resource = metadataReader.getResource();
        this.beanName = beanName;
    }

    public ConfigurationClass(Class<?> clazz, @Nullable ConfigurationClass importedBy) {
        this.metadata = AnnotationMetadata.introspect(clazz);
        this.resource = new DescriptiveResource(clazz.getName());
        this.importedBy.add(importedBy);
    }

    public ConfigurationClass(MetadataReader metadataReader, @Nullable ConfigurationClass importedBy) {
        this.metadata = metadataReader.getAnnotationMetadata();
        this.resource = metadataReader.getResource();
        this.importedBy.add(importedBy);
    }

    public AnnotationMetadata getMetadata() {
        return this.metadata;
    }

    public String getSimpleName() {
        return ClassUtils.getShortName(getMetadata().getClassName());
    }

    public void addImportBeanDefinitionRegistrar(ImportBeanDefinitionRegistrar registrar, AnnotationMetadata importingClassMetadata) {
        this.importBeanDefinitionRegistrars.put(registrar, importingClassMetadata);
    }

    public boolean isImported() {
        return !this.importedBy.isEmpty();
    }

    public void mergeImportedBy(ConfigurationClass otherConfigClass) {
        this.importedBy.addAll(otherConfigClass.importedBy);
    }

    public void addBeanMethod(BeanMethod method) {
        this.beanMethods.add(method);
    }

    public Set<BeanMethod> getBeanMethods() {
        return this.beanMethods;
    }

    public Resource getResource() {
        return this.resource;
    }

    @Nullable
    public String getBeanName() {
        return this.beanName;
    }

    public void setBeanName(String beanName) {
        this.beanName = beanName;
    }
}
