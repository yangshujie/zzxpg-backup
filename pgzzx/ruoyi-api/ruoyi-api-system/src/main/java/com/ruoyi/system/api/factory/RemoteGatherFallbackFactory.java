package com.ruoyi.system.api.factory;

import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.system.api.RemoteGatherService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.IOException;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.TimeoutException;

@Component
public class RemoteGatherFallbackFactory implements FallbackFactory<RemoteGatherService> {
    private static final Logger log = LoggerFactory.getLogger(RemoteGatherFallbackFactory.class);

    @Override
    public RemoteGatherService create(Throwable throwable)
    {
        log.error("采集任务执行失败:{}", throwable.getMessage());
        return new RemoteGatherService()
        {
            @Override
            public AjaxResult exec(@PathVariable("id") int id, @RequestParam("type")int type) throws URISyntaxException, IOException, TimeoutException, NoSuchAlgorithmException, KeyManagementException
            {
                return AjaxResult.error("采集任务执行失败:" + throwable.getMessage());
            }

            @Override
            public AjaxResult test_gather_print(int id) {
                return AjaxResult.error("测试采集打印任务失败:" + throwable.getMessage());
            }

        };
    }
}
