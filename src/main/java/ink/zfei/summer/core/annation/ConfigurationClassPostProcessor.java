package ink.zfei.summer.core.annation;

import ink.zfei.summer.annation.Import;
import ink.zfei.summer.beans.BeanDefinitionRegistry;
import ink.zfei.summer.beans.BeanDefinitionRegistryPostProcessor;
import ink.zfei.summer.context.AnnotationConfigApplicationContext;
import ink.zfei.summer.core.AbstractApplicationContext;
import ink.zfei.summer.core.GenericBeanDefinition;
import ink.zfei.summer.core.ImportSelector;
import ink.zfei.summer.util.AnnationUtil;

import java.util.*;

public class ConfigurationClassPostProcessor implements BeanDefinitionRegistryPostProcessor {


    public static ThreadLocal<Map<String, RootBeanDefination>> configBeanInfos = ThreadLocal.withInitial(() -> new HashMap<>());

    @Override
    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) {

        AnnotationConfigApplicationContext context = (AnnotationConfigApplicationContext) registry;
        List<String> tmpList = new ArrayList<>(context.getConfiguationNames());
        tmpList.forEach(configuationName -> {
            loadFromConfiguration(context, configuationName);
        });


    }

    private void loadFromConfiguration(AnnotationConfigApplicationContext registry, String configuationName) {
        try {
            Class clazz = Class.forName(configuationName);
            Import in = (Import) AnnationUtil.findAnnotation(clazz, Import.class);
            if (in != null) {
                Class selectedClass = in.value()[0];
                GenericBeanDefinition importBean = new GenericBeanDefinition();
                importBean.setBeanClassName(selectedClass.getCanonicalName());
                importBean.setId("internal_" + selectedClass.getSimpleName());

                if (ImportSelector.class.isAssignableFrom(selectedClass)) {
                    ImportSelector importSelector = (ImportSelector) selectedClass.newInstance();
                    String[] importSelectors = importSelector.selectImports(clazz);
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
                            registry.registerBeanDefinition(selectBean);
                            registry.registerBeanDefinition(importBean);
                        }
                    });
                } else {
                    registry.registerBeanDefinition(importBean);
                }
            }

        } catch (ClassNotFoundException | IllegalAccessException | InstantiationException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void postProcessBeanFactory(AbstractApplicationContext abstractApplicationContext) {

    }
}
