package ink.zfei.xmlParse;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

public class Bean {

    private String id;
    private String beanClass;
    private String initMethod;

    public List<Bean.Property> getProperty() {
        return property;
    }

    public void addProperty(String name, String value, String ref) {

        Property property = new Property();
        property.setName(name);
        property.setValue(value);
        property.setRef(ref);
        if (this.property.contains(property)) {
            throw new RuntimeException("property 不能重复");
        }
        this.property.add(property);
    }

    private List<Property> property = new CopyOnWriteArrayList<>();

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    private String scope;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getBeanClass() {
        return beanClass;
    }

    public void setBeanClass(String beanClass) {
        this.beanClass = beanClass;
    }

    public String getInitMethod() {
        return initMethod;
    }

    public void setInitMethod(String initMethod) {
        this.initMethod = initMethod;
    }

    public static class Property {

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }

        public String getRef() {
            return ref;
        }

        public void setRef(String ref) {
            this.ref = ref;
        }

        private String name;
        private String value;
        private String ref;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Property property = (Property) o;
            return name.equals(property.name);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name);
        }
    }

}
