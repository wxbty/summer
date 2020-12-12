package ink.zfei.summer.core;

import ink.zfei.summer.util.Assert;
import ink.zfei.summer.util.ClassUtils;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Collection;
import java.util.Iterator;

public final class Conventions {

    /**
     * Suffix added to names when using arrays.
     */
    private static final String PLURAL_SUFFIX = "List";


    private Conventions() {
    }


    /**
     * Determine the conventional variable name for the supplied {@code Object}
     * based on its concrete type. The convention used is to return the
     * un-capitalized short name of the {@code Class}, according to JavaBeans
     * property naming rules.
     * <p>For example:<br>
     * {@code com.myapp.Product} becomes {@code "product"}<br>
     * {@code com.myapp.MyProduct} becomes {@code "myProduct"}<br>
     * {@code com.myapp.UKProduct} becomes {@code "UKProduct"}<br>
     * <p>For arrays the pluralized version of the array component type is used.
     * For {@code Collection}s an attempt is made to 'peek ahead' to determine
     * the component type and return its pluralized version.
     * @param value the value to generate a variable name for
     * @return the generated variable name
     */
    public static String getVariableName(Object value) {
        Assert.notNull(value, "Value must not be null");
        Class<?> valueClass;
        boolean pluralize = false;

        if (value.getClass().isArray()) {
            valueClass = value.getClass().getComponentType();
            pluralize = true;
        }
        else if (value instanceof Collection) {
            Collection<?> collection = (Collection<?>) value;
            if (collection.isEmpty()) {
                throw new IllegalArgumentException(
                        "Cannot generate variable name for an empty Collection");
            }
            Object valueToCheck = peekAhead(collection);
            valueClass = getClassForValue(valueToCheck);
            pluralize = true;
        }
        else {
            valueClass = getClassForValue(value);
        }

        String name = ClassUtils.getShortNameAsProperty(valueClass);
        return (pluralize ? pluralize(name) : name);
    }

    /**
     * Convert {@code String}s in attribute name format (e.g. lowercase, hyphens
     * separating words) into property name format (camel-case). For example
     * {@code transaction-manager} becomes {@code "transactionManager"}.
     */
    public static String attributeNameToPropertyName(String attributeName) {
        Assert.notNull(attributeName, "'attributeName' must not be null");
        if (!attributeName.contains("-")) {
            return attributeName;
        }
        char[] chars = attributeName.toCharArray();
        char[] result = new char[chars.length -1]; // not completely accurate but good guess
        int currPos = 0;
        boolean upperCaseNext = false;
        for (char c : chars) {
            if (c == '-') {
                upperCaseNext = true;
            }
            else if (upperCaseNext) {
                result[currPos++] = Character.toUpperCase(c);
                upperCaseNext = false;
            }
            else {
                result[currPos++] = c;
            }
        }
        return new String(result, 0, currPos);
    }

    /**
     * Return an attribute name qualified by the given enclosing {@link Class}.
     * For example the attribute name '{@code foo}' qualified by {@link Class}
     * '{@code com.myapp.SomeClass}' would be '{@code com.myapp.SomeClass.foo}'
     */
    public static String getQualifiedAttributeName(Class<?> enclosingClass, String attributeName) {
        Assert.notNull(enclosingClass, "'enclosingClass' must not be null");
        Assert.notNull(attributeName, "'attributeName' must not be null");
        return enclosingClass.getName() + '.' + attributeName;
    }


    /**
     * Determine the class to use for naming a variable containing the given value.
     * <p>Will return the class of the given value, except when encountering a
     * JDK proxy, in which case it will determine the 'primary' interface
     * implemented by that proxy.
     * @param value the value to check
     * @return the class to use for naming a variable
     */
    private static Class<?> getClassForValue(Object value) {
        Class<?> valueClass = value.getClass();
        if (Proxy.isProxyClass(valueClass)) {
            Class<?>[] ifcs = valueClass.getInterfaces();
            for (Class<?> ifc : ifcs) {
                if (!ClassUtils.isJavaLanguageInterface(ifc)) {
                    return ifc;
                }
            }
        }
        else if (valueClass.getName().lastIndexOf('$') != -1 && valueClass.getDeclaringClass() == null) {
            // '$' in the class name but no inner class -
            // assuming it's a special subclass (e.g. by OpenJPA)
            valueClass = valueClass.getSuperclass();
        }
        return valueClass;
    }

    /**
     * Pluralize the given name.
     */
    private static String pluralize(String name) {
        return name + PLURAL_SUFFIX;
    }

    /**
     * Retrieve the {@code Class} of an element in the {@code Collection}.
     * The exact element for which the {@code Class} is retrieved will depend
     * on the concrete {@code Collection} implementation.
     */
    private static <E> E peekAhead(Collection<E> collection) {
        Iterator<E> it = collection.iterator();
        if (!it.hasNext()) {
            throw new IllegalStateException(
                    "Unable to peek ahead in non-empty collection - no element found");
        }
        E value = it.next();
        if (value == null) {
            throw new IllegalStateException(
                    "Unable to peek ahead in non-empty collection - only null element found");
        }
        return value;
    }

}
