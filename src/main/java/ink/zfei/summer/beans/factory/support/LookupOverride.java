package ink.zfei.summer.beans.factory.support;

import ink.zfei.summer.lang.Nullable;
import ink.zfei.summer.util.ObjectUtils;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

public class LookupOverride extends MethodOverride {

    @Nullable
    private final String beanName;

    @Nullable
    private Method method;


    /**
     * Construct a new LookupOverride.
     * @param methodName the name of the method to override
     * @param beanName the name of the bean in the current {@code BeanFactory}
     * that the overridden method should return (may be {@code null})
     */
    public LookupOverride(String methodName, @Nullable String beanName) {
        super(methodName);
        this.beanName = beanName;
    }

    /**
     * Construct a new LookupOverride.
     * @param method the method to override
     * @param beanName the name of the bean in the current {@code BeanFactory}
     * that the overridden method should return (may be {@code null})
     */
    public LookupOverride(Method method, @Nullable String beanName) {
        super(method.getName());
        this.method = method;
        this.beanName = beanName;
    }


    /**
     * Return the name of the bean that should be returned by this method.
     */
    @Nullable
    public String getBeanName() {
        return this.beanName;
    }

    /**
     * Match the specified method by {@link Method} reference or method name.
     * <p>For backwards compatibility reasons, in a scenario with overloaded
     * non-abstract methods of the given name, only the no-arg variant of a
     * method will be turned into a container-driven lookup method.
     * <p>In case of a provided {@link Method}, only straight matches will
     * be considered, usually demarcated by the {@code @Lookup} annotation.
     */
    @Override
    public boolean matches(Method method) {
        if (this.method != null) {
            return method.equals(this.method);
        }
        else {
            return (method.getName().equals(getMethodName()) && (!isOverloaded() ||
                    Modifier.isAbstract(method.getModifiers()) || method.getParameterCount() == 0));
        }
    }


    @Override
    public boolean equals(@Nullable Object other) {
        if (!(other instanceof LookupOverride) || !super.equals(other)) {
            return false;
        }
        LookupOverride that = (LookupOverride) other;
        return (ObjectUtils.nullSafeEquals(this.method, that.method) &&
                ObjectUtils.nullSafeEquals(this.beanName, that.beanName));
    }

    @Override
    public int hashCode() {
        return (29 * super.hashCode() + ObjectUtils.nullSafeHashCode(this.beanName));
    }

    @Override
    public String toString() {
        return "LookupOverride for method '" + getMethodName() + "'";
    }

}
