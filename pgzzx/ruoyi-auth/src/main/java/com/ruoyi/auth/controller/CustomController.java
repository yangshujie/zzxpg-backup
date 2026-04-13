package com.ruoyi.auth.controller;

import com.ruoyi.common.core.web.domain.AjaxResult;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;

@RequestMapping("/custom")
@RestController
public class CustomController {

    @Value("${custom.nacosLogPath}")
    private String nacosLogPath;

    @GetMapping("/getNacosLogSize")
    @ApiOperation("获取nacos日志文件夹大小")
    public AjaxResult getNacosLogSize(){
        try {
            File dir = new File(nacosLogPath);
            return AjaxResult.success(Math.floor((FileUtils.sizeOf(dir) / (1024 * 1024 * 1024d))*100)/100d);
        }catch (Exception e){
            return AjaxResult.error("查询失败", e.getMessage());
        }
    }
}
