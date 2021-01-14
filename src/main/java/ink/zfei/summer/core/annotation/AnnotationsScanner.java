package ink.zfei.summer.core.annotation;

import ink.zfei.summer.core.BridgeMethodResolver;
import ink.zfei.summer.core.Ordered;
import ink.zfei.summer.core.ResolvableType;
import ink.zfei.summer.lang.Nullable;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiPredicate;

import ink.zfei.summer.core.annotation.MergedAnnotations.SearchStrategy;
import ink.zfei.summer.util.ObjectUtils;
import ink.zfei.summer.util.ReflectionUtils;

/**
 * 分别处理 processClass、processMethod、processElement 三种场景。
 * 对于  processClass、processMethod 二种场景会根据查找策略 SearchStrategy
 * 递归查找父类或接口，代码会复杂一些
 */
abstract class AnnotationsScanner {

    private static final Annotation[] NO_ANNOTATIONS = {};

    private static final Method[] NO_METHODS = {};


    private static final Map<AnnotatedElement, Annotation[]> declaredAnnotationCache =
            new ConcurrentHashMap<>(256);

    private static final Map<Class<?>, Method[]> baseTypeMethodsCache =
            new ConcurrentHashMap<>(256);


    private AnnotationsScanner() {
    }


    /**
     * Scan the hierarchy of the specified element for relevant annotations and
     * call the processor as required.
     *
     * @param context        an optional context object that will be passed back to the
     *                       processor
     * @param source         the source element to scan
     * @param searchStrategy the search strategy to use
     * @param processor      the processor that receives the annotations
     * @return the result of {@link AnnotationsProcessor#finish(Object)}
     */
    @Nullable
    static <C, R> R scan(C context, AnnotatedElement source, SearchStrategy searchStrategy,
                         AnnotationsProcessor<C, R> processor) {

        return scan(context, source, searchStrategy, processor, null);
    }

    /**
     * Scan the hierarchy of the specified element for relevant annotations and
     * call the processor as required.
     *
     * @param context        an optional context object that will be passed back to the
     *                       processor
     * @param source         the source element to scan
     * @param searchStrategy the search strategy to use
     * @param processor      the processor that receives the annotations
     * @param classFilter    an optional filter that can be used to entirely filter
     *                       out a specific class from the hierarchy
     * @return the result of {@link AnnotationsProcessor#finish(Object)}
     */
    @Nullable
    static <C, R> R scan(C context, AnnotatedElement source, SearchStrategy searchStrategy,
                         AnnotationsProcessor<C, R> processor, @Nullable BiPredicate<C, Class<?>> classFilter) {

        R result = process(context, source, searchStrategy, processor, classFilter);
        return processor.finish(result);
    }

    @Nullable
    private static <C, R> R process(C context, AnnotatedElement source,
                                    SearchStrategy searchStrategy, AnnotationsProcessor<C, R> processor,
                                    @Nullable BiPredicate<C, Class<?>> classFilter) {

        if (source instanceof Class) {
            return processClass(context, (Class<?>) source, searchStrategy, processor, classFilter);
        }
        if (source instanceof Method) {
            return processMethod(context, (Method) source, searchStrategy, processor, classFilter);
        }
        return processElement(context, source, processor, classFilter);
    }

    @Nullable
    private static <C, R> R processClass(C context, Class<?> source,
                                         SearchStrategy searchStrategy, AnnotationsProcessor<C, R> processor,
                                         @Nullable BiPredicate<C, Class<?>> classFilter) {

        switch (searchStrategy) {
            case DIRECT:
                return processElement(context, source, processor, classFilter);
            case INHERITED_ANNOTATIONS:
                return processClassInheritedAnnotations(context, source, searchStrategy, processor, classFilter);
            case SUPERCLASS:
                return processClassHierarchy(context, source, processor, classFilter, false, false);
            case TYPE_HIERARCHY:
                return processClassHierarchy(context, source, processor, classFilter, true, false);
            case TYPE_HIERARCHY_AND_ENCLOSING_CLASSES:
                return processClassHierarchy(context, source, processor, classFilter, true, true);
        }
        throw new IllegalStateException("Unsupported search strategy " + searchStrategy);
    }

    @Nullable
    private static <C, R> R processClassInheritedAnnotations(C context, Class<?> source,
                                                             SearchStrategy searchStrategy, AnnotationsProcessor<C, R> processor, @Nullable BiPredicate<C, Class<?>> classFilter) {

        try {
            if (isWithoutHierarchy(source, searchStrategy)) {
                return processElement(context, source, processor, classFilter);
            }
            Annotation[] relevant = null;
            int remaining = Integer.MAX_VALUE;
            int aggregateIndex = 0;
            Class<?> root = source;
            while (source != null && source != Object.class && remaining > 0 &&
                    !hasPlainJavaAnnotationsOnly(source)) {
                R result = processor.doWithAggregate(context, aggregateIndex);
                if (result != null) {
                    return result;
                }
                if (isFiltered(source, context, classFilter)) {
                    continue;
                }
                Annotation[] declaredAnnotations =
                        getDeclaredAnnotations(context, source, classFilter, true);
                if (relevant == null && declaredAnnotations.length > 0) {
                    relevant = root.getAnnotations();
                    remaining = relevant.length;
                }
                for (int i = 0; i < declaredAnnotations.length; i++) {
                    if (declaredAnnotations[i] != null) {
                        boolean isRelevant = false;
                        for (int relevantIndex = 0; relevantIndex < relevant.length; relevantIndex++) {
                            if (relevant[relevantIndex] != null &&
                                    declaredAnnotations[i].annotationType() == relevant[relevantIndex].annotationType()) {
                                isRelevant = true;
                                relevant[relevantIndex] = null;
                                remaining--;
                                break;
                            }
                        }
                        if (!isRelevant) {
                            declaredAnnotations[i] = null;
                        }
                    }
                }
                result = processor.doWithAnnotations(context, aggregateIndex, source, declaredAnnotations);
                if (result != null) {
                    return result;
                }
                source = source.getSuperclass();
                aggregateIndex++;
            }
        } catch (Throwable ex) {
//            AnnotationUtils.handleIntrospectionFailure(source, ex);
        }
        return null;
    }

    @Nullable
    private static <C, R> R processClassHierarchy(C context, Class<?> source,
                                                  AnnotationsProcessor<C, R> processor, @Nullable BiPredicate<C, Class<?>> classFilter,
                                                  boolean includeInterfaces, boolean includeEnclosing) {

        return processClassHierarchy(context, new int[]{0}, source, processor,
                classFilter, includeInterfaces, includeEnclosing);
    }

    @Nullable
    private static <C, R> R processClassHierarchy(C context, int[] aggregateIndex, Class<?> source,
                                                  AnnotationsProcessor<C, R> processor, @Nullable BiPredicate<C, Class<?>> classFilter,
                                                  boolean includeInterfaces, boolean includeEnclosing) {

        try {
            R result = processor.doWithAggregate(context, aggregateIndex[0]);
            if (result != null) {
                return result;
            }
            if (hasPlainJavaAnnotationsOnly(source)) {
                return null;
            }
            Annotation[] annotations = getDeclaredAnnotations(context, source, classFilter, false);
            result = processor.doWithAnnotations(context, aggregateIndex[0], source, annotations);
            if (result != null) {
                return result;
            }
            aggregateIndex[0]++;
            if (includeInterfaces) {
                for (Class<?> interfaceType : source.getInterfaces()) {
                    R interfacesResult = processClassHierarchy(context, aggregateIndex,
                            interfaceType, processor, classFilter, true, includeEnclosing);
                    if (interfacesResult != null) {
                        return interfacesResult;
                    }
                }
            }
            Class<?> superclass = source.getSuperclass();
            if (superclass != Object.class && superclass != null) {
                R superclassResult = processClassHierarchy(context, aggregateIndex,
                        superclass, processor, classFilter, includeInterfaces, includeEnclosing);
                if (superclassResult != null) {
                    return superclassResult;
                }
            }
            if (includeEnclosing) {
                // Since merely attempting to load the enclosing class may result in
                // automatic loading of sibling nested classes that in turn results
                // in an exception such as NoClassDefFoundError, we wrap the following
                // in its own dedicated try-catch block in order not to preemptively
                // halt the annotation scanning process.
                try {
                    Class<?> enclosingClass = source.getEnclosingClass();
                    if (enclosingClass != null) {
                        R enclosingResult = processClassHierarchy(context, aggregateIndex,
                                enclosingClass, processor, classFilter, includeInterfaces, true);
                        if (enclosingResult != null) {
                            return enclosingResult;
                        }
                    }
                } catch (Throwable ex) {
//                    AnnotationUtils.handleIntrospectionFailure(source, ex);
                }
            }
        } catch (Throwable ex) {
//            AnnotationUtils.handleIntrospectionFailure(source, ex);
        }
        return null;
    }

    @Nullable
    private static <C, R> R processMethod(C context, Method source,
                                          SearchStrategy searchStrategy, AnnotationsProcessor<C, R> processor,
                                          @Nullable BiPredicate<C, Class<?>> classFilter) {

        switch (searchStrategy) {
            case DIRECT:
            case INHERITED_ANNOTATIONS:
                return processMethodInheritedAnnotations(context, source, processor, classFilter);
            case SUPERCLASS:
                return processMethodHierarchy(context, new int[]{0}, source.getDeclaringClass(),
                        processor, classFilter, source, false);
            case TYPE_HIERARCHY:
            case TYPE_HIERARCHY_AND_ENCLOSING_CLASSES:
                return processMethodHierarchy(context, new int[]{0}, source.getDeclaringClass(),
                        processor, classFilter, source, true);
        }
        throw new IllegalStateException("Unsupported search strategy " + searchStrategy);
    }

    @Nullable
    private static <C, R> R processMethodInheritedAnnotations(C context, Method source,
                                                              AnnotationsProcessor<C, R> processor, @Nullable BiPredicate<C, Class<?>> classFilter) {

        try {
            R result = processor.doWithAggregate(context, 0);
            return (result != null ? result :
                    processMethodAnnotations(context, 0, source, processor, classFilter));
        } catch (Throwable ex) {
//            AnnotationUtils.handleIntrospectionFailure(source, ex);
        }
        return null;
    }

    @Nullable
    private static <C, R> R processMethodHierarchy(C context, int[] aggregateIndex,
                                                   Class<?> sourceClass, AnnotationsProcessor<C, R> processor,
                                                   @Nullable BiPredicate<C, Class<?>> classFilter, Method rootMethod,
                                                   boolean includeInterfaces) {

        try {
            R result = processor.doWithAggregate(context, aggregateIndex[0]);
            if (result != null) {
                return result;
            }
            if (hasPlainJavaAnnotationsOnly(sourceClass)) {
                return null;
            }
            boolean calledProcessor = false;
            if (sourceClass == rootMethod.getDeclaringClass()) {
                result = processMethodAnnotations(context, aggregateIndex[0],
                        rootMethod, processor, classFilter);
                calledProcessor = true;
                if (result != null) {
                    return result;
                }
            } else {
                for (Method candidateMethod : getBaseTypeMethods(context, sourceClass, classFilter)) {
                    if (candidateMethod != null && isOverride(rootMethod, candidateMethod)) {
                        result = processMethodAnnotations(context, aggregateIndex[0],
                                candidateMethod, processor, classFilter);
                        calledProcessor = true;
                        if (result != null) {
                            return result;
                        }
                    }
                }
            }
            if (Modifier.isPrivate(rootMethod.getModifiers())) {
                return null;
            }
            if (calledProcessor) {
                aggregateIndex[0]++;
            }
            if (includeInterfaces) {
                for (Class<?> interfaceType : sourceClass.getInterfaces()) {
                    R interfacesResult = processMethodHierarchy(context, aggregateIndex,
                            interfaceType, processor, classFilter, rootMethod, true);
                    if (interfacesResult != null) {
                        return interfacesResult;
                    }
                }
            }
            Class<?> superclass = sourceClass.getSuperclass();
            if (superclass != Object.class && superclass != null) {
                R superclassResult = processMethodHierarchy(context, aggregateIndex,
                        superclass, processor, classFilter, rootMethod, includeInterfaces);
                if (superclassResult != null) {
                    return superclassResult;
                }
            }
        } catch (Throwable ex) {
//            AnnotationUtils.handleIntrospectionFailure(rootMethod, ex);
        }
        return null;
    }

    private static <C> Method[] getBaseTypeMethods(
            C context, Class<?> baseType, @Nullable BiPredicate<C, Class<?>> classFilter) {

        if (baseType == Object.class || hasPlainJavaAnnotationsOnly(baseType) ||
                isFiltered(baseType, context, classFilter)) {
            return NO_METHODS;
        }

        Method[] methods = baseTypeMethodsCache.get(baseType);
        if (methods == null) {
            boolean isInterface = baseType.isInterface();
            methods = isInterface ? baseType.getMethods() : ReflectionUtils.getDeclaredMethods(baseType);
            int cleared = 0;
            for (int i = 0; i < methods.length; i++) {
                if ((!isInterface && Modifier.isPrivate(methods[i].getModifiers())) ||
                        hasPlainJavaAnnotationsOnly(methods[i]) ||
                        getDeclaredAnnotations(methods[i], false).length == 0) {
                    methods[i] = null;
                    cleared++;
                }
            }
            if (cleared == methods.length) {
                methods = NO_METHODS;
            }
            baseTypeMethodsCache.put(baseType, methods);
        }
        return methods;
    }

    private static boolean isOverride(Method rootMethod, Method candidateMethod) {
        return (!Modifier.isPrivate(candidateMethod.getModifiers()) &&
                candidateMethod.getName().equals(rootMethod.getName()) &&
                hasSameParameterTypes(rootMethod, candidateMethod));
    }

    private static boolean hasSameParameterTypes(Method rootMethod, Method candidateMethod) {
        if (candidateMethod.getParameterCount() != rootMethod.getParameterCount()) {
            return false;
        }
        Class<?>[] rootParameterTypes = rootMethod.getParameterTypes();
        Class<?>[] candidateParameterTypes = candidateMethod.getParameterTypes();
        if (Arrays.equals(candidateParameterTypes, rootParameterTypes)) {
            return true;
        }
        return hasSameGenericTypeParameters(rootMethod, candidateMethod,
                rootParameterTypes);
    }

    private static boolean hasSameGenericTypeParameters(
            Method rootMethod, Method candidateMethod, Class<?>[] rootParameterTypes) {

        Class<?> sourceDeclaringClass = rootMethod.getDeclaringClass();
        Class<?> candidateDeclaringClass = candidateMethod.getDeclaringClass();
        if (!candidateDeclaringClass.isAssignableFrom(sourceDeclaringClass)) {
            return false;
        }
        for (int i = 0; i < rootParameterTypes.length; i++) {
            Class<?> resolvedParameterType = ResolvableType.forMethodParameter(
                    candidateMethod, i, sourceDeclaringClass).resolve();
            if (rootParameterTypes[i] != resolvedParameterType) {
                return false;
            }
        }
        return true;
    }

    @Nullable
    private static <C, R> R processMethodAnnotations(C context, int aggregateIndex, Method source,
                                                     AnnotationsProcessor<C, R> processor, @Nullable BiPredicate<C, Class<?>> classFilter) {

        Annotation[] annotations = getDeclaredAnnotations(context, source, classFilter, false);
        R result = processor.doWithAnnotations(context, aggregateIndex, source, annotations);
        if (result != null) {
            return result;
        }
        Method bridgedMethod = BridgeMethodResolver.findBridgedMethod(source);
        if (bridgedMethod != source) {
            Annotation[] bridgedAnnotations = getDeclaredAnnotations(context, bridgedMethod, classFilter, true);
            for (int i = 0; i < bridgedAnnotations.length; i++) {
                if (ObjectUtils.containsElement(annotations, bridgedAnnotations[i])) {
                    bridgedAnnotations[i] = null;
                }
            }
            return processor.doWithAnnotations(context, aggregateIndex, source, bridgedAnnotations);
        }
        return null;
    }

    /**
     * 调用 getDeclaredAnnotations 获取注解 annotations。
     * MergedAnnotationFinder 处理查找到的 annotations 注解，返回一个最佳的结果
     */
    @Nullable
    private static <C, R> R processElement(C context, AnnotatedElement source,
                                           AnnotationsProcessor<C, R> processor, @Nullable BiPredicate<C, Class<?>> classFilter) {

        try {
            R result = processor.doWithAggregate(context, 0);
            return (result != null ? result : processor.doWithAnnotations(
                    context, 0, source, getDeclaredAnnotations(context, source, classFilter, false)));
        } catch (Throwable ex) {
//            AnnotationUtils.handleIntrospectionFailure(source, ex);
        }
        return null;
    }

    private static <C, R> Annotation[] getDeclaredAnnotations(C context,
                                                              AnnotatedElement source, @Nullable BiPredicate<C, Class<?>> classFilter, boolean copy) {

        if (source instanceof Class && isFiltered((Class<?>) source, context, classFilter)) {
            return NO_ANNOTATIONS;
        }
        if (source instanceof Method && isFiltered(((Method) source).getDeclaringClass(), context, classFilter)) {
            return NO_ANNOTATIONS;
        }
        return getDeclaredAnnotations(source, copy);
    }

    @SuppressWarnings("unchecked")
    @Nullable
    static <A extends Annotation> A getDeclaredAnnotation(AnnotatedElement source, Class<A> annotationType) {
        Annotation[] annotations = getDeclaredAnnotations(source, false);
        for (Annotation annotation : annotations) {
            if (annotation != null && annotationType == annotation.annotationType()) {
                return (A) annotation;
            }
        }
        return null;
    }

    /**
     * 获取注解元素（类、方法、属性）所有声明注解，忽略jdk自带注解即spring特殊注解@Nullable
     * 缓存
     */
    static Annotation[] getDeclaredAnnotations(AnnotatedElement source, boolean defensive) {
        boolean cached = false;
        Annotation[] annotations = declaredAnnotationCache.get(source);
        if (annotations != null) {
            cached = true;
        } else {
            //返回该元素上直接存在的所有注解
            annotations = source.getDeclaredAnnotations();
            if (annotations.length != 0) {
                //是否所有注解都不是用户注解
                boolean allIgnored = true;
                for (int i = 0; i < annotations.length; i++) {
                    Annotation annotation = annotations[i];
                    //
                    if (isIgnorable(annotation.annotationType()) ||
                            !AttributeMethods.forAnnotationType(annotation.annotationType()).isValid(annotation)) {
                        annotations[i] = null;
                    } else {
                        allIgnored = false;
                    }
                }
                annotations = (allIgnored ? NO_ANNOTATIONS : annotations);
                //Member接口是反射包，描述成员（属性、方法）的接口
                if (source instanceof Class || source instanceof Member) {
                    declaredAnnotationCache.put(source, annotations);
                    cached = true;
                }
            }
        }
        if (!defensive || annotations.length == 0 || !cached) {
            return annotations;
        }
        return annotations.clone();
    }

    private static <C> boolean isFiltered(
            Class<?> sourceClass, C context, @Nullable BiPredicate<C, Class<?>> classFilter) {

        return (classFilter != null && classFilter.test(context, sourceClass));
    }

    private static boolean isIgnorable(Class<?> annotationType) {
        return AnnotationFilter.PLAIN.matches(annotationType);
    }

    /**
     * 初步判断是否没有需要处理的spring注解，返回true没有注解，false需要进一步判断
     * 1、java.包下的类
     * 2、如果只搜索当前层，看当前层声明注解是否存在
     */
    static boolean isKnownEmpty(AnnotatedElement source, SearchStrategy searchStrategy) {
        //只有jdk自带注解，不用继续了，当空处理
        if (hasPlainJavaAnnotationsOnly(source)) {
            return true;
        }
        if (searchStrategy == SearchStrategy.DIRECT || isWithoutHierarchy(source, searchStrategy)) {
            //桥接方法，jdk泛型为了兼容之前的字节码
            if (source instanceof Method && ((Method) source).isBridge()) {
                return false;
            }
            return getDeclaredAnnotations(source, false).length == 0;
        }
        return false;
    }

    static boolean hasPlainJavaAnnotationsOnly(@Nullable Object annotatedElement) {
        if (annotatedElement instanceof Class) {
            return hasPlainJavaAnnotationsOnly((Class<?>) annotatedElement);
        } else if (annotatedElement instanceof Member) {
            return hasPlainJavaAnnotationsOnly(((Member) annotatedElement).getDeclaringClass());
        } else {
            return false;
        }
    }

    static boolean hasPlainJavaAnnotationsOnly(Class<?> type) {
        return (type.getName().startsWith("java.") || type == Ordered.class);
    }

    /**
     * 如果没有层级，则返回查找直接注解结果
     */
    private static boolean isWithoutHierarchy(AnnotatedElement source, SearchStrategy searchStrategy) {
        if (source == Object.class) {
            return true;
        }
        if (source instanceof Class) {
            Class<?> sourceClass = (Class<?>) source;
            //判断是否没有父类（接口）
            boolean noSuperTypes = (sourceClass.getSuperclass() == Object.class &&
                    sourceClass.getInterfaces().length == 0);
            return (searchStrategy == SearchStrategy.TYPE_HIERARCHY_AND_ENCLOSING_CLASSES ? noSuperTypes &&
                    sourceClass.getEnclosingClass() == null : noSuperTypes);
        }
        if (source instanceof Method) {
            Method sourceMethod = (Method) source;
            //私有不再继续，否则递归判断
            return (Modifier.isPrivate(sourceMethod.getModifiers()) ||
                    isWithoutHierarchy(sourceMethod.getDeclaringClass(), searchStrategy));
        }
        return true;
    }

    static void clearCache() {
        declaredAnnotationCache.clear();
        baseTypeMethodsCache.clear();
    }

}