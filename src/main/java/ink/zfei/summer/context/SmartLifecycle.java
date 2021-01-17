package ink.zfei.summer.context;

/**
 * 在LifeCycle的基础上增加了对容器上下文的刷新感知能力。
 * 同时继承了Phase接口（排序、此处忽略）提供了Bean 创建和销毁一定的干预能力
 */
public interface SmartLifecycle extends Lifecycle{

    default boolean isAutoStartup() {
        return true;
    }

    default void stop(Runnable callback) {
        stop();
        callback.run();
    }
}
