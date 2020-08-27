package ink.zfei.demo;

import ink.zfei.context.AnnotationConfigApplicationContext;
import ink.zfei.context.ClassPathXmlApplicationContext;
import ink.zfei.demo.annotation.NonOrder;
import ink.zfei.demo.annotation.Order;
import ink.zfei.demo.annotation.SubOrder;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.net.URISyntaxException;

public class AnnationStarter {


    public static void main(String[] args) throws IOException, URISyntaxException, ClassNotFoundException {
//        AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext("ink.zfei.demo.annotation");
//        SubOrder order = (SubOrder) ctx.getBean("subOrder");
//        System.out.println(order);
//        NonOrder nonOrder = (NonOrder) ctx.getBean("nonOrder");
//        System.out.println(nonOrder);

        Class clazz = Class.forName("ink.zfei.demo.annotation.SubOrder");
        Annotation[] components = clazz.getAnnotations();
        System.out.println(components);

//
    }

}
