package ink.zfei.summer.context.annotation;

import ink.zfei.summer.annation.Component;
import ink.zfei.summer.beans.BeanDefinitionRegistry;
import ink.zfei.summer.beans.factory.config.BeanDefinition;
import ink.zfei.summer.beans.factory.config.BeanDefinitionHolder;
import ink.zfei.summer.beans.factory.support.BeanNameGenerator;
import ink.zfei.summer.core.annotation.AnnotationConfigUtils;
import ink.zfei.summer.core.io.support.ResourcePatternResolver;
import ink.zfei.summer.util.AnnationUtil;
import ink.zfei.summer.util.Assert;
import ink.zfei.summer.util.StringUtils;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.*;

import static ink.zfei.summer.util.AnnationUtil.resolveBasePackage;

public class ClassPathBeanDefinitionScanner extends ClassPathScanningCandidateComponentProvider{

    private final BeanDefinitionRegistry registry;

    private BeanNameGenerator beanNameGenerator = AnnotationBeanNameGenerator.INSTANCE;

    public ClassPathBeanDefinitionScanner(BeanDefinitionRegistry registry) {
        this.registry = registry;
    }

    private boolean includeAnnotationConfig = true;

    /**
     * Perform a scan within the specified base packages.
     *
     * @param basePackages the packages to check for annotated classes
     * @return 返回这次扫描注册了bean的数量
     */
    public int scan(String... basePackages) {
        int beanCountAtScanStart = this.registry.getBeanDefinitionCount();

        doScan(basePackages);
        // Register annotation config processors, if necessary.
        if (this.includeAnnotationConfig) {
            AnnotationConfigUtils.registerAnnotationConfigProcessors(this.registry);
        }

        return (this.registry.getBeanDefinitionCount() - beanCountAtScanStart);
    }

    protected Set<BeanDefinitionHolder> doScan(String... basePackages) {

        Assert.notEmpty(basePackages, "At least one base package must be specified");
        Set<BeanDefinitionHolder> beanDefinitions = new LinkedHashSet<>();
        for (String basePackage : basePackages) {
            Set<BeanDefinition> candidates = findCandidateComponents(basePackage);
            for (BeanDefinition candidate : candidates) {
                String beanName = this.beanNameGenerator.generateBeanName(candidate, this.registry);
                BeanDefinitionHolder definitionHolder = new BeanDefinitionHolder(candidate, beanName);
                //代理
                //                definitionHolder =
//                        AnnotationConfigUtils.applyScopedProxyMode(scopeMetadata, definitionHolder, this.registry);
                beanDefinitions.add(definitionHolder);
                this.registry.registerBeanDefinition(beanName, candidate);
            }
        }

        return beanDefinitions;
    }





    /**
     * Specify whether to register annotation config post-processors.
     * <p>The default is to register the post-processors. Turn this off
     * to be able to ignore the annotations or to process them differently.
     */
    public void setIncludeAnnotationConfig(boolean includeAnnotationConfig) {
        this.includeAnnotationConfig = includeAnnotationConfig;
    }
}
