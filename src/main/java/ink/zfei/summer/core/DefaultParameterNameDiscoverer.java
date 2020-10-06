package ink.zfei.summer.core;

public class DefaultParameterNameDiscoverer extends PrioritizedParameterNameDiscoverer {

    public DefaultParameterNameDiscoverer() {
        addDiscoverer(new StandardReflectionParameterNameDiscoverer());
        //todo  用asm的api解析beanMetadata
//        addDiscoverer(new LocalVariableTableParameterNameDiscoverer());
    }

}
