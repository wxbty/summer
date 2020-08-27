package ink.zfei.demo.annotation;

public class NonOrder {

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
