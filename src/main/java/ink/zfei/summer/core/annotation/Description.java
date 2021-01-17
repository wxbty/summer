package ink.zfei.summer.core.annotation;

import java.lang.annotation.*;

@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Description {

    /**
     * The textual description to associate with the bean definition.
     */
    String value();

}
