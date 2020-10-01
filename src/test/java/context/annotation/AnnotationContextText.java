package context.annotation;

import bean.*;
import bean.annotation.NonOrder;
import bean.annotation.Order;
import ink.zfei.summer.context.AnnotationConfigApplicationContext;
import org.junit.Assert;
import org.junit.Test;

public class AnnotationContextText {

    @Test
    public void scanBasePackage() {
        System.out.println(Order.class.getPackage().getName());
        AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(Order.class.getPackage().getName());
        Order order = (Order) ctx.getBean("order");
        Assert.assertNotNull(order);
        NonOrder nonOrder = (NonOrder) ctx.getBean("nonOrder");
        Assert.assertNull(nonOrder);
    }

    @Test
    public void configurationClass() {
        AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(MyConfiguation.class);
        Device device = (Device) ctx.getBean("device");
        Assert.assertNotNull(device);
    }

    @Test
    public void singletonBean() {
        AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(MyConfiguation.class);
        Device device = (Device) ctx.getBean("device");
        Person person = (Person) ctx.getBean("person");
        Assert.assertSame(person, device.getPerson());
    }

    @Test
    public void importBean() {
        AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(MyConfiguation.class);
        Player player = (Player) ctx.getBean(Player.class);
        Assert.assertNotNull(player);
    }

    @Test
    public void importSelect() {
        AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(MyConfiguation.class);
        Water water = (Water) ctx.getBean("water");
        Assert.assertNotNull(water);
    }

}
