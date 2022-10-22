package cn.throwx.octopus.server.infra.config;

import cn.throwx.octopus.server.infra.support.keygen.DbBatchApplyGenerator;
import cn.throwx.octopus.server.infra.support.keygen.GeneratorEnums;
import cn.throwx.octopus.server.infra.support.keygen.SequenceGenerator;
import cn.throwx.octopus.server.infra.support.keygen.SnowflakeSequenceGenerator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

/**
 * @author thorwable
 * @description
 * @since 2020/6/15 17:15
 */
@Configuration
public class SequenceGeneratorAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public SequenceGenerator snowflakeSequenceGenerator(@Value("${compress.code.generate}") String generatorEnums,
                                                        @Value("${snowflake.worker.id}") Long workerId,
                                                        @Value("${snowflake.data.center.id}") Long dataCenterId) {
        SequenceGenerator sequenceGenerator;
        if (GeneratorEnums.SnowFlake.equals(generatorEnums)) {
            SnowflakeSequenceGenerator snowflakeSequenceGenerator = new SnowflakeSequenceGenerator(workerId, dataCenterId);
            snowflakeSequenceGenerator.init();
            sequenceGenerator = snowflakeSequenceGenerator;
        } else {
            sequenceGenerator = new DbBatchApplyGenerator();
        }
        return sequenceGenerator;
    }

}
