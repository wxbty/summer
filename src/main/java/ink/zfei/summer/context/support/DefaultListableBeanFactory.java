package ink.zfei.summer.context.support;

import ink.zfei.summer.beans.BeanDefinitionRegistry;
import ink.zfei.summer.beans.BeanPostProcessor;
import ink.zfei.summer.beans.BeanWrapper;
import ink.zfei.summer.beans.factory.*;
import ink.zfei.summer.beans.factory.config.BeanDefinition;
import ink.zfei.summer.beans.factory.config.BeanDefinitionHolder;
import ink.zfei.summer.beans.factory.config.ConfigurableListableBeanFactory;
import ink.zfei.summer.beans.factory.config.NamedBeanHolder;
import ink.zfei.summer.beans.factory.support.AbstractAutowireCapableBeanFactory;
import ink.zfei.summer.beans.factory.support.GenericBeanDefinition;
import ink.zfei.summer.beans.factory.support.NullBean;
import ink.zfei.summer.core.OrderComparator;
import ink.zfei.summer.core.ResolvableType;
import ink.zfei.summer.lang.Nullable;
import ink.zfei.summer.util.Assert;
import ink.zfei.summer.util.ClassUtils;
import ink.zfei.summer.util.StringUtils;

import java.lang.annotation.Annotation;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class DefaultListableBeanFactory extends AbstractAutowireCapableBeanFactory implements ConfigurableListableBeanFactory, BeanDefinitionRegistry {

    public DefaultListableBeanFactory() {
        super();
    }

    public DefaultListableBeanFactory(@Nullable BeanFactory parentBeanFactory) {
        super(parentBeanFactory);
    }

    private final Map<String, BeanDefinition> beanDefinitionMap = new ConcurrentHashMap<>(256);
    private volatile List<String> beanDefinitionNames = new ArrayList<>(256);
    private final ConcurrentMap<String, BeanWrapper> factoryBeanInstanceCache = new ConcurrentHashMap<>();
    private final Map<Class<?>, String[]> allBeanNamesByType = new ConcurrentHashMap<>(64);
    private final Map<Class<?>, String[]> singletonBeanNamesByType = new ConcurrentHashMap<>(64);
    private volatile Set<String> manualSingletonNames = new LinkedHashSet<>(16);


    @Nullable
    private Comparator<Object> dependencyComparator;
    private volatile String[] frozenBeanDefinitionNames;


    @Override
    public <T> T getBean(Class<T> requiredType) {
        return getBean(requiredType, (Object[]) null);
    }

    @Override
    public <T> T getBean(String name, Class<T> requiredType) {
        return doGetBean(name, requiredType, null, false);
    }

    @Override
    public <T> T getBean(Class<T> requiredType, @Nullable Object... args) {
        Assert.notNull(requiredType, "Required type must not be null");
        Object resolved = resolveBean(ResolvableType.forRawClass(requiredType), args, false);
        if (resolved == null) {
            throw new NoSuchBeanDefinitionException(requiredType);
        }
        return (T) resolved;
    }

    @Override
    public Object getBean(String name, Object... args) {
        return null;
    }

    @Override
    public void addBeanPostProcessor(String id, BeanPostProcessor beanPostProcessor) {

    }

    @Override
    public String[] getAliases(String name) {
        return new String[0];
    }


    @Nullable
    private <T> T resolveBean(ResolvableType requiredType, @Nullable Object[] args, boolean nonUniqueAsNull) {
        NamedBeanHolder<T> namedBean = resolveNamedBean(requiredType, args, nonUniqueAsNull);
        if (namedBean != null) {
            return namedBean.getBeanInstance();
        }
        //todo 忽略父容器
//        BeanFactory parent = getParentBeanFactory();
//        if (parent instanceof DefaultListableBeanFactory) {
//            return ((DefaultListableBeanFactory) parent).resolveBean(requiredType, args, nonUniqueAsNull);
//        }
//        else if (parent != null) {
//            ObjectProvider<T> parentProvider = parent.getBeanProvider(requiredType);
//            if (args != null) {
//                return parentProvider.getObject(args);
//            }
//            else {
//                return (nonUniqueAsNull ? parentProvider.getIfUnique() : parentProvider.getIfAvailable());
//            }
//        }
        return null;
    }

    private <T> NamedBeanHolder<T> resolveNamedBean(
            ResolvableType requiredType, @Nullable Object[] args, boolean nonUniqueAsNull) {

        Assert.notNull(requiredType, "Required type must not be null");
        String[] candidateNames = getBeanNamesForType(requiredType);

        if (candidateNames.length > 1) {
            List<String> autowireCandidates = new ArrayList<>(candidateNames.length);
            for (String beanName : candidateNames) {
                //isAutowireCandidate()默认是true
                // 什么场景下会出现：没有定义该BeanDefinition，但根据类型可以查找到该beanName?
                if (!containsBeanDefinition(beanName) || getBeanDefinition(beanName).isAutowireCandidate()) {
                    autowireCandidates.add(beanName);
                }
            }
            if (!autowireCandidates.isEmpty()) {
                candidateNames = StringUtils.toStringArray(autowireCandidates);
            }
        }

        if (candidateNames.length == 1) {
            String beanName = candidateNames[0];
            return new NamedBeanHolder<>(beanName, (T) getBean(beanName, requiredType.toClass(), args));
        } else if (candidateNames.length > 1) {
            Map<String, Object> candidates = new LinkedHashMap<>(candidateNames.length);
            for (String beanName : candidateNames) {
                if (containsSingleton(beanName) && args == null) {
                    Object beanInstance = getBean(beanName);
                    candidates.put(beanName, (beanInstance instanceof NullBean ? null : beanInstance));
                } else {
                    candidates.put(beanName, getType(beanName));
                }
            }
            String candidateName = determinePrimaryCandidate(candidates, requiredType.toClass());
            if (candidateName == null) {
                candidateName = determineHighestPriorityCandidate(candidates, requiredType.toClass());
            }
            if (candidateName != null) {
                Object beanInstance = candidates.get(candidateName);
                if (beanInstance == null || beanInstance instanceof Class) {
                    beanInstance = getBean(candidateName, requiredType.toClass(), args);
                }
                return new NamedBeanHolder<>(candidateName, (T) beanInstance);
            }
            if (!nonUniqueAsNull) {
                throw new NoUniqueBeanDefinitionException(requiredType, candidates.keySet());
            }
        }

        return null;
    }

    @Override
    public boolean containsBeanDefinition(String beanName) {
        Assert.notNull(beanName, "Bean name must not be null");
        return this.beanDefinitionMap.containsKey(beanName);
    }


    protected String determinePrimaryCandidate(Map<String, Object> candidates, Class<?> requiredType) {
        String primaryBeanName = null;
        for (Map.Entry<String, Object> entry : candidates.entrySet()) {
            String candidateBeanName = entry.getKey();
            Object beanInstance = entry.getValue();
            if (isPrimary(candidateBeanName, beanInstance)) {
                if (primaryBeanName != null) {
                    boolean candidateLocal = containsBeanDefinition(candidateBeanName);
                    boolean primaryLocal = containsBeanDefinition(primaryBeanName);
                    if (candidateLocal && primaryLocal) {
                        throw new NoUniqueBeanDefinitionException(requiredType, candidates.size(),
                                "more than one 'primary' bean found among candidates: " + candidates.keySet());
                    } else if (candidateLocal) {
                        primaryBeanName = candidateBeanName;
                    }
                } else {
                    primaryBeanName = candidateBeanName;
                }
            }
        }
        return primaryBeanName;
    }

    protected boolean isPrimary(String beanName, Object beanInstance) {
        String transformedBeanName = transformedBeanName(beanName);
        if (containsBeanDefinition(transformedBeanName)) {
            return getMergedLocalBeanDefinition(transformedBeanName).isPrimary();
        }
        BeanFactory parent = getParentBeanFactory();
        return (parent instanceof DefaultListableBeanFactory &&
                ((DefaultListableBeanFactory) parent).isPrimary(transformedBeanName, beanInstance));
    }


    protected String determineHighestPriorityCandidate(Map<String, Object> candidates, Class<?> requiredType) {
        String highestPriorityBeanName = null;
        Integer highestPriority = null;
        for (Map.Entry<String, Object> entry : candidates.entrySet()) {
            String candidateBeanName = entry.getKey();
            Object beanInstance = entry.getValue();
            if (beanInstance != null) {
                Integer candidatePriority = getPriority(beanInstance);
                if (candidatePriority != null) {
                    if (highestPriorityBeanName != null) {
                        if (candidatePriority.equals(highestPriority)) {
                            throw new NoUniqueBeanDefinitionException(requiredType, candidates.size(),
                                    "Multiple beans found with the same priority ('" + highestPriority +
                                            "') among candidates: " + candidates.keySet());
                        } else if (candidatePriority < highestPriority) {
                            highestPriorityBeanName = candidateBeanName;
                            highestPriority = candidatePriority;
                        }
                    } else {
                        highestPriorityBeanName = candidateBeanName;
                        highestPriority = candidatePriority;
                    }
                }
            }
        }
        return highestPriorityBeanName;
    }

    protected Integer getPriority(Object beanInstance) {
        Comparator<Object> comparator = getDependencyComparator();
        if (comparator instanceof OrderComparator) {
            return ((OrderComparator) comparator).getPriority(beanInstance);
        }
        return null;
    }

    public Comparator<Object> getDependencyComparator() {
        return this.dependencyComparator;
    }

    @Override
    public void registerBeanDefinition(String beanName, BeanDefinition beanDefinition) {

        Assert.hasText(beanName, "Bean name must not be empty");
        Assert.notNull(beanDefinition, "BeanDefinition must not be null");
//      ((AbstractBeanDefinition) beanDefinition).validate();

        BeanDefinition existingDefinition = this.beanDefinitionMap.get(beanName);
        if (existingDefinition != null) {
            this.beanDefinitionMap.put(beanName, beanDefinition);
        } else {
            // Still in startup registration phase
            this.beanDefinitionMap.put(beanName, beanDefinition);
            this.beanDefinitionNames.add(beanName);
//                removeManualSingletonName(beanName);
//            this.frozenBeanDefinitionNames = null;
        }
    }

    @Override
    public BeanDefinition getBeanDefinition(String beanName) {
        BeanDefinition bd = this.beanDefinitionMap.get(beanName);
        if (bd == null) {
            if (logger.isTraceEnabled()) {
                logger.trace("No bean named '" + beanName + "' found in " + this);
            }
            throw new NoSuchBeanDefinitionException(beanName);
        }
        return bd;
    }


    public Object evaluateBeanDefinitionString(String className, GenericBeanDefinition mbd) {

        return className;
    }


    protected Object resolveBeforeInstantiation(String beanName, GenericBeanDefinition mbd) {
        Object bean = null;
        // Make sure bean class is actually resolved at this point.
        Class<?> targetType = determineTargetType(beanName, mbd);
        if (targetType != null) {
            bean = applyBeanPostProcessorsBeforeInstantiation(targetType, beanName);
            if (bean != null) {
                bean = applyBeanPostProcessorsAfterInitialization(bean, beanName);
            }
        }
        return bean;
    }

    @Override
    public void removeBeanDefinition(String beanName) throws NoSuchBeanDefinitionException {
        Assert.hasText(beanName, "'beanName' must not be empty");

        BeanDefinition bd = this.beanDefinitionMap.remove(beanName);
        if (bd == null) {
            if (logger.isTraceEnabled()) {
                logger.trace("No bean named '" + beanName + "' found in " + this);
            }
            throw new NoSuchBeanDefinitionException(beanName);
        }

        // Still in startup registration phase
        this.beanDefinitionNames.remove(beanName);

//        this.frozenBeanDefinitionNames = null;

//        resetBeanDefinition(beanName);
    }


    @Override
    public BeanFactory getParentBeanFactory() {
        return null;
    }

    @Override
    public int getBeanDefinitionCount() {
        return this.beanDefinitionMap.size();
    }

    @Override
    public String[] getBeanDefinitionNames() {
        String[] frozenNames = this.frozenBeanDefinitionNames;
        if (frozenNames != null) {
            return frozenNames.clone();
        } else {
            return StringUtils.toStringArray(this.beanDefinitionNames);
        }
    }

    @Override
    public String[] getBeanNamesForType(ResolvableType type) {
        return getBeanNamesForType(type, true, true);
    }

    @Override
    public String[] getBeanNamesForType(ResolvableType type, boolean includeNonSingletons, boolean allowEagerInit) {
        Class<?> resolved = type.resolve();
        if (resolved != null && !type.hasGenerics()) {
            return getBeanNamesForType(resolved, includeNonSingletons, allowEagerInit);
        }
        else {
            return doGetBeanNamesForType(type, includeNonSingletons, allowEagerInit);
        }
    }

    @Override
    public String[] getBeanNamesForType(@Nullable Class<?> type) {
        return getBeanNamesForType(type, true, true);
    }

    @Override
    public String[] getBeanNamesForType(@Nullable Class<?> type, boolean includeNonSingletons, boolean allowEagerInit) {
        if (!isConfigurationFrozen() || type == null || !allowEagerInit) {
            return doGetBeanNamesForType(ResolvableType.forRawClass(type), includeNonSingletons, allowEagerInit);
        }
        Map<Class<?>, String[]> cache =
                (includeNonSingletons ? this.allBeanNamesByType : this.singletonBeanNamesByType);
        String[] resolvedBeanNames = cache.get(type);
        if (resolvedBeanNames != null) {
            return resolvedBeanNames;
        }
        resolvedBeanNames = doGetBeanNamesForType(ResolvableType.forRawClass(type), includeNonSingletons, true);
        if (ClassUtils.isCacheSafe(type, getBeanClassLoader())) {
            cache.put(type, resolvedBeanNames);
        }
        return resolvedBeanNames;
    }

    @Override
    public <T> Map<String, T> getBeansOfType(@Nullable Class<T> type) {
        return getBeansOfType(type, true, true);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> Map<String, T> getBeansOfType(@Nullable Class<T> type, boolean includeNonSingletons, boolean allowEagerInit) {

        String[] beanNames = getBeanNamesForType(type, includeNonSingletons, allowEagerInit);
        Map<String, T> result = new LinkedHashMap<>(beanNames.length);
        for (String beanName : beanNames) {
            Object beanInstance = getBean(beanName);
            if (!(beanInstance instanceof NullBean)) {
                result.put(beanName, (T) beanInstance);
            }

        }
        return result;
    }

    @Override
    public String[] getBeanNamesForAnnotation(Class<? extends Annotation> annotationType) {
        List<String> result = new ArrayList<>();
        for (String beanName : this.beanDefinitionNames) {
            BeanDefinition beanDefinition = getBeanDefinition(beanName);
            if (!beanDefinition.isAbstract() && findAnnotationOnBean(beanName, annotationType) != null) {
                result.add(beanName);
            }
        }
        for (String beanName : this.manualSingletonNames) {
            if (!result.contains(beanName) && findAnnotationOnBean(beanName, annotationType) != null) {
                result.add(beanName);
            }
        }
        return StringUtils.toStringArray(result);
    }

    @Override
    public Map<String, Object> getBeansWithAnnotation(Class<? extends Annotation> annotationType) {
        String[] beanNames = getBeanNamesForAnnotation(annotationType);
        Map<String, Object> result = new LinkedHashMap<>(beanNames.length);
        for (String beanName : beanNames) {
            Object beanInstance = getBean(beanName);
            if (!(beanInstance instanceof NullBean)) {
                result.put(beanName, beanInstance);
            }
        }
        return result;
    }

    @Override
    public <A extends Annotation> A findAnnotationOnBean(String beanName, Class<A> annotationType) throws NoSuchBeanDefinitionException {
        return null;
    }


    private String[] doGetBeanNamesForType(ResolvableType type, boolean includeNonSingletons, boolean allowEagerInit) {
        List<String> result = new ArrayList<>();

        // Check all bean definitions.
        for (String beanName : this.beanDefinitionNames) {
            // Only consider bean as eligible if the bean name
            // is not defined as alias for some other bean.
            GenericBeanDefinition mbd = getMergedLocalBeanDefinition(beanName);
            // Only check bean definition if it is complete.
            if (!mbd.isAbstract() && (allowEagerInit ||
                    (mbd.hasBeanClass() || !mbd.isLazyInit()))) {
                boolean isFactoryBean = isFactoryBean(beanName, mbd);
                BeanDefinitionHolder dbd = mbd.getDecoratedDefinition();
                boolean matchFound = false;
                boolean allowFactoryBeanInit = allowEagerInit || containsSingleton(beanName);
                boolean isNonLazyDecorated = dbd != null && !mbd.isLazyInit();
                if (!isFactoryBean) {
                    if (includeNonSingletons || isSingleton(beanName, mbd, dbd)) {
                        matchFound = isTypeMatch(beanName, type, allowFactoryBeanInit);
                    }
                } else {
                    if (includeNonSingletons || isNonLazyDecorated ||
                            (allowFactoryBeanInit && isSingleton(beanName, mbd, dbd))) {
                        matchFound = isTypeMatch(beanName, type, allowFactoryBeanInit);
                    }
                    if (!matchFound) {
                        // In case of FactoryBean, try to match FactoryBean instance itself next.
                        beanName = FACTORY_BEAN_PREFIX + beanName;
                        matchFound = isTypeMatch(beanName, type, allowFactoryBeanInit);
                    }
                }
                if (matchFound) {
                    result.add(beanName);
                }
            }

        }


        // Check manually registered singletons too.
        for (String beanName : this.manualSingletonNames) {
            try {
                // In case of FactoryBean, match object created by FactoryBean.
                if (isFactoryBean(beanName)) {
                    if ((includeNonSingletons || isSingleton(beanName)) && isTypeMatch(beanName, type)) {
                        result.add(beanName);
                        // Match found for this bean: do not match FactoryBean itself anymore.
                        continue;
                    }
                    // In case of FactoryBean, try to match FactoryBean itself next.
                    beanName = FACTORY_BEAN_PREFIX + beanName;
                }
                // Match raw bean instance (might be raw FactoryBean).
                if (isTypeMatch(beanName, type)) {
                    result.add(beanName);
                }
            } catch (NoSuchBeanDefinitionException ex) {
                logger.trace(String.format("Failed to check manually registered singleton with name '%s'", beanName), ex);
            }
        }

        return StringUtils.toStringArray(result);
    }

    private volatile boolean configurationFrozen = false;

    @Override
    public boolean isConfigurationFrozen() {
        return this.configurationFrozen;
    }

    @Override
    public void preInstantiateSingletons() {
        List<String> beanNames = new ArrayList<>(this.beanDefinitionNames);
        //2、遍历bean定义信息，实例化bean

        for (String beanName : beanNames) {
            //todo 过滤 if (!bd.isAbstract() && bd.isSingleton() && !bd.isLazyInit())
            if (isFactoryBean(beanName)) {
                Object bean = getBean(FACTORY_BEAN_PREFIX + beanName);
                if (bean instanceof FactoryBean) {
                    //todo SmartFactoryBean
                    getBean(beanName);
                }
            } else {
                getBean(beanName);
            }
        }
    }

    private boolean isSingleton(String beanName, GenericBeanDefinition mbd, @Nullable BeanDefinitionHolder dbd) {
        return (dbd != null ? mbd.isSingleton() : isSingleton(beanName));
    }

    @Override
    public boolean isSingleton(String name) throws NoSuchBeanDefinitionException {
        String beanName = transformedBeanName(name);

        Object beanInstance = getSingleton(beanName, false);
        if (beanInstance != null) {
            if (beanInstance instanceof FactoryBean) {
                return (BeanFactoryUtils.isFactoryDereference(name) || ((FactoryBean<?>) beanInstance).isSingleton());
            } else {
                return !BeanFactoryUtils.isFactoryDereference(name);
            }
        }


        GenericBeanDefinition mbd = getMergedLocalBeanDefinition(beanName);

        // In case of FactoryBean, return singleton status of created object if not a dereference.
        if (mbd.isSingleton()) {
            if (isFactoryBean(beanName, mbd)) {
                if (BeanFactoryUtils.isFactoryDereference(name)) {
                    return true;
                }
                FactoryBean<?> factoryBean = (FactoryBean<?>) getBean(FACTORY_BEAN_PREFIX + beanName);
                return factoryBean.isSingleton();
            } else {
                return !BeanFactoryUtils.isFactoryDereference(name);
            }
        } else {
            return false;
        }
    }


    @Override
    public <T> T createBean(Class<T> beanClass) {
        return null;
    }

    @Override
    public void autowireBean(Object existingBean) {

    }

    @Override
    public Object configureBean(Object existingBean, String beanName) {
        return null;
    }

    @Override
    public Object createBean(Class<?> beanClass, int autowireMode, boolean dependencyCheck) {
        return null;
    }

    @Override
    public Object autowire(Class<?> beanClass, int autowireMode, boolean dependencyCheck) {
        return null;
    }

    @Override
    public <T> NamedBeanHolder<T> resolveNamedBean(Class<T> requiredType) {
        return null;
    }
}
