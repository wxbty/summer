package ink.zfei.summer.beans;

import com.sun.corba.se.impl.io.TypeMismatchException;
import ink.zfei.summer.core.ResolvableType;
import ink.zfei.summer.core.convert.Property;
import ink.zfei.summer.core.convert.TypeDescriptor;
import ink.zfei.summer.lang.Nullable;
import ink.zfei.summer.util.ReflectionUtils;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.security.*;

public class BeanWrapperImpl extends AbstractNestablePropertyAccessor implements BeanWrapper {

    /**
     * Cached introspections results for this object, to prevent encountering
     * the cost of JavaBeans introspection every time.
     */
    @Nullable
    private CachedIntrospectionResults cachedIntrospectionResults;

    /**
     * The security context used for invoking the property methods.
     */
    @Nullable
    private AccessControlContext acc;


    /**
     * Create a new empty BeanWrapperImpl. Wrapped instance needs to be set afterwards.
     * Registers default editors.
     *
     * @see #setWrappedInstance
     */
    public BeanWrapperImpl() {
        this(true);
    }

    /**
     * Create a new empty BeanWrapperImpl. Wrapped instance needs to be set afterwards.
     *
     * @param registerDefaultEditors whether to register default editors
     *                               (can be suppressed if the BeanWrapper won't need any type conversion)
     * @see #setWrappedInstance
     */
    public BeanWrapperImpl(boolean registerDefaultEditors) {
        super(registerDefaultEditors);
    }

    /**
     * Create a new BeanWrapperImpl for the given object.
     *
     * @param object the object wrapped by this BeanWrapper
     */
    public BeanWrapperImpl(Object object) {
        super(object);
    }

    /**
     * Create a new BeanWrapperImpl, wrapping a new instance of the specified class.
     *
     * @param clazz class to instantiate and wrap
     */
    public BeanWrapperImpl(Class<?> clazz) {
        super(clazz);
    }

    /**
     * Create a new BeanWrapperImpl for the given object,
     * registering a nested path that the object is in.
     *
     * @param object     the object wrapped by this BeanWrapper
     * @param nestedPath the nested path of the object
     * @param rootObject the root object at the top of the path
     */
    public BeanWrapperImpl(Object object, String nestedPath, Object rootObject) {
        super(object, nestedPath, rootObject);
    }

    /**
     * Create a new BeanWrapperImpl for the given object,
     * registering a nested path that the object is in.
     *
     * @param object     the object wrapped by this BeanWrapper
     * @param nestedPath the nested path of the object
     * @param parent     the containing BeanWrapper (must not be {@code null})
     */
    private BeanWrapperImpl(Object object, String nestedPath, BeanWrapperImpl parent) {
        super(object, nestedPath, parent);
        setSecurityContext(parent.acc);
    }


    /**
     * Set a bean instance to hold, without any unwrapping of {@link java.util.Optional}.
     *
     * @param object the actual target object
     * @see #setWrappedInstance(Object)
     * @since 4.3
     */
    public void setBeanInstance(Object object) {
        this.wrappedObject = object;
        this.rootObject = object;
        this.typeConverterDelegate = new TypeConverterDelegate(this, this.wrappedObject);
        setIntrospectionClass(object.getClass());
    }

    @Override
    public void setWrappedInstance(Object object, @Nullable String nestedPath, @Nullable Object rootObject) {
        super.setWrappedInstance(object, nestedPath, rootObject);
        setIntrospectionClass(getWrappedClass());
    }

    /**
     * Set the class to introspect.
     * Needs to be called when the target object changes.
     *
     * @param clazz the class to introspect
     */
    protected void setIntrospectionClass(Class<?> clazz) {
        if (this.cachedIntrospectionResults != null && this.cachedIntrospectionResults.getBeanClass() != clazz) {
            this.cachedIntrospectionResults = null;
        }
    }

    /**
     * Obtain a lazily initialized CachedIntrospectionResults instance
     * for the wrapped object.
     */
    private CachedIntrospectionResults getCachedIntrospectionResults() {
        if (this.cachedIntrospectionResults == null) {
            this.cachedIntrospectionResults = CachedIntrospectionResults.forClass(getWrappedClass());
        }
        return this.cachedIntrospectionResults;
    }

    /**
     * Set the security context used during the invocation of the wrapped instance methods.
     * Can be null.
     */
    public void setSecurityContext(@Nullable AccessControlContext acc) {
        this.acc = acc;
    }

    /**
     * Return the security context used during the invocation of the wrapped instance methods.
     * Can be null.
     */
    @Nullable
    public AccessControlContext getSecurityContext() {
        return this.acc;
    }


    @Nullable
    public Object convertForProperty(@Nullable Object value, String propertyName) throws TypeMismatchException {
        CachedIntrospectionResults cachedIntrospectionResults = getCachedIntrospectionResults();
        PropertyDescriptor pd = cachedIntrospectionResults.getPropertyDescriptor(propertyName);
        if (pd == null) {
            throw new RuntimeException(
                    "No property '" + propertyName + "' found");
        }
        TypeDescriptor td = cachedIntrospectionResults.getTypeDescriptor(pd);
        if (td == null) {
            td = cachedIntrospectionResults.addTypeDescriptor(pd, new TypeDescriptor(property(pd)));
        }
        return convertForProperty(propertyName, null, value, td);
    }

    private Property property(PropertyDescriptor pd) {
        GenericTypeAwarePropertyDescriptor gpd = (GenericTypeAwarePropertyDescriptor) pd;
        return new Property(gpd.getBeanClass(), gpd.getReadMethod(), gpd.getWriteMethod(), gpd.getName());
    }

    @Override
    @Nullable
    protected BeanPropertyHandler getLocalPropertyHandler(String propertyName) {
        PropertyDescriptor pd = getCachedIntrospectionResults().getPropertyDescriptor(propertyName);
        return (pd != null ? new BeanPropertyHandler(pd) : null);
    }

    @Override
    protected BeanWrapperImpl newNestedPropertyAccessor(Object object, String nestedPath) {
        return new BeanWrapperImpl(object, nestedPath, this);
    }


    @Override
    public PropertyDescriptor[] getPropertyDescriptors() {
        return getCachedIntrospectionResults().getPropertyDescriptors();
    }

    @Override
    public PropertyDescriptor getPropertyDescriptor(String propertyName) {
        BeanWrapperImpl nestedBw = (BeanWrapperImpl) getPropertyAccessorForPropertyPath(propertyName);
        String finalPath = getFinalPath(nestedBw, propertyName);
        PropertyDescriptor pd = nestedBw.getCachedIntrospectionResults().getPropertyDescriptor(finalPath);
        if (pd == null) {
            throw new RuntimeException(
                    "No property '" + propertyName + "' found");
        }
        return pd;
    }


    private class BeanPropertyHandler extends PropertyHandler {

        private final PropertyDescriptor pd;

        public BeanPropertyHandler(PropertyDescriptor pd) {
            super(pd.getPropertyType(), pd.getReadMethod() != null, pd.getWriteMethod() != null);
            this.pd = pd;
        }

        @Override
        public ResolvableType getResolvableType() {
            return ResolvableType.forMethodReturnType(this.pd.getReadMethod());
        }

        @Override
        public TypeDescriptor toTypeDescriptor() {
            return new TypeDescriptor(property(this.pd));
        }

        @Override
        @Nullable
        public TypeDescriptor nested(int level) {
            return TypeDescriptor.nested(property(this.pd), level);
        }

        @Override
        @Nullable
        public Object getValue() throws Exception {
            final Method readMethod = this.pd.getReadMethod();
            if (System.getSecurityManager() != null) {
                AccessController.doPrivileged((PrivilegedAction<Object>) () -> {
                    ReflectionUtils.makeAccessible(readMethod);
                    return null;
                });
                try {
                    return AccessController.doPrivileged((PrivilegedExceptionAction<Object>) () ->
                            readMethod.invoke(getWrappedInstance(), (Object[]) null), acc);
                } catch (PrivilegedActionException pae) {
                    throw pae.getException();
                }
            } else {
                ReflectionUtils.makeAccessible(readMethod);
                return readMethod.invoke(getWrappedInstance(), (Object[]) null);
            }
        }

        @Override
        public void setValue(final @Nullable Object value) throws Exception {
            final Method writeMethod = (this.pd instanceof GenericTypeAwarePropertyDescriptor ?
                    ((GenericTypeAwarePropertyDescriptor) this.pd).getWriteMethodForActualAccess() :
                    this.pd.getWriteMethod());
            if (System.getSecurityManager() != null) {
                AccessController.doPrivileged((PrivilegedAction<Object>) () -> {
                    ReflectionUtils.makeAccessible(writeMethod);
                    return null;
                });
                try {
                    AccessController.doPrivileged((PrivilegedExceptionAction<Object>) () ->
                            writeMethod.invoke(getWrappedInstance(), value), acc);
                } catch (PrivilegedActionException ex) {
                    throw ex.getException();
                }
            } else {
                ReflectionUtils.makeAccessible(writeMethod);
                writeMethod.invoke(getWrappedInstance(), value);
            }
        }
    }

}
