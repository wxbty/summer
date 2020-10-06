package ink.zfei.summer.beans.factory.config;

import ink.zfei.summer.lang.Nullable;

import java.io.Serializable;

public final class AutowiredPropertyMarker implements Serializable {

    /**
     * The canonical instance for the autowired marker value.
     */
    public static final Object INSTANCE = new AutowiredPropertyMarker();


    private AutowiredPropertyMarker() {
    }

    private Object readResolve() {
        return INSTANCE;
    }


    @Override
    public boolean equals(@Nullable Object obj) {
        return (this == obj);
    }

    @Override
    public int hashCode() {
        return AutowiredPropertyMarker.class.hashCode();
    }

    @Override
    public String toString() {
        return "(autowired)";
    }

}
