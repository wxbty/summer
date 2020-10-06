package ink.zfei.summer.core.annotation;

import ink.zfei.summer.annation.Component;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@Component
public @interface Configuration {



    boolean proxyBeanMethods() default true;

}
