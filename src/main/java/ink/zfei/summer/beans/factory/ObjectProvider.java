package ink.zfei.summer.beans.factory;

import ink.zfei.summer.lang.Nullable;

import java.util.Iterator;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Stream;

public interface ObjectProvider<T> extends ObjectFactory<T>, Iterable<T> {

    
    T getObject(Object... args) ;

   
    @Nullable
    T getIfAvailable() ;

    /**
     * Return an instance (possibly shared or independent) of the object
     * managed by this factory.
     * @param defaultSupplier a callback for supplying a default object
     * if none is present in the factory
     * @return an instance of the bean, or the supplied default object
     * if no such bean is available
     * @ in case of creation errors
     * @since 5.0
     * @see #getIfAvailable()
     */
    default T getIfAvailable(Supplier<T> defaultSupplier)  {
        T dependency = getIfAvailable();
        return (dependency != null ? dependency : defaultSupplier.get());
    }

    /**
     * Consume an instance (possibly shared or independent) of the object
     * managed by this factory, if available.
     * @param dependencyConsumer a callback for processing the target object
     * if available (not called otherwise)
     * @ in case of creation errors
     * @since 5.0
     * @see #getIfAvailable()
     */
    default void ifAvailable(Consumer<T> dependencyConsumer)  {
        T dependency = getIfAvailable();
        if (dependency != null) {
            dependencyConsumer.accept(dependency);
        }
    }

    /**
     * Return an instance (possibly shared or independent) of the object
     * managed by this factory.
     * @return an instance of the bean, or {@code null} if not available or
     * not unique (i.e. multiple candidates found with none marked as primary)
     * @ in case of creation errors
     * @see #getObject()
     */
    @Nullable
    T getIfUnique() ;

    /**
     * Return an instance (possibly shared or independent) of the object
     * managed by this factory.
     * @param defaultSupplier a callback for supplying a default object
     * if no unique candidate is present in the factory
     * @return an instance of the bean, or the supplied default object
     * if no such bean is available or if it is not unique in the factory
     * (i.e. multiple candidates found with none marked as primary)
     * @ in case of creation errors
     * @since 5.0
     * @see #getIfUnique()
     */
    default T getIfUnique(Supplier<T> defaultSupplier)  {
        T dependency = getIfUnique();
        return (dependency != null ? dependency : defaultSupplier.get());
    }

    /**
     * Consume an instance (possibly shared or independent) of the object
     * managed by this factory, if unique.
     * @param dependencyConsumer a callback for processing the target object
     * if unique (not called otherwise)
     * @ in case of creation errors
     * @since 5.0
     * @see #getIfAvailable()
     */
    default void ifUnique(Consumer<T> dependencyConsumer)  {
        T dependency = getIfUnique();
        if (dependency != null) {
            dependencyConsumer.accept(dependency);
        }
    }

    /**
     * Return an {@link Iterator} over all matching object instances,
     * without specific ordering guarantees (but typically in registration order).
     * @since 5.1
     * @see #stream()
     */
    @Override
    default Iterator<T> iterator() {
        return stream().iterator();
    }

    /**
     * Return a sequential {@link Stream} over all matching object instances,
     * without specific ordering guarantees (but typically in registration order).
     * @since 5.1
     * @see #iterator()
     * @see #orderedStream()
     */
    default Stream<T> stream() {
        throw new UnsupportedOperationException("Multi element access not supported");
    }


    default Stream<T> orderedStream() {
        throw new UnsupportedOperationException("Ordered element access not supported");
    }

}
