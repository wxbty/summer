package ink.zfei.summer.beans.factory.support;

import ink.zfei.summer.beans.BeanMetadataElement;
import ink.zfei.summer.lang.Nullable;
import ink.zfei.summer.util.Assert;
import ink.zfei.summer.util.ObjectUtils;

import java.lang.reflect.Method;

public abstract class MethodOverride implements BeanMetadataElement {

    private final String methodName;

    private boolean overloaded = true;

    @Nullable
    private Object source;


    /**
     * Construct a new override for the given method.
     * @param methodName the name of the method to override
     */
    protected MethodOverride(String methodName) {
        Assert.notNull(methodName, "Method name must not be null");
        this.methodName = methodName;
    }


    /**
     * Return the name of the method to be overridden.
     */
    public String getMethodName() {
        return this.methodName;
    }

    /**
     * Set whether the overridden method is <em>overloaded</em> (i.e., whether argument
     * type matching needs to occur to disambiguate methods of the same name).
     * <p>Default is {@code true}; can be switched to {@code false} to optimize
     * runtime performance.
     */
    protected void setOverloaded(boolean overloaded) {
        this.overloaded = overloaded;
    }

    /**
     * Return whether the overridden method is <em>overloaded</em> (i.e., whether argument
     * type matching needs to occur to disambiguate methods of the same name).
     */
    protected boolean isOverloaded() {
        return this.overloaded;
    }

    /**
     * Set the configuration source {@code Object} for this metadata element.
     * <p>The exact type of the object will depend on the configuration mechanism used.
     */
    public void setSource(@Nullable Object source) {
        this.source = source;
    }

    @Override
    @Nullable
    public Object getSource() {
        return this.source;
    }

    /**
     * Subclasses must override this to indicate whether they <em>match</em> the
     * given method. This allows for argument list checking as well as method
     * name checking.
     * @param method the method to check
     * @return whether this override matches the given method
     */
    public abstract boolean matches(Method method);


    @Override
    public boolean equals(@Nullable Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof MethodOverride)) {
            return false;
        }
        MethodOverride that = (MethodOverride) other;
        return (ObjectUtils.nullSafeEquals(this.methodName, that.methodName) &&
                ObjectUtils.nullSafeEquals(this.source, that.source));
    }

    @Override
    public int hashCode() {
        int hashCode = ObjectUtils.nullSafeHashCode(this.methodName);
        hashCode = 29 * hashCode + ObjectUtils.nullSafeHashCode(this.source);
        return hashCode;
    }

}
