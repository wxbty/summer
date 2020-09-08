package ink.zfei.summer.demo;

import ink.zfei.summer.context.AnnotationConfigApplicationContext;
import ink.zfei.summer.demo.annotation.NonOrder;
import ink.zfei.summer.demo.annotation.SubOrder;

import java.io.IOException;
import java.net.URISyntaxException;

public class AnnationStarter {


    public static void main(String[] args) throws IOException, URISyntaxException, ClassNotFoundException {
//        scanBasePackage();
        configuation();

    }

    private static void scanBasePackage() throws IOException, URISyntaxException, ClassNotFoundException {
        AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext("ink.zfei.demo.annotation");
        SubOrder order = (SubOrder) ctx.getBean("subOrder");
        System.out.println(order);
        NonOrder nonOrder = (NonOrder) ctx.getBean("nonOrder");
        System.out.println(nonOrder);
    }

    private static void configuation() throws IOException, URISyntaxException, ClassNotFoundException {
        AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(TestConfiguation.class);
        Person person = (Person) ctx.getBean("getPerson");
        System.out.println(person);

    }

}
