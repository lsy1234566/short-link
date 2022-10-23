package cn.throwx.octopus.server.infra.support.spring;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.stereotype.Component;

import java.util.LinkedList;


/**
 * 添加关闭监听
 */
@Slf4j
@Component
public class SpringShutdownEventListening implements ApplicationListener<ContextClosedEvent> {

    private final LinkedList<Runnable> runnables = new LinkedList<>();

    @Override
    public void onApplicationEvent(ContextClosedEvent event) {
        log.info("监听到Spring关闭");
        for (Runnable runnable : runnables) {
            runnable.run();
        }
        log.info("执行完成Spring关闭任务");
    }

    /**
     * 添加关闭监听
     *
     * @param runnable 执行器
     */
    public void setListening(Runnable runnable) {
        runnables.add(runnable);
    }

}

