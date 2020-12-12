package ink.zfei.summer.context.support;

import ink.zfei.summer.beans.BeanDefinitionRegistry;
import ink.zfei.summer.beans.BeanDefinitionRegistryPostProcessor;
import ink.zfei.summer.beans.BeanFactoryPostProcessor;
import ink.zfei.summer.beans.BeanPostProcessor;
import ink.zfei.summer.beans.factory.config.ConfigurableListableBeanFactory;
import ink.zfei.summer.beans.factory.support.MergedBeanDefinitionPostProcessor;
import ink.zfei.summer.core.AbstractApplicationContext;
import ink.zfei.summer.core.Ordered;
import ink.zfei.summer.core.PriorityOrdered;
import ink.zfei.summer.util.StringUtils;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

public class PostProcessorRegistrationDelegate {
    private PostProcessorRegistrationDelegate() {
    }

    public static void invokeBeanFactoryPostProcessors(
            ConfigurableListableBeanFactory beanFactory, List<BeanFactoryPostProcessor> beanFactoryPostProcessors) {


        List<BeanDefinitionRegistryPostProcessor> registryProcessors = new ArrayList<>();
        // Invoke BeanDefinitionRegistryPostProcessors first, if any.
        Set<String> processedBeans = new HashSet<>();
        if (beanFactory instanceof BeanDefinitionRegistry) {
            BeanDefinitionRegistry registry = (BeanDefinitionRegistry) beanFactory;
            List<BeanFactoryPostProcessor> regularPostProcessors = new ArrayList<>();
            for (BeanFactoryPostProcessor postProcessor : beanFactoryPostProcessors) {
                if (postProcessor instanceof BeanDefinitionRegistryPostProcessor) {
                    BeanDefinitionRegistryPostProcessor registryProcessor =
                            (BeanDefinitionRegistryPostProcessor) postProcessor;
                    registryProcessor.postProcessBeanDefinitionRegistry(registry);
                    registryProcessors.add(registryProcessor);
                }
                else {
                    regularPostProcessors.add(postProcessor);
                }
            }

            List<BeanDefinitionRegistryPostProcessor> currentRegistryProcessors = new ArrayList<>();

            String[] postProcessorNames =
                    beanFactory.getBeanNamesForType(BeanDefinitionRegistryPostProcessor.class, true, false);
            for (String ppName : postProcessorNames) {
                if (beanFactory.isTypeMatch(ppName, PriorityOrdered.class)) {
                    currentRegistryProcessors.add(beanFactory.getBean(ppName, BeanDefinitionRegistryPostProcessor.class));
                    processedBeans.add(ppName);
                }
            }
            registryProcessors.addAll(currentRegistryProcessors);
            invokeBeanDefinitionRegistryPostProcessors(currentRegistryProcessors, registry);
        }



//        List<String> tmpBeanDefinitionNames = new ArrayList<>(beanDefinitionNames);
//        //1、扫描beanDefination,找到BeanFactoryPostProcessor实现，实例化
//        //1、遍历beanDefination，把BeanPostProcessors类型的bean
//        tmpBeanDefinitionNames.stream().filter(id -> {
//            String beanClass = beanDefinitionMap.get(id).getBeanClassName();
//            if (StringUtils.isEmpty(beanClass)) {
//                return false;
//            }
//            try {
//                Class clazz = Class.forName(beanClass);
//                return BeanFactoryPostProcessor.class.isAssignableFrom(clazz);
//            } catch (ClassNotFoundException e) {
//                return false;
//            }
//        }).forEach(id -> {
//            //2、提前注册，实例化
//            BeanFactoryPostProcessor beanFactoryPostProcessor = (BeanFactoryPostProcessor) getBean(id);
//            if (beanFactoryPostProcessor instanceof BeanDefinitionRegistryPostProcessor) {
//                BeanDefinitionRegistryPostProcessor registryPostProcessor = (BeanDefinitionRegistryPostProcessor) beanFactoryPostProcessor;
//                if (this instanceof BeanDefinitionRegistry) {
//                    registryPostProcessor.postProcessBeanDefinitionRegistry((BeanDefinitionRegistry) this);
//                }
//            }
//
//            beanFactoryPostProcessor.postProcessBeanFactory(this);
//
////            this.beanFactoryPostProcessorMap.put(id, beanFactoryPostProcessor);
//            beanFactoryPostProcessors.add(beanFactoryPostProcessor);
//
//
//        });

        //2、把当前的spring容器传入BeanFactoryPostProcessor，执行invoke方法

    }

    /**
     * Invoke the given BeanDefinitionRegistryPostProcessor beans.
     */
    private static void invokeBeanDefinitionRegistryPostProcessors(
            Collection<? extends BeanDefinitionRegistryPostProcessor> postProcessors, BeanDefinitionRegistry registry) {

        for (BeanDefinitionRegistryPostProcessor postProcessor : postProcessors) {
            postProcessor.postProcessBeanDefinitionRegistry(registry);
        }
    }


    private static void registerBeanPostProcessors(
            ConfigurableListableBeanFactory beanFactory, List<BeanPostProcessor> postProcessors) {

        for (BeanPostProcessor postProcessor : postProcessors) {
            beanFactory.addBeanPostProcessor(postProcessor);
        }
    }

    public static void registerBeanPostProcessors(
            ConfigurableListableBeanFactory beanFactory, AbstractApplicationContext applicationContext) {

        String[] postProcessorNames = beanFactory.getBeanNamesForType(BeanPostProcessor.class, true, false);

        // Register BeanPostProcessorChecker that logs an info message when
        // a bean is created during BeanPostProcessor instantiation, i.e. when
        // a bean is not eligible for getting processed by all BeanPostProcessors.
        int beanProcessorTargetCount = beanFactory.getBeanPostProcessorCount() + 1 + postProcessorNames.length;
//        beanFactory.addBeanPostProcessor(new BeanPostProcessorChecker(beanFactory, beanProcessorTargetCount));

        // Separate between BeanPostProcessors that implement PriorityOrdered,
        // Ordered, and the rest.
        List<BeanPostProcessor> priorityOrderedPostProcessors = new ArrayList<>();
        List<BeanPostProcessor> internalPostProcessors = new ArrayList<>();
        List<String> orderedPostProcessorNames = new ArrayList<>();
        List<String> nonOrderedPostProcessorNames = new ArrayList<>();
        for (String ppName : postProcessorNames) {
            if (beanFactory.isTypeMatch(ppName, PriorityOrdered.class)) {
                BeanPostProcessor pp = beanFactory.getBean(ppName, BeanPostProcessor.class);
                priorityOrderedPostProcessors.add(pp);
                if (pp instanceof MergedBeanDefinitionPostProcessor) {
                    internalPostProcessors.add(pp);
                }
            }
            else if (beanFactory.isTypeMatch(ppName, Ordered.class)) {
                orderedPostProcessorNames.add(ppName);
            }
            else {
                nonOrderedPostProcessorNames.add(ppName);
            }
        }

        // First, register the BeanPostProcessors that implement PriorityOrdered.
//        sortPostProcessors(priorityOrderedPostProcessors, beanFactory);
        registerBeanPostProcessors(beanFactory, priorityOrderedPostProcessors);

        // Next, register the BeanPostProcessors that implement Ordered.
        List<BeanPostProcessor> orderedPostProcessors = new ArrayList<>(orderedPostProcessorNames.size());
        for (String ppName : orderedPostProcessorNames) {
            BeanPostProcessor pp = beanFactory.getBean(ppName, BeanPostProcessor.class);
            orderedPostProcessors.add(pp);
            if (pp instanceof MergedBeanDefinitionPostProcessor) {
                internalPostProcessors.add(pp);
            }
        }
//        sortPostProcessors(orderedPostProcessors, beanFactory);
        registerBeanPostProcessors(beanFactory, orderedPostProcessors);

        // Now, register all regular BeanPostProcessors.
        List<BeanPostProcessor> nonOrderedPostProcessors = new ArrayList<>(nonOrderedPostProcessorNames.size());
        for (String ppName : nonOrderedPostProcessorNames) {
            BeanPostProcessor pp = beanFactory.getBean(ppName, BeanPostProcessor.class);
            nonOrderedPostProcessors.add(pp);
            if (pp instanceof MergedBeanDefinitionPostProcessor) {
                internalPostProcessors.add(pp);
            }
        }
        registerBeanPostProcessors(beanFactory, nonOrderedPostProcessors);

        // Finally, re-register all internal BeanPostProcessors.
//        sortPostProcessors(internalPostProcessors, beanFactory);
        registerBeanPostProcessors(beanFactory, internalPostProcessors);

        // Re-register post-processor for detecting inner beans as ApplicationListeners,
        // moving it to the end of the processor chain (for picking up proxies etc).
//        beanFactory.addBeanPostProcessor(new ApplicationListenerDetector(applicationContext));
    }

}
