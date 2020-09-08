package ink.zfei.summer.demo;

import ink.zfei.summer.context.ClassPathXmlApplicationContext;

import java.io.IOException;

public class XmlStarter {

    public static void main(String[] args) throws IOException {
        ClassPathXmlApplicationContext ctx = new ClassPathXmlApplicationContext("beans.xml");
        Person person = (Person) ctx.getBean("person");
//        System.out.println(person.getDevice());
        System.out.println(person);
        Device device = (Device) ctx.getBean("device");
//        System.out.println(device == device1);
//        System.out.println(device);
        System.out.println(device.getPerson());
    }

}
