package ink.zfei.summer.core.convert;

import ink.zfei.summer.lang.Nullable;

public interface ConversionService {


    boolean canConvert(@Nullable Class<?> sourceType, Class<?> targetType);


    boolean canConvert(@Nullable TypeDescriptor sourceType, TypeDescriptor targetType);


    @Nullable
    <T> T convert(@Nullable Object source, Class<T> targetType);

    /**
     * Convert the given {@code source} to the specified {@code targetType}.
     * The TypeDescriptors provide additional context about the source and target locations
     * where conversion will occur, often object fields or property locations.
     * @param source the source object to convert (may be {@code null})
     * @param sourceType context about the source type to convert from
     * (may be {@code null} if source is {@code null})
     * @param targetType context about the target type to convert to (required)
     * @return the converted object, an instance of {@link TypeDescriptor#getObjectType() targetType}
     * @throws ConversionException if a conversion exception occurred
     * @throws IllegalArgumentException if targetType is {@code null},
     * or {@code sourceType} is {@code null} but source is not {@code null}
     */
    @Nullable
    Object convert(@Nullable Object source, @Nullable TypeDescriptor sourceType, TypeDescriptor targetType);

}
