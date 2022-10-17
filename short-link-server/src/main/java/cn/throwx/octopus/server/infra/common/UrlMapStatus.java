package cn.throwx.octopus.server.infra.common;


/**
 * @author thorwable
 * @description 短链映射状态
 * @since 2020/6/15 20:21
 */

public enum UrlMapStatus {

    /**
     * 可用状态
     */
    AVAILABLE(1),

    /**
     * 非法状态
     */
    INVALID(2),

    ;

    private final Integer value;

    UrlMapStatus(Integer value) {
        this.value = value;
    }

    public Integer getValue() {
        return value;
    }
}
