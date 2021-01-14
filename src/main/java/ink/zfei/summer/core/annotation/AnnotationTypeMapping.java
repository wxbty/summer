package ink.zfei.summer.core.annotation;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.*;
import ink.zfei.summer.core.annotation.AnnotationTypeMapping.MirrorSets.MirrorSet;
import ink.zfei.summer.lang.Nullable;
import ink.zfei.summer.util.ObjectUtils;
import ink.zfei.summer.util.ReflectionUtils;

/**
 * 提供一个注解在root注解上下文之间的映射信息
 * 该实例AnnotationTypeMappings创建的，创建的过程根据注解的注释体系从下往上进行
 * 上层实例的source指向下层实例，所有实例的root指向最下层的实例
 */
final class AnnotationTypeMapping {

    private static final MirrorSet[] EMPTY_MIRROR_SETS = new MirrorSet[0];

    /**
     * 假设当前AnnotationTypeMapping实例为MA，映射@A注解
     */

    /**
     * 上层注解，例如：@A上注解类@R，则MA的source指向了@R对应的AnnotationTypeMapping实例
     */
    @Nullable
    private final AnnotationTypeMapping source;

    /**
     * 根注解
     */
    private final AnnotationTypeMapping root;

    /**
     * 离root的距离
     */
    private final int distance;

    /**
     * 对应的注解类型，此处为A
     */
    private final Class<? extends Annotation> annotationType;

    /**
     * 涉及到的注解类型列表，包含source和当前注解
     */
    private final List<Class<? extends Annotation>> metaTypes;

    /**
     * 当前注解实例
     */
    @Nullable
    private final Annotation annotation;

    /**
     * 注解的属性列表包装类
     */
    private final AttributeMethods attributes;

    /**
     * MirrorSet集合
     * 一个MirrorSet代表注解属性别名和另一层级对应属性的集合
     */
    private final MirrorSets mirrorSets;
    //每个属性在root中对应的同名属性的索引。
    private final int[] aliasMappings;
    //方便访问属性的映射消息，如果在root中有别名，则优先获取
    private final int[] conventionMappings;
    //与annotationValueSource是相匹配的，定义每个属性最终从哪个注解的哪个属性获取值
    private final int[] annotationValueMappings;

    private final AnnotationTypeMapping[] annotationValueSource;

    private final boolean synthesizable;
    //本注解声明的所有属性方法的所有别名集合。最后用于注解定义，然后会清空
    private final Set<Method> claimedAliases = new HashSet<>();


    AnnotationTypeMapping(@Nullable AnnotationTypeMapping source,
                          Class<? extends Annotation> annotationType, @Nullable Annotation annotation) {

        this.source = source;
        this.root = (source != null ? source.getRoot() : this);
        this.distance = (source == null ? 0 : source.getDistance() + 1);
        this.annotationType = annotationType;
        this.metaTypes = merge(
                source != null ? source.getMetaTypes() : null,
                annotationType);
        this.annotation = annotation;
        this.attributes = AttributeMethods.forAnnotationType(annotationType);
        this.mirrorSets = new MirrorSets();
        this.aliasMappings = filledIntArray(this.attributes.size());
        this.conventionMappings = filledIntArray(this.attributes.size());
        this.annotationValueMappings = filledIntArray(this.attributes.size());
        this.annotationValueSource = new AnnotationTypeMapping[this.attributes.size()];
//        processAliases();
        //生成从root访问属性的方便属性方法信息
        addConventionMappings();
        addConventionAnnotationValues();
        this.synthesizable = computeSynthesizableFlag();
    }


    private static <T> List<T> merge(@Nullable List<T> existing, T element) {
        if (existing == null) {
            return Collections.singletonList(element);
        }
        List<T> merged = new ArrayList<>(existing.size() + 1);
        merged.addAll(existing);
        merged.add(element);
        return Collections.unmodifiableList(merged);
    }






    private boolean isAliasPair(Method target) {
        return (this.annotationType == target.getDeclaringClass());
    }

    private boolean isCompatibleReturnType(Class<?> attributeType, Class<?> targetType) {
        return (attributeType == targetType || attributeType == targetType.getComponentType());
    }

    private int getFirstRootAttributeIndex(Collection<Method> aliases) {
        AttributeMethods rootAttributes = this.root.getAttributes();
        for (int i = 0; i < rootAttributes.size(); i++) {
            if (aliases.contains(rootAttributes.get(i))) {
                return i;
            }
        }
        return -1;
    }

    private void addConventionMappings() {
        if (this.distance == 0) {
            return;
        }
        AttributeMethods rootAttributes = this.root.getAttributes();
        //此时，元素值全为-1
        int[] mappings = this.conventionMappings;
        for (int i = 0; i < mappings.length; i++) {
            String name = this.attributes.get(i).getName();
            MirrorSet mirrors = getMirrorSets().getAssigned(i);
            //root中是否存在同名属性
            int mapped = rootAttributes.indexOf(name);
            //root中存在同名属性，且属性名不为value
            if (!MergedAnnotation.VALUE.equals(name) && mapped != -1) {
                //存储root中的属性方法value
                mappings[i] = mapped;
                if (mirrors != null) {
                    for (int j = 0; j < mirrors.size(); j++) {
                        //同一属性的所有别名，设置成一样的root属性index
                        mappings[mirrors.getAttributeIndex(j)] = mapped;
                    }
                }
            }
        }
    }

    private void addConventionAnnotationValues() {
        for (int i = 0; i < this.attributes.size(); i++) {
            Method attribute = this.attributes.get(i);
            boolean isValueAttribute = MergedAnnotation.VALUE.equals(attribute.getName());
            AnnotationTypeMapping mapping = this;
            while (mapping != null && mapping.distance > 0) {
                int mapped = mapping.getAttributes().indexOf(attribute.getName());
                if (mapped != -1 && isBetterConventionAnnotationValue(i, isValueAttribute, mapping)) {
                    this.annotationValueMappings[i] = mapped;
                    this.annotationValueSource[i] = mapping;
                }
                mapping = mapping.source;
            }
        }
    }
    //是更好的注解值获取属性方法。value属性优先，distance较小的优先
    private boolean isBetterConventionAnnotationValue(int index, boolean isValueAttribute,
                                                      AnnotationTypeMapping mapping) {

        if (this.annotationValueMappings[index] == -1) {
            return true;
        }
        int existingDistance = this.annotationValueSource[index].distance;
        return !isValueAttribute && existingDistance > mapping.distance;
    }

    @SuppressWarnings("unchecked")
    private boolean computeSynthesizableFlag() {
        // Uses @AliasFor for local aliases?
        for (int index : this.aliasMappings) {
            if (index != -1) {
                return true;
            }
        }

        // Uses @AliasFor for attribute overrides in meta-annotations?

        // Uses convention-based attribute overrides in meta-annotations?
        for (int index : this.conventionMappings) {
            if (index != -1) {
                return true;
            }
        }

        // Has nested annotations or arrays of annotations that are synthesizable?
        if (getAttributes().hasNestedAnnotation()) {
            AttributeMethods attributeMethods = getAttributes();
            for (int i = 0; i < attributeMethods.size(); i++) {
                Method method = attributeMethods.get(i);
                Class<?> type = method.getReturnType();
                if (type.isAnnotation() || (type.isArray() && type.getComponentType().isAnnotation())) {
                    Class<? extends Annotation> annotationType =
                            (Class<? extends Annotation>) (type.isAnnotation() ? type : type.getComponentType());
                    AnnotationTypeMapping mapping = AnnotationTypeMappings.forAnnotationType(annotationType).get(0);
                    if (mapping.isSynthesizable()) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    /**
     * Method called after all mappings have been set. At this point no further
     * lookups from child mappings will occur.
     */
    void afterAllMappingsSet() {
//        validateAllAliasesClaimed();
        for (int i = 0; i < this.mirrorSets.size(); i++) {
            validateMirrorSet(this.mirrorSets.get(i));
        }
        this.claimedAliases.clear();
    }



    private void validateMirrorSet(MirrorSet mirrorSet) {
        Method firstAttribute = mirrorSet.get(0);
        Object firstDefaultValue = firstAttribute.getDefaultValue();
        for (int i = 1; i <= mirrorSet.size() - 1; i++) {
            Method mirrorAttribute = mirrorSet.get(i);
            Object mirrorDefaultValue = mirrorAttribute.getDefaultValue();
            if (firstDefaultValue == null || mirrorDefaultValue == null) {
                throw new AnnotationConfigurationException(String.format(
                        "Misconfigured aliases: %s and %s must declare default values.",
                        AttributeMethods.describe(firstAttribute), AttributeMethods.describe(mirrorAttribute)));
            }
            if (!ObjectUtils.nullSafeEquals(firstDefaultValue, mirrorDefaultValue)) {
                throw new AnnotationConfigurationException(String.format(
                        "Misconfigured aliases: %s and %s must declare the same default value.",
                        AttributeMethods.describe(firstAttribute), AttributeMethods.describe(mirrorAttribute)));
            }
        }
    }

    /**
     * Get the root mapping.
     * @return the root mapping
     */
    AnnotationTypeMapping getRoot() {
        return this.root;
    }

    /**
     * Get the source of the mapping or {@code null}.
     * @return the source of the mapping
     */
    @Nullable
    AnnotationTypeMapping getSource() {
        return this.source;
    }

    /**
     * Get the distance of this mapping.
     * @return the distance of the mapping
     */
    int getDistance() {
        return this.distance;
    }

    /**
     * Get the type of the mapped annotation.
     * @return the annotation type
     */
    Class<? extends Annotation> getAnnotationType() {
        return this.annotationType;
    }

    List<Class<? extends Annotation>> getMetaTypes() {
        return this.metaTypes;
    }

    /**
     * Get the source annotation for this mapping. This will be the
     * meta-annotation, or {@code null} if this is the root mapping.
     * @return the source annotation of the mapping
     */
    @Nullable
    Annotation getAnnotation() {
        return this.annotation;
    }

    /**
     * Get the annotation attributes for the mapping annotation type.
     * @return the attribute methods
     */
    AttributeMethods getAttributes() {
        return this.attributes;
    }

    /**
     * Get the related index of an alias mapped attribute, or {@code -1} if
     * there is no mapping. The resulting value is the index of the attribute on
     * the root annotation that can be invoked in order to obtain the actual
     * value.
     * @param attributeIndex the attribute index of the source attribute
     * @return the mapped attribute index or {@code -1}
     */
    int getAliasMapping(int attributeIndex) {
        return this.aliasMappings[attributeIndex];
    }

    /**
     * Get the related index of a convention mapped attribute, or {@code -1}
     * if there is no mapping. The resulting value is the index of the attribute
     * on the root annotation that can be invoked in order to obtain the actual
     * value.
     * @param attributeIndex the attribute index of the source attribute
     * @return the mapped attribute index or {@code -1}
     */
    int getConventionMapping(int attributeIndex) {
        return this.conventionMappings[attributeIndex];
    }

    /**
     * Get a mapped attribute value from the most suitable
     * {@link #getAnnotation() meta-annotation}.
     * <p>The resulting value is obtained from the closest meta-annotation,
     * taking into consideration both convention and alias based mapping rules.
     * For root mappings, this method will always return {@code null}.
     * @param attributeIndex the attribute index of the source attribute
     * @param metaAnnotationsOnly if only meta annotations should be considered.
     * If this parameter is {@code false} then aliases within the annotation will
     * also be considered.
     * @return the mapped annotation value, or {@code null}
     */
    @Nullable
    Object getMappedAnnotationValue(int attributeIndex, boolean metaAnnotationsOnly) {
        int mappedIndex = this.annotationValueMappings[attributeIndex];
        if (mappedIndex == -1) {
            return null;
        }
        AnnotationTypeMapping source = this.annotationValueSource[attributeIndex];
        if (source == this && metaAnnotationsOnly) {
            return null;
        }
        return ReflectionUtils.invokeMethod(source.attributes.get(mappedIndex), source.annotation);
    }

    /**
     * Determine if the specified value is equivalent to the default value of the
     * attribute at the given index.
     * @param attributeIndex the attribute index of the source attribute
     * @param value the value to check
     * @param valueExtractor the value extractor used to extract values from any
     * nested annotations
     * @return {@code true} if the value is equivalent to the default value
     */
    boolean isEquivalentToDefaultValue(int attributeIndex, Object value, ValueExtractor valueExtractor) {

        Method attribute = this.attributes.get(attributeIndex);
        return isEquivalentToDefaultValue(attribute, value, valueExtractor);
    }

    /**
     * Get the mirror sets for this type mapping.
     * @return the attribute mirror sets
     */
    MirrorSets getMirrorSets() {
        return this.mirrorSets;
    }

    /**
     * Determine if the mapped annotation is <em>synthesizable</em>.
     * <p>Consult the documentation for {@link MergedAnnotation#synthesize()}
     * for an explanation of what is considered synthesizable.
     * @return {@code true} if the mapped annotation is synthesizable
     * @since 5.2.6
     */
    boolean isSynthesizable() {
        return this.synthesizable;
    }


    private static int[] filledIntArray(int size) {
        int[] array = new int[size];
        Arrays.fill(array, -1);
        return array;
    }

    private static boolean isEquivalentToDefaultValue(Method attribute, Object value,
                                                      ValueExtractor valueExtractor) {

        return areEquivalent(attribute.getDefaultValue(), value, valueExtractor);
    }

    private static boolean areEquivalent(@Nullable Object value, @Nullable Object extractedValue,
                                         ValueExtractor valueExtractor) {

        if (ObjectUtils.nullSafeEquals(value, extractedValue)) {
            return true;
        }
        if (value instanceof Class && extractedValue instanceof String) {
            return areEquivalent((Class<?>) value, (String) extractedValue);
        }
        if (value instanceof Class[] && extractedValue instanceof String[]) {
            return areEquivalent((Class[]) value, (String[]) extractedValue);
        }
        if (value instanceof Annotation) {
            return areEquivalent((Annotation) value, extractedValue, valueExtractor);
        }
        return false;
    }

    private static boolean areEquivalent(Class<?>[] value, String[] extractedValue) {
        if (value.length != extractedValue.length) {
            return false;
        }
        for (int i = 0; i < value.length; i++) {
            if (!areEquivalent(value[i], extractedValue[i])) {
                return false;
            }
        }
        return true;
    }

    private static boolean areEquivalent(Class<?> value, String extractedValue) {
        return value.getName().equals(extractedValue);
    }

    private static boolean areEquivalent(Annotation annotation, @Nullable Object extractedValue,
                                         ValueExtractor valueExtractor) {

        AttributeMethods attributes = AttributeMethods.forAnnotationType(annotation.annotationType());
        for (int i = 0; i < attributes.size(); i++) {
            Method attribute = attributes.get(i);
            Object value1 = ReflectionUtils.invokeMethod(attribute, annotation);
            Object value2;
            if (extractedValue instanceof TypeMappedAnnotation) {
                value2 = ((TypeMappedAnnotation<?>) extractedValue).getValue(attribute.getName()).orElse(null);
            }
            else {
                value2 = valueExtractor.extract(attribute, extractedValue);
            }
            if (!areEquivalent(value1, value2, valueExtractor)) {
                return false;
            }
        }
        return true;
    }


    /**
     * A collection of {@link MirrorSet} instances that provides details of all
     * defined mirrors.
     */
    class MirrorSets {

        private MirrorSet[] mirrorSets;
        //每个属性方法引用的MirrorSet的index。未引用MirrorSet设置为-1
        private final MirrorSet[] assigned;

        MirrorSets() {
            this.assigned = new MirrorSet[attributes.size()];
            this.mirrorSets = EMPTY_MIRROR_SETS;
        }
        //对每个mapping，此方法会调用多次。解析的最终属性是同一个属性方法的，作为一个镜像组。
        //aliases：每个属性方法的所有层级的别名
        void updateFrom(Collection<Method> aliases) {
            MirrorSet mirrorSet = null;
            //别名属性的个数
            int size = 0;
            //上一个别名属性的下标
            int last = -1;
            for (int i = 0; i < attributes.size(); i++) {
                Method attribute = attributes.get(i);
                //本注解定义的属性是其他层级属性的别名
                if (aliases.contains(attribute)) {
                    size++;
                    if (size > 1) {
                        if (mirrorSet == null) {
                            mirrorSet = new MirrorSet();
                            this.assigned[last] = mirrorSet;
                        }
                        this.assigned[i] = mirrorSet;
                    }
                    last = i;
                }
            }
            if (mirrorSet != null) {
                mirrorSet.update();
                Set<MirrorSet> unique = new LinkedHashSet<>(Arrays.asList(this.assigned));
                unique.remove(null);
                this.mirrorSets = unique.toArray(EMPTY_MIRROR_SETS);
            }
        }

        int size() {
            return this.mirrorSets.length;
        }

        MirrorSet get(int index) {
            return this.mirrorSets[index];
        }

        @Nullable
        MirrorSet getAssigned(int attributeIndex) {
            return this.assigned[attributeIndex];
        }

        /**
         * 返回本mapping每个属性最终取值的属性方法的序号数组
         * source：注解
         * annotation：注解实例
         */
        int[] resolve(@Nullable Object source, @Nullable Object annotation, ValueExtractor valueExtractor) {
            int[] result = new int[attributes.size()];
            for (int i = 0; i < result.length; i++) {
                result[i] = i;
            }
            for (int i = 0; i < size(); i++) {
                MirrorSet mirrorSet = get(i);
                int resolved = mirrorSet.resolve(source, annotation, valueExtractor);
                for (int j = 0; j < mirrorSet.size; j++) {
                    result[mirrorSet.indexes[j]] = resolved;
                }
            }
            return result;
        }


        /**
         * 用于本注解里属性最终解析为同一个属性方法的信息集合
         * 如果没有为同一个属性的别名（@Alisfor），则不会产生MirrorSet实例
         * 解析为同一个属性方法的一组镜像产生一个MirrorSet实例
         */
        class MirrorSet {

            //此MirrorSet被引用里多少次
            private int size;
            /**
             * 表示MirrorSet每次被引用属性的序号。
             * 注解镜像属性方法索引数组，size为属性方法数量。数组下标代表找到的第n-1个镜像
             * 方法，值为镜像方法的索引
             * 例如方法3和4互相镜像，则该数组为0：3，1：4，后续元素值都为-1
             */
            private final int[] indexes = new int[attributes.size()];
            //更新状态，根据MirrorSets.assigned
            void update() {
                this.size = 0;
                Arrays.fill(this.indexes, -1);
                for (int i = 0; i < MirrorSets.this.assigned.length; i++) {
                    if (MirrorSets.this.assigned[i] == this) {
                        this.indexes[this.size] = i;
                        this.size++;
                    }
                }
            }
            //返回第一个不是方法默认值的下标
            <A> int resolve(@Nullable Object source, @Nullable A annotation, ValueExtractor valueExtractor) {
                int result = -1;
                Object lastValue = null;
                for (int i = 0; i < this.size; i++) {
                    Method attribute = attributes.get(this.indexes[i]);
                    //获取到属性的值
                    Object value = valueExtractor.extract(attribute, annotation);
                    boolean isDefaultValue = (value == null ||
                            isEquivalentToDefaultValue(attribute, value, valueExtractor));
                    //如果属性值不是默认值，并且与上一个不相等，则继续判断
                    if (isDefaultValue || ObjectUtils.nullSafeEquals(lastValue, value)) {
                        if (result == -1) {
                            result = this.indexes[i];
                        }
                        continue;
                    }
                    //上一个值不为null，并且与上一个值不相等，则抛出异常
                    if (lastValue != null && !ObjectUtils.nullSafeEquals(lastValue, value)) {
                        String on = (source != null) ? " declared on " + source : "";
                        throw new AnnotationConfigurationException(String.format(
                                "Different @AliasFor mirror values for annotation [%s]%s; attribute '%s' " +
                                        "and its alias '%s' are declared with values of [%s] and [%s].",
                                getAnnotationType().getName(), on,
                                attributes.get(result).getName(),
                                attribute.getName(),
                                ObjectUtils.nullSafeToString(lastValue),
                                ObjectUtils.nullSafeToString(value)));
                    }
                    //更新result和lastValue
                    result = this.indexes[i];
                    lastValue = value;
                }
                return result;
            }

            int size() {
                return this.size;
            }

            Method get(int index) {
                int attributeIndex = this.indexes[index];
                return attributes.get(attributeIndex);
            }

            int getAttributeIndex(int index) {
                return this.indexes[index];
            }
        }
    }

}
