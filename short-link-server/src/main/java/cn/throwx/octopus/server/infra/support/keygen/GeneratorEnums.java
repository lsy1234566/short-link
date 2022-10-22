package cn.throwx.octopus.server.infra.support.keygen;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * snowflake-雪花 dbBatchApply-
 */
@Getter
@AllArgsConstructor
public enum GeneratorEnums {
    SnowFlake("snowflake", "雪花"),
    BatchDbApply("dbBatchApply", "数据库算法");

    private String type;
    private String description;
}
