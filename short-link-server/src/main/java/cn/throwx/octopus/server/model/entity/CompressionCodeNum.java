package cn.throwx.octopus.server.model.entity;

import java.time.OffsetDateTime;

public class CompressionCodeNum {
    private Long id;

    private Long currentNum;

    private Long endNum;

    private Long workId;

    private Integer versionId;

    private OffsetDateTime updateTime;

    private OffsetDateTime createTime;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getCurrentNum() {
        return currentNum;
    }

    public void setCurrentNum(Long currentNum) {
        this.currentNum = currentNum;
    }

    public Long getEndNum() {
        return endNum;
    }

    public void setEndNum(Long endNum) {
        this.endNum = endNum;
    }

    public Long getWorkId() {
        return workId;
    }

    public void setWorkId(Long workId) {
        this.workId = workId;
    }

    public Integer getVersionId() {
        return versionId;
    }

    public void setVersionId(Integer versionId) {
        this.versionId = versionId;
    }

    public OffsetDateTime getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(OffsetDateTime updateTime) {
        this.updateTime = updateTime;
    }

    public OffsetDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(OffsetDateTime createTime) {
        this.createTime = createTime;
    }
}