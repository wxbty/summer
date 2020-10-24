package ink.zfei.summer.core.annotation;

import ink.zfei.summer.lang.Nullable;
import ink.zfei.summer.util.Assert;
import ink.zfei.summer.util.ReflectionUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

final class AttributeMethods {

    static final AttributeMethods NONE = new AttributeMethods(null, new Method[0]);


    private static final Map<Class<? extends Annotation>, AttributeMethods> cache =
            new ConcurrentHashMap<>();

    private static final Comparator<Method> methodComparator = (m1, m2) -> {
        if (m1 != null && m2 != null) {
            return m1.getName().compareTo(m2.getName());
        }
        return m1 != null ? -1 : 1;
    };


    @Nullable
    private final Class<? extends Annotation> annotationType;

    private final Method[] attributeMethods;

    private final boolean[] canThrowTypeNotPresentException;

    private final boolean hasDefaultValueMethod;

    private final boolean hasNestedAnnotation;


    private AttributeMethods(@Nullable Class<? extends Annotation> annotationType, Method[] attributeMethods) {
        this.annotationType = annotationType;
        this.attributeMethods = attributeMethods;
        this.canThrowTypeNotPresentException = new boolean[attributeMethods.length];
        boolean foundDefaultValueMethod = false;
        boolean foundNestedAnnotation = false;
        for (int i = 0; i < attributeMethods.length; i++) {
            Method method = this.attributeMethods[i];
            Class<?> type = method.getReturnType();
            if (method.getDefaultValue() != null) {
                foundDefaultValueMethod = true;
            }
            if (type.isAnnotation() || (type.isArray() && type.getComponentType().isAnnotation())) {
                foundNestedAnnotation = true;
            }
            ReflectionUtils.makeAccessible(method);
            this.canThrowTypeNotPresentException[i] = (type == Class.class || type == Class[].class || type.isEnum());
        }
        this.hasDefaultValueMethod = foundDefaultValueMethod;
        this.hasNestedAnnotation = foundNestedAnnotation;
    }


    /**
     * Determine if this instance only contains a single attribute named
     * {@code value}.
     * @return {@code true} if there is only a value attribute
     */
    boolean hasOnlyValueAttribute() {
        return (this.attributeMethods.length == 1 &&
                MergedAnnotation.VALUE.equals(this.attributeMethods[0].getName()));
    }


    /**
     * Determine if values from the given annotation can be safely accessed without
     * causing any {@link TypeNotPresentException TypeNotPresentExceptions}.
     * @param annotation the annotation to check
     * @return {@code true} if all values are present
     * @see #validate(Annotation)
     */
    boolean isValid(Annotation annotation) {
        assertAnnotation(annotation);
        for (int i = 0; i < size(); i++) {
            if (canThrowTypeNotPresentException(i)) {
                try {
                    get(i).invoke(annotation);
                }
                catch (Throwable ex) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Check if values from the given annotation can be safely accessed without causing
     * any {@link TypeNotPresentException TypeNotPresentExceptions}. In particular,
     * this method is designed to cover Google App Engine's late arrival of such
     * exceptions for {@code Class} values (instead of the more typical early
     * {@code Class.getAnnotations() failure}.
     * @param annotation the annotation to validate
     * @throws IllegalStateException if a declared {@code Class} attribute could not be read
     * @see #isValid(Annotation)
     */
    void validate(Annotation annotation) {
        assertAnnotation(annotation);
        for (int i = 0; i < size(); i++) {
            if (canThrowTypeNotPresentException(i)) {
                try {
                    get(i).invoke(annotation);
                }
                catch (Throwable ex) {
                    throw new IllegalStateException("Could not obtain annotation attribute value for " +
                            get(i).getName() + " declared on " + annotation.annotationType(), ex);
                }
            }
        }
    }

    private void assertAnnotation(Annotation annotation) {
        Assert.notNull(annotation, "Annotation must not be null");
        if (this.annotationType != null) {
            Assert.isInstanceOf(this.annotationType, annotation);
        }
    }

    /**
     * Get the attribute with the specified name or {@code null} if no
     * matching attribute exists.
     * @param name the attribute name to find
     * @return the attribute method or {@code null}
     */
    @Nullable
    Method get(String name) {
        int index = indexOf(name);
        return index != -1 ? this.attributeMethods[index] : null;
    }

    /**
     * Get the attribute at the specified index.
     * @param index the index of the attribute to return
     * @return the attribute method
     * @throws IndexOutOfBoundsException if the index is out of range
     * (<tt>index &lt; 0 || index &gt;= size()</tt>)
     */
    Method get(int index) {
        return this.attributeMethods[index];
    }

    /**
     * Determine if the attribute at the specified index could throw a
     * {@link TypeNotPresentException} when accessed.
     * @param index the index of the attribute to check
     * @return {@code true} if the attribute can throw a
     * {@link TypeNotPresentException}
     */
    boolean canThrowTypeNotPresentException(int index) {
        return this.canThrowTypeNotPresentException[index];
    }

    /**
     * Get the index of the attribute with the specified name, or {@code -1}
     * if there is no attribute with the name.
     * @param name the name to find
     * @return the index of the attribute, or {@code -1}
     */
    int indexOf(String name) {
        for (int i = 0; i < this.attributeMethods.length; i++) {
            if (this.attributeMethods[i].getName().equals(name)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Get the index of the specified attribute, or {@code -1} if the
     * attribute is not in this collection.
     * @param attribute the attribute to find
     * @return the index of the attribute, or {@code -1}
     */
    int indexOf(Method attribute) {
        for (int i = 0; i < this.attributeMethods.length; i++) {
            if (this.attributeMethods[i].equals(attribute)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Get the number of attributes in this collection.
     * @return the number of attributes
     */
    int size() {
        return this.attributeMethods.length;
    }

    /**
     * Determine if at least one of the attribute methods has a default value.
     * @return {@code true} if there is at least one attribute method with a default value
     */
    boolean hasDefaultValueMethod() {
        return this.hasDefaultValueMethod;
    }

    /**
     * Determine if at least one of the attribute methods is a nested annotation.
     * @return {@code true} if there is at least one attribute method with a nested
     * annotation type
     */
    boolean hasNestedAnnotation() {
        return this.hasNestedAnnotation;
    }


    /**
     * Get the attribute methods for the given annotation type.
     * @param annotationType the annotation type
     * @return the attribute methods for the annotation type
     */
    static AttributeMethods forAnnotationType(@Nullable Class<? extends Annotation> annotationType) {
        if (annotationType == null) {
            return NONE;
        }
        return cache.computeIfAbsent(annotationType, AttributeMethods::compute);
    }

    private static AttributeMethods compute(Class<? extends Annotation> annotationType) {
        Method[] methods = annotationType.getDeclaredMethods();
        int size = methods.length;
        for (int i = 0; i < methods.length; i++) {
            if (!isAttributeMethod(methods[i])) {
                methods[i] = null;
                size--;
            }
        }
        if (size == 0) {
            return NONE;
        }
        Arrays.sort(methods, methodComparator);
        Method[] attributeMethods = Arrays.copyOf(methods, size);
        return new AttributeMethods(annotationType, attributeMethods);
    }

    private static boolean isAttributeMethod(Method method) {
        return (method.getParameterCount() == 0 && method.getReturnType() != void.class);
    }

    /**
     * Create a description for the given attribute method suitable to use in
     * exception messages and logs.
     * @param attribute the attribute to describe
     * @return a description of the attribute
     */
    static String describe(@Nullable Method attribute) {
        if (attribute == null) {
            return "(none)";
        }
        return describe(attribute.getDeclaringClass(), attribute.getName());
    }

    /**
     * Create a description for the given attribute method suitable to use in
     * exception messages and logs.
     * @param annotationType the annotation type
     * @param attributeName the attribute name
     * @return a description of the attribute
     */
    static String describe(@Nullable Class<?> annotationType, @Nullable String attributeName) {
        if (attributeName == null) {
            return "(none)";
        }
        String in = (annotationType != null ? " in annotation [" + annotationType.getName() + "]" : "");
        return "attribute '" + attributeName + "'" + in;
    }

}