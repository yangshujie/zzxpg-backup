package com.ruoyi.controller.zhpg;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.service.zhpg.ICalcTaskService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * XXL-JOB 执行结果回调接口
 * 由 XXL-JOB Admin 或执行器回调通知任务执行结果
 */
@Slf4j
@RestController
@RequestMapping("/zhpg/calc/callback")
public class CalcTaskCallbackController extends BaseController {

    private final ICalcTaskService calcTaskService;

    public CalcTaskCallbackController(ICalcTaskService calcTaskService) {
        this.calcTaskService = calcTaskService;
    }

    /**
     * XXL-JOB 执行成功回调（新版，支持reportUrl）
     * @param taskId 任务ID
     * @param callbackData 回调数据，包含resultJson和reportUrl等
     */
    @PostMapping("/success/{taskId}")
    public AjaxResult onSuccess(@PathVariable Long taskId, @RequestBody JSONObject callbackData) {
        log.info("XXL-JOB回调: 任务执行成功, taskId={}, callbackData={}", taskId, callbackData);

        // 解析回调数据
        String resultJson = callbackData.getString("resultJson");
        String reportUrl = callbackData.getString("reportUrl");
        String wordUrl = callbackData.getString("wordUrl");
        String wpsUrl = callbackData.getString("wpsUrl");

        // 如果resultJson为空，直接使用整个callbackData作为结果
        if (resultJson == null || resultJson.isEmpty()) {
            resultJson = callbackData.toJSONString();
        }

        calcTaskService.handleXxlJobCallbackWithReport(taskId, true, resultJson, reportUrl, wordUrl, wpsUrl);
        return AjaxResult.success();
    }

    /**
     * XXL-JOB 执行失败回调
     */
    @PostMapping("/fail/{taskId}")
    public AjaxResult onFail(@PathVariable Long taskId, @RequestBody String errorMessage) {
        log.info("XXL-JOB回调: 任务执行失败, taskId={}, error={}", taskId, errorMessage);
        calcTaskService.handleXxlJobCallback(taskId, false, errorMessage);
        return AjaxResult.success();
    }

    /**
     * XXL-JOB 统一回调接口（内部调用）
     * 由XXL-JOB Admin通过API调用
     */
    @PostMapping("/xxljob")
    public AjaxResult xxlJobCallback(@RequestBody JSONObject callbackParam) {
        Long taskId = callbackParam.getLong("taskId");
        Integer handleCode = callbackParam.getInteger("handleCode");
        String handleMsg = callbackParam.getString("handleMsg");

        log.info("XXL-JOB回调: taskId={}, handleCode={}, handleMsg={}", taskId, handleCode, handleMsg);

        if (taskId == null) {
            return AjaxResult.error("taskId is required");
        }

        boolean success = Integer.valueOf(200).equals(handleCode);
        if (success) {
            // 尝试从通用回调体中提取报告URL，保持与 /success/{taskId} 端点一致
            String reportUrl = callbackParam.getString("reportUrl");
            String wordUrl   = callbackParam.getString("wordUrl");
            String wpsUrl    = callbackParam.getString("wpsUrl");
            calcTaskService.handleXxlJobCallbackWithReport(taskId, true, handleMsg, reportUrl, wordUrl, wpsUrl);
        } else {
            calcTaskService.handleXxlJobCallback(taskId, false, handleMsg);
        }
        return AjaxResult.success();
    }

    /**
     * 更新计算进度和日志
     */
    @PostMapping("/progress/{taskId}")
    public AjaxResult onProgress(@PathVariable Long taskId, @RequestBody JSONObject progressData) {
        int progress = progressData.getIntValue("progress");
        String message = progressData.getString("message");
        calcTaskService.updateTaskProgress(taskId, progress, message);
        return AjaxResult.success();
    }
}
