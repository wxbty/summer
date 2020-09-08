package ink.zfei.summer.demo;

import ink.zfei.summer.context.ManualApplicationContext;
import ink.zfei.summer.core.GenericBeanDefinition;

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

        Map<String, GenericBeanDefinition> beanDefinationMap = new ConcurrentHashMap<String, GenericBeanDefinition>();
        GenericBeanDefinition beanDefination1 = new GenericBeanDefinition();
        beanDefination1.setId("person");
        beanDefination1.setBeanClassName("ink.zfei.demo.Person");
        beanDefination1.setInitMethodName(null);
        beanDefinationMap.put(beanDefination1.getId(), beanDefination1);

        StartWebServerListener listener = new StartWebServerListener();

        ManualApplicationContext beanFactory = new ManualApplicationContext(beanDefinationMap,listener);


        Person person = (Person) beanFactory.getBean("person");
        System.out.println(person);


    }
}
