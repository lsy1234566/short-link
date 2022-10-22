package cn.throwx.octopus.server.service;

import cn.throwx.octopus.server.OctopusServerApplication;
import cn.throwx.octopus.server.infra.common.UrlMapStatus;
import cn.throwx.octopus.server.infra.support.keygen.DbBatchApplyGenerator;
import cn.throwx.octopus.server.model.entity.UrlMap;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * @author throwable
 * @version v1
 * @description
 * @since 2020/12/27 17:55
 */
@Slf4j
@SpringBootTest(classes = OctopusServerApplication.class, properties = "spring.profiles.active=local")
@RunWith(SpringRunner.class)
public class UrlMapServiceTest {

    @Autowired
    private UrlMapService urlMapService;

    @Test
    public void createUrlMap() {
        String domain = "localhost:9099";
        UrlMap urlMap = new UrlMap();
        urlMap.setUrlStatus(UrlMapStatus.AVAILABLE.getValue());
        urlMap.setLongUrl("https://throwx.cn/2020/08/24/canal-ha-cluster-guide");
        urlMap.setDescription("测试短链");
        String url = urlMapService.createUrlMap(domain, urlMap);
        log.info("生成的短链:{}", url);
        urlMapService.generateBatchCompressionCodes(1L);
    }


   Map<Long, String> idan= new ConcurrentHashMap<>();

    @Autowired
    private DbBatchApplyGenerator dbBatchApplyGenerator;

    @Test
    public void batch() throws InterruptedException {
        new  Thread(()->{
            for (int i = 0; i < 100000; i++) {
                Long generate = dbBatchApplyGenerator.generate(1L);
                if (idan.get(generate)!=null) {
                    System.out.println("重复");
                }else{
                    idan.put(generate, "1");
                }
            }
        }).start();
        new  Thread(()->{
            for (int i = 0; i < 100000; i++) {
                Long generate = dbBatchApplyGenerator.generate(1L);
                if (idan.get(generate)!=null) {
                    System.out.println("重复");
                }else{
                    idan.put(generate, "1");
                }
            }
        }).start();
        new  Thread(()->{
            for (int i = 0; i < 100000; i++) {
                Long generate = dbBatchApplyGenerator.generate(1L);
                if (idan.get(generate)!=null) {
                    System.out.println("重复");
                }else{
                    idan.put(generate, "1");
                }
            }
        }).start();
        new  Thread(()->{
            for (int i = 0; i < 100000; i++) {
                Long generate = dbBatchApplyGenerator.generate(1L);
                if (idan.get(generate)!=null) {
                    System.out.println("重复");
                }else{
                    idan.put(generate, "1");
                }
            }
        }).start();

        TimeUnit.SECONDS.sleep(10000);
    }
}