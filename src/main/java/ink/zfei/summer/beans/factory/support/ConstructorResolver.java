package ink.zfei.summer.beans.factory.support;

import com.sun.corba.se.impl.io.TypeMismatchException;
import ink.zfei.summer.beans.BeanMetadataElement;
import ink.zfei.summer.beans.BeanWrapper;
import ink.zfei.summer.beans.BeanWrapperImpl;
import ink.zfei.summer.beans.TypeConverter;
import ink.zfei.summer.beans.factory.BeanCreationException;
import ink.zfei.summer.beans.factory.BeanDefinitionStoreException;
import ink.zfei.summer.beans.factory.InjectionPoint;
import ink.zfei.summer.beans.factory.NoSuchBeanDefinitionException;
import ink.zfei.summer.beans.factory.config.AutowireCapableBeanFactory;
import ink.zfei.summer.beans.factory.config.ConstructorArgumentValues;
import ink.zfei.summer.beans.factory.config.DependencyDescriptor;
import ink.zfei.summer.core.AbstractApplicationContext;
import ink.zfei.summer.core.MethodParameter;
import ink.zfei.summer.core.NamedThreadLocal;
import ink.zfei.summer.core.ParameterNameDiscoverer;
import ink.zfei.summer.core.convert.ConversionService;
import ink.zfei.summer.lang.Nullable;
import ink.zfei.summer.util.Assert;
import ink.zfei.summer.util.ClassUtils;
import ink.zfei.summer.util.ReflectionUtils;
import org.apache.commons.logging.Log;

import java.beans.ConstructorProperties;
import java.lang.reflect.*;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.*;

public class ConstructorResolver {

    private static final Object[] EMPTY_ARGS = new Object[0];

    /**
     * Marker for autowired arguments in a cached argument array, to be later replaced
     * by a {@linkplain #resolveAutowiredArgument resolved autowired argument}.
     */
    private static final Object autowiredArgumentMarker = new Object();

    private static final NamedThreadLocal<InjectionPoint> currentInjectionPoint =
            new NamedThreadLocal<>("Current injection point");


    private final Log logger;

    private final AbstractAutowireCapableBeanFactory beanFactory;

    /**
     * Create a new ConstructorResolver for the given factory and instantiation strategy.
     *
     * @param beanFactory the BeanFactory to work with
     */
    public ConstructorResolver(AbstractAutowireCapableBeanFactory beanFactory) {
        this.beanFactory = beanFactory;
        this.logger = beanFactory.getLogger();
    }


    /**
     * 到这一步，哪些构造器（多个候选）已经决定，获取构造器的参数类型和bd中已解析的参数vals，反射匹配生成实例
     * 这个方法要根据xml配置文件中惟一的bean配置找到合适的构造方法（可能有多个）并返回实例换包装类，
     *
     * @param beanName     the name of the bean
     * @param mbd          the merged bean definition for the bean
     * @param chosenCtors  chosen candidate constructors (or {@code null} if none)
     * @param explicitArgs argument values passed in programmatically via the getBean method,
     *                     or {@code null} if none (-> use constructor argument values from bean definition)
     * @return a BeanWrapper for the new instance
     */
    public BeanWrapper autowireConstructor(String beanName, GenericBeanDefinition mbd,
                                           @Nullable Constructor<?>[] chosenCtors, @Nullable Object[] explicitArgs) {

        BeanWrapperImpl bw = new BeanWrapperImpl();
        this.beanFactory.initBeanWrapper(bw);

        Constructor<?> constructorToUse = null;
        ArgumentsHolder argsHolderToUse = null;
        //最终被使用的参数值（被注入的实例对象）集合
        Object[] argsToUse = null;

        if (explicitArgs != null) {
            //外部程序显示调用，如beanFactory.getBean("name",objs)
            argsToUse = explicitArgs;
        } else {
            Object[] argsToResolve = null;
            constructorToUse = (Constructor<?>) mbd.resolvedConstructorOrFactoryMethod;
            if (constructorToUse != null && mbd.constructorArgumentsResolved) {
                // Found a cached constructor...
                argsToUse = mbd.resolvedConstructorArguments;
                if (argsToUse == null) {
                    argsToResolve = mbd.preparedConstructorArguments;
                }
            }
            if (argsToResolve != null) {
                //如给定方法的构造函数 A(int,int)则通过此方法后就会把配置中的("1","1")转换为(1,1)
                argsToUse = resolvePreparedArguments(beanName, mbd, bw, constructorToUse, argsToResolve, true);
            }
        }

        if (constructorToUse == null || argsToUse == null) {
            // Take specified constructors, if any.
            Constructor<?>[] candidates = chosenCtors;
            if (candidates == null) {
                Class<?> beanClass = mbd.getBeanClass();
                candidates = (mbd.isNonPublicAccessAllowed() ?
                        beanClass.getDeclaredConstructors() : beanClass.getConstructors());

            }

            if (candidates.length == 1 && explicitArgs == null && !mbd.hasConstructorArgumentValues()) {
                Constructor<?> uniqueCandidate = candidates[0];
                if (uniqueCandidate.getParameterCount() == 0) {
                    mbd.resolvedConstructorOrFactoryMethod = uniqueCandidate;
                    mbd.constructorArgumentsResolved = true;
                    mbd.resolvedConstructorArguments = EMPTY_ARGS;
                    bw.setBeanInstance(instantiate(beanName, mbd, uniqueCandidate, EMPTY_ARGS));
                    return bw;
                }
            }

            // Need to resolve the constructor.
            boolean autowiring = (chosenCtors != null ||
                    mbd.getResolvedAutowireMode() == AutowireCapableBeanFactory.AUTOWIRE_CONSTRUCTOR);
            ConstructorArgumentValues resolvedValues = null;

            /*
             * minNrOfArgs 的计算为后面寻找合适的构造方法做了准备，因为如果参数数量小于bean定义中配置的参数数量，那么肯定是不合适的构造函数。
             */
            int minNrOfArgs;
            if (explicitArgs != null) {
                minNrOfArgs = explicitArgs.length;
            } else {
                ConstructorArgumentValues cargs = mbd.getConstructorArgumentValues();
                resolvedValues = new ConstructorArgumentValues();
                /*
                 * 根据bean定义配置计算构造函数至少有几个参数
                 *     <bean id="persion" class="xyz.coolblog.autowire.Person">
                 *         <constructor-arg index="0" value="xiaoming">;
                 *         <constructor-arg index="2" value="man">;
                 *     </bean>
                 * 此时 minNrOfArgs = maxIndex + 1 = 3，除了计算 minNrOfArgs，
                 * 下面的方法还会将 cargs 中的参数数据转存到 resolvedValues 中
                 */
                minNrOfArgs = resolveConstructorArguments(beanName, mbd, bw, cargs, resolvedValues);
            }

            //先根据权限（public>protected）,再根据参数数量（倒序）排序
            AutowireUtils.sortConstructors(candidates);
            int minTypeDiffWeight = Integer.MAX_VALUE;
            Set<Constructor<?>> ambiguousConstructors = null;

            for (Constructor<?> candidate : candidates) {
                int parameterCount = candidate.getParameterCount();

                /*
                 * 下面的 if 分支的用途是：若匹配到到合适的构造方法了，提前结束 for 循环
                 * constructorToUse != null 这个条件比较好理解，下面分析一下条件 argsToUse.length > paramTypes.length：
                 * 前面说到 AutowireUtils.sortConstructors(candidates) 用于对构造方法进行
                 * 排序，排序规则如下：
                 *   1. 具有 public 访问权限的构造方法排在非 public 构造方法前
                 *   2. 参数数量多的构造方法排在前面
                 *
                 * 假设现在有一组构造方法按照上面的排序规则进行排序，排序结果如下（省略参数名称）：
                 *
                 *   1. public Hello(Object, Object, Object)
                 *   2. public Hello(Object, Object)
                 *   3. public Hello(Object)
                 *   4. protected Hello(Integer, Object, Object, Object)
                 *   5. protected Hello(Integer, Object, Object)
                 *   6. protected Hello(Integer, Object)
                 *
                 * argsToUse = [num1, obj2]，可以匹配上的构造方法2和构造方法6。由于构造方法2有
                 * 更高的访问权限，所以没理由不选他（尽管后者在参数类型上更加匹配）。由于构造方法3
                 * 参数数量 < argsToUse.length，参数数量上不匹配，也不应该选。所以
                 * argsToUse.length > paramTypes.length 这个条件用途是：在条件
                 * constructorToUse != null 成立的情况下，通过判断参数数量与参数值数量
                 * （argsToUse.length）是否一致，来决定是否提前终止构造方法匹配逻辑。
                 */

                if (constructorToUse != null && argsToUse != null && argsToUse.length > parameterCount) {
                    // Already found greedy constructor that can be satisfied ->  已经找到合适的构造器
                    // do not look any further, there are only less greedy constructors left. 参数只会更少
                    //在已找到数量匹配的构造器下，只有一种情况会继续判断，就是数量匹配+参数更精准匹配（普通类>Object）
                    break;
                }
                //如果参数数量小于bean定义中配置的参数数量，不考虑
                if (parameterCount < minNrOfArgs) {
                    continue;
                }

                ArgumentsHolder argsHolder;
                Class<?>[] paramTypes = candidate.getParameterTypes();
                if (resolvedValues != null) {
                    //xml中配置的值最终形态对象，如ref=person，这里就是Person对象实例
                    String[] paramNames = null;
                    ParameterNameDiscoverer pnd = this.beanFactory.getParameterNameDiscoverer();
                    if (pnd != null) {
                        paramNames = pnd.getParameterNames(candidate);
                    }
                    argsHolder = createArgumentArray(beanName, mbd, resolvedValues, bw, paramTypes, paramNames,
                            getUserDeclaredConstructor(candidate), autowiring, candidates.length == 1);

                } else {
                    // Explicit arguments given -> arguments length must match exactly.
                    if (parameterCount != explicitArgs.length) {
                        continue;
                    }
                    argsHolder = new ArgumentsHolder(explicitArgs);
                }

                int typeDiffWeight = argsHolder.getAssignabilityWeight(paramTypes);
                // Choose this constructor if it represents the closest match.
                if (typeDiffWeight < minTypeDiffWeight) {
                    constructorToUse = candidate;
                    argsHolderToUse = argsHolder;
                    argsToUse = argsHolder.arguments;
                    minTypeDiffWeight = typeDiffWeight;
                    ambiguousConstructors = null;
                } else if (constructorToUse != null && typeDiffWeight == minTypeDiffWeight) {
                    if (ambiguousConstructors == null) {
                        ambiguousConstructors = new LinkedHashSet<>();
                        ambiguousConstructors.add(constructorToUse);
                    }
                    ambiguousConstructors.add(candidate);
                }
            }
        }

        Assert.state(argsToUse != null, "Unresolved constructor arguments");
        bw.setBeanInstance(instantiate(beanName, mbd, constructorToUse, argsToUse));
        return bw;
    }


    private Object instantiate(
            String beanName, GenericBeanDefinition mbd, Constructor<?> constructorToUse, Object[] argsToUse) {

        InstantiationStrategy strategy = this.beanFactory.getInstantiationStrategy();

        return strategy.instantiate(mbd, beanName, this.beanFactory, constructorToUse, argsToUse);

    }


    public void resolveFactoryMethodIfPossible(GenericBeanDefinition mbd) {
        Class<?> factoryClass;
        boolean isStatic;
        if (mbd.getFactoryBeanName() != null) {
            factoryClass = this.beanFactory.getType(mbd.getFactoryBeanName());
            isStatic = false;
        } else {
            factoryClass = mbd.getBeanClass();
            isStatic = true;
        }
        Assert.state(factoryClass != null, "Unresolvable factory class");
        factoryClass = ClassUtils.getUserClass(factoryClass);

        Method[] candidates = getCandidateMethods(factoryClass, mbd);
        Method uniqueCandidate = null;
        for (Method candidate : candidates) {
            if (Modifier.isStatic(candidate.getModifiers()) == isStatic && mbd.isFactoryMethod(candidate)) {
                if (uniqueCandidate == null) {
                    uniqueCandidate = candidate;
                } else if (isParamMismatch(uniqueCandidate, candidate)) {
                    uniqueCandidate = null;
                    break;
                }
            }
        }
        mbd.factoryMethodToIntrospect = uniqueCandidate;
    }

    private boolean isParamMismatch(Method uniqueCandidate, Method candidate) {
        int uniqueCandidateParameterCount = uniqueCandidate.getParameterCount();
        int candidateParameterCount = candidate.getParameterCount();
        return (uniqueCandidateParameterCount != candidateParameterCount ||
                !Arrays.equals(uniqueCandidate.getParameterTypes(), candidate.getParameterTypes()));
    }


    private Method[] getCandidateMethods(Class<?> factoryClass, GenericBeanDefinition mbd) {
        if (System.getSecurityManager() != null) {
            return AccessController.doPrivileged((PrivilegedAction<Method[]>) () ->
                    (mbd.isNonPublicAccessAllowed() ?
                            ReflectionUtils.getAllDeclaredMethods(factoryClass) : factoryClass.getMethods()));
        } else {
            return (mbd.isNonPublicAccessAllowed() ?
                    ReflectionUtils.getAllDeclaredMethods(factoryClass) : factoryClass.getMethods());
        }
    }

    /**
     * Instantiate the bean using a named factory method. The method may be static, if the
     * bean definition parameter specifies a class, rather than a "factory-bean", or
     * an instance variable on a factory object itself configured using Dependency Injection.
     * <p>Implementation requires iterating over the static or instance methods with the
     * name specified in the RootBeanDefinition (the method may be overloaded) and trying
     * to match with the parameters. We don't have the types attached to constructor args,
     * so trial and error is the only way to go here. The explicitArgs array may contain
     * argument values passed in programmatically via the corresponding getBean method.
     *
     * @param beanName     the name of the bean
     * @param mbd          the merged bean definition for the bean
     * @param explicitArgs argument values passed in programmatically via the getBean
     *                     method, or {@code null} if none (-> use constructor argument values from bean definition)
     * @return a BeanWrapper for the new instance
     */
    public BeanWrapper instantiateUsingFactoryMethod(
            String beanName, GenericBeanDefinition mbd, @Nullable Object[] explicitArgs) {

        BeanWrapperImpl bw = new BeanWrapperImpl();
        this.beanFactory.initBeanWrapper(bw);

        Object factoryBean;
        Class<?> factoryClass;
        boolean isStatic;

        String factoryBeanName = mbd.getFactoryBeanName();
        if (factoryBeanName != null) {
            if (factoryBeanName.equals(beanName)) {
                throw new BeanDefinitionStoreException(beanName,
                        "factory-bean reference points back to the same bean definition");
            }
            factoryBean = this.beanFactory.getBean(factoryBeanName);
            if (mbd.isSingleton() && this.beanFactory.containsSingleton(beanName)) {
                throw new BeanDefinitionStoreException(factoryBeanName, "dup");
            }
            factoryClass = factoryBean.getClass();
            isStatic = false;
        } else {
            // It's a static factory method on the bean class.
            if (!mbd.hasBeanClass()) {
                throw new BeanDefinitionStoreException(beanName,
                        "bean definition declares neither a bean class nor a factory-bean reference");
            }
            factoryBean = null;
            factoryClass = mbd.getBeanClass();
            isStatic = true;
        }

        Method factoryMethodToUse = null;
        ArgumentsHolder argsHolderToUse = null;
        Object[] argsToUse = null;

        if (explicitArgs != null) {
            argsToUse = explicitArgs;
        } else {
            Object[] argsToResolve = null;
            factoryMethodToUse = (Method) mbd.resolvedConstructorOrFactoryMethod;
            if (factoryMethodToUse != null && mbd.constructorArgumentsResolved) {
                // Found a cached factory method...
                argsToUse = mbd.resolvedConstructorArguments;
                if (argsToUse == null) {
                    argsToResolve = mbd.preparedConstructorArguments;
                }
            }
            if (argsToResolve != null) {
                argsToUse = resolvePreparedArguments(beanName, mbd, bw, factoryMethodToUse, argsToResolve, true);
            }
        }

        if (factoryMethodToUse == null || argsToUse == null) {
            // Need to determine the factory method...
            // Try all methods with this name to see if they match the given arguments.
            factoryClass = ClassUtils.getUserClass(factoryClass);

            List<Method> candidates = null;
            if (mbd.isFactoryMethodUnique) {
                if (factoryMethodToUse == null) {
                    factoryMethodToUse = mbd.getResolvedFactoryMethod();
                }
                if (factoryMethodToUse != null) {
                    candidates = Collections.singletonList(factoryMethodToUse);
                }
            }
            if (candidates == null) {
                candidates = new ArrayList<>();
                Method[] rawCandidates = getCandidateMethods(factoryClass, mbd);
                for (Method candidate : rawCandidates) {
                    if (Modifier.isStatic(candidate.getModifiers()) == isStatic && mbd.isFactoryMethod(candidate)) {
                        candidates.add(candidate);
                    }
                }
            }

            if (candidates.size() == 1 && explicitArgs == null && !mbd.hasConstructorArgumentValues()) {
                Method uniqueCandidate = candidates.get(0);
                if (uniqueCandidate.getParameterCount() == 0) {
                    mbd.factoryMethodToIntrospect = uniqueCandidate;
                    mbd.resolvedConstructorOrFactoryMethod = uniqueCandidate;
                    mbd.constructorArgumentsResolved = true;
                    mbd.resolvedConstructorArguments = EMPTY_ARGS;
                    bw.setBeanInstance(instantiate(beanName, mbd, factoryBean, uniqueCandidate, EMPTY_ARGS));
                    return bw;
                }
            }

            if (candidates.size() > 1) {  // explicitly skip immutable singletonList
                candidates.sort(AutowireUtils.EXECUTABLE_COMPARATOR);
            }

            ConstructorArgumentValues resolvedValues = null;
            boolean autowiring = (mbd.getResolvedAutowireMode() == AutowireCapableBeanFactory.AUTOWIRE_CONSTRUCTOR);
            int minTypeDiffWeight = Integer.MAX_VALUE;
            Set<Method> ambiguousFactoryMethods = null;

            int minNrOfArgs;
            if (explicitArgs != null) {
                minNrOfArgs = explicitArgs.length;
            } else {
                // We don't have arguments passed in programmatically, so we need to resolve the
                // arguments specified in the constructor arguments held in the bean definition.
                if (mbd.hasConstructorArgumentValues()) {
                    ConstructorArgumentValues cargs = mbd.getConstructorArgumentValues();
                    resolvedValues = new ConstructorArgumentValues();
                    minNrOfArgs = resolveConstructorArguments(beanName, mbd, bw, cargs, resolvedValues);
                } else {
                    minNrOfArgs = 0;
                }
            }

            for (Method candidate : candidates) {
                int parameterCount = candidate.getParameterCount();

                if (parameterCount >= minNrOfArgs) {
                    ArgumentsHolder argsHolder;

                    Class<?>[] paramTypes = candidate.getParameterTypes();
                    if (explicitArgs != null) {
                        // Explicit arguments given -> arguments length must match exactly.
                        if (paramTypes.length != explicitArgs.length) {
                            continue;
                        }
                        argsHolder = new ArgumentsHolder(explicitArgs);
                    } else {
                        // Resolved constructor arguments: type conversion and/or autowiring necessary.
                        String[] paramNames = null;
                        ParameterNameDiscoverer pnd = this.beanFactory.getParameterNameDiscoverer();
                        if (pnd != null) {
                            paramNames = pnd.getParameterNames(candidate);
                        }
                        argsHolder = createArgumentArray(beanName, mbd, resolvedValues, bw,
                                paramTypes, paramNames, candidate, autowiring, candidates.size() == 1);

                    }

                    int typeDiffWeight = 0;
                    // Choose this factory method if it represents the closest match.
                    if (typeDiffWeight < minTypeDiffWeight) {
                        factoryMethodToUse = candidate;
                        argsHolderToUse = argsHolder;
                        argsToUse = argsHolder.arguments;
                        minTypeDiffWeight = typeDiffWeight;
                        ambiguousFactoryMethods = null;
                    }
                    // Find out about ambiguity: In case of the same type difference weight
                    // for methods with the same number of parameters, collect such candidates
                    // and eventually raise an ambiguity exception.
                    // However, only perform that check in non-lenient constructor resolution mode,
                    // and explicitly ignore overridden methods (with the same parameter signature).
                    else if (factoryMethodToUse != null && typeDiffWeight == minTypeDiffWeight &&

                            paramTypes.length == factoryMethodToUse.getParameterCount() &&
                            !Arrays.equals(paramTypes, factoryMethodToUse.getParameterTypes())) {
                        if (ambiguousFactoryMethods == null) {
                            ambiguousFactoryMethods = new LinkedHashSet<>();
                            ambiguousFactoryMethods.add(factoryMethodToUse);
                        }
                        ambiguousFactoryMethods.add(candidate);
                    }
                }
            }


            List<String> argTypes = new ArrayList<>(minNrOfArgs);
            if (explicitArgs != null) {
                for (Object arg : explicitArgs) {
                    argTypes.add(arg != null ? arg.getClass().getSimpleName() : "null");
                }
            }


            if (explicitArgs == null && argsHolderToUse != null) {
                mbd.factoryMethodToIntrospect = factoryMethodToUse;
            }
        }

        bw.setBeanInstance(instantiate(beanName, mbd, factoryBean, factoryMethodToUse, argsToUse));
        return bw;
    }

    private Object instantiate(String beanName, GenericBeanDefinition mbd,
                               @Nullable Object factoryBean, Method factoryMethod, Object[] args) {


        return this.beanFactory.getInstantiationStrategy().instantiate(
                mbd, beanName, this.beanFactory, factoryBean, factoryMethod, args);

    }

    /**
     * 将此bean的构造函数参数解析为resolveValues对象
     */
    private int resolveConstructorArguments(String beanName, GenericBeanDefinition mbd, BeanWrapper bw,
                                            ConstructorArgumentValues cargs, ConstructorArgumentValues resolvedValues) {

        TypeConverter customConverter = this.beanFactory.getCustomTypeConverter();
        TypeConverter converter = (customConverter != null ? customConverter : bw);
        //对Bean属性原始值的解析
        BeanDefinitionValueResolver valueResolver =
                new BeanDefinitionValueResolver(this.beanFactory, beanName, mbd, converter);

        //获取xml里type和index参数数量之和，这里并不代表所有数量，因为有可能会有自动注入的参数
        int minNrOfArgs = cargs.getArgumentCount();

        //遍历所有xml中带index的参数
        for (Map.Entry<Integer, ConstructorArgumentValues.ValueHolder> entry : cargs.getIndexedArgumentValues().entrySet()) {
            int index = entry.getKey();
            if (index < 0) {
                throw new BeanCreationException(beanName,
                        "Invalid constructor argument index: " + index);
            }
            /**
             * 如果下标大于参数个数，则参数个数应该是index+1
             * 比如一个构造器，第一个参数是自动注入，第二个是xml里index=1注入
             * 这里minNrOfArgs=1，因为只有一个参数在xml里配置，index=1，最后结果最小参数数量应该是2
             * 这里老版本有个bug，判断条件是index>minNrOfArgs，造成明明有2个参数，但是没有赋值index+1，最终返回1
             */
            if (index + 1 > minNrOfArgs) {
                minNrOfArgs = index + 1;
            }
            ConstructorArgumentValues.ValueHolder valueHolder = entry.getValue();
            if (valueHolder.isConverted()) {
                resolvedValues.addIndexedArgumentValue(index, valueHolder);
            } else {
                Object resolvedValue =
                        valueResolver.resolveValueIfNecessary("constructor argument", valueHolder.getValue());
                ConstructorArgumentValues.ValueHolder resolvedValueHolder =
                        new ConstructorArgumentValues.ValueHolder(resolvedValue, valueHolder.getType(), valueHolder.getName());
                resolvedValueHolder.setSource(valueHolder);
                resolvedValues.addIndexedArgumentValue(index, resolvedValueHolder);
            }
        }

        //遍历所有xml中带type的参数
        for (ConstructorArgumentValues.ValueHolder valueHolder : cargs.getGenericArgumentValues()) {
            if (valueHolder.isConverted()) {
                //如果属性已经解析过，直接加到已解析缓存中
                resolvedValues.addGenericArgumentValue(valueHolder);
            } else {
                Object resolvedValue =
                        valueResolver.resolveValueIfNecessary("constructor argument", valueHolder.getValue());
                ConstructorArgumentValues.ValueHolder resolvedValueHolder = new ConstructorArgumentValues.ValueHolder(
                        resolvedValue, valueHolder.getType(), valueHolder.getName());
                resolvedValueHolder.setSource(valueHolder);
                resolvedValues.addGenericArgumentValue(resolvedValueHolder);
            }
        }

        return minNrOfArgs;
    }

    /**
     * Create an array of arguments to invoke a constructor or factory method,
     * given the resolved constructor argument values.
     */
    private ArgumentsHolder createArgumentArray(
            String beanName, GenericBeanDefinition mbd, @Nullable ConstructorArgumentValues resolvedValues,
            BeanWrapper bw, Class<?>[] paramTypes, @Nullable String[] paramNames, Executable executable,
            boolean autowiring, boolean fallback) {

        TypeConverter customConverter = this.beanFactory.getCustomTypeConverter();
        TypeConverter converter = (customConverter != null ? customConverter : bw);

        ArgumentsHolder args = new ArgumentsHolder(paramTypes.length);
        Set<ConstructorArgumentValues.ValueHolder> usedValueHolders = new HashSet<>(paramTypes.length);
        Set<String> autowiredBeanNames = new LinkedHashSet<>(4);

        for (int paramIndex = 0; paramIndex < paramTypes.length; paramIndex++) {
            Class<?> paramType = paramTypes[paramIndex];
            String paramName = (paramNames != null ? paramNames[paramIndex] : "");
            // Try to find matching constructor argument value, either indexed or generic.
            ConstructorArgumentValues.ValueHolder valueHolder = null;
            if (resolvedValues != null) {
                valueHolder = resolvedValues.getArgumentValue(paramIndex, paramType, paramName, usedValueHolders);
                // If we couldn't find a direct match and are not supposed to autowire,
                // let's try the next generic, untyped argument value as fallback:
                // it could match after type conversion (for example, String -> int).
                if (valueHolder == null && (!autowiring || paramTypes.length == resolvedValues.getArgumentCount())) {
                    valueHolder = resolvedValues.getGenericArgumentValue(null, null, usedValueHolders);
                }
            }
            if (valueHolder != null) {
                // We found a potential match - let's give it a try.
                // Do not consider the same value definition multiple times!
                usedValueHolders.add(valueHolder);
                Object originalValue = valueHolder.getValue();
                Object convertedValue;
                if (valueHolder.isConverted()) {
                    convertedValue = valueHolder.getConvertedValue();
                    args.preparedArguments[paramIndex] = convertedValue;
                } else {
                    MethodParameter methodParam = MethodParameter.forExecutable(executable, paramIndex);
                    try {
                        convertedValue = converter.convertIfNecessary(originalValue, paramType, methodParam);
                    } catch (TypeMismatchException ex) {
                        throw new RuntimeException(ex);
                    }
                    Object sourceHolder = valueHolder.getSource();
                    if (sourceHolder instanceof ConstructorArgumentValues.ValueHolder) {
                        Object sourceValue = ((ConstructorArgumentValues.ValueHolder) sourceHolder).getValue();
                        args.resolveNecessary = true;
                        args.preparedArguments[paramIndex] = sourceValue;
                    }
                }
                args.arguments[paramIndex] = convertedValue;
                args.rawArguments[paramIndex] = originalValue;
            } else {
                MethodParameter methodParam = MethodParameter.forExecutable(executable, paramIndex);
                // No explicit match found: we're either supposed to autowire or
                // have to fail creating an argument array for the given constructor.
                if (!autowiring) {
                    throw new RuntimeException();
                }
                Object autowiredArgument = resolveAutowiredArgument(
                        methodParam, beanName, autowiredBeanNames, converter, fallback);
                args.rawArguments[paramIndex] = autowiredArgument;
                args.arguments[paramIndex] = autowiredArgument;
                args.preparedArguments[paramIndex] = autowiredArgumentMarker;
                args.resolveNecessary = true;
            }
        }

        for (String autowiredBeanName : autowiredBeanNames) {
            this.beanFactory.registerDependentBean(autowiredBeanName, beanName);
            if (logger.isDebugEnabled()) {
                logger.debug("Autowiring by type from bean name '" + beanName +
                        "' via " + (executable instanceof Constructor ? "constructor" : "factory method") +
                        " to bean named '" + autowiredBeanName + "'");
            }
        }

        return args;
    }

    /**
     * Resolve the prepared arguments stored in the given bean definition.
     */
    private Object[] resolvePreparedArguments(String beanName, GenericBeanDefinition mbd, BeanWrapper bw,
                                              Executable executable, Object[] argsToResolve, boolean fallback) {

        TypeConverter customConverter = this.beanFactory.getCustomTypeConverter();
        TypeConverter converter = (customConverter != null ? customConverter : bw);
        BeanDefinitionValueResolver valueResolver =
                new BeanDefinitionValueResolver(this.beanFactory, beanName, mbd, converter);
        Class<?>[] paramTypes = executable.getParameterTypes();

        Object[] resolvedArgs = new Object[argsToResolve.length];
        for (int argIndex = 0; argIndex < argsToResolve.length; argIndex++) {
            Object argValue = argsToResolve[argIndex];
            MethodParameter methodParam = MethodParameter.forExecutable(executable, argIndex);
            if (argValue == autowiredArgumentMarker) {
                argValue = resolveAutowiredArgument(methodParam, beanName, null, converter, fallback);
            } else if (argValue instanceof BeanMetadataElement) {
                argValue = valueResolver.resolveValueIfNecessary("constructor argument", argValue);
            } else if (argValue instanceof String) {
                argValue = this.beanFactory.evaluateBeanDefinitionString((String) argValue, mbd);
            }
            Class<?> paramType = paramTypes[argIndex];
            try {
                resolvedArgs[argIndex] = converter.convertIfNecessary(argValue, paramType, methodParam);
            } catch (TypeMismatchException ex) {
                throw new RuntimeException(ex);
            }
        }
        return resolvedArgs;
    }

    protected Constructor<?> getUserDeclaredConstructor(Constructor<?> constructor) {
        Class<?> declaringClass = constructor.getDeclaringClass();
        Class<?> userClass = ClassUtils.getUserClass(declaringClass);
        if (userClass != declaringClass) {
            try {
                return userClass.getDeclaredConstructor(constructor.getParameterTypes());
            } catch (NoSuchMethodException ex) {
                // No equivalent constructor on user class (superclass)...
                // Let's proceed with the given constructor as we usually would.
            }
        }
        return constructor;
    }

    /**
     * Template method for resolving the specified argument which is supposed to be autowired.
     */
    @Nullable
    protected Object resolveAutowiredArgument(MethodParameter param, String beanName,
                                              @Nullable Set<String> autowiredBeanNames, TypeConverter typeConverter, boolean fallback) {

        Class<?> paramType = param.getParameterType();
        if (InjectionPoint.class.isAssignableFrom(paramType)) {
            InjectionPoint injectionPoint = currentInjectionPoint.get();
            if (injectionPoint == null) {
                throw new IllegalStateException("No current InjectionPoint available for " + param);
            }
            return injectionPoint;
        }
        return this.beanFactory.resolveDependency(
                new DependencyDescriptor(param, true), beanName, autowiredBeanNames, typeConverter);

    }

    public static InjectionPoint setCurrentInjectionPoint(@Nullable InjectionPoint injectionPoint) {
        InjectionPoint old = currentInjectionPoint.get();
        if (injectionPoint != null) {
            currentInjectionPoint.set(injectionPoint);
        } else {
            currentInjectionPoint.remove();
        }
        return old;
    }


    /**
     * Private inner class for holding argument combinations.
     */
    private static class ArgumentsHolder {

        public final Object[] rawArguments;

        public final Object[] arguments;

        public final Object[] preparedArguments;

        public boolean resolveNecessary = false;

        public ArgumentsHolder(int size) {
            this.rawArguments = new Object[size];
            this.arguments = new Object[size];
            this.preparedArguments = new Object[size];
        }

        public ArgumentsHolder(Object[] args) {
            this.rawArguments = args;
            this.arguments = args;
            this.preparedArguments = args;
        }

        public int getAssignabilityWeight(Class<?>[] paramTypes) {
            for (int i = 0; i < paramTypes.length; i++) {
                if (!ClassUtils.isAssignableValue(paramTypes[i], this.arguments[i])) {
                    return Integer.MAX_VALUE;
                }
            }
            for (int i = 0; i < paramTypes.length; i++) {
                if (!ClassUtils.isAssignableValue(paramTypes[i], this.rawArguments[i])) {
                    return Integer.MAX_VALUE - 512;
                }
            }
            return Integer.MAX_VALUE - 1024;
        }


    }

}
