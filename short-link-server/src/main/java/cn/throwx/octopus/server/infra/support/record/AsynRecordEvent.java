package cn.throwx.octopus.server.infra.support.record;

import cn.throwx.octopus.server.application.consumer.request.TransformEvent;
import cn.throwx.octopus.server.infra.common.TimeZoneConstant;
import cn.throwx.octopus.server.infra.support.spring.SpringShutdownEventListening;
import cn.throwx.octopus.server.infra.util.BeanCopierUtils;
import cn.throwx.octopus.server.model.entity.TransformEventRecord;
import cn.throwx.octopus.server.service.TransformEventService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.SmartLifecycle;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
public class AsynRecordEvent implements RecordEvent, SmartLifecycle {

    @Autowired
    private TransformEventService transformEventService;

    @Autowired
    private SpringShutdownEventListening springShutdownEventListening;

    private final LinkedBlockingQueue<TransformEvent> linkedBlockingQueue = new LinkedBlockingQueue(Integer.MAX_VALUE);

    @Override
    public void start() {
        ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(1);
        //容器关闭监听
        springShutdownEventListening.setListening(execTask);
        //每0.2秒处理一次批任务
        scheduledExecutorService.scheduleWithFixedDelay(execTask, 200, 200, TimeUnit.MILLISECONDS);
    }


    @Override
    public void transformEvent(TransformEvent transformEvent) {
        if (!linkedBlockingQueue.offer(transformEvent)) {
            log.error("记录请求失败:", transformEvent);
        }
    }

    /**
     * 存储任务
     */
    private Runnable execTask = () -> {
        //每0.2秒处理一次
        List<TransformEventRecord> transformEvents = new LinkedList<>();
        while (true) {
            TransformEvent event = linkedBlockingQueue.poll();
            if (event != null) {
                TransformEventRecord record = new TransformEventRecord();
                BeanCopierUtils.X.copy(event, record);
                record.setRecordTime(OffsetDateTime.ofInstant(Instant.ofEpochMilli(event.getTimestamp()), TimeZoneConstant.CHINA.getZoneId()));
                record.setTransformStatus(event.getTransformStatusValue());
                record.setShortUrl(event.getShortUrlString());
                record.setLongUrl(event.getLongUrlString());
                transformEvents.add(record);
            }else {
                break;
            }
        }
        if (CollectionUtils.isEmpty(transformEvents)) {
            return;
        }
        transformEventService.recordTransformEvent(transformEvents);
    };

    @Override
    public void stop() {
    }

    @Override
    public boolean isRunning() {
        return false;
    }
}
