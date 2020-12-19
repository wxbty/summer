package ink.zfei.summer.context.annotation;

import ink.zfei.summer.annation.Import;
import ink.zfei.summer.beans.BeanDefinitionRegistry;
import ink.zfei.summer.beans.BeanDefinitionRegistryPostProcessor;
import ink.zfei.summer.beans.factory.annotation.AnnotatedGenericBeanDefinition;
import ink.zfei.summer.beans.factory.config.BeanDefinition;
import ink.zfei.summer.beans.factory.config.BeanDefinitionHolder;
import ink.zfei.summer.context.AnnotationConfigApplicationContext;
import ink.zfei.summer.context.annotation.ConfigurationClassUtils;
import ink.zfei.summer.core.*;
import ink.zfei.summer.beans.factory.support.GenericBeanDefinition;
import ink.zfei.summer.core.annotation.Bean;
import ink.zfei.summer.core.annotation.Configuration;
import ink.zfei.summer.core.annotation.RootBeanDefination;
import ink.zfei.summer.core.io.DefaultResourceLoader;
import ink.zfei.summer.core.io.ResourceLoader;
import ink.zfei.summer.core.type.classreading.CachingMetadataReaderFactory;
import ink.zfei.summer.core.type.classreading.MetadataReaderFactory;
import ink.zfei.summer.util.AnnationUtil;

import java.lang.reflect.Method;
import java.util.*;

public class ConfigurationClassPostProcessor implements BeanDefinitionRegistryPostProcessor, PriorityOrdered {

    private MetadataReaderFactory metadataReaderFactory = new CachingMetadataReaderFactory();
    public static ThreadLocal<Map<String, RootBeanDefination>> configBeanInfos = ThreadLocal.withInitial(() -> new HashMap<>());
    private ConfigurationClassBeanDefinitionReader reader;
    private ResourceLoader resourceLoader = new DefaultResourceLoader();

    @Override
    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) {


        processConfigBeanDefinitions(registry);


    }

    public void processConfigBeanDefinitions(BeanDefinitionRegistry registry) {

        List<BeanDefinitionHolder> configCandidates = new ArrayList<>();
        List<BeanDefinitionRegistryPostProcessor> currentRegistryProcessors = new ArrayList<>();
        String[] candidateNames = registry.getBeanDefinitionNames();
        for (String beanName : candidateNames) {
            BeanDefinition beanDef = registry.getBeanDefinition(beanName);
            //如果是@Configuration,beanDef信息先缓存起来
            if (ConfigurationClassUtils.checkConfigurationClassCandidate(beanDef, this.metadataReaderFactory)) {
                configCandidates.add(new BeanDefinitionHolder(beanDef, beanName));
            }
        }

        // Return immediately if no @Configuration classes were found
        if (configCandidates.isEmpty()) {
            return;
        }

        ConfigurationClassParser parser = new ConfigurationClassParser(metadataReaderFactory, registry, resourceLoader);
        Set<BeanDefinitionHolder> candidates = new LinkedHashSet<>(configCandidates);
        Set<ConfigurationClass> alreadyParsed = new HashSet<>(configCandidates.size());

        do {
            parser.parse(candidates);
            Set<ConfigurationClass> configClasses = new LinkedHashSet<>(parser.getConfigurationClasses());
            configClasses.removeAll(alreadyParsed);
// Read the model and create bean definitions based on its content
            if (this.reader == null) {
                this.reader = new ConfigurationClassBeanDefinitionReader(
                        registry);
            }

            this.reader.loadBeanDefinitions(configClasses);
            alreadyParsed.addAll(configClasses);
            candidates.clear();

        } while (!candidates.isEmpty());

        //1、scan
        //2、this.reader.loadBeanDefinitions(configClasses);
        for (BeanDefinitionHolder bh : configCandidates) {
            loadFromConfiguration(registry, bh.getBeanDefinition().getBeanClassName());
        }


    }

    private void loadFromConfiguration(BeanDefinitionRegistry registry, String configuationName) {
        try {
            Class clazz = Class.forName(configuationName);
            Import in = (Import) AnnationUtil.findAnnotation(clazz, Import.class);
            if (in != null) {
                Class[] selectedClasss = in.value();
                for (Class selectedClass : selectedClasss) {
                    GenericBeanDefinition importBean = new GenericBeanDefinition();
                    importBean.setBeanClassName(selectedClass.getCanonicalName());
                    importBean.setId(selectedClass.getClass().getPackage().getName() + selectedClass.getSimpleName());

                    if (ImportSelector.class.isAssignableFrom(selectedClass)) {
                        ImportSelector importSelector = (ImportSelector) selectedClass.newInstance();
                        String[] importSelectors = importSelector.selectImports(clazz);
                        if (importSelectors == null) {
                            continue;
                        }
                        Arrays.stream(importSelectors).forEach(beanClassName -> {
                            GenericBeanDefinition selectBean = new GenericBeanDefinition();
                            selectBean.setBeanClassName(beanClassName);
                            Class outClass = null;
                            try {
                                outClass = Class.forName(beanClassName);
                            } catch (ClassNotFoundException e) {
                                e.printStackTrace();
                            }
                            if (AnnationUtil.isAnnotation(outClass, Configuration.class)) {
                                AnnotatedGenericBeanDefinition abd = new AnnotatedGenericBeanDefinition(outClass);
                                String beanName = AnnotationBeanNameGenerator.INSTANCE.generateBeanName(abd, registry);
                                registry.registerBeanDefinition(beanName, abd);
                                loadFromConfiguration(registry, outClass.getName());

                            } else {
                                String beanName = outClass.getSimpleName();
                                beanName = beanName.substring(0, 1).toLowerCase() + beanName.substring(1);
                                selectBean.setId(beanName);
                                registry.registerBeanDefinition(beanName, selectBean);
                                registry.registerBeanDefinition(importBean.getId(), importBean);
                            }
                        });
                    } else if (ImportBeanDefinitionRegistrar.class.isAssignableFrom(selectedClass)) {
                        ImportBeanDefinitionRegistrar registrar = (ImportBeanDefinitionRegistrar) selectedClass.newInstance();
                        registrar.registerBeanDefinitions(registry, clazz);

                    } else {
                        registry.registerBeanDefinition(importBean.getId(), importBean);
                    }
                }
            }

            String beanName = clazz.getSimpleName();
            beanName = beanName.substring(0, 1).toLowerCase() + beanName.substring(1);

            Method[] methods = clazz.getMethods();
            for (Method method : methods) {
                Bean bean = method.getAnnotation(Bean.class);
                if (bean != null) {
                    GenericBeanDefinition definition = new GenericBeanDefinition();
                    definition.setId(method.getName());
                    definition.setFactoryBeanName(beanName);
                    definition.setFactoryMethodName(method.getName());
                    registry.registerBeanDefinition(method.getName(), definition);
                }
            }
        } catch (ClassNotFoundException | IllegalAccessException | InstantiationException e) {
            e.printStackTrace();
        }


    }

    private void loadBeanDefinitionsFromRegistrars(AnnotationConfigApplicationContext registry, String name) {
    }

    @Override
    public void postProcessBeanFactory(AbstractApplicationContext abstractApplicationContext) {

    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;  // within PriorityOrdered
    }
}
