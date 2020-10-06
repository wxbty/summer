package ink.zfei.summer.beans;

import java.beans.PropertyDescriptor;

public interface BeanWrapper extends ConfigurablePropertyAccessor {

    /**
     * Specify a limit for array and collection auto-growing.
     * <p>Default is unlimited on a plain BeanWrapper.
     * @since 4.1
     */
    void setAutoGrowCollectionLimit(int autoGrowCollectionLimit);

    /**
     * Return the limit for array and collection auto-growing.
     * @since 4.1
     */
    int getAutoGrowCollectionLimit();

    /**
     * Return the bean instance wrapped by this object.
     */
    Object getWrappedInstance();

    /**
     * Return the type of the wrapped bean instance.
     */
    Class<?> getWrappedClass();

    /**
     * Obtain the PropertyDescriptors for the wrapped object
     * (as determined by standard JavaBeans introspection).
     * @return the PropertyDescriptors for the wrapped object
     */
    PropertyDescriptor[] getPropertyDescriptors();


    PropertyDescriptor getPropertyDescriptor(String propertyName);

}
