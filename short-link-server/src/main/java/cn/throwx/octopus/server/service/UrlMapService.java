package cn.throwx.octopus.server.service;

import cn.throwx.octopus.server.cache.UrlMapCacheManager;
import cn.throwx.octopus.server.filter.TransformContext;
import cn.throwx.octopus.server.filter.TransformFilterChain;
import cn.throwx.octopus.server.filter.TransformFilterChainFactory;
import cn.throwx.octopus.server.infra.common.CommonConstant;
import cn.throwx.octopus.server.infra.common.CompressionCodeStatus;
import cn.throwx.octopus.server.infra.common.LockKey;
import cn.throwx.octopus.server.infra.common.UrlMapStatus;
import cn.throwx.octopus.server.infra.support.keygen.SequenceGenerator;
import cn.throwx.octopus.server.infra.support.keygen.SnowflakeSequenceGenerator;
import cn.throwx.octopus.server.infra.support.lock.DistributedLock;
import cn.throwx.octopus.server.infra.support.lock.DistributedLockFactory;
import cn.throwx.octopus.server.infra.util.ConversionUtils;
import cn.throwx.octopus.server.model.entity.CompressionCode;
import cn.throwx.octopus.server.model.entity.DomainConf;
import cn.throwx.octopus.server.model.entity.UrlMap;
import cn.throwx.octopus.server.repository.CompressionCodeDao;
import cn.throwx.octopus.server.repository.DomainConfDao;
import cn.throwx.octopus.server.repository.UrlMapDao;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.validator.routines.UrlValidator;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * @author throwable
 * @version v1
 * @description
 * @since 2020/12/27 16:06
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UrlMapService implements BeanFactoryAware {

    private final TransformFilterChainFactory transformFilterChainFactory;
    private final UrlMapCacheManager urlMapCacheManager;
    private final DistributedLockFactory distributedLockFactory;
    private final SequenceGenerator sequenceGenerator;
    private final DomainConfDao domainConfDao;
    private final UrlMapDao urlMapDao;
    private final CompressionCodeDao compressionCodeDao;

    private final UrlValidator urlValidator = new UrlValidator(new String[]{CommonConstant.HTTP_PROTOCOL,
            CommonConstant.HTTPS_PROTOCOL});

    @Value("${compress.code.batch:100}")
    private Integer compressCodeBatch;

    @Value("${compress.code.expect.length:6}")
    private Integer compressCodeExpectLength;

    private UrlMapService self;

    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        self = beanFactory.getBean(UrlMapService.class);
    }

    /**
     * ??????URL??????
     *
     * @param transformContext transformContext
     */
    public void processTransform(TransformContext transformContext) {
        // ????????????????????????
        long start = System.nanoTime();
        if (log.isDebugEnabled()) {
            log.debug("Entry TransformFilterChain...");
        }
        // ??????????????????
        TransformFilterChain chain = transformFilterChainFactory.buildTransformFilterChain(transformContext);
        try {
            chain.doFilter(transformContext);
        } finally {
            chain.release();
            transformContext.release();
            if (log.isDebugEnabled()) {
                log.debug("Exit TransformFilterChain,cost {} ms...", TimeUnit.NANOSECONDS.toMillis((System.nanoTime() - start)));
            }
        }
    }

    /**
     * ??????????????????
     *
     * @param domain       domain
     * @param insertEntity insertEntity
     * @return String String
     */
    public String createUrlMap(String domain, UrlMap insertEntity) {
        DistributedLock lock = distributedLockFactory.provideDistributedLock(LockKey.CREATE_URL_MAP.getCode());
        try {
            lock.lock(LockKey.CREATE_URL_MAP.getReleaseTime(), TimeUnit.MILLISECONDS);
            CompressionCode compressionCode = getAvailableCompressCode(1L);
            Assert.isTrue(Objects.nonNull(compressionCode) &&
                    CompressionCodeStatus.AVAILABLE.getValue().equals(compressionCode.getCodeStatus()), "???????????????????????????????????????");
            String longUrl = insertEntity.getLongUrl();
            Assert.isTrue(urlValidator.isValid(longUrl), String.format("??????[%s]??????", longUrl));
            DomainConf domainConf = domainConfDao.selectByDomain(domain);
            Assert.notNull(domainConf, String.format("???????????????[c:%s]", domain));
            UrlMap urlMap = new UrlMap();
            urlMap.setLongUrl(insertEntity.getLongUrl());
            String code = compressionCode.getCompressionCode();
            String shortUrl = String.format("%s://%s/%s", domainConf.getProtocol(), domainConf.getDomainValue(), code);
            urlMap.setShortUrl(shortUrl);
            urlMap.setCompressionCode(code);
            urlMap.setUrlStatus(insertEntity.getUrlStatus());
            urlMap.setDescription(insertEntity.getDescription());
            // ??????????????????
            urlMap.setShortUrlDigest(DigestUtils.sha1Hex(urlMap.getShortUrl()));
            urlMap.setLongUrlDigest(DigestUtils.sha1Hex(urlMap.getLongUrl()));
            CompressionCode updater = new CompressionCode();
            updater.setCodeStatus(CompressionCodeStatus.USED.getValue());
            updater.setId(compressionCode.getId());
            // ??????
            self.saveUrlMapAndUpdateCompressCode(urlMap, updater);
            // ????????????
            urlMapCacheManager.refreshUrlMapCache(urlMap);
            return shortUrl;
        } finally {
            lock.unlock();
        }
    }

    /**
     * ??????????????????
     *
     * @param updater updater
     * @return Long
     */
    public Long editUrlMap(UrlMap updater) {
        DistributedLock lock = distributedLockFactory.provideDistributedLock(LockKey.EDIT_URL_MAP.getCode());
        try {
            lock.lock(LockKey.EDIT_URL_MAP.getReleaseTime(), TimeUnit.MILLISECONDS);
            UrlMap urlMap = urlMapDao.selectByPrimaryKey(updater.getId());
            Assert.notNull(urlMap, String.format("?????????????????????[u:%s]", updater.getId()));
            if (StringUtils.isNotBlank(updater.getDescription())) {
                urlMap.setDescription(updater.getDescription());
            }
            if (StringUtils.isNotBlank(updater.getLongUrl())) {
                urlMap.setLongUrl(updater.getLongUrl());
                urlMap.setLongUrlDigest(DigestUtils.sha1Hex(urlMap.getLongUrl()));
            }
            if (null != updater.getUrlStatus()) {
                urlMap.setUrlStatus(updater.getUrlStatus());
            }
            urlMapDao.updateByPrimaryKeySelective(urlMap);
            urlMapCacheManager.refreshUrlMapCache(urlMap);
            return updater.getId();
        } finally {
            lock.unlock();
        }
    }

    /**
     * ??????????????????????????????????????????
     *
     * @param urlMap          urlMap
     * @param compressionCode compressionCode
     */
    @Transactional(rollbackFor = Exception.class)
    public void saveUrlMapAndUpdateCompressCode(UrlMap urlMap, CompressionCode compressionCode) {
        compressionCodeDao.updateByPrimaryKeySelective(compressionCode);
        urlMapDao.insertSelective(urlMap);
    }

    /**
     * ??????????????????????????????
     *
     * @return CompressCode
     */
    private CompressionCode getAvailableCompressCode(Long workId) {
        CompressionCode compressionCode = compressionCodeDao.getLatestAvailableCompressionCode();
        if (Objects.nonNull(compressionCode)) {
            return compressionCode;
        } else {
            generateBatchCompressionCodes(workId);
            return Objects.requireNonNull(compressionCodeDao.getLatestAvailableCompressionCode());
        }
    }

    /**
     * ?????????????????????
     */
    public void generateBatchCompressionCodes(Long workId) {
        for (int i = 0; i < compressCodeBatch; i++) {
            long sequence = sequenceGenerator.generate(workId);
            CompressionCode compressionCode = new CompressionCode();
            compressionCode.setSequenceValue(String.valueOf(sequence));
            //????????????,?????????????????????????????????
            String code = ConversionUtils.X.encode62(sequence);
            if (sequenceGenerator  instanceof SnowflakeSequenceGenerator){
                code = code.substring(code.length() - compressCodeExpectLength);
            }
            compressionCode.setCompressionCode(code);
            try {
                compressionCodeDao.insertSelective(compressionCode);
            } catch (Exception e) {
                log.error("?????????????????????????????????:?????????????????????????????????,?????????", e);
            }
        }
    }

    /**
     * ??????????????????
     *
     * @param id id
     */
    public void deleteUrlMap(Long id) {
        UrlMap urlMap = urlMapDao.selectByPrimaryKey(id);
        UrlMap updater = new UrlMap();
        updater.setDeleted(1);
        updater.setId(urlMap.getId());
        urlMapDao.updateByPrimaryKeySelective(updater);
        urlMapCacheManager.refreshUrlMapCache(urlMap);
    }

    /**
     * ??????????????????
     *
     * @param id id
     */
    public void disableUrlMap(Long id) {
        UrlMap urlMap = urlMapDao.selectByPrimaryKey(id);
        UrlMap updater = new UrlMap();
        updater.setUrlStatus(UrlMapStatus.INVALID.getValue());
        updater.setId(urlMap.getId());
        urlMapDao.updateByPrimaryKeySelective(updater);
        urlMapCacheManager.refreshUrlMapCache(urlMap);
    }

    /**
     * ??????????????????
     *
     * @param id id
     */
    public void enableUrlMap(Long id) {
        UrlMap urlMap = urlMapDao.selectByPrimaryKey(id);
        urlMap.setUrlStatus(UrlMapStatus.AVAILABLE.getValue());
        UrlMap updater = new UrlMap();
        updater.setUrlStatus(urlMap.getUrlStatus());
        updater.setId(urlMap.getId());
        urlMapDao.updateByPrimaryKeySelective(updater);
        urlMapCacheManager.refreshUrlMapCache(urlMap);
    }
}
