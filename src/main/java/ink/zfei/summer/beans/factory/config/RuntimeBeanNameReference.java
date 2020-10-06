package ink.zfei.summer.beans.factory.config;

import ink.zfei.summer.lang.Nullable;
import ink.zfei.summer.util.Assert;

public class RuntimeBeanNameReference implements BeanReference {

    private final String beanName;

    @Nullable
    private Object source;


    /**
     * Create a new RuntimeBeanNameReference to the given bean name.
     * @param beanName name of the target bean
     */
    public RuntimeBeanNameReference(String beanName) {
        Assert.hasText(beanName, "'beanName' must not be empty");
        this.beanName = beanName;
    }

    @Override
    public String getBeanName() {
        return this.beanName;
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


    @Override
    public boolean equals(@Nullable Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof RuntimeBeanNameReference)) {
            return false;
        }
        RuntimeBeanNameReference that = (RuntimeBeanNameReference) other;
        return this.beanName.equals(that.beanName);
    }

    @Override
    public int hashCode() {
        return this.beanName.hashCode();
    }

    @Override
    public String toString() {
        return '<' + getBeanName() + '>';
    }

}
