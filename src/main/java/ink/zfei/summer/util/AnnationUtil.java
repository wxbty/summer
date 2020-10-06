package ink.zfei.summer.util;

import ink.zfei.summer.core.annotation.Configuration;

import java.io.File;
import java.lang.annotation.Annotation;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
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

    public static String resolveBasePackage(String basePackage) {
        return basePackage.replace('.', '/');
    }

    public static URI toURI(String location) throws URISyntaxException {
        return new URI(replace(location, " ", "%20"));
    }


    public static String replace(String inString, String oldPattern, String newPattern) {
        if (newPattern == null) {
            return inString;
        }
        int index = inString.indexOf(oldPattern);
        if (index == -1) {
            // no occurrence -> can return input as-is
            return inString;
        }

        int capacity = inString.length();
        if (newPattern.length() > oldPattern.length()) {
            capacity += 16;
        }
        StringBuilder sb = new StringBuilder(capacity);

        int pos = 0;  // our position in the old string
        int patLen = oldPattern.length();
        while (index >= 0) {
            sb.append(inString, pos, index);
            sb.append(newPattern);
            pos = index + patLen;
            index = inString.indexOf(oldPattern, pos);
        }

        // append any characters to the right of a match
        sb.append(inString, pos, inString.length());
        return sb.toString();
    }

    public static File[] listDirectory(File dir) {
        File[] files = dir.listFiles();
        if (files == null) {
            return new File[0];
        }
        Arrays.sort(files, Comparator.comparing(File::getName));
        return files;
    }
}
