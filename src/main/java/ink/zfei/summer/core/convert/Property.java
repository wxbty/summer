package ink.zfei.summer.core.convert;

import ink.zfei.summer.core.MethodParameter;
import ink.zfei.summer.lang.Nullable;
import ink.zfei.summer.util.ObjectUtils;
import ink.zfei.summer.util.ReflectionUtils;
import ink.zfei.summer.util.StringUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class Property {

    private static Map<Property, Annotation[]> annotationCache = new ConcurrentHashMap<>();

    private final Class<?> objectType;

    @Nullable
    private final Method readMethod;

    @Nullable
    private final Method writeMethod;

    private final String name;

    private final MethodParameter methodParameter;

    @Nullable
    private Annotation[] annotations;


    public Property(Class<?> objectType, @Nullable Method readMethod, @Nullable Method writeMethod) {
        this(objectType, readMethod, writeMethod, null);
    }

    public Property(
            Class<?> objectType, @Nullable Method readMethod, @Nullable Method writeMethod, @Nullable String name) {

        this.objectType = objectType;
        this.readMethod = readMethod;
        this.writeMethod = writeMethod;
        this.methodParameter = resolveMethodParameter();
        this.name = (name != null ? name : resolveName());
    }


    /**
     * The object declaring this property, either directly or in a superclass the object extends.
     */
    public Class<?> getObjectType() {
        return this.objectType;
    }

    /**
     * The name of the property: e.g. 'foo'
     */
    public String getName() {
        return this.name;
    }

    /**
     * The property type: e.g. {@code java.lang.String}
     */
    public Class<?> getType() {
        return this.methodParameter.getParameterType();
    }

    /**
     * The property getter method: e.g. {@code getFoo()}
     */
    @Nullable
    public Method getReadMethod() {
        return this.readMethod;
    }

    /**
     * The property setter method: e.g. {@code setFoo(String)}
     */
    @Nullable
    public Method getWriteMethod() {
        return this.writeMethod;
    }


    // package private

    MethodParameter getMethodParameter() {
        return this.methodParameter;
    }

    Annotation[] getAnnotations() {
        if (this.annotations == null) {
            this.annotations = resolveAnnotations();
        }
        return this.annotations;
    }


    // internal helpers

    private String resolveName() {
        if (this.readMethod != null) {
            int index = this.readMethod.getName().indexOf("get");
            if (index != -1) {
                index += 3;
            }
            else {
                index = this.readMethod.getName().indexOf("is");
                if (index == -1) {
                    throw new IllegalArgumentException("Not a getter method");
                }
                index += 2;
            }
            return StringUtils.uncapitalize(this.readMethod.getName().substring(index));
        }
        else if (this.writeMethod != null) {
            int index = this.writeMethod.getName().indexOf("set");
            if (index == -1) {
                throw new IllegalArgumentException("Not a setter method");
            }
            index += 3;
            return StringUtils.uncapitalize(this.writeMethod.getName().substring(index));
        }
        else {
            throw new IllegalStateException("Property is neither readable nor writeable");
        }
    }

    private MethodParameter resolveMethodParameter() {
        MethodParameter read = resolveReadMethodParameter();
        MethodParameter write = resolveWriteMethodParameter();
        if (write == null) {
            if (read == null) {
                throw new IllegalStateException("Property is neither readable nor writeable");
            }
            return read;
        }
        if (read != null) {
            Class<?> readType = read.getParameterType();
            Class<?> writeType = write.getParameterType();
            if (!writeType.equals(readType) && writeType.isAssignableFrom(readType)) {
                return read;
            }
        }
        return write;
    }

    @Nullable
    private MethodParameter resolveReadMethodParameter() {
        if (getReadMethod() == null) {
            return null;
        }
        return new MethodParameter(getReadMethod(), -1).withContainingClass(getObjectType());
    }

    @Nullable
    private MethodParameter resolveWriteMethodParameter() {
        if (getWriteMethod() == null) {
            return null;
        }
        return new MethodParameter(getWriteMethod(), 0).withContainingClass(getObjectType());
    }

    private Annotation[] resolveAnnotations() {
        Annotation[] annotations = annotationCache.get(this);
        if (annotations == null) {
            Map<Class<? extends Annotation>, Annotation> annotationMap = new LinkedHashMap<>();
            addAnnotationsToMap(annotationMap, getReadMethod());
            addAnnotationsToMap(annotationMap, getWriteMethod());
            addAnnotationsToMap(annotationMap, getField());
            annotations = annotationMap.values().toArray(new Annotation[0]);
            annotationCache.put(this, annotations);
        }
        return annotations;
    }

    private void addAnnotationsToMap(
            Map<Class<? extends Annotation>, Annotation> annotationMap, @Nullable AnnotatedElement object) {

        if (object != null) {
            for (Annotation annotation : object.getAnnotations()) {
                annotationMap.put(annotation.annotationType(), annotation);
            }
        }
    }

    @Nullable
    private Field getField() {
        String name = getName();
        if (!StringUtils.hasLength(name)) {
            return null;
        }
        Field field = null;
        Class<?> declaringClass = declaringClass();
        if (declaringClass != null) {
            field = ReflectionUtils.findField(declaringClass, name);
            if (field == null) {
                // Same lenient fallback checking as in CachedIntrospectionResults...
                field = ReflectionUtils.findField(declaringClass, StringUtils.uncapitalize(name));
                if (field == null) {
                    field = ReflectionUtils.findField(declaringClass, StringUtils.capitalize(name));
                }
            }
        }
        return field;
    }

    @Nullable
    private Class<?> declaringClass() {
        if (getReadMethod() != null) {
            return getReadMethod().getDeclaringClass();
        }
        else if (getWriteMethod() != null) {
            return getWriteMethod().getDeclaringClass();
        }
        else {
            return null;
        }
    }


    @Override
    public boolean equals(@Nullable Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Property)) {
            return false;
        }
        Property otherProperty = (Property) other;
        return (ObjectUtils.nullSafeEquals(this.objectType, otherProperty.objectType) &&
                ObjectUtils.nullSafeEquals(this.name, otherProperty.name) &&
                ObjectUtils.nullSafeEquals(this.readMethod, otherProperty.readMethod) &&
                ObjectUtils.nullSafeEquals(this.writeMethod, otherProperty.writeMethod));
    }

    @Override
    public int hashCode() {
        return (ObjectUtils.nullSafeHashCode(this.objectType) * 31 + ObjectUtils.nullSafeHashCode(this.name));
    }

}
