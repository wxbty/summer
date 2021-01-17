package ink.zfei.summer.core.type;

public interface MethodMetadata extends AnnotatedTypeMetadata{

    String getMethodName();

    boolean isStatic();
}
