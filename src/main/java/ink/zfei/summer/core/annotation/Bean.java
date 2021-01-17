package ink.zfei.summer.core.annotation;

import java.lang.annotation.*;

@Target({ElementType.METHOD, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Bean {


    boolean autowireCandidate() default true;


    String initMethod() default "";

//    @AliasFor("name") name是别名
    String[] value() default {};

}