package ink.zfei.demo.annotation;

import ink.zfei.annation.Component;

@Component
public class Order {

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return "Order{" +
                "name='" + name + '\'' +
                '}';
    }

    public void setName(String name) {
        this.name = name;
    }

    private String name;
}
