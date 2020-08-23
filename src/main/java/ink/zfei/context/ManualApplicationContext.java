package ink.zfei.context;

import ink.zfei.core.AbstractApplicationContext;
import ink.zfei.core.ApplicationListener;
import ink.zfei.core.BeanDefinition;
import ink.zfei.demo.StartWebServerListener;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ManualApplicationContext extends AbstractApplicationContext {

    Map<String, BeanDefinition> beanDefinationMap = new ConcurrentHashMap<String, BeanDefinition>();
    List<ApplicationListener> listenerList = new ArrayList<>();


    public ManualApplicationContext(Map<String, BeanDefinition> beanDefinationMap, StartWebServerListener listener) throws IOException {
        super();
        this.beanDefinationMap = beanDefinationMap;
        addListener(listener);
        refresh();

    }


    @Override
    protected Map<String, BeanDefinition> loadBeanDefination() {
        return beanDefinationMap;
    }

    @Override
    protected Collection<ApplicationListener> getApplicationListeners() {
        return listenerList;
    }

    public void addListener(ApplicationListener listener) {
        listenerList.add(listener);
    }
}
