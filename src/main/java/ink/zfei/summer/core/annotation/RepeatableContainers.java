package ink.zfei.summer.core.annotation;

import ink.zfei.summer.lang.Nullable;
import ink.zfei.summer.util.Assert;
import ink.zfei.summer.util.ObjectUtils;
import ink.zfei.summer.util.ReflectionUtils;

import java.lang.annotation.Annotation;
import java.lang.annotation.Repeatable;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 用于处理可重复注解
 */
public abstract class RepeatableContainers {

    @Nullable
    private final RepeatableContainers parent;


    private RepeatableContainers(@Nullable RepeatableContainers parent) {
        this.parent = parent;
    }


    /**
     * 在一个可重复注解和包含注解之间建立关系。
     * Add an additional explicit relationship between a contained and
     * repeatable annotation.
     *
     * @param container  the container type
     * @param repeatable the contained repeatable type
     * @return a new {@link RepeatableContainers} instance
     */
    public RepeatableContainers and(Class<? extends Annotation> container,
                                    Class<? extends Annotation> repeatable) {

        return new ExplicitRepeatableContainer(this, repeatable, container);
    }

    /**
     * 查找 container 注解中的可重复注解@Repeatable
     */
    @Nullable
    Annotation[] findRepeatedAnnotations(Annotation annotation) {
        if (this.parent == null) {
            return null;
        }
        return this.parent.findRepeatedAnnotations(annotation);
    }


    @Override
    public boolean equals(@Nullable Object other) {
        if (other == this) {
            return true;
        }
        if (other == null || getClass() != other.getClass()) {
            return false;
        }
        return ObjectUtils.nullSafeEquals(this.parent, ((RepeatableContainers) other).parent);
    }

    @Override
    public int hashCode() {
        return ObjectUtils.nullSafeHashCode(this.parent);
    }


    /**
     * Create a {@link RepeatableContainers} instance that searches using Java's
     * {@link Repeatable @Repeatable} annotation.
     *
     * @return a {@link RepeatableContainers} instance
     */
    public static RepeatableContainers standardRepeatables() {
        return StandardRepeatableContainers.INSTANCE;
    }

    /**
     * Create a {@link RepeatableContainers} instance that uses a defined
     * container and repeatable type.
     *
     * @param repeatable the contained repeatable annotation
     * @param container  the container annotation or {@code null}. If specified,
     *                   this annotation must declare a {@code value} attribute returning an array
     *                   of repeatable annotations. If not specified, the container will be
     *                   deduced by inspecting the {@code @Repeatable} annotation on
     *                   {@code repeatable}.
     * @return a {@link RepeatableContainers} instance
     */
    public static RepeatableContainers of(
            Class<? extends Annotation> repeatable, @Nullable Class<? extends Annotation> container) {

        return new ExplicitRepeatableContainer(null, repeatable, container);
    }

    /**
     * Create a {@link RepeatableContainers} instance that does not expand any
     * repeatable annotations.
     *
     * @return a {@link RepeatableContainers} instance
     */
    public static RepeatableContainers none() {
        return NoRepeatableContainers.INSTANCE;
    }


    /**
     * 这是 JDK 中标准的 @Repeatable  可重复注解，findRepeatedAnnotations 方法返回 container.value()。
     * Standard {@link RepeatableContainers} implementation that searches using
     * Java's {@link Repeatable @Repeatable} annotation.
     */
    private static class StandardRepeatableContainers extends RepeatableContainers {

        private static final Map<Class<? extends Annotation>, Object> cache = new ConcurrentHashMap<>();

        private static final Object NONE = new Object();

        private static StandardRepeatableContainers INSTANCE = new StandardRepeatableContainers();

        StandardRepeatableContainers() {
            super(null);
        }

        @Override
        @Nullable
        Annotation[] findRepeatedAnnotations(Annotation annotation) {
            Method method = getRepeatedAnnotationsMethod(annotation.annotationType());
            if (method != null) {
                return (Annotation[]) ReflectionUtils.invokeMethod(method, annotation);
            }
            return super.findRepeatedAnnotations(annotation);
        }

        @Nullable
        private static Method getRepeatedAnnotationsMethod(Class<? extends Annotation> annotationType) {
            Object result = cache.computeIfAbsent(annotationType,
                    StandardRepeatableContainers::computeRepeatedAnnotationsMethod);
            return (result != NONE ? (Method) result : null);
        }

        private static Object computeRepeatedAnnotationsMethod(Class<? extends Annotation> annotationType) {
            AttributeMethods methods = AttributeMethods.forAnnotationType(annotationType);
            if (methods.hasOnlyValueAttribute()) {
                Method method = methods.get(0);
                Class<?> returnType = method.getReturnType();
                if (returnType.isArray()) {
                    Class<?> componentType = returnType.getComponentType();
                    if (Annotation.class.isAssignableFrom(componentType) &&
                            componentType.isAnnotationPresent(Repeatable.class)) {
                        return method;
                    }
                }
            }
            return NONE;
        }
    }


    /**
     * 这是非标准的可重复注解，需要传递 container 和 repeatable 两个注解类型参数。
     * 参数需要满足以下三个条件：一是 container 注解中有 vlaue 属性，
     * 二是 value 方法的返回值类型为数组，三是数组的类型为 repeatable 注解类型。
     * 也就是说 container 注解中除了 value 属性外还可以有其它属性，
     * findRepeatedAnnotations 方法返回 container.value()。
     * A single explicit mapping.
     */
    private static class ExplicitRepeatableContainer extends RepeatableContainers {

        //可重复的注解
        private final Class<? extends Annotation> repeatable;
        //容器注解。
        private final Class<? extends Annotation> container;
        //value值方法
        private final Method valueMethod;

        ExplicitRepeatableContainer(@Nullable RepeatableContainers parent,
                                    Class<? extends Annotation> repeatable, @Nullable Class<? extends Annotation> container) {

            super(parent);
            Assert.notNull(repeatable, "Repeatable must not be null");
            //如果容器为null，则通过 repeatable 注解 获取
            if (container == null) {
                container = deduceContainer(repeatable);
            }
            //获取container的value属性
            Method valueMethod = AttributeMethods.forAnnotationType(container).get(MergedAnnotation.VALUE);
            //验证
            try {
                if (valueMethod == null) {
                    throw new NoSuchMethodException("No value method found");
                }
                Class<?> returnType = valueMethod.getReturnType();
                if (!returnType.isArray() || returnType.getComponentType() != repeatable) {
                    throw new RuntimeException("Container type [" +
                            container.getName() +
                            "] must declare a 'value' attribute for an array of type [" +
                            repeatable.getName() + "]");
                }
            } catch (NoSuchMethodException e) {
                throw new RuntimeException(e);
            }

            this.repeatable = repeatable;
            this.container = container;
            this.valueMethod = valueMethod;
        }

        //推算containe
        private Class<? extends Annotation> deduceContainer(Class<? extends Annotation> repeatable) {
            //获取注解的 @Repeatable 实例
            Repeatable annotation = repeatable.getAnnotation(Repeatable.class);
            Assert.notNull(annotation, () -> "Annotation type must be a repeatable annotation: " +
                    "failed to resolve container type for " + repeatable.getName());
            //获取实例的value属性，获取到container注解类型
            return annotation.value();
        }

        @Override
        @Nullable
        Annotation[] findRepeatedAnnotations(Annotation annotation) {
            if (this.container.isAssignableFrom(annotation.annotationType())) {
                return (Annotation[]) ReflectionUtils.invokeMethod(this.valueMethod, annotation);
            }
            return super.findRepeatedAnnotations(annotation);
        }

        @Override
        public boolean equals(@Nullable Object other) {
            if (!super.equals(other)) {
                return false;
            }
            ExplicitRepeatableContainer otherErc = (ExplicitRepeatableContainer) other;
            return (this.container.equals(otherErc.container) && this.repeatable.equals(otherErc.repeatable));
        }

        @Override
        public int hashCode() {
            int hashCode = super.hashCode();
            hashCode = 31 * hashCode + this.container.hashCode();
            hashCode = 31 * hashCode + this.repeatable.hashCode();
            return hashCode;
        }
    }


    /**
     * 没有可重复注解，findRepeatedAnnotations 方法始终返回 null。
     * No repeatable containers.
     */
    private static class NoRepeatableContainers extends RepeatableContainers {

        private static NoRepeatableContainers INSTANCE = new NoRepeatableContainers();

        NoRepeatableContainers() {
            super(null);
        }
    }

}
