package ink.zfei.core;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public abstract class AbstractApplicationContext implements ApplicationContext {


    protected Map<String, BeanDefination> beanDefinationMap = new ConcurrentHashMap<String, BeanDefination>();
    protected Map<String, Object> beanSingleMap = new ConcurrentHashMap<String, Object>();

    public AbstractApplicationContext() {
    }


    public void refresh() throws IOException {
        //1、从外界获取bean定义信息
        beanDefinationMap = loadBeanDefination();

        //2、遍历bean定义信息，实例化bean
        beanDefinationMap.entrySet().stream().forEach(entry -> {
            BeanDefination beanDefination = entry.getValue();
            try {
                Class clazz = Class.forName(beanDefination.getBeanClass());
                Object bean = clazz.newInstance();


                //3、初始化bean
                String initMethodName = beanDefination.getInitMethod();
                if (initMethodName != null && initMethodName.length() > 0) {
                    Method method = clazz.getDeclaredMethod(initMethodName);
                    method.invoke(bean);
                }

                beanSingleMap.put(beanDefination.getId(), bean);
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InstantiationException e) {
                e.printStackTrace();
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            }
        });


        //4、发布refresh事件，遍历listener,分别执行
        RefreshApplicationEvent event = new RefreshApplicationEvent();
        publishEvent(event);

    }

    protected abstract Map<String, BeanDefination> loadBeanDefination() throws IOException;

    @Override
    public void publishEvent(ApplicationEvent event) {

        for (ApplicationListener applicationListener : getApplicationListeners()) {
            applicationListener.onApplicationEvent(event);
        }

    }

    protected Collection<ApplicationListener> getApplicationListeners() {
        List<ApplicationListener> listeners = beanSingleMap.entrySet().stream().filter(entry -> (entry.getValue() instanceof ApplicationListener)).map(entry -> (ApplicationListener) entry.getValue()).collect(Collectors.toList());
        return listeners;
    }


    @Override
    public Object getBean(String id) {
        return beanSingleMap.get(id);
    }

}
