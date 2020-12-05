package ink.zfei.summer.beans.factory.annotation;

import ink.zfei.summer.beans.factory.BeanCreationException;
import ink.zfei.summer.beans.factory.config.SmartInstantiationAwareBeanPostProcessor;
import ink.zfei.summer.beans.factory.support.GenericBeanDefinition;
import ink.zfei.summer.beans.factory.support.MergedBeanDefinitionPostProcessor;
import ink.zfei.summer.core.Ordered;
import ink.zfei.summer.core.PriorityOrdered;
import ink.zfei.summer.core.annotation.AnnotationAttributes;
import ink.zfei.summer.core.annotation.MergedAnnotation;
import ink.zfei.summer.core.annotation.MergedAnnotations;
import ink.zfei.summer.util.ClassUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class AutowiredAnnotationBeanPostProcessor implements SmartInstantiationAwareBeanPostProcessor, MergedBeanDefinitionPostProcessor, PriorityOrdered {

    private final Set<Class<? extends Annotation>> autowiredAnnotationTypes = new LinkedHashSet<>(4);
    private String requiredParameterName = "required";
    private boolean requiredParameterValue = true;
    private int order = Ordered.LOWEST_PRECEDENCE - 2;

    public AutowiredAnnotationBeanPostProcessor() {
        this.autowiredAnnotationTypes.add(Autowired.class);
        this.autowiredAnnotationTypes.add(Value.class);
    }

    @Override
    public void postProcessMergedBeanDefinition(GenericBeanDefinition beanDefinition, Class<?> beanType, String beanName) {

    }

    @Override
    public int getOrder() {
        return this.order;
    }

    @Override
    public Constructor<?>[] determineCandidateConstructors(Class<?> beanClass, final String beanName) {
        Constructor<?>[] rawCandidates = beanClass.getDeclaredConstructors();

        Constructor<?> requiredConstructor = null;
        Constructor<?> defaultConstructor = null;
        List<Constructor<?>> candidates = new ArrayList<>(rawCandidates.length);

        for (Constructor<?> candidate : rawCandidates) {
            MergedAnnotation<?> ann = findAutowiredAnnotation(candidate);
            if (ann == null) {
                //父类有没有@Autowired注解
                Class<?> userClass = ClassUtils.getUserClass(beanClass);
                if (userClass != beanClass) {
                    try {
                        Constructor<?> superCtor =
                                userClass.getDeclaredConstructor(candidate.getParameterTypes());
                        ann = findAutowiredAnnotation(superCtor);
                    } catch (NoSuchMethodException ex) {
                        // Simply proceed, no equivalent superclass constructor found...
                    }
                }
            }
            if (ann != null) {
                //@Autowired的 required =true只能有一个（默认true）
                if (requiredConstructor != null) {
                    throw new BeanCreationException(beanName,
                            "Invalid autowire-marked constructor: " + candidate +
                                    ". Found constructor with 'required' Autowired annotation already: " +
                                    requiredConstructor);
                }
                boolean required = determineRequiredStatus(ann);
                if (required) {
                    //再次判断
                    if (!candidates.isEmpty()) {
                        throw new BeanCreationException(beanName,
                                "Invalid autowire-marked constructors: " + candidate +
                                        ". Found constructor with 'required' Autowired annotation: " +
                                        candidate);
                    }
                    requiredConstructor = candidate;
                }
                candidates.add(candidate);
            } else if (candidate.getParameterCount() == 0) {
                defaultConstructor = candidate;
            }
        }

        Constructor<?>[] candidateConstructors;

        if (!candidates.isEmpty()) {
            // 没有指定required=true，使用默认无参构造器
            if (requiredConstructor == null && defaultConstructor != null) {
                candidates.add(defaultConstructor);
            }
            candidateConstructors = candidates.toArray(new Constructor<?>[0]);
        } else if (rawCandidates.length == 1 && rawCandidates[0].getParameterCount() > 0) {
            //只有一个没得选
            candidateConstructors = new Constructor<?>[]{rawCandidates[0]};
        } else {
            candidateConstructors = new Constructor<?>[0];
        }

        return (candidateConstructors.length > 0 ? candidateConstructors : null);
    }

    private MergedAnnotation<?> findAutowiredAnnotation(AccessibleObject ao) {
        MergedAnnotations annotations = MergedAnnotations.from(ao);
        for (Class<? extends Annotation> type : this.autowiredAnnotationTypes) {
            MergedAnnotation<?> annotation = annotations.get(type);
            if (annotation.isPresent()) {
                return annotation;
            }
        }
        return null;
    }

    protected boolean determineRequiredStatus(MergedAnnotation<?> ann) {
        // The following (AnnotationAttributes) cast is required on JDK 9+.
        return determineRequiredStatus((AnnotationAttributes)
                ann.asMap(mergedAnnotation -> new AnnotationAttributes(mergedAnnotation.getType())));
    }

    protected boolean determineRequiredStatus(AnnotationAttributes ann) {
        return (!ann.containsKey(this.requiredParameterName) ||
                this.requiredParameterValue == ann.getBoolean(this.requiredParameterName));
    }
}
