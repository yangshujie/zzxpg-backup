package com.ruoyi.system.api;

import com.ruoyi.common.core.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.system.api.factory.RemoteXxlAdminFallbackFactory;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(contextId = "remoteXxlAdminService", value = ServiceNameConstants.Xxladmin_SERVICE, fallbackFactory = RemoteXxlAdminFallbackFactory.class)
//为每个FeignClient手动指定不同的contextId，contextId的作用是用来区分FeignClient实例
//value对应nacos里的服务名
public interface RemoteXxlAdminService {


    @RequestMapping("/jobinfo/getRoundtaskNum")
    public AjaxResult getRoundtaskNum();

    @RequestMapping("/jobinfo/getNextTriggerTime")
    public AjaxResult getNextTriggerTime();

}
