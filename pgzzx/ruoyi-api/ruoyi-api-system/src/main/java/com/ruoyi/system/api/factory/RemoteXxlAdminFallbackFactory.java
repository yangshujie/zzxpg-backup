package com.ruoyi.system.api.factory;

import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.system.api.RemoteXxlAdminService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

@Component
public class RemoteXxlAdminFallbackFactory implements FallbackFactory<RemoteXxlAdminService>{


    private static final Logger log = LoggerFactory.getLogger(RemoteXxlAdminFallbackFactory.class);

    @Override
    public RemoteXxlAdminService create(Throwable throwable)
    {
        log.error("查询表执行失败:{}", throwable.getMessage());
        return new RemoteXxlAdminService()
        {
            @Override
            public AjaxResult getRoundtaskNum()
            {
                return AjaxResult.error("查询表执行失败:" + throwable.getMessage());
            }

            @Override
            public AjaxResult getNextTriggerTime() {
                return null;
            }


        };
    }

}
