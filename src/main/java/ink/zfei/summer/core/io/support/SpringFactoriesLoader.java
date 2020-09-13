package ink.zfei.summer.core.io.support;

import ink.zfei.summer.util.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.*;

public class SpringFactoriesLoader {


    public static final String FACTORIES_RESOURCE_LOCATION = "META-INF/spring.factories";

    public static List<String> loadFactoryNames(Class<?> factoryType, ClassLoader classLoader) {
        String factoryTypeName = factoryType.getName();
        return loadSpringFactories(classLoader).getOrDefault(factoryTypeName, Collections.emptyList());
    }


    private static Map<String, List<String>> loadSpringFactories(ClassLoader classLoader) {
        try {
            Enumeration<URL> urls = (classLoader != null ?
                    classLoader.getResources(FACTORIES_RESOURCE_LOCATION) :
                    ClassLoader.getSystemResources(FACTORIES_RESOURCE_LOCATION));
            Map<String, List<String>> result = new HashMap<>();
            while (urls.hasMoreElements()) {
                URL url = urls.nextElement();
                InputStream is = url.openStream();
                Properties properties = new Properties();
                properties.load(is);
                for (Map.Entry<?, ?> entry : properties.entrySet()) {
                    String factoryTypeName = ((String) entry.getKey()).trim();
                    for (String factoryImplementationName : StringUtils.commaDelimitedListToStringArray((String) entry.getValue())) {
                        List<String> values = result.computeIfAbsent(factoryTypeName, k -> new LinkedList<>());
                        values.add(factoryImplementationName.trim());
                        result.put(factoryTypeName, values);
                    }
                }
            }
            return result;
        } catch (IOException ex) {
            throw new IllegalArgumentException("Unable to load factories from location [" +
                    FACTORIES_RESOURCE_LOCATION + "]", ex);
        }
    }
}
