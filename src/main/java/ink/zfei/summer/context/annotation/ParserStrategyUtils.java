package ink.zfei.summer.context.annotation;

import ink.zfei.summer.beans.BeanDefinitionRegistry;
import ink.zfei.summer.beans.BeanInstantiationException;
import ink.zfei.summer.beans.factory.Aware;
import ink.zfei.summer.beans.factory.BeanFactory;
import ink.zfei.summer.beans.factory.BeanFactoryAware;
import ink.zfei.summer.beans.factory.config.ConfigurableBeanFactory;
import ink.zfei.summer.core.env.Environment;
import ink.zfei.summer.core.io.ResourceLoader;
import ink.zfei.summer.lang.Nullable;
import ink.zfei.summer.util.Assert;
import ink.zfei.summer.util.BeanUtils;

import java.lang.reflect.Constructor;

public class ParserStrategyUtils {

    @SuppressWarnings("unchecked")
    static <T> T instantiateClass(Class<?> clazz, Class<T> assignableTo,
                                  ResourceLoader resourceLoader, BeanDefinitionRegistry registry) {

        Assert.notNull(clazz, "Class must not be null");
        Assert.isAssignable(assignableTo, clazz);
        if (clazz.isInterface()) {
            throw new BeanInstantiationException(clazz, "Specified class is an interface");
        }
        ClassLoader classLoader = (registry instanceof ConfigurableBeanFactory ?
                ((ConfigurableBeanFactory) registry).getBeanClassLoader() : resourceLoader.getClassLoader());
        //构造器注入
        T instance = (T) createInstance(clazz, resourceLoader, registry, classLoader);
        //aware接口注入
        ParserStrategyUtils.invokeAwareMethods(instance, resourceLoader, registry, classLoader);
        return instance;
    }

    //根据参数类型，自动注入beanFactory或classLader
    private static Object createInstance(Class<?> clazz,
                                         ResourceLoader resourceLoader, BeanDefinitionRegistry registry,
                                         @Nullable ClassLoader classLoader) {

        Constructor<?>[] constructors = clazz.getDeclaredConstructors();
        if (constructors.length == 1 && constructors[0].getParameterCount() > 0) {
            try {
                Constructor<?> constructor = constructors[0];
                Object[] args = resolveArgs(constructor.getParameterTypes(), resourceLoader, registry, classLoader);
                return BeanUtils.instantiateClass(constructor, args);
            } catch (Exception ex) {
                throw new BeanInstantiationException(clazz, "No suitable constructor found", ex);
            }
        }
        return BeanUtils.instantiateClass(clazz);
    }

    private static Object[] resolveArgs(Class<?>[] parameterTypes,
                                        ResourceLoader resourceLoader,
                                        BeanDefinitionRegistry registry, @Nullable ClassLoader classLoader) {

        Object[] parameters = new Object[parameterTypes.length];
        for (int i = 0; i < parameterTypes.length; i++) {
            parameters[i] = resolveParameter(parameterTypes[i],
                    resourceLoader, registry, classLoader);
        }
        return parameters;
    }


    private static Object resolveParameter(Class<?> parameterType,
                                           ResourceLoader resourceLoader,
                                           BeanDefinitionRegistry registry, @Nullable ClassLoader classLoader) {


        if (parameterType == ResourceLoader.class) {
            return resourceLoader;
        }
        if (parameterType == BeanFactory.class) {
            return (registry instanceof BeanFactory ? registry : null);
        }
        if (parameterType == ClassLoader.class) {
            return classLoader;
        }
        throw new IllegalStateException("Illegal method parameter type: " + parameterType.getName());
    }


    private static void invokeAwareMethods(Object parserStrategyBean,
                                           ResourceLoader resourceLoader, BeanDefinitionRegistry registry, @Nullable ClassLoader classLoader) {

        if (parserStrategyBean instanceof Aware) {

            if (parserStrategyBean instanceof BeanFactoryAware && registry instanceof BeanFactory) {
                ((BeanFactoryAware) parserStrategyBean).setBeanFactory((BeanFactory) registry);
            }
        }
    }
}
