package ink.zfei.summer.core.io;

import ink.zfei.summer.util.ResourceUtils;

public interface ResourceLoader {

    String CLASSPATH_URL_PREFIX = ResourceUtils.CLASSPATH_URL_PREFIX;


    Resource getResource(String location);

    ClassLoader getClassLoader();
}
