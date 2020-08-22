package ink.zfei.demo;

import ink.zfei.context.ClassPathXmlApplicationContext;

import java.io.IOException;

public class XmlStarter {


    public static void main(String[] args) throws IOException {
        ClassPathXmlApplicationContext ctx = new ClassPathXmlApplicationContext("beans.xml");
        Person person = (Person) ctx.getBean("person");
        System.out.println(person);
    }

}
