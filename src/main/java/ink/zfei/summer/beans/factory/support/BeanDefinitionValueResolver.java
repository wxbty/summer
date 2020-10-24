package ink.zfei.summer.beans.factory.support;

import ink.zfei.summer.beans.BeanWrapper;
import ink.zfei.summer.beans.TypeConverter;
import ink.zfei.summer.beans.factory.BeanCreationException;
import ink.zfei.summer.beans.factory.BeanFactoryUtils;
import ink.zfei.summer.beans.factory.FactoryBean;
import ink.zfei.summer.beans.factory.config.*;
import ink.zfei.summer.core.AbstractApplicationContext;
import ink.zfei.summer.lang.Nullable;
import ink.zfei.summer.util.ObjectUtils;

import java.lang.reflect.Array;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static ink.zfei.summer.beans.factory.BeanFactoryUtils.transformedBeanName;

public class BeanDefinitionValueResolver {

    private final AbstractApplicationContext beanFactory;

    private final String beanName;

    private final GenericBeanDefinition beanDefinition;

    private final TypeConverter typeConverter;


    /**
     * Create a BeanDefinitionValueResolver for the given BeanFactory and BeanDefinition.
     *
     * @param beanFactory    the BeanFactory to resolve against
     * @param beanName       the name of the bean that we work on
     * @param beanDefinition the BeanDefinition of the bean that we work on
     * @param typeConverter  the TypeConverter to use for resolving TypedStringValues
     */
    public BeanDefinitionValueResolver(AbstractApplicationContext beanFactory, String beanName,
                                       BeanDefinition beanDefinition, TypeConverter typeConverter) {

        this.beanFactory = beanFactory;
        this.beanName = beanName;
        this.beanDefinition = (GenericBeanDefinition) beanDefinition;
        this.typeConverter = typeConverter;
    }


    /**
     * Given a PropertyValue, return a value, resolving any references to other
     * beans in the factory if necessary. The value could be:
     * <li>A BeanDefinition, which leads to the creation of a corresponding
     * new bean instance. Singleton flags and names of such "inner beans"
     * are always ignored: Inner beans are anonymous prototypes.
     * <li>A RuntimeBeanReference, which must be resolved.
     * <li>A ManagedList. This is a special collection that may contain
     * RuntimeBeanReferences or Collections that will need to be resolved.
     * <li>A ManagedSet. May also contain RuntimeBeanReferences or
     * Collections that will need to be resolved.
     * <li>A ManagedMap. In this case the value may be a RuntimeBeanReference
     * or Collection that will need to be resolved.
     * <li>An ordinary object or {@code null}, in which case it's left alone.
     *
     * @param argName the name of the argument that the value is defined for
     * @param value   the value object to resolve
     * @return the resolved object
     */
    @Nullable
    public Object resolveValueIfNecessary(Object argName, @Nullable Object value) {
        // We must check each value to see whether it requires a runtime reference
        // to another bean to be resolved.
        if (value instanceof RuntimeBeanReference) {
            //如果是依赖的是引用，从bean工厂中获取对应实例返回
            RuntimeBeanReference ref = (RuntimeBeanReference) value;
            return resolveReference(argName, ref);
        } else if (value instanceof RuntimeBeanNameReference) {
            String refName = ((RuntimeBeanNameReference) value).getBeanName();
            refName = String.valueOf(doEvaluate(refName));
            if (!this.beanFactory.containsBean(refName)) {
                throw new RuntimeException(
                        "Invalid bean name '" + refName + "' in bean reference for " + argName);
            }
            return refName;
        } else if (value instanceof BeanDefinitionHolder) {
            // Resolve BeanDefinitionHolder: contains BeanDefinition with name and aliases.
            BeanDefinitionHolder bdHolder = (BeanDefinitionHolder) value;
            return resolveInnerBean(argName, bdHolder.getBeanName(), bdHolder.getBeanDefinition());
        } else if (value instanceof BeanDefinition) {
            // Resolve plain BeanDefinition, without contained name: use dummy name.
            BeanDefinition bd = (BeanDefinition) value;
            String innerBeanName = "(inner bean)" + BeanFactoryUtils.GENERATED_BEAN_NAME_SEPARATOR +
                    ObjectUtils.getIdentityHexString(bd);
            return resolveInnerBean(argName, innerBeanName, bd);
        } else if (value instanceof DependencyDescriptor) {
            Set<String> autowiredBeanNames = new LinkedHashSet<>(4);
            Object result = this.beanFactory.resolveDependency(
                    (DependencyDescriptor) value, this.beanName, autowiredBeanNames, this.typeConverter);
            for (String autowiredBeanName : autowiredBeanNames) {
                if (this.beanFactory.containsBean(autowiredBeanName)) {
                    this.beanFactory.registerDependentBean(autowiredBeanName, this.beanName);
                }
            }
            return result;
        }
        // todo ManagedArray  ManagedList  ManagedSet ManagedMap ManagedProperties
        else if (value instanceof TypedStringValue) {
            // Convert value to target type here.
            //TypedStringValue 并不是字符串类型，包含了string、int、long等，根据类型把字符串转换成对应类型对象
            TypedStringValue typedStringValue = (TypedStringValue) value;
            Object valueObject = evaluate(typedStringValue);
            try {
                Class<?> resolvedTargetType = resolveTargetType(typedStringValue);
                if (resolvedTargetType != null) {
                    return this.typeConverter.convertIfNecessary(valueObject, resolvedTargetType);
                } else {
                    return valueObject;
                }
            } catch (Throwable ex) {
                // Improve the message by showing the context.
                throw new BeanCreationException(
                        this.beanName,
                        "Error converting typed String value for " + argName);
            }
        } else {
            return evaluate(value);
        }
    }

    /**
     * Evaluate the given value as an expression, if necessary.
     *
     * @param value the candidate value (may be an expression)
     * @return the resolved value
     */
    @Nullable
    protected Object evaluate(TypedStringValue value) {
        Object result = doEvaluate(value.getValue());
        if (!ObjectUtils.nullSafeEquals(result, value.getValue())) {
            value.setDynamic();
        }
        return result;
    }

    /**
     * Evaluate the given value as an expression, if necessary.
     *
     * @param value the original value (may be an expression)
     * @return the resolved value if necessary, or the original value
     */
    @Nullable
    protected Object evaluate(@Nullable Object value) {
        if (value instanceof String) {
            return doEvaluate((String) value);
        } else if (value instanceof String[]) {
            String[] values = (String[]) value;
            boolean actuallyResolved = false;
            Object[] resolvedValues = new Object[values.length];
            for (int i = 0; i < values.length; i++) {
                String originalValue = values[i];
                Object resolvedValue = doEvaluate(originalValue);
                if (resolvedValue != originalValue) {
                    actuallyResolved = true;
                }
                resolvedValues[i] = resolvedValue;
            }
            return (actuallyResolved ? resolvedValues : values);
        } else {
            return value;
        }
    }

    /**
     * Evaluate the given String value as an expression, if necessary.
     *
     * @param value the original value (may be an expression)
     * @return the resolved value if necessary, or the original String value
     */
    @Nullable
    private Object doEvaluate(@Nullable String value) {
        return this.beanFactory.evaluateBeanDefinitionString(value, this.beanDefinition);
    }

    /**
     * Resolve the target type in the given TypedStringValue.
     *
     * @param value the TypedStringValue to resolve
     * @return the resolved target type (or {@code null} if none specified)
     * @throws ClassNotFoundException if the specified type cannot be resolved
     * @see TypedStringValue#resolveTargetType
     */
    @Nullable
    protected Class<?> resolveTargetType(TypedStringValue value) throws ClassNotFoundException {
        if (value.hasTargetType()) {
            return value.getTargetType();
        }
        return value.resolveTargetType(this.beanFactory.getBeanClassLoader());
    }

    /**
     * Resolve a reference to another bean in the factory.
     * 从bean工厂中解析依赖的引用对象（引用依赖注入最终实现）
     */
    @Nullable
    private Object resolveReference(Object argName, RuntimeBeanReference ref) {
        Object bean;
        Class<?> beanType = ref.getBeanType();
        String resolvedName;
        if (beanType != null) {
            NamedBeanHolder<?> namedBean = this.beanFactory.resolveNamedBean(beanType);
            bean = namedBean.getBeanInstance();
            resolvedName = namedBean.getBeanName();
        } else {
            resolvedName = String.valueOf(doEvaluate(ref.getBeanName()));
            bean = this.beanFactory.getBean(resolvedName);
        }
        this.beanFactory.registerDependentBean(resolvedName, this.beanName);
        if (bean instanceof NullBean) {
            bean = null;
        }
        return bean;

    }

    /**
     * Resolve an inner bean definition.
     *
     * @param argName       the name of the argument that the inner bean is defined for
     * @param innerBeanName the name of the inner bean
     * @param innerBd       the bean definition for the inner bean
     * @return the resolved inner bean instance
     */
    @Nullable
    private Object resolveInnerBean(Object argName, String innerBeanName, BeanDefinition innerBd) {
        GenericBeanDefinition mbd = null;
        mbd = (GenericBeanDefinition) this.beanFactory.getBeanDefinition(innerBeanName);
        // Check given bean name whether it is unique. If not already unique,
        // add counter - increasing the counter until the name is unique.
        String actualInnerBeanName = innerBeanName;
        if (mbd.isSingleton()) {
            actualInnerBeanName = adaptInnerBeanName(innerBeanName);
        }
//            this.beanFactory.registerContainedBean(actualInnerBeanName, this.beanName);
        // Guarantee initialization of beans that the inner bean depends on.

        // Actually create the inner bean instance now...
        Object innerBean = this.beanFactory.createBean(actualInnerBeanName, mbd, null);
        if (innerBean instanceof FactoryBean) {
            innerBean = this.beanFactory.getObjectFromFactoryBean(
                    (FactoryBean<?>) innerBean, actualInnerBeanName);
        }
        if (innerBean instanceof NullBean) {
            innerBean = null;
        }
        return innerBean;

    }

    /**
     * Checks the given bean name whether it is unique. If not already unique,
     * a counter is added, increasing the counter until the name is unique.
     *
     * @param innerBeanName the original name for the inner bean
     * @return the adapted name for the inner bean
     */
    private String adaptInnerBeanName(String innerBeanName) {
        String actualInnerBeanName = innerBeanName;
        int counter = 0;
        String prefix = innerBeanName + BeanFactoryUtils.GENERATED_BEAN_NAME_SEPARATOR;
        while (isBeanNameInUse(actualInnerBeanName)) {
            counter++;
            actualInnerBeanName = prefix + counter;
        }
        return actualInnerBeanName;
    }

    public boolean isBeanNameInUse(String beanName) {
        return  containsLocalBean(beanName) || hasDependentBean(beanName);
    }

    private final Map<String, Set<String>> dependentBeanMap = new ConcurrentHashMap<>(64);

    public boolean containsLocalBean(String name) {
        String beanName = transformedBeanName(name);
        return ((containsSingleton(beanName)  &&
                (!BeanFactoryUtils.isFactoryDereference(name))));
    }
    public boolean containsSingleton(String beanName) {
        return this.beanFactory.containsBean(beanName);
    }


    protected boolean hasDependentBean(String beanName) {
        return this.dependentBeanMap.containsKey(beanName);
    }
    /**
     * For each element in the managed array, resolve reference if necessary.
     */
    private Object resolveManagedArray(Object argName, List<?> ml, Class<?> elementType) {
        Object resolved = Array.newInstance(elementType, ml.size());
        for (int i = 0; i < ml.size(); i++) {
            Array.set(resolved, i, resolveValueIfNecessary(new KeyedArgName(argName, i), ml.get(i)));
        }
        return resolved;
    }

    /**
     * For each element in the managed list, resolve reference if necessary.
     */
    private List<?> resolveManagedList(Object argName, List<?> ml) {
        List<Object> resolved = new ArrayList<>(ml.size());
        for (int i = 0; i < ml.size(); i++) {
            resolved.add(resolveValueIfNecessary(new KeyedArgName(argName, i), ml.get(i)));
        }
        return resolved;
    }

    /**
     * For each element in the managed set, resolve reference if necessary.
     */
    private Set<?> resolveManagedSet(Object argName, Set<?> ms) {
        Set<Object> resolved = new LinkedHashSet<>(ms.size());
        int i = 0;
        for (Object m : ms) {
            resolved.add(resolveValueIfNecessary(new KeyedArgName(argName, i), m));
            i++;
        }
        return resolved;
    }

    /**
     * For each element in the managed map, resolve reference if necessary.
     */
    private Map<?, ?> resolveManagedMap(Object argName, Map<?, ?> mm) {
        Map<Object, Object> resolved = new LinkedHashMap<>(mm.size());
        mm.forEach((key, value) -> {
            Object resolvedKey = resolveValueIfNecessary(argName, key);
            Object resolvedValue = resolveValueIfNecessary(new KeyedArgName(argName, key), value);
            resolved.put(resolvedKey, resolvedValue);
        });
        return resolved;
    }


    /**
     * Holder class used for delayed toString building.
     */
    private static class KeyedArgName {

        private final Object argName;

        private final Object key;

        public KeyedArgName(Object argName, Object key) {
            this.argName = argName;
            this.key = key;
        }

        @Override
        public String toString() {
            return this.argName + " with key " + BeanWrapper.PROPERTY_KEY_PREFIX +
                    this.key + BeanWrapper.PROPERTY_KEY_SUFFIX;
        }
    }

}
