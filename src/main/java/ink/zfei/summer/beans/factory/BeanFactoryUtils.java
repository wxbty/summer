package ink.zfei.summer.beans.factory;

import ink.zfei.summer.core.AbstractApplicationContext;
import ink.zfei.summer.lang.Nullable;
import ink.zfei.summer.util.Assert;

import java.lang.annotation.Annotation;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public abstract class BeanFactoryUtils {

    /**
     * Separator for generated bean names. If a class name or parent name is not
     * unique, "#1", "#2" etc will be appended, until the name becomes unique.
     */
    public static final String GENERATED_BEAN_NAME_SEPARATOR = "#";

    /**
     * Cache from name with factory bean prefix to stripped name without dereference.
     * @since 5.1
     * @see BeanFactory#FACTORY_BEAN_PREFIX
     */
    private static final Map<String, String> transformedBeanNameCache = new ConcurrentHashMap<>();


    /**
     * Return whether the given name is a factory dereference
     * (beginning with the factory dereference prefix).
     * @param name the name of the bean
     * @return whether the given name is a factory dereference
     * @see BeanFactory#FACTORY_BEAN_PREFIX
     */
    public static boolean isFactoryDereference(@Nullable String name) {
        return (name != null && name.startsWith(BeanFactory.FACTORY_BEAN_PREFIX));
    }

    /**
     * Return the actual bean name, stripping out the factory dereference
     * prefix (if any, also stripping repeated factory prefixes if found).
     * @param name the name of the bean
     * @return the transformed name
     * @see BeanFactory#FACTORY_BEAN_PREFIX
     */
    public static String transformedBeanName(String name) {
        Assert.notNull(name, "'name' must not be null");
        if (!name.startsWith(BeanFactory.FACTORY_BEAN_PREFIX)) {
            return name;
        }
        return transformedBeanNameCache.computeIfAbsent(name, beanName -> {
            do {
                beanName = beanName.substring(BeanFactory.FACTORY_BEAN_PREFIX.length());
            }
            while (beanName.startsWith(BeanFactory.FACTORY_BEAN_PREFIX));
            return beanName;
        });
    }

    public static boolean isGeneratedBeanName(@Nullable String name) {
        return (name != null && name.contains(GENERATED_BEAN_NAME_SEPARATOR));
    }

    /**
     * Extract the "raw" bean name from the given (potentially generated) bean name,
     * excluding any "#..." suffixes which might have been added for uniqueness.
     * @param name the potentially generated bean name
     * @return the raw bean name
     * @see #GENERATED_BEAN_NAME_SEPARATOR
     */
    public static String originalBeanName(String name) {
        Assert.notNull(name, "'name' must not be null");
        int separatorIndex = name.indexOf(GENERATED_BEAN_NAME_SEPARATOR);
        return (separatorIndex != -1 ? name.substring(0, separatorIndex) : name);
    }

    public static String[] beanNamesForTypeIncludingAncestors(AbstractApplicationContext lbf, Class<?> type) {
        Assert.notNull(lbf, "ListableBeanFactory must not be null");
        String[] result = lbf.getBeanNamesForType(type);
        return result;
    }



}
