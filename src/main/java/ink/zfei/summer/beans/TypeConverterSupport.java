package ink.zfei.summer.beans;

import com.sun.corba.se.impl.io.TypeMismatchException;
import ink.zfei.summer.core.MethodParameter;
import ink.zfei.summer.core.convert.TypeDescriptor;
import ink.zfei.summer.lang.Nullable;
import ink.zfei.summer.util.Assert;

import java.lang.reflect.Field;

public abstract class TypeConverterSupport extends PropertyEditorRegistrySupport implements TypeConverter {


    TypeConverterDelegate typeConverterDelegate;

    @Override
    @Nullable
    public <T> T convertIfNecessary(@Nullable Object value, @Nullable Class<T> requiredType) throws TypeMismatchException {
        return convertIfNecessary(value, requiredType, TypeDescriptor.valueOf(requiredType));
    }

    @Override
    @Nullable
    public <T> T convertIfNecessary(@Nullable Object value, @Nullable Class<T> requiredType,
                                    @Nullable MethodParameter methodParam) throws TypeMismatchException {

        return convertIfNecessary(value, requiredType,
                (methodParam != null ? new TypeDescriptor(methodParam) : TypeDescriptor.valueOf(requiredType)));
    }

    @Override
    @Nullable
    public <T> T convertIfNecessary(@Nullable Object value, @Nullable Class<T> requiredType, @Nullable Field field)
            throws TypeMismatchException {

        return convertIfNecessary(value, requiredType,
                (field != null ? new TypeDescriptor(field) : TypeDescriptor.valueOf(requiredType)));
    }


}
