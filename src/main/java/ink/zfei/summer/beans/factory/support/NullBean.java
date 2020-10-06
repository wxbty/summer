package ink.zfei.summer.beans.factory.support;

import ink.zfei.summer.lang.Nullable;

public final class NullBean {

    public NullBean() {
    }


    @Override
    public boolean equals(@Nullable Object obj) {
        return (this == obj || obj == null);
    }

    @Override
    public int hashCode() {
        return NullBean.class.hashCode();
    }

    @Override
    public String toString() {
        return "null";
    }

}
