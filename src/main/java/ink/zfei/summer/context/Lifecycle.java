package ink.zfei.summer.context;

/**
 * 定义Spring容器对象的生命周期，任何spring管理对象都可以实现该接口。
 * 当ApplicationContext本身接收启动和停止信号(例如在运行时停止/重启场景)时，
 * spring容器将在容器上下文中找出所有实现了LifeCycle及其子类接口的类，并一一调用它们实现的类。
 * spring是通过委托给生命周期处理器LifecycleProcessor来实现这一点的。
 *
 * 常规的Lifecycle接口只是在容器上下文【显式】的调用start()/stop()方法时，
 * 才会去回调Lifecycle的实现类的start stop方法逻辑。并不意味着在上下文刷新时自动启动。
 */
public interface Lifecycle {

    void start();

    void stop();

    boolean isRunning();
}
