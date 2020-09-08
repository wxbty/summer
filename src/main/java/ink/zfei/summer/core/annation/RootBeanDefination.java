package ink.zfei.summer.core.annation;

import ink.zfei.summer.core.GenericBeanDefinition;

public class RootBeanDefination extends GenericBeanDefinition {


    public Class getConfiguationClass() {
        return configuationClass;
    }

    public void setConfiguationClass(Class configuationClass) {
        this.configuationClass = configuationClass;
    }

    private Class configuationClass;


}
