package ink.zfei.summer.demo;

import ink.zfei.summer.core.ApplicationEvent;
import ink.zfei.summer.core.ApplicationListener;

public class StartWebServerListener implements ApplicationListener {
    @Override
    public void onApplicationEvent(ApplicationEvent event) {
        System.out.println("summer容器启动完，该启动web容器了 。。。。");
    }
}
