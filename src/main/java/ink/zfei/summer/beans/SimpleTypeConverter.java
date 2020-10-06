package ink.zfei.summer.beans;

public class SimpleTypeConverter extends TypeConverterSupport {

    public SimpleTypeConverter() {
        this.typeConverterDelegate = new TypeConverterDelegate(this);
        registerDefaultEditors();
    }

}
