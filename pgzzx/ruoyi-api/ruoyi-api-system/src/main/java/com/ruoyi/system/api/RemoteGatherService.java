package com.ruoyi.system.api;


import com.ruoyi.common.core.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.system.api.factory.RemoteGatherFallbackFactory;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.IOException;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.TimeoutException;

@FeignClient(contextId = "remoteGatherService", value = ServiceNameConstants.SYSTEM_SERVICE, fallbackFactory = RemoteGatherFallbackFactory.class)
public interface RemoteGatherService {

    @GetMapping("/gathertemplate/exec/{id}")
    public AjaxResult exec(@PathVariable("id") int id, @RequestParam("type")int type) throws URISyntaxException, IOException, TimeoutException, NoSuchAlgorithmException, KeyManagementException;

    @PostMapping(value = "/gathertemplate/test_gather_print")
    public AjaxResult test_gather_print(int id);
}
