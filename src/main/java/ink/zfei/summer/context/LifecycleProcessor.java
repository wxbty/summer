package ink.zfei.summer.context;

/**
 * 负责管理ApplicationContext生命周期
 */
public interface LifecycleProcessor extends Lifecycle{

    //容器刷新时触发
    void onRefresh();

    /**
     * Notification of context close phase, e.g. for auto-stopping components.
     */
    void onClose();
}
