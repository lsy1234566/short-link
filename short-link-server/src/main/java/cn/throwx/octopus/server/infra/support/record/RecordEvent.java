package cn.throwx.octopus.server.infra.support.record;

import cn.throwx.octopus.server.application.consumer.request.TransformEvent;

/**
 * 记录转换
 */
public interface RecordEvent {

    void transformEvent(TransformEvent transformEvent);
}
