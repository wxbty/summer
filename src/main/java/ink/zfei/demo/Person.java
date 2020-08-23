package ink.zfei.demo;

import ink.zfei.core.InitializingBean;

public class Person implements InitializingBean,Iperson {

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

    @Override
    public void say() {
        System.out.println("hello world!");
    }

//    public void init()
//    {
//        System.out.println("person init...");
//    }

    @Override
    public String toString() {
        return "Person{" +
                "name='" + name + '\'' +
                ", age=" + age +
                '}';
    }

    @Override
    public void afterPropertiesSet() {
        System.out.println("afterPropertiesSet .....");
    }
}
