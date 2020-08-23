package ink.zfei.demo;

import ink.zfei.context.ManualApplicationContext;
import ink.zfei.core.BeanDefinition;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Starter {


    @Override
    public String toString() {
        return "Starter{" +
                "name='" + name + '\'' +
                '}';
    }

    private String name = "zhangsan";

    public static void main(String[] args) throws IOException {

        Map<String, BeanDefinition> beanDefinationMap = new ConcurrentHashMap<String, BeanDefinition>();
        BeanDefinition beanDefination1 = new BeanDefinition();
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
