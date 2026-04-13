package com.ruoyi.zhpgcalc.controller;

import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.zhpgcalc.dto.CalcExecuteRequest;
import com.ruoyi.zhpgcalc.dto.CalcExecuteResponse;
import com.ruoyi.zhpgcalc.service.CalcExecutorService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * 综合计算执行器REST接口
 * <p>
 * 供ZHPG模块通过HTTP调用执行综合计算
 */
@Api(tags = "综合计算执行器")
@Slf4j
@RestController
@RequestMapping("/zhpg-calc")
public class ZhpgCalcController {

    @Resource
    private CalcExecutorService calcExecutorService;

    /**
     * 执行综合计算
     */
    @ApiOperation("执行综合计算")
    @PostMapping("/execute")
    public AjaxResult execute(@RequestBody CalcExecuteRequest request) {
        log.info("收到计算请求, taskId: {}, taskName: {}", request.getTaskId(), request.getTaskName());

        try {
            // 与 XXL-Job @XxlJob("zhpgCalcHandler") 共用 CalcExecutorService，便于评估服务同步调用与调度器异步触发同源
            CalcExecuteResponse response = calcExecutorService.execute(request);
            return AjaxResult.success(response);
        } catch (Exception e) {
            log.error("计算执行失败, taskId: {}", request.getTaskId(), e);
            return AjaxResult.error("计算执行失败: " + e.getMessage());
        }
    }
}
