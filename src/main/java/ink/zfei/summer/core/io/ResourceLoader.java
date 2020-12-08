package ink.zfei.summer.core.io;

public interface ResourceLoader {

    Resource getResource(String location);

    ClassLoader getClassLoader();
}
