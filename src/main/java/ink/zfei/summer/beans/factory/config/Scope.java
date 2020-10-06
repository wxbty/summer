package ink.zfei.summer.beans.factory.config;

import ink.zfei.summer.beans.factory.ObjectFactory;
import ink.zfei.summer.lang.Nullable;

public interface Scope {


    Object get(String name, ObjectFactory<?> objectFactory);

    /**
     * Remove the object with the given {@code name} from the underlying scope.
     * <p>Returns {@code null} if no object was found; otherwise
     * returns the removed {@code Object}.
     * <p>Note that an implementation should also remove a registered destruction
     * callback for the specified object, if any. It does, however, <i>not</i>
     * need to <i>execute</i> a registered destruction callback in this case,
     * since the object will be destroyed by the caller (if appropriate).
     * <p><b>Note: This is an optional operation.</b> Implementations may throw
     * {@link UnsupportedOperationException} if they do not support explicitly
     * removing an object.
     * @param name the name of the object to remove
     * @return the removed object, or {@code null} if no object was present
     * @throws IllegalStateException if the underlying scope is not currently active
     * @see #registerDestructionCallback
     */
    @Nullable
    Object remove(String name);


    void registerDestructionCallback(String name, Runnable callback);

    /**
     * Resolve the contextual object for the given key, if any.
     * E.g. the HttpServletRequest object for key "request".
     * @param key the contextual key
     * @return the corresponding object, or {@code null} if none found
     * @throws IllegalStateException if the underlying scope is not currently active
     */
    @Nullable
    Object resolveContextualObject(String key);


    @Nullable
    String getConversationId();

}
