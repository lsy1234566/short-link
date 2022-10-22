package cn.throwx.octopus.server.infra.support.keygen;

import cn.throwx.octopus.server.infra.util.ConversionUtils;
import cn.throwx.octopus.server.model.entity.CompressionCodeNum;
import cn.throwx.octopus.server.model.entity.CompressionCodeNumExample;
import cn.throwx.octopus.server.repository.mapper.CompressionCodeNumMapper;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 基于数据库批量申请一批id
 */
@Scope("prototype")
@Slf4j
public class DbBatchApplyGenerator implements SequenceGenerator {

    @Value("${compress.code.batch:1000}")
    private Integer compressCodeBatch;

    @Value("${compress.code.expect.length:6}")
    private Integer compressCodeExpectLength;

    private final Map<Long, BatchNum> longBatchNumConcurrentHashMap = new HashMap<>();

    @Autowired
    private CompressionCodeNumMapper compressionCodeNumMapper;

    /**
     * 这里加了同步标识,其他操作靠数据库锁
     *
     * @param workId 业务标识
     * @return 编号
     */
    @Override
    public synchronized long generate(Long workId) {
        BatchNum batchNum = longBatchNumConcurrentHashMap.computeIfAbsent(workId, (newWorkId) -> {
            BatchNum newBatchNum = new BatchNum(newWorkId);
            applyBatchNum(newBatchNum);
            return newBatchNum;
        });
        long id = batchNum.currentNum.incrementAndGet();
        if (batchNum.getMaxNum().get() == id) {
            applyBatchNum(batchNum);
        }
        return id;
    }

    /**
     * 获取批数据-数据库乐观锁
     *
     * @param batchNum 业务标识
     */
    private void applyBatchNum(BatchNum batchNum) {
        //查询当前到了哪一批
        CompressionCodeNum compressionCodeNum = searchLastNumRecord(batchNum.getWorkId());
        //申请下一批
        applyToRecode(batchNum, compressionCodeNum);
    }


    /**
     * 申请下一批次的编号
     *
     * @param currentCompressionCodeNum 当前记录
     */
    @SneakyThrows
    private void applyToRecode(BatchNum batchNum, @NonNull CompressionCodeNum currentCompressionCodeNum) {
        while (true) {
            Long startNum = currentCompressionCodeNum.getEndNum() + 1;
            Long endNum = startNum + compressCodeBatch;
            Integer versionId = currentCompressionCodeNum.getVersionId();
            Integer nextVersionId = versionId + 1;
            int effectiveNum = compressionCodeNumMapper.casApplyNum(startNum, endNum, nextVersionId, batchNum.getWorkId(), versionId);
            if (effectiveNum >= 1) {
                //减一是因为那边要用increment
                batchNum.getCurrentNum().set(startNum - 1);
                batchNum.getMaxNum().set(endNum);
                break;
            }
            TimeUnit.MILLISECONDS.sleep(200);
            currentCompressionCodeNum = searchLastNumRecord(batchNum.workId);
        }

    }


    /**
     * 查询当前批次数据
     *
     * @param workId 业务标识
     * @return
     */
    private CompressionCodeNum searchLastNumRecord(Long workId) {
        CompressionCodeNumExample compressionCodeNumExample = new CompressionCodeNumExample();
        compressionCodeNumExample.createCriteria().andWorkIdEqualTo(workId);
        List<CompressionCodeNum> compressionCodeNums = compressionCodeNumMapper.selectByExample(compressionCodeNumExample);
        if (CollectionUtils.isEmpty(compressionCodeNums)) {
            log.debug("未发现初始分配记录-即将初始化");
            initBatchNum(workId);
            //重新查询
            compressionCodeNums = compressionCodeNumMapper.selectByExample(compressionCodeNumExample);
            if (CollectionUtils.isEmpty(compressionCodeNums)) {
                log.error("未发现初始分配记录-且初始化失败初始化");
                return null;
            }
        }
        return compressionCodeNums.get(0);
    }


    /**
     * 初始化乐观锁
     *
     * @param workId 业务标识
     */
    @Transactional(rollbackFor = Exception.class)
    void initBatchNum(Long workId) {
        //计算起始位
        Double pow = Math.pow(ConversionUtils.getSCALE(), compressCodeExpectLength - 1);
        Long startNum = pow.longValue();
        //插入数据库
        compressionCodeNumMapper.initNum(workId, startNum, startNum + compressCodeBatch, 1);
    }

    @Getter
    private class BatchNum {

        public BatchNum(Long workId) {
            this.workId = workId;
        }

        /**
         * 最大可达num
         */
        private AtomicLong maxNum = new AtomicLong();

        /**
         * 当前num
         */
        private AtomicLong currentNum = new AtomicLong();

        /**
         * 业务标识
         */
        private Long workId;
    }
}
