package context.xml;

import ink.zfei.summer.context.ClassPathXmlApplicationContext;
import bean.Device;
import bean.Person;
import org.junit.Assert;
import org.junit.Test;


public class XmlContextTest {

    @Test
    public void scanBasePackage() {
        ClassPathXmlApplicationContext ctx = new ClassPathXmlApplicationContext("beans.xml");
        Person person = (Person) ctx.getBean("person");
        Assert.assertNotNull(person);
    }

    @Test
    public void depInject() {
        ClassPathXmlApplicationContext ctx = new ClassPathXmlApplicationContext("beans.xml");
        Device device = (Device) ctx.getBean("device");
        Assert.assertNotNull(device.getPerson());
    }
}
