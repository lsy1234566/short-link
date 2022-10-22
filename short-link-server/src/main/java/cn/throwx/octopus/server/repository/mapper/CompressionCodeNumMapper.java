package cn.throwx.octopus.server.repository.mapper;

import cn.throwx.octopus.server.model.entity.CompressionCodeNum;
import cn.throwx.octopus.server.model.entity.CompressionCodeNumExample;

import java.util.List;

import org.apache.ibatis.annotations.Param;

public interface CompressionCodeNumMapper {
    int deleteByPrimaryKey(Long id);

    int insert(CompressionCodeNum record);

    int insertSelective(CompressionCodeNum record);

    List<CompressionCodeNum> selectByExample(CompressionCodeNumExample example);

    CompressionCodeNum selectByPrimaryKey(Long id);

    int updateByExampleSelective(@Param("record") CompressionCodeNum record, @Param("example") CompressionCodeNumExample example);

    int updateByExample(@Param("record") CompressionCodeNum record, @Param("example") CompressionCodeNumExample example);

    int updateByPrimaryKeySelective(CompressionCodeNum record);

    int updateByPrimaryKey(CompressionCodeNum record);

    int initNum(@Param("workId") Long workId, @Param("currentNum") Long currentNum,
                @Param("endNum") Long endNum, @Param("versionId") Integer versionId);

    int casApplyNum(@Param("startNum") Long startNum,
                    @Param("endNum") Long endNum,
                    @Param("nextVersionId") Integer nextVersionId,
                    @Param("workId") Long workId,
                    @Param("versionId") Integer versionId);
}