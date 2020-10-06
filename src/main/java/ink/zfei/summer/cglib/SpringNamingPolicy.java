package ink.zfei.summer.cglib;

import net.sf.cglib.core.DefaultNamingPolicy;

public class SpringNamingPolicy extends DefaultNamingPolicy {

    public static final SpringNamingPolicy INSTANCE = new SpringNamingPolicy();

    @Override
    protected String getTag() {
        return "BySpringCGLIB";
    }

}
