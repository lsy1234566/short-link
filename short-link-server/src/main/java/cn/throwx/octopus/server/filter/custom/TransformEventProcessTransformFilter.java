package cn.throwx.octopus.server.filter.custom;

import cn.throwx.octopus.server.application.consumer.request.TransformEvent;
import cn.throwx.octopus.server.filter.TransformContext;
import cn.throwx.octopus.server.filter.TransformFilter;
import cn.throwx.octopus.server.filter.TransformFilterChain;
import cn.throwx.octopus.server.infra.common.RabbitQueue;
import cn.throwx.octopus.server.infra.support.record.RecordEvent;
import cn.throwx.octopus.server.infra.util.JacksonUtils;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * @author throwable
 * @description 转换事件处理过滤器
 * @since 2020/7/21 19:19
 */
@Slf4j
@Scope(scopeName = ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Component
public class TransformEventProcessTransformFilter implements TransformFilter {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private RecordEvent recordEvent;

    @Override
    public int order() {
        return 4;
    }

    @Override
    public void init(TransformContext context) {

    }

    @Override
    public void doFilter(TransformFilterChain chain,
                         TransformContext context) {
        TransformEvent event = new TransformEvent();
        event.setClientIp(context.getParam(TransformContext.PARAM_CLIENT_ID_KEY));
        event.setTimestamp(System.currentTimeMillis());
        event.setCompressionCode(context.getCompressionCode());
        event.setUserAgent(context.getParam(TransformContext.PARAM_UA_KEY));
        event.setCookieValue(context.getParam(TransformContext.PARAM_COOKIE_KEY));
        event.setShortUrlString(context.getParam(TransformContext.PARAM_SHORT_URL_KEY));
        event.setLongUrlString(context.getParam(TransformContext.PARAM_TARGET_LONG_URL_KEY));
        event.setTransformStatusValue(context.getTransformStatusValue());
        recordEvent.transformEvent(event);
        chain.doFilter(context);
    }

}
