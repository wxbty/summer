package ink.zfei.core.annation;

import ink.zfei.annation.Component;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Component
public @interface Configuration {



    boolean proxyBeanMethods() default true;

}
