package ink.zfei.summer.core.annotation;

import ink.zfei.summer.beans.factory.support.GenericBeanDefinition;

public class RootBeanDefination extends GenericBeanDefinition {


    public Class getConfiguationClass() {
        return configuationClass;
    }

    public void setConfiguationClass(Class configuationClass) {
        this.configuationClass = configuationClass;
    }

    private Class configuationClass;


}
