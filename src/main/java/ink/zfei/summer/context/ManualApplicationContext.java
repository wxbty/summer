package ink.zfei.summer.context;

import ink.zfei.summer.core.AbstractApplicationContext;
import ink.zfei.summer.core.ApplicationListener;
import ink.zfei.summer.core.GenericBeanDefinition;
import ink.zfei.summer.demo.StartWebServerListener;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ManualApplicationContext extends AbstractApplicationContext {

    Map<String, GenericBeanDefinition> beanDefinationMap = new ConcurrentHashMap<String, GenericBeanDefinition>();
    List<ApplicationListener> listenerList = new ArrayList<>();


    public ManualApplicationContext(Map<String, GenericBeanDefinition> beanDefinationMap, StartWebServerListener listener) throws IOException {
        super();
        this.beanDefinationMap = beanDefinationMap;
        addListener(listener);
        refresh();

    }


    @Override
    protected Map<String, GenericBeanDefinition> loadBeanDefination() {
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
