package ink.zfei.demo;

import ink.zfei.core.FactoryBean;

public class Device  implements FactoryBean {

    private String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }


    private int status;

    public void init() {
        System.out.println("person init...");
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return "Device{" +
                "name='" + name + '\'' +
                ", status=" + status +
                '}';
    }

    @Override
    public Object getObject() {
        System.out.println("通过factorybean创建的 bean");
        return new Device();
    }
}
