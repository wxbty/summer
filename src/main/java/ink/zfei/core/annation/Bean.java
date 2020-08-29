package ink.zfei.core.annation;

import java.lang.annotation.*;

@Target({ElementType.METHOD, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Bean {


    boolean autowireCandidate() default true;


    String initMethod() default "";



}