package bean;

import ink.zfei.summer.annation.Component;
import ink.zfei.summer.beans.factory.InitializingBean;
import context.common.Iperson;

@Component
public class Person implements InitializingBean, Iperson {

//    @Override
//    public String toString() {
//        return "Person{" +
//                "name='" + name + '\'' +
//                ", age=" + age +
//                ", device=" + device +
//                '}';
//    }

    private String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    private int age;

    public void setDevice(Device device) {
        this.device = device;
    }

    public Device getDevice() {
        return device;
    }

    private Device device;

    @Override
    public void say() {
        System.out.println("hello world!");
    }

//    public void init()
//    {
//        System.out.println("person init...");
//    }



    @Override
    public void afterPropertiesSet() {
        System.out.println("afterPropertiesSet .....");
    }
}
