package cn.throwx.octopus.server.infra.support.record;

import cn.throwx.octopus.server.application.consumer.request.TransformEvent;
import cn.throwx.octopus.server.infra.common.TimeZoneConstant;
import cn.throwx.octopus.server.infra.util.BeanCopierUtils;
import cn.throwx.octopus.server.infra.util.JacksonUtils;
import cn.throwx.octopus.server.model.entity.TransformEventRecord;
import cn.throwx.octopus.server.service.TransformEventService;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.time.OffsetDateTime;

public class DirectRecordEvent implements RecordEvent {

    @Autowired
    private TransformEventService transformEventService;

    @Override
    public void transformEvent(TransformEvent event) {
        TransformEventRecord record = new TransformEventRecord();
        BeanCopierUtils.X.copy(event, record);
        record.setRecordTime(OffsetDateTime.ofInstant(Instant.ofEpochMilli(event.getTimestamp()), TimeZoneConstant.CHINA.getZoneId()));
        record.setTransformStatus(event.getTransformStatusValue());
        record.setShortUrl(event.getShortUrlString());
        record.setLongUrl(event.getLongUrlString());
        transformEventService.recordTransformEvent(record);
    }

}
