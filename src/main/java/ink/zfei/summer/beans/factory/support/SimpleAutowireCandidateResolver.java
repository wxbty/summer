package ink.zfei.summer.beans.factory.support;

import ink.zfei.summer.beans.factory.FactoryBean;
import ink.zfei.summer.beans.factory.config.BeanDefinition;
import ink.zfei.summer.beans.factory.config.BeanDefinitionHolder;
import ink.zfei.summer.beans.factory.config.DependencyDescriptor;
import ink.zfei.summer.core.*;
import ink.zfei.summer.lang.Nullable;
import ink.zfei.summer.util.ClassUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Properties;

public class SimpleAutowireCandidateResolver implements AutowireCandidateResolver, ApplicationContextAware {

    /**
     * Shared instance of {@code SimpleAutowireCandidateResolver}.
     *
     * @since 5.2.7
     */
    public static final SimpleAutowireCandidateResolver INSTANCE = new SimpleAutowireCandidateResolver();

    private ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }


    @Override
    public boolean isAutowireCandidate(BeanDefinitionHolder bdHolder, DependencyDescriptor descriptor) {
        boolean checkBd = bdHolder.getBeanDefinition().isAutowireCandidate();
        if (!checkBd) {
            return false;
        }
        boolean match = checkGenericTypeMatch(bdHolder, descriptor);
        if (match) {
            match = checkQualifiers(bdHolder, descriptor.getAnnotations());
            if (match) {
                MethodParameter methodParam = descriptor.getMethodParameter();
                if (methodParam != null) {
                    Method method = methodParam.getMethod();
                    if (method == null || void.class == method.getReturnType()) {
                        match = checkQualifiers(bdHolder, methodParam.getMethodAnnotations());
                    }
                }
            }
        }
        return match;
    }

    protected boolean checkQualifiers(BeanDefinitionHolder bdHolder, Annotation[] annotationsToSearch) {

        return true;
    }

    /**
     * Checks whether the given annotation type is a recognized qualifier type.
     */
    protected boolean isQualifier(Class<? extends Annotation> annotationType) {

        return false;
    }

    @Override
    public boolean isRequired(DependencyDescriptor descriptor) {
        return descriptor.isRequired();
    }

    @Override
    public boolean hasQualifier(DependencyDescriptor descriptor) {
        return false;
    }

    @Override
    @Nullable
    public Object getSuggestedValue(DependencyDescriptor descriptor) {
        return null;
    }

    @Override
    @Nullable
    public Object getLazyResolutionProxyIfNecessary(DependencyDescriptor descriptor, @Nullable String beanName) {
        return null;
    }

    /**
     * This implementation returns {@code this} as-is.
     *
     * @see #INSTANCE
     */
    @Override
    public AutowireCandidateResolver cloneIfNecessary() {
        return this;
    }


    protected boolean checkGenericTypeMatch(BeanDefinitionHolder bdHolder, DependencyDescriptor descriptor) {
        ResolvableType dependencyType = descriptor.getResolvableType();
        if (dependencyType.getType() instanceof Class) {
            // No generic type -> we know it's a Class type-match, so no need to check again.
            return true;
        }

        ResolvableType targetType = null;
        boolean cacheType = false;
        GenericBeanDefinition rbd = null;
        if (bdHolder.getBeanDefinition() instanceof GenericBeanDefinition) {
            rbd = (GenericBeanDefinition) bdHolder.getBeanDefinition();
        }
        if (rbd != null) {
            targetType = rbd.targetType;
            if (targetType == null) {
                cacheType = true;
                // First, check factory method return type, if applicable
                targetType = getReturnTypeForFactoryMethod(rbd, descriptor);
                if (targetType == null) {
                    GenericBeanDefinition dbd = getResolvedDecoratedDefinition(rbd);
                    if (dbd != null) {
                        targetType = dbd.targetType;
                        if (targetType == null) {
                            targetType = getReturnTypeForFactoryMethod(dbd, descriptor);
                        }
                    }
                }
            }
        }

        if (targetType == null) {
            // Regular case: straight bean instance, with BeanFactory available.
            if (this.applicationContext != null) {
                Class<?> beanType = this.applicationContext.getType(bdHolder.getBeanName());
                if (beanType != null) {
                    targetType = ResolvableType.forClass(ClassUtils.getUserClass(beanType));
                }
            }
            // Fallback: no BeanFactory set, or no type resolvable through it
            // -> best-effort match against the target class if applicable.
            if (targetType == null && rbd != null && rbd.hasBeanClass() && rbd.getFactoryMethodName() == null) {
                Class<?> beanClass = rbd.getBeanClass();
                if (!FactoryBean.class.isAssignableFrom(beanClass)) {
                    targetType = ResolvableType.forClass(ClassUtils.getUserClass(beanClass));
                }
            }
        }

        if (targetType == null) {
            return true;
        }
        if (cacheType) {
            rbd.targetType = targetType;
        }
        if (descriptor.fallbackMatchAllowed() &&
                (targetType.hasUnresolvableGenerics() || targetType.resolve() == Properties.class)) {
            // Fallback matches allow unresolvable generics, e.g. plain HashMap to Map<String,String>;
            // and pragmatically also java.util.Properties to any Map (since despite formally being a
            // Map<Object,Object>, java.util.Properties is usually perceived as a Map<String,String>).
            return true;
        }
        // Full check for complex generic type match...
        return dependencyType.isAssignableFrom(targetType);
    }

    protected ResolvableType getReturnTypeForFactoryMethod(GenericBeanDefinition rbd, DependencyDescriptor descriptor) {
        // Should typically be set for any kind of factory method, since the BeanFactory
        // pre-resolves them before reaching out to the AutowireCandidateResolver...
        ResolvableType returnType = rbd.factoryMethodReturnType;
        if (returnType == null) {
            Method factoryMethod = rbd.getResolvedFactoryMethod();
            if (factoryMethod != null) {
                returnType = ResolvableType.forMethodReturnType(factoryMethod);
            }
        }
        if (returnType != null) {
            Class<?> resolvedClass = returnType.resolve();
            if (resolvedClass != null && descriptor.getDependencyType().isAssignableFrom(resolvedClass)) {
                // Only use factory method metadata if the return type is actually expressive enough
                // for our dependency. Otherwise, the returned instance type may have matched instead
                // in case of a singleton instance having been registered with the container already.
                return returnType;
            }
        }
        return null;
    }

    protected GenericBeanDefinition getResolvedDecoratedDefinition(GenericBeanDefinition rbd) {
        BeanDefinitionHolder decDef = rbd.getDecoratedDefinition();
        if (decDef != null) {
            AbstractApplicationContext clbf = (AbstractApplicationContext) this.applicationContext;
            if (clbf.containsBeanDefinition(decDef.getBeanName())) {
                BeanDefinition dbd = clbf.getBeanDefinition(decDef.getBeanName());
                if (dbd instanceof GenericBeanDefinition) {
                    return (GenericBeanDefinition) dbd;
                }
            }
        }
        return null;
    }
}