package ink.zfei.summer.context.annotation;

import ink.zfei.summer.annation.Component;
import ink.zfei.summer.beans.BeanDefinitionRegistry;
import ink.zfei.summer.beans.factory.config.BeanDefinition;
import ink.zfei.summer.beans.factory.config.BeanDefinitionHolder;
import ink.zfei.summer.beans.factory.support.BeanNameGenerator;
import ink.zfei.summer.core.annotation.AnnotationConfigUtils;
import ink.zfei.summer.util.AnnationUtil;
import ink.zfei.summer.util.Assert;
import ink.zfei.summer.util.StringUtils;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.*;

public class ClassPathBeanDefinitionScanner {

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
            }
        }

        return beanDefinitions;
    }

    public Set<BeanDefinition> findCandidateComponents(String basePackage) {
//        if (this.componentsIndex != null && indexSupportsIncludeFilters()) {
//            return addCandidateComponentsFromIndex(this.componentsIndex, basePackage);
//        }    spring5新出indexer机制，加快spring扫描注解的速度
//        1、@Component 上附加@Indexed注解，spring5默认已加
//        2、引入spring-context-indexer依赖
//        3、项目编译时，自动生成 METE-INF/spring.components
//        4、CandidateComponentsIndexLoader 读取并加载文件，代替扫描，提升性能

        try {
            return scanCandidateComponents(basePackage);
        } catch (IOException | ClassNotFoundException | URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }


    public Set<BeanDefinition> scanCandidateComponents(String basePackage) throws IOException, URISyntaxException, ClassNotFoundException {

        String path = AnnationUtil.resolveBasePackage(basePackage);

        ClassLoader cl = Thread.currentThread().getContextClassLoader();

        List<URL> list = new ArrayList<>();
        Enumeration<URL> resourceUrls = (cl != null ? cl.getResources(path) : ClassLoader.getSystemResources(path));
        while (resourceUrls.hasMoreElements()) {
            URL url = resourceUrls.nextElement();
            list.add(url);
        }

        URL url = list.get(0);
        File dir = new File(AnnationUtil.toURI(url.toString()).getSchemeSpecificPart());
        return loadScanBean(basePackage, dir);

    }


    private Set<BeanDefinition> loadScanBean(String basePackages, File dir) throws ClassNotFoundException {

        Set<BeanDefinition> candidates = new LinkedHashSet<>();

        for (File content : AnnationUtil.listDirectory(dir)) {
            if (content.isDirectory()) {
                candidates.addAll(loadScanBean(basePackages + "." + content.getName(), content));
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
                ScannedGenericBeanDefinition sbd = new ScannedGenericBeanDefinition();
                sbd.setBeanClassName(className);
                this.registry.registerBeanDefinition(beanName, sbd);
                candidates.add(sbd);
            }

        }
        return candidates;
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
