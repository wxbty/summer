package ink.zfei.demo;

import ink.zfei.context.ManualApplicationContext;
import ink.zfei.core.BeanDefination;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Starter {

    public static void main(String[] args) throws IOException {

        Map<String, BeanDefination> beanDefinationMap = new ConcurrentHashMap<String, BeanDefination>();
        BeanDefination beanDefination1 = new BeanDefination();
        beanDefination1.setId("person");
        beanDefination1.setBeanClass("ink.zfei.demo.Person");
        beanDefination1.setInitMethod(null);
        beanDefinationMap.put(beanDefination1.getId(), beanDefination1);

        StartWebServerListener listener = new StartWebServerListener();

        ManualApplicationContext beanFactory = new ManualApplicationContext(beanDefinationMap,listener);


        Person person = (Person) beanFactory.getBean("person");
        System.out.println(person);


    }
}
