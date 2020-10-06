package ink.zfei.summer.core.annotation;

import ink.zfei.summer.lang.Nullable;

import java.lang.reflect.Method;

@FunctionalInterface
interface ValueExtractor {

    /**
     * Extract the annotation attribute represented by the supplied {@link Method}
     * from the supplied source {@link Object}.
     */
    @Nullable
    Object extract(Method attribute, @Nullable Object object);

}
