package com.ruoyi.zhpgcalc.job;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.ruoyi.zhpgcalc.dto.CalcExecuteRequest;
import com.ruoyi.zhpgcalc.dto.CalcExecuteResponse;
import com.ruoyi.zhpgcalc.service.CalcExecutorService;
import com.ruoyi.zhpgcalc.service.ReportGenerationService;
import com.ruoyi.zhpgcalc.ZhpgCallbackClient;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * 综合计算流程XXL-Job处理器（异步回调模式）
 *
 * 执行流程：
 * 1. 接收 XXL-JOB 派发任务
 * 2. 解析任务参数（包含完整的计算配置和加权树）
 * 3. 执行综合计算
 * 4. 通过 HTTP 回调通知 ZHPG 执行结果
 * 5. 使用 XxlJobHelper 记录执行日志到 XXL-JOB Admin
 */
@Slf4j
@Service
public class ZhpgCalcJobService {

    @Resource
    private CalcExecutorService calcExecutorService;

    @Resource
    private ZhpgCallbackClient zhpgCallbackClient;

    @Resource
    private ReportGenerationService reportGenerationService;

    /**
     * 综合计算执行Handler
     * <p>
     * 在XXL-Job Admin中配置:
     * - JobHandler: zhpgCalcHandler
     * - 执行参数: CalcExecuteRequest的JSON字符串（由 ZHPG 生成）
     */
    @XxlJob("zhpgCalcHandler")
    public void zhpgCalcHandler() {
        String jobParam = XxlJobHelper.getJobParam();
        log.info("zhpgCalcHandler received params: {}", jobParam);

        // 记录开始日志到 XXL-JOB Admin
        XxlJobHelper.log("开始执行综合计算任务...");

        try {
            if (jobParam == null || jobParam.trim().isEmpty()) {
                XxlJobHelper.log("ERROR: job param is empty");
                XxlJobHelper.handleFail("job param is empty");
                return;
            }

            // 解析请求参数
            CalcExecuteRequest request = JSONObject.parseObject(jobParam, CalcExecuteRequest.class);
            if (request == null || request.getTaskId() == null) {
                XxlJobHelper.log("ERROR: invalid job param, taskId is required");
                XxlJobHelper.handleFail("invalid job param: taskId is required");
                return;
            }

            Long taskId = request.getTaskId();
            XxlJobHelper.log("任务ID: {}, 模板: {}, 指标体系: {}",
                    taskId, request.getTemplateName(), request.getIndicatorSystemName());

            // 执行计算
            XxlJobHelper.log("开始执行综合计算...");
            CalcExecuteResponse response = calcExecutorService.execute(request);

            String resultJson = JSON.toJSONString(response);
            XxlJobHelper.log("计算完成: score={}, grade={}, conclusion={}",
                    response.getScore(), response.getGrade(), response.getConclusion());

            // 生成评估报告
            ReportGenerationService.ReportUrls reportUrls = null;
            try {
                XxlJobHelper.log("开始生成评估报告...");
                reportUrls = reportGenerationService.generateAndUploadReport(taskId, response, jobParam);
                if (reportUrls != null) {
                    XxlJobHelper.log("报告生成完成: pdfUrl={}, wordUrl={}", reportUrls.getPdfUrl(), reportUrls.getWordUrl());
                } else {
                    XxlJobHelper.log("WARN: 未配置报告模板，跳过报告生成");
                }
            } catch (Throwable reportEx) {
                // 使用 Throwable 捕获所有异常和错误（包括 NoSuchMethodError 等）
                log.error("报告生成失败, taskId={}", taskId, reportEx);
                XxlJobHelper.log("ERROR: 报告生成失败: {}", reportEx.getMessage());
                // 报告生成失败会记录错误但不影响主流程
            }

            // 回调通知 ZHPG（如果配置了回调地址）
            if (zhpgCallbackClient.isAvailable()) {
                XxlJobHelper.log("回调 ZHPG 服务...");
                if (reportUrls != null) {
                    zhpgCallbackClient.notifySuccessWithReport(taskId, resultJson,
                            reportUrls.getPdfUrl(), reportUrls.getWordUrl(), reportUrls.getWpsUrl());
                } else {
                    zhpgCallbackClient.notifySuccess(taskId, resultJson);
                }
                XxlJobHelper.log("回调完成");
            } else {
                XxlJobHelper.log("WARN: ZHPG回调地址未配置，结果不会自动同步到业务系统");
            }

            // 记录成功日志到 XXL-JOB Admin，带上核心结果，便于在调度日志页直接查看
            XxlJobHelper.log("任务执行成功完成: taskId={}", taskId);
            XxlJobHelper.handleSuccess(String.format("taskId=%s, score=%s, grade=%s",
                    taskId, response.getScore(), response.getGrade()));

        } catch (Exception e) {
            log.error("zhpgCalcHandler执行失败", e);

            Long taskId = null;
            try {
                CalcExecuteRequest req = JSONObject.parseObject(jobParam, CalcExecuteRequest.class);
                if (req != null) {
                    taskId = req.getTaskId();
                }
            } catch (Exception ignore) {
            }

            String errorMessage = e.getMessage() == null ? "unknown error" : e.getMessage();

            // 回调通知 ZHPG 失败
            if (taskId != null && zhpgCallbackClient.isAvailable()) {
                try {
                    zhpgCallbackClient.notifyFailure(taskId, errorMessage);
                } catch (Exception callbackEx) {
                    log.error("回调失败通知失败: taskId={}", taskId, callbackEx);
                }
            }

            // 记录失败日志到 XXL-JOB Admin
            XxlJobHelper.log("ERROR: 任务执行失败: {}", errorMessage);
            XxlJobHelper.handleFail("zhpgCalcHandler执行失败: " + errorMessage);
        }
    }
}
