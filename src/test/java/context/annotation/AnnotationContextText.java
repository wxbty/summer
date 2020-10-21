package context.annotation;

import bean.*;
import bean.annotation.Order;
import context.common.CommonProcessorConfiguation;
import context.common.Iperson;
import ink.zfei.summer.beans.factory.NoSuchBeanDefinitionException;
import ink.zfei.summer.context.AnnotationConfigApplicationContext;
import ink.zfei.summer.core.ApplicationListener;
import ink.zfei.summer.core.io.support.SpringFactoriesLoader;
import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Proxy;
import java.util.List;

public class AnnotationContextText {

    @Test
    public void scanBasePackage() {
        System.out.println(Order.class.getPackage().getName());
        AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(Order.class.getPackage().getName());
        Order order = (Order) ctx.getBean("order");
        Assert.assertNotNull(order);
        try {
            ctx.getBean("nonOrder");
        } catch (NoSuchBeanDefinitionException e) {
            Assert.assertNotNull(e);
        }
    }

    @Test
    public void configurationClass() {
        AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(MyConfiguation.class);
        Device device = (Device) ctx.getBean("device");
        Assert.assertNotNull(device);
    }

    @Test
    public void resource() {
        AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(MyConfiguation.class);
        Player player = (Player) ctx.getBean("player");
        player.name();
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

    @Test
    public void beanPostProcessor() {
        AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(CommonProcessorConfiguation.class);
        Iperson person = (Iperson) ctx.getBean("person");
        person.say();
        Assert.assertNotNull(person);
        Assert.assertTrue(person instanceof Proxy);
    }

    @Test
    public void loadSpringFactories() {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        List<String> list = SpringFactoriesLoader.loadFactoryNames(ApplicationListener.class, loader);
        Assert.assertTrue(list.contains("context.common.StartWebServerListener"));
    }

    @Test
    public void factoryBean() {
        AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(CommonProcessorConfiguation.class);
        Object personMapper = ctx.getBean("personMapper");
        Assert.assertTrue(personMapper instanceof PersonMapper);
    }

    @Test
    public void ConstructorInject() {
        AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(Person.class.getPackage().getName());
        IndexController indexController = (IndexController) ctx.getBean("indexController");
        Assert.assertNotNull(indexController);
        Assert.assertNotNull(indexController.getPerson());
    }

}