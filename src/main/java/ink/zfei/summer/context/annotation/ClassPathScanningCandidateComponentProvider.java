package ink.zfei.summer.context.annotation;

import ink.zfei.summer.annation.Component;
import ink.zfei.summer.beans.factory.annotation.AnnotatedGenericBeanDefinition;
import ink.zfei.summer.beans.factory.config.BeanDefinition;
import ink.zfei.summer.core.io.Resource;
import ink.zfei.summer.core.io.support.PathMatchingResourcePatternResolver;
import ink.zfei.summer.core.io.support.ResourcePatternResolver;
import ink.zfei.summer.core.type.classreading.CachingMetadataReaderFactory;
import ink.zfei.summer.core.type.classreading.MetadataReader;
import ink.zfei.summer.core.type.classreading.MetadataReaderFactory;
import ink.zfei.summer.lang.Nullable;
import ink.zfei.summer.util.AnnationUtil;
import ink.zfei.summer.util.StringUtils;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.*;

import static ink.zfei.summer.util.AnnationUtil.resolveBasePackage;

public class ClassPathScanningCandidateComponentProvider {

    static final String DEFAULT_RESOURCE_PATTERN = "**/*.class";

    private MetadataReaderFactory metadataReaderFactory;

    private String resourcePattern = DEFAULT_RESOURCE_PATTERN;

    @Nullable
    private ResourcePatternResolver resourcePatternResolver;

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

//        String packageSearchPath = ResourcePatternResolver.CLASSPATH_ALL_URL_PREFIX +
//                resolveBasePackage(basePackage) + '/' + this.resourcePattern;
//        Resource[] resources = getResourcePatternResolver().getResources(packageSearchPath);

        String path = resolveBasePackage(basePackage);

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

                //根据package扫描的bean，spring使用asm解析class文件，获取class元信息，这样不会提早触发类加载
                //ScannedGenericBeanDefinition 在spring中是asm的实现，这里用反射实现，和AnnotatedGenericBeanDefinition一模一样
                ScannedGenericBeanDefinition abd = new ScannedGenericBeanDefinition(aClass);

                candidates.add(abd);
            }

        }
        return candidates;
    }

    private ResourcePatternResolver getResourcePatternResolver() {
        if (this.resourcePatternResolver == null) {
            this.resourcePatternResolver = new PathMatchingResourcePatternResolver();
        }
        return this.resourcePatternResolver;
    }

    public final MetadataReaderFactory getMetadataReaderFactory() {
        if (this.metadataReaderFactory == null) {
            this.metadataReaderFactory = new CachingMetadataReaderFactory();
        }
        return this.metadataReaderFactory;
    }
}
