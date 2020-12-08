package ink.zfei.summer.core.io.support;

import ink.zfei.summer.core.io.Resource;
import ink.zfei.summer.core.io.ResourceLoader;

import java.io.IOException;

public interface ResourcePatternResolver extends ResourceLoader {

    String CLASSPATH_ALL_URL_PREFIX = "classpath*:";

    Resource[] getResources(String locationPattern) throws IOException;
}
