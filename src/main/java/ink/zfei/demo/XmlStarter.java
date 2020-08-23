package ink.zfei.demo;

import ink.zfei.context.ClassPathXmlApplicationContext;

import java.io.IOException;

public class XmlStarter {


    public static void main(String[] args) throws IOException {
        ClassPathXmlApplicationContext ctx = new ClassPathXmlApplicationContext("beans.xml");
        Starter starter = (Starter) ctx.getBean("starter");
        System.out.println(starter);
//        System.out.println(person);
//        Device device1 = (Device) ctx.getBean("device");
//        System.out.println(device == device1);
//        System.out.println(device);
    }

}
