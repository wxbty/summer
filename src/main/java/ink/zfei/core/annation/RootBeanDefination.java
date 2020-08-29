package ink.zfei.core.annation;

import ink.zfei.core.ApplicationContext;
import ink.zfei.core.BeanDefinition;

public class RootBeanDefination extends BeanDefinition {


    public Class getConfiguationClass() {
        return configuationClass;
    }

    public void setConfiguationClass(Class configuationClass) {
        this.configuationClass = configuationClass;
    }

    private Class configuationClass;


}
