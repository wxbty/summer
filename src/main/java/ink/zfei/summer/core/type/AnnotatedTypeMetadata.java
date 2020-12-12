package ink.zfei.summer.core.type;

import ink.zfei.summer.core.annotation.MergedAnnotation;
import ink.zfei.summer.core.annotation.MergedAnnotationSelectors;
import ink.zfei.summer.core.annotation.MergedAnnotations;
import ink.zfei.summer.lang.Nullable;

import java.lang.annotation.Annotation;
import java.util.Map;

/**
 *  所有可以加注解的元素都属于AnnotatedTypeMetadata
 *  比如class、method、field
 */
public interface AnnotatedTypeMetadata {

    /* 获取元素上所有的封装过的注解  */
    MergedAnnotations getAnnotations();

    /* 当前元素上是否有指定的注解（全类名）  */
    default boolean isAnnotated(String annotationName) {
        return getAnnotations().isPresent(annotationName);
    }

    /**
     *  这个厉害：取得指定类型注解的所有的属性 - 值（k-v）
     * 	classValuesAsString：若是true表示 Class用它的字符串的全类名来表示。
     * 	这样可以避免Class被提前加载
     */
    @Nullable
    default Map<String, Object> getAnnotationAttributes(String annotationName) {
        return getAnnotationAttributes(annotationName, false);
    }

    @Nullable
    default Map<String, Object> getAnnotationAttributes(String annotationName,
                                                        boolean classValuesAsString) {

        MergedAnnotation<Annotation> annotation = getAnnotations().get(annotationName,
                null, MergedAnnotationSelectors.firstDirectlyDeclared());
        if (!annotation.isPresent()) {
            return null;
        }
        return annotation.asAnnotationAttributes(MergedAnnotation.Adapt.values(classValuesAsString, true));
    }
}
