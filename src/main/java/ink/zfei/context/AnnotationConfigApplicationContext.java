package ink.zfei.context;

import com.sun.istack.internal.Nullable;
import ink.zfei.annation.Component;
import ink.zfei.core.AbstractApplicationContext;
import ink.zfei.core.BeanDefinition;
import ink.zfei.core.annation.RootBeanDefination;
import ink.zfei.core.annation.AnnotationConfigUtils;
import ink.zfei.core.annation.Bean;
import ink.zfei.core.annation.Configuration;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;


public class AnnotationConfigApplicationContext extends AbstractApplicationContext {

    Map<String, String> maps = new ConcurrentHashMap<>();

    public List<BeanDefinition> getConfigBeanDefinitions() {
        return configBeanDefinitions;
    }

    public List<BeanDefinition> configBeanDefinitions = new ArrayList<>();

    public AnnotationConfigApplicationContext(String basePackages) throws IOException, URISyntaxException, ClassNotFoundException {
        super();
        scan(basePackages);
        try {
            refresh();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public AnnotationConfigApplicationContext(Class<?> componentClasses) {

        //1、注册一个bean工厂后置处理器，负责注册配置类里拿到的bean信息
        AnnotationConfigUtils.registerAnnotationConfigProcessors(this);

        //2、解析传入的配置类
        register(componentClasses);

        try {
            refresh();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private void register(Class<?> componentClasses) {

        Configuration annotations = componentClasses.getAnnotation(Configuration.class);
        if (annotations == null) {
            throw new RuntimeException("不是配置类");
        }
        BeanDefinition abd = new BeanDefinition();
        String beanName = componentClasses.getSimpleName();
        beanName = beanName.substring(0, 1).toLowerCase() + beanName.substring(1);
        abd.setId(beanName);
        abd.setBeanClass(componentClasses.getName());
        registerBeanDefinition(abd);

//        boolean isPorxy = annotations.proxyBeanMethods();
        Method[] methods = componentClasses.getMethods();
        for (Method method : methods) {
            Bean bean = method.getAnnotation(Bean.class);
            if (bean != null) {
                BeanDefinition definition = new BeanDefinition();
                definition.setId(method.getName());
                definition.setBeanClass(method.getReturnType().getName());
                definition.setFactoryBeanName(beanName);
                definition.setFactoryMethodName(method.getName());
                configBeanDefinitions.add(definition);
            }
        }

    }

    @Override
    protected Map<String, BeanDefinition> loadBeanDefination() {

        return maps.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, entry -> {
            BeanDefinition beanDefinition = new BeanDefinition();
            beanDefinition.setId(entry.getKey());
            beanDefinition.setBeanClass(entry.getValue());
            beanDefinition.setScope("singleton");
            return beanDefinition;
        }));
    }


    public void scan(String basePackages) throws IOException, URISyntaxException, ClassNotFoundException {
        if (basePackages == null) {
            throw new RuntimeException("At least one base package must be specified");
        }
        String path = resolveBasePackage(basePackages);

        ClassLoader cl = Thread.currentThread().getContextClassLoader();

        List<URL> list = new ArrayList<>();
        Enumeration<URL> resourceUrls = (cl != null ? cl.getResources(path) : ClassLoader.getSystemResources(path));
        while (resourceUrls.hasMoreElements()) {
            URL url = resourceUrls.nextElement();
            list.add(url);
        }

        URL url = list.get(0);
        File dir = new File(toURI(url.toString()).getSchemeSpecificPart());
        for (File content : listDirectory(dir)) {

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
                if (StringUtils.isBlank(beanName)) {
                    beanName = aClass.getName();
                    beanName = beanName.substring(0, 1).toLowerCase() + beanName.substring(1);
                }
                maps.put(beanName, className);
            }

        }

    }

    protected File[] listDirectory(File dir) {
        File[] files = dir.listFiles();
        if (files == null) {
            return new File[0];
        }
        Arrays.sort(files, Comparator.comparing(File::getName));
        return files;
    }

    private String resolveBasePackage(String basePackage) {
        return basePackage.replace('.', '/');
    }

    public static URI toURI(String location) throws URISyntaxException {
        return new URI(replace(location, " ", "%20"));
    }


    public static String replace(String inString, String oldPattern, @Nullable String newPattern) {
        if (newPattern == null) {
            return inString;
        }
        int index = inString.indexOf(oldPattern);
        if (index == -1) {
            // no occurrence -> can return input as-is
            return inString;
        }

        int capacity = inString.length();
        if (newPattern.length() > oldPattern.length()) {
            capacity += 16;
        }
        StringBuilder sb = new StringBuilder(capacity);

        int pos = 0;  // our position in the old string
        int patLen = oldPattern.length();
        while (index >= 0) {
            sb.append(inString, pos, index);
            sb.append(newPattern);
            pos = index + patLen;
            index = inString.indexOf(oldPattern, pos);
        }

        // append any characters to the right of a match
        sb.append(inString, pos, inString.length());
        return sb.toString();
    }

}
