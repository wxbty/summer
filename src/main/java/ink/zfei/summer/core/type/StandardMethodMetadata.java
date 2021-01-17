package ink.zfei.summer.core.type;

import ink.zfei.summer.core.annotation.MergedAnnotations;
import ink.zfei.summer.core.annotation.RepeatableContainers;
import ink.zfei.summer.util.Assert;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

public class StandardMethodMetadata implements MethodMetadata{

    private final MergedAnnotations mergedAnnotations;

    private final Method introspectedMethod;

    public StandardMethodMetadata(Method introspectedMethod, boolean nestedAnnotationsAsMap) {
        Assert.notNull(introspectedMethod, "Method must not be null");
        this.introspectedMethod = introspectedMethod;
        this.mergedAnnotations = MergedAnnotations.from(
                introspectedMethod, MergedAnnotations.SearchStrategy.DIRECT, RepeatableContainers.none());
    }

    @Override
    public MergedAnnotations getAnnotations() {
        return this.mergedAnnotations;
    }

    @Override
    public String getMethodName() {
        return this.introspectedMethod.getName();
    }

    @Override
    public boolean isStatic() {
        return Modifier.isStatic(this.introspectedMethod.getModifiers());
    }

    public final Method getIntrospectedMethod() {
        return this.introspectedMethod;
    }
}
