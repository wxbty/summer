package ink.zfei.summer.beans.factory;

import ink.zfei.summer.core.MethodParameter;
import ink.zfei.summer.lang.Nullable;
import ink.zfei.summer.util.Assert;
import ink.zfei.summer.util.ObjectUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.Member;

/**
 * 描述一个注入点
 * 1、一个实例方法(构造函数或者成员方法)的某个参数(参数类型，参数索引)及该参数上的注解信息;
 * 如insertName(@Param("name") String name)
 * 2、实例成员属性以及该属性上的注解信息；
 * 如@Resource Car car
 */
public class InjectionPoint {

    /**
     * 第1点的参数信息，内含参数注解
     */
    @Nullable
    protected MethodParameter methodParameter;

    /**
     * 第2点的属性
     */
    @Nullable
    protected Field field;

    /**
     * 属性的所有注解信息
     */
    @Nullable
    private volatile Annotation[] fieldAnnotations;


    /**
     * Create an injection point descriptor for a method or constructor parameter.
     * 构造函数，用于包装一个方法参数
     * @param methodParameter the MethodParameter to wrap
     */
    public InjectionPoint(MethodParameter methodParameter) {
        Assert.notNull(methodParameter, "MethodParameter must not be null");
        this.methodParameter = methodParameter;
    }

    /**
     * Create an injection point descriptor for a field.
     *
     * @param field the field to wrap
     */
    public InjectionPoint(Field field) {
        Assert.notNull(field, "Field must not be null");
        this.field = field;
    }

    /**
     * Copy constructor.
     *
     * @param original the original descriptor to create a copy from
     */
    protected InjectionPoint(InjectionPoint original) {
        this.methodParameter = (original.methodParameter != null ?
                new MethodParameter(original.methodParameter) : null);
        this.field = original.field;
        this.fieldAnnotations = original.fieldAnnotations;
    }

    /**
     * Just available for serialization purposes in subclasses.
     */
    protected InjectionPoint() {
    }


    /**
     * Return the wrapped MethodParameter, if any.
     * <p>Note: Either MethodParameter or Field is available.
     *
     * @return the MethodParameter, or {@code null} if none
     */
    @Nullable
    public MethodParameter getMethodParameter() {
        return this.methodParameter;
    }

    /**
     * Return the wrapped Field, if any.
     * <p>Note: Either MethodParameter or Field is available.
     *
     * @return the Field, or {@code null} if none
     */
    @Nullable
    public Field getField() {
        return this.field;
    }

    /**
     * Return the wrapped MethodParameter, assuming it is present.
     *
     * @return the MethodParameter (never {@code null})
     * @throws IllegalStateException if no MethodParameter is available
     * @since 5.0
     */
    protected final MethodParameter obtainMethodParameter() {
        Assert.state(this.methodParameter != null, "Neither Field nor MethodParameter");
        return this.methodParameter;
    }

    /**
     * Obtain the annotations associated with the wrapped field or method/constructor parameter.
     */
    public Annotation[] getAnnotations() {
        if (this.field != null) {
            Annotation[] fieldAnnotations = this.fieldAnnotations;
            if (fieldAnnotations == null) {
                fieldAnnotations = this.field.getAnnotations();
                this.fieldAnnotations = fieldAnnotations;
            }
            return fieldAnnotations;
        } else {
            return obtainMethodParameter().getParameterAnnotations();
        }
    }

    /**
     * Retrieve a field/parameter annotation of the given type, if any.
     *
     * @param annotationType the annotation type to retrieve
     * @return the annotation instance, or {@code null} if none found
     * @since 4.3.9
     */
    @Nullable
    public <A extends Annotation> A getAnnotation(Class<A> annotationType) {
        return (this.field != null ? this.field.getAnnotation(annotationType) :
                obtainMethodParameter().getParameterAnnotation(annotationType));
    }

    /**
     * Return the type declared by the underlying field or method/constructor parameter,
     * indicating the injection type.
     */
    public Class<?> getDeclaredType() {
        return (this.field != null ? this.field.getType() : obtainMethodParameter().getParameterType());
    }

    /**
     * Returns the wrapped member, containing the injection point.
     *
     * @return the Field / Method / Constructor as Member
     */
    public Member getMember() {
        return (this.field != null ? this.field : obtainMethodParameter().getMember());
    }

    /**
     * Return the wrapped annotated element.
     * <p>Note: In case of a method/constructor parameter, this exposes
     * the annotations declared on the method or constructor itself
     * (i.e. at the method/constructor level, not at the parameter level).
     * Use {@link #getAnnotations()} to obtain parameter-level annotations in
     * such a scenario, transparently with corresponding field annotations.
     *
     * @return the Field / Method / Constructor as AnnotatedElement
     */
    public AnnotatedElement getAnnotatedElement() {
        return (this.field != null ? this.field : obtainMethodParameter().getAnnotatedElement());
    }


    @Override
    public boolean equals(@Nullable Object other) {
        if (this == other) {
            return true;
        }
        if (other == null || getClass() != other.getClass()) {
            return false;
        }
        InjectionPoint otherPoint = (InjectionPoint) other;
        return (ObjectUtils.nullSafeEquals(this.field, otherPoint.field) &&
                ObjectUtils.nullSafeEquals(this.methodParameter, otherPoint.methodParameter));
    }

    @Override
    public int hashCode() {
        return (this.field != null ? this.field.hashCode() : ObjectUtils.nullSafeHashCode(this.methodParameter));
    }

    @Override
    public String toString() {
        return (this.field != null ? "field '" + this.field.getName() + "'" : String.valueOf(this.methodParameter));
    }

}
