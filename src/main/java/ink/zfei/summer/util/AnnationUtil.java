package ink.zfei.summer.util;

import ink.zfei.summer.core.annation.Configuration;

import java.lang.annotation.Annotation;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.List;

public class AnnationUtil {

    static List<Class<? extends Annotation>> filterMapping = new ArrayList<>();

    static {
        filterMapping.add(Target.class);
        filterMapping.add(Retention.class);
        filterMapping.add(Documented.class);
    }

    public static <A extends Annotation> Annotation findAnnotation(Class<?> clazz, Class<A> annotationType) {
        if (annotationType == null) {
            return null;
        }
        Annotation[] annotations = clazz.getDeclaredAnnotations();
        if (annotations.length == 0) {
            return null;
        }
        for (Annotation annotation : annotations) {
            if (filterMapping.contains(annotation.annotationType())) {
                continue;
            }
            Annotation res = mapping(annotation.annotationType(), annotation, annotationType);
            if (res != null) {
                return res;
            }
        }
        return null;

    }

    //某个class里有没有我要的注解
    private static <A extends Annotation> Annotation mapping(Class<?> annationclazz, Annotation ann, Class<A> annotationType) {

        if (annationclazz == annotationType) {
            return ann;
        }
        Annotation[] annotations = annationclazz.getDeclaredAnnotations();
        if (annotations.length == 0) {
            return null;
        } else {
            for (Annotation annotation : annotations) {
                if (filterMapping.contains(annotation.annotationType())) {
                    continue;
                }
                Annotation res = mapping(annotation.annotationType(), annotation, annotationType);
                if (res != null) {
                    return res;
                }
            }
        }
        return null;
    }

    public static boolean isAnnotation(Class outClass, Class<Configuration> configurationClass) {

        return findAnnotation(outClass, configurationClass) != null;
    }
}
