package ink.zfei.summer.core.annotation;

import java.lang.annotation.Annotation;
import java.util.Collection;

public class AnnotationUtils {

    public static boolean isCandidateClass(Class<?> clazz, Collection<Class<? extends Annotation>> annotationTypes) {
        for (Class<? extends Annotation> annotationType : annotationTypes) {
            if (isCandidateClass(clazz, annotationType)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isCandidateClass(Class<?> clazz, Class<? extends Annotation> annotationType) {
        return isCandidateClass(clazz, annotationType.getName());
    }

    public static boolean isCandidateClass(Class<?> clazz, String annotationName) {
        if (annotationName.startsWith("java.")) {
            return true;
        }
        if (AnnotationsScanner.hasPlainJavaAnnotationsOnly(clazz)) {
            return false;
        }
        return true;
    }
}
