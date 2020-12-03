package ink.zfei.summer.context.support;

import ink.zfei.summer.beans.factory.BeanCreationException;
import ink.zfei.summer.beans.factory.FactoryBean;
import ink.zfei.summer.beans.factory.support.DefaultSingletonBeanRegistry;
import ink.zfei.summer.beans.factory.support.NullBean;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public abstract class FactoryBeanRegistrySupport extends DefaultSingletonBeanRegistry {

    private final Map<String, Object> factoryBeanObjectCache = new ConcurrentHashMap<>(16);


    public Object getObjectFromFactoryBean(FactoryBean<?> factory, String beanName) {
        if (factory.isSingleton() && containsSingleton(beanName)) {
            Object object = this.factoryBeanObjectCache.get(beanName);
            if (object == null) {
                object = doGetObjectFromFactoryBean(factory, beanName);
                // Only post-process and store if not put there already during getObject() call above
                // (e.g. because of circular reference processing triggered by custom getBean calls)
                Object alreadyThere = this.factoryBeanObjectCache.get(beanName);
                if (alreadyThere != null) {
                    object = alreadyThere;
                } else {
                    if (containsSingleton(beanName)) {
                        this.factoryBeanObjectCache.put(beanName, object);
                    }
                }
            }
            return object;
        } else {
            return doGetObjectFromFactoryBean(factory, beanName);
        }
    }

    private Object doGetObjectFromFactoryBean(final FactoryBean<?> factory, final String beanName)
            throws BeanCreationException {

        Object object = factory.getObject();
        // Do not accept a null value for a FactoryBean that's not fully
        // initialized yet: Many FactoryBeans just return null then.
        if (object == null) {

            object = new NullBean();
        }
        return object;
    }
}
