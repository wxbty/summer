package ink.zfei.summer.core.annotation;

import ink.zfei.summer.annation.Import;
import ink.zfei.summer.beans.BeanDefinitionRegistry;
import ink.zfei.summer.beans.BeanDefinitionRegistryPostProcessor;
import ink.zfei.summer.context.AnnotationConfigApplicationContext;
import ink.zfei.summer.core.AbstractApplicationContext;
import ink.zfei.summer.beans.factory.support.GenericBeanDefinition;
import ink.zfei.summer.core.ImportBeanDefinitionRegistrar;
import ink.zfei.summer.core.ImportSelector;
import ink.zfei.summer.util.AnnationUtil;

import java.util.*;

public class ConfigurationClassPostProcessor implements BeanDefinitionRegistryPostProcessor {


    public static ThreadLocal<Map<String, RootBeanDefination>> configBeanInfos = ThreadLocal.withInitial(() -> new HashMap<>());

    @Override
    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) {


        processConfigBeanDefinitions(registry);


    }

    public void processConfigBeanDefinitions(BeanDefinitionRegistry registry) {
        //1、scan
        //2、this.reader.loadBeanDefinitions(configClasses);
        AnnotationConfigApplicationContext context = (AnnotationConfigApplicationContext) registry;
        List<String> tmpList = new ArrayList<>(context.getConfigurationNames());
        tmpList.forEach(configurationName -> loadFromConfiguration(context, configurationName));

    }

    private void loadFromConfiguration(AnnotationConfigApplicationContext registry, String configuationName) {
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
                                registry.register(outClass);
                                loadFromConfiguration(registry, outClass.getName());

                            } else {
                                String beanName = outClass.getSimpleName();
                                beanName = beanName.substring(0, 1).toLowerCase() + beanName.substring(1);
                                selectBean.setId(beanName);
                                registry.registerBeanDefinition(beanName,selectBean);
                                registry.registerBeanDefinition(importBean.getId(), importBean);
                            }
                        });
                    } else if (ImportBeanDefinitionRegistrar.class.isAssignableFrom(selectedClass)) {
                        ImportBeanDefinitionRegistrar registrar = (ImportBeanDefinitionRegistrar) selectedClass.newInstance();
                        registrar.registerBeanDefinitions(registry, clazz);

                    } else {
                        registry.registerBeanDefinition(importBean.getId(),importBean);
                    }
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
}
