package ink.zfei.summer.core.annation;

import ink.zfei.summer.annation.Component;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Component
public @interface Configuration {



    boolean proxyBeanMethods() default true;

}
