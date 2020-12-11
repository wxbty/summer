package ink.zfei.summer.core.type;

import ink.zfei.summer.util.Assert;

public class StandardClassMetadata implements ClassMetadata {

    private final Class<?> introspectedClass;

    public StandardClassMetadata(Class<?> introspectedClass) {
        Assert.notNull(introspectedClass, "Class must not be null");
        this.introspectedClass = introspectedClass;
    }

    @Override
    public String getClassName() {
        return null;
    }

    @Override
    public boolean isInterface() {
        return false;
    }

    @Override
    public boolean isAnnotation() {
        return false;
    }

    @Override
    public boolean isAbstract() {
        return false;
    }
}
