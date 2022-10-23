package cn.throwx.octopus.server.infra.support.record;

import cn.throwx.octopus.server.application.consumer.request.TransformEvent;
import cn.throwx.octopus.server.infra.common.RabbitQueue;
import cn.throwx.octopus.server.infra.util.JacksonUtils;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;

public class RabbitRecordEvent implements RecordEvent {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Override
    public void transformEvent(TransformEvent event) {
        rabbitTemplate.convertAndSend(
                RabbitQueue.TRANSFORM_EVENT_QUEUE.getExchangeName(),
                RabbitQueue.TRANSFORM_EVENT_QUEUE.getRoutingKey(),
                JacksonUtils.X.format(event)
        );
    }

}
