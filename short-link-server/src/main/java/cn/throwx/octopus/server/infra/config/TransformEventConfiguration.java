package cn.throwx.octopus.server.infra.config;


import cn.throwx.octopus.server.infra.support.record.AsynRecordEvent;
import cn.throwx.octopus.server.infra.support.record.DirectRecordEvent;
import cn.throwx.octopus.server.infra.support.record.RabbitRecordEvent;
import cn.throwx.octopus.server.infra.support.record.RecordEvent;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TransformEventConfiguration {

    /**
     * 初始化记录记录
     *
     * @return 记录对象
     */
    @Bean
    @ConditionalOnProperty(prefix = "octopus.record", value = "type", havingValue = "asyndb")
    @ConditionalOnMissingBean(RecordEvent.class)
    public RecordEvent recordEventByAsynDb() {
        return new AsynRecordEvent();
    }

    @Bean
    @ConditionalOnProperty(prefix = "octopus.record", value = "type", havingValue = "db")
    public RecordEvent recordEventByDb() {
        return new DirectRecordEvent();
    }

    @Bean
    @ConditionalOnProperty(prefix = "octopus.record", value = "type", havingValue = "mq")
    public RecordEvent recordEventByMq() {
        return new RabbitRecordEvent();
    }

}
