package ink.zfei.summer.beans;

import com.sun.corba.se.impl.io.TypeMismatchException;
import ink.zfei.summer.core.MethodParameter;
import ink.zfei.summer.core.convert.TypeDescriptor;
import ink.zfei.summer.lang.Nullable;

import java.lang.reflect.Field;

public interface TypeConverter {

    @Nullable
    <T> T convertIfNecessary(@Nullable Object value, @Nullable Class<T> requiredType) throws TypeMismatchException;


    @Nullable
    <T> T convertIfNecessary(@Nullable Object value, @Nullable Class<T> requiredType,
                             @Nullable MethodParameter methodParam) throws TypeMismatchException;

    @Nullable
    <T> T convertIfNecessary(@Nullable Object value, @Nullable Class<T> requiredType, @Nullable Field field)
            throws TypeMismatchException;


    @Nullable
    default <T> T convertIfNecessary(@Nullable Object value, @Nullable Class<T> requiredType,
                                     @Nullable TypeDescriptor typeDescriptor) throws TypeMismatchException {

        throw new UnsupportedOperationException("TypeDescriptor resolution not supported");
    }

}
