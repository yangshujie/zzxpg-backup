package com.ruoyi.service.impl.zhpg;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ruoyi.common.core.constant.HttpStatus;
import com.ruoyi.common.core.exception.ServiceException;
import com.ruoyi.common.security.utils.SecurityUtils;
import com.ruoyi.domain.zhpg.AlgorithmInfo;
import com.ruoyi.domain.zhpg.CalcFlowTemplate;
import com.ruoyi.domain.zhpg.CalcTask;
import com.ruoyi.domain.zhpg.CalcTaskStageLog;
import com.ruoyi.domain.zhpg.EvalIndicatorSystem;
import com.ruoyi.domain.zhpg.EvalResult;
import com.ruoyi.domain.zhpg.ReportTemplate;
import com.ruoyi.domain.zhpg.dto.CalcExecutionRequest;
import com.ruoyi.domain.zhpg.dto.CalcTaskAsyncResult;
import com.ruoyi.domain.zhpg.dto.ReportDownloadData;
import com.ruoyi.domain.zhpg.dto.ReportTemplateRenderRequest;
import com.ruoyi.domain.zhpg.dto.ObjectiveWeightComputeResult;
import com.ruoyi.domain.zhpg.dto.WeightApplyResult;
import com.ruoyi.mapper.zhpg.CalcTaskMapper;
import com.ruoyi.mapper.zhpg.CalcTaskStageLogMapper;
import com.ruoyi.service.zhpg.IAlgorithmInfoService;
import com.ruoyi.service.zhpg.ICalcFlowTemplateService;
import com.ruoyi.service.zhpg.ICalcTaskService;
import com.ruoyi.service.zhpg.IEvalIndicatorSystemService;
import com.ruoyi.service.zhpg.IEvalResultService;
import com.ruoyi.service.zhpg.IIndicatorTreeWeightService;
import com.ruoyi.service.zhpg.IObjectiveWeightService;
import com.ruoyi.service.zhpg.IReportTemplateService;
import com.ruoyi.zhpg.util.ZhpgIndicatorSystemTreeHelper;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.client.SimpleClientHttpRequestFactory;

import java.text.SimpleDateFormat;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.TimeUnit;

/**
 * 计算任务服务实现（异步模式）
 *
 * 执行流程：
 * 1. /calc/run 创建任务（状态 PENDING）→ 触发 XXL-JOB → 立即返回 taskId
 * 2. XXL-JOB 执行器（ZhpgCalcExecutor）接收任务并执行
 * 3. 执行器通过 XXL-JOB 回调通知 ZHPG
 * 4. ZHPG 更新任务状态（SUCCESS/FAILED）
 * 5. 前端通过 /calc/task/{id}/status 轮询查询状态
 */
@Slf4j
@Service
public class CalcTaskServiceImpl extends ServiceImpl<CalcTaskMapper, CalcTask> implements ICalcTaskService {

    private final CalcTaskStageLogMapper stageLogMapper;
    private final ICalcFlowTemplateService calcFlowTemplateService;
    private final IEvalIndicatorSystemService indicatorSystemService;
    private final IIndicatorTreeWeightService indicatorTreeWeightService;
    private final IObjectiveWeightService objectiveWeightService;
    private final IAlgorithmInfoService algorithmInfoService;
    private final IEvalResultService evalResultService;
    private final IReportTemplateService reportTemplateService;
    private final XxlJobAdminClient xxlJobAdminClient;

    @Value("${zhpg.calc.xxl-job.admin-base-url:}")
    private String xxlJobAdminBaseUrl;

    public CalcTaskServiceImpl(CalcTaskStageLogMapper stageLogMapper,
                               ICalcFlowTemplateService calcFlowTemplateService,
                               IEvalIndicatorSystemService indicatorSystemService,
                               IIndicatorTreeWeightService indicatorTreeWeightService,
                               IObjectiveWeightService objectiveWeightService,
                               IAlgorithmInfoService algorithmInfoService,
                               IEvalResultService evalResultService,
                               IReportTemplateService reportTemplateService,
                               XxlJobAdminClient xxlJobAdminClient) {
        this.stageLogMapper = stageLogMapper;
        this.calcFlowTemplateService = calcFlowTemplateService;
        this.indicatorSystemService = indicatorSystemService;
        this.indicatorTreeWeightService = indicatorTreeWeightService;
        this.objectiveWeightService = objectiveWeightService;
        this.algorithmInfoService = algorithmInfoService;
        this.evalResultService = evalResultService;
        this.reportTemplateService = reportTemplateService;
        this.xxlJobAdminClient = xxlJobAdminClient;
    }

    @Override
    public Page<CalcTask> selectTaskPage(Page<?> page, CalcTask query) {
        QueryWrapper<CalcTask> wrapper = new QueryWrapper<>();
        if (query != null) {
            wrapper.like(StringUtils.isNotBlank(query.getTaskName()), "task_name", query.getTaskName());
            wrapper.eq(query.getCalcFlowTemplateId() != null, "calc_flow_template_id", query.getCalcFlowTemplateId());
            wrapper.eq(StringUtils.isNotBlank(query.getRunStatus()), "run_status", query.getRunStatus());
            wrapper.eq(StringUtils.isNotBlank(query.getCurrentStage()), "current_stage", query.getCurrentStage());
        }
        wrapper.orderByDesc("create_time");
        return baseMapper.selectPage((Page<CalcTask>) page, wrapper);
    }

    @Override
    public CalcTask selectTaskDetail(Long id) {
        CalcTask task = baseMapper.selectById(id);
        if (task == null) {
            return null;
        }
        QueryWrapper<CalcTaskStageLog> wrapper = new QueryWrapper<>();
        wrapper.eq("calc_task_id", id).orderByAsc("stage_order");
        task.setStageLogs(stageLogMapper.selectList(wrapper));
        return task;
    }

    /**
     * 异步发起计算任务
     *
     * 1. 校验模板和指标体系
     * 2. 创建任务记录（状态 PENDING）
     * 3. 准备执行参数（加权树等预处理）
     * 4. 触发 XXL-JOB
     * 5. 立即返回（不等待执行完成）
     */
    @Override
    public CalcTask run(CalcExecutionRequest request) {
        // 1. 校验模板
        CalcFlowTemplate template = calcFlowTemplateService.selectTemplateDetail(request.getCalcFlowTemplateId());
        if (template == null) {
            throw new ServiceException("calc flow template not found");
        }
        if (!"PUBLISHED".equals(template.getStatus()) && !"TESTING".equals(template.getStatus())) {
            throw new ServiceException("only published or testing template can be executed");
        }

        // 2. 校验指标体系
        Long indicatorSystemId = request.getIndicatorSystemId();
        if (indicatorSystemId == null) {
            throw new ServiceException("未指定指标体系：计算流程模板不绑定指标体系，请在发起计算请求中传入 indicatorSystemId");
        }
        EvalIndicatorSystem indicatorSystem = indicatorSystemService.getById(indicatorSystemId);
        if (indicatorSystem == null) {
            throw new ServiceException("indicator system not found");
        }

        // 3. 解析配置
        JSONObject configRoot = parseConfig(template.getConfigJson());
        JSONObject stages = configRoot.getJSONObject("stages");
        if (stages == null) {
            throw new ServiceException("template configJson missing stages");
        }
        JSONObject scheduleStage = stages.getJSONObject("scheduleConfig");
        JSONObject weightStage = stages.getJSONObject("weightCalc");
        JSONObject comprehensiveStage = stages.getJSONObject("comprehensiveCalc");
        JSONObject runtimePolicy = configRoot.getJSONObject("runtimePolicy");
        JSONObject scheduleConfig = stageConfig(scheduleStage);
        JSONObject weightConfig = stageConfig(weightStage);
        JSONObject comprehensiveConfig = stageConfig(comprehensiveStage);

        // 4. 创建任务记录（PENDING 状态）
        CalcTask task = new CalcTask();
        task.setTaskName(StringUtils.isNotBlank(request.getTaskName()) ? request.getTaskName() : buildTaskName(template));
        // 优先使用请求显式传入的 assessTaskId，否则从所选指标体系的关联需求ID获取
        Long assessTaskId = request.getAssessTaskId();
        if (assessTaskId == null || assessTaskId <= 0) {
            assessTaskId = indicatorSystem.getRequirementId();
            if (assessTaskId == null) {
                log.warn("指标体系[{}]未关联需求ID，外部接口模式下将降级为模拟数据", indicatorSystemId);
            }
        }
        task.setAssessTaskId(assessTaskId);
        task.setCalcFlowTemplateId(template.getId());
        task.setTemplateSnapshotJson(template.getConfigJson());
        task.setRunStatus("PENDING");  // 初始状态：待执行
        task.setCurrentStage("SCHEDULE_CONFIG");
        task.setProgressPercent(0);
        task.setLogTraceId("CALC-" + System.currentTimeMillis());
        task.setCreateBy(SecurityUtils.getUsername());
        task.setCreateTime(new Date());
        baseMapper.insert(task);

        // 5. 预计算加权树（在提交前准备好）
        String weightedTreeJson;
        try {
            if (shouldSkipWeightRecalculationStage(weightStage, weightConfig)) {
                // 直接使用指标体系权重
                weightedTreeJson = ZhpgIndicatorSystemTreeHelper.jsonForWeightCalculation(indicatorSystem);
                log.info("任务 {} 使用指标体系权重", task.getId());
            } else {
                // 调用算法服务计算客观权重（类似指标体系客观赋权，但不持久化到数据库）
                log.info("任务 {} 开始调用算法服务计算权重", task.getId());
                JSONObject options = new JSONObject();
                options.put("persist", false);  // 不覆盖指标体系权重，仅本次任务使用
                // 可选：从配置中读取样本行数
                int sampleRows = weightConfig.getIntValue("mockSampleRows", 8);
                options.put("mockSampleRows", sampleRows);
                Long overrideAlgorithmId = weightConfig.getLong("overrideAlgorithmId");
                if (overrideAlgorithmId == null || overrideAlgorithmId <= 0) {
                    // 兼容旧字段名：weightAlgorithmId
                    overrideAlgorithmId = weightConfig.getLong("weightAlgorithmId");
                }
                if (overrideAlgorithmId != null && overrideAlgorithmId > 0) {
                    options.put("overrideAlgorithmId", overrideAlgorithmId);
                }
                String missingWeightPolicy = weightConfig.getString("missingWeightPolicy");
                if (StringUtils.isNotBlank(missingWeightPolicy)) {
                    options.put("missingWeightPolicy", missingWeightPolicy);
                }

                ObjectiveWeightComputeResult weightResult = objectiveWeightService.computeForSystem(
                        indicatorSystem.getId(),
                        options,
                        SecurityUtils.getUsername()
                );
                weightedTreeJson = weightResult.getIndicatorTreeWeight();
                log.info("任务 {} 算法权重计算完成，调用算法 {} 次", task.getId(), weightResult.getAlgorithmCallCount());
            }
        } catch (Exception e) {
            task.setRunStatus("FAILED");
            task.setResultSummaryJson("{\"message\":\"权重计算失败: " + e.getMessage() + "\"}");
            baseMapper.updateById(task);
            throw new ServiceException("权重计算失败: " + e.getMessage());
        }

        // 6. 构建 XXL-JOB 执行参数
        JSONObject executorParam = new JSONObject();
        executorParam.put("taskId", task.getId());
        executorParam.put("taskName", task.getTaskName());
        executorParam.put("templateId", template.getId());
        executorParam.put("templateName", template.getTemplateName());
        executorParam.put("indicatorSystemId", indicatorSystem.getId());
        executorParam.put("indicatorSystemName", indicatorSystem.getSystemName());
        executorParam.put("weightedTreeJson", weightedTreeJson);
        enrichComprehensiveConfigForExecutor(comprehensiveConfig);
        executorParam.put("comprehensiveConfig", comprehensiveConfig);
        executorParam.put("scheduleConfig", scheduleConfig);
        executorParam.put("runtimePolicy", runtimePolicy);

        // 补充任务属性，供报告占位符 TASK_PROPERTY 映射使用
        executorParam.put("startTime", task.getStartTime() != null
                ? new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(task.getStartTime()) : "");
        executorParam.put("assessTaskId", task.getAssessTaskId());

        // 报告输出配置
        JSONObject reportConfig = stageConfig(stages.getJSONObject("reportOutput"));
        executorParam.put("reportConfig", reportConfig);

        // 如果配置了报告模板，将模板内容传递给执行器
        Long reportTemplateId = reportConfig.getLong("reportTemplateId");
        if (reportTemplateId != null && reportTemplateId > 0) {
            try {
                ReportTemplate reportTemplate = reportTemplateService.getById(reportTemplateId);
                if (reportTemplate != null && reportTemplate.getDeleted() != 1) {
                    JSONObject templateData = new JSONObject();
                    templateData.put("templateId", reportTemplate.getId());
                    templateData.put("templateName", reportTemplate.getTemplateName());
                    templateData.put("htmlContent", reportTemplate.getHtmlContent());
                    executorParam.put("reportTemplate", templateData);
                    log.info("任务 {} 关联报告模板: templateId={}, templateName={}",
                            task.getId(), reportTemplate.getId(), reportTemplate.getTemplateName());
                } else {
                    log.warn("报告模板不存在或已删除: templateId={}", reportTemplateId);
                }
            } catch (Exception e) {
                log.error("获取报告模板失败: templateId={}", reportTemplateId, e);
            }
        }

        // 7. 触发 XXL-JOB
        try {
            // 检查 XXL-JOB 是否可用
            if (!xxlJobAdminClient.isAvailable()) {
                throw new ServiceException("XXL-JOB Admin 未配置，请检查 Nacos 配置 zhpg.calc.xxl-job.admin-base-url");
            }

            // 确保模板已注册到 XXL-JOB
            if (template.getXxlJobId() == null || template.getXxlJobId() <= 0) {
                int jobId = calcFlowTemplateService.ensureXxlJobRegistered(template);
                if (jobId <= 0) {
                    throw new ServiceException("XXL-JOB 任务注册失败，请检查 XXL-JOB Admin 配置");
                }
                // 更新模板对象的 jobId（数据库已更新，这里更新内存对象）
                template.setXxlJobId(jobId);
            }

            // 同步 admin 中的展示名称，避免页面长期显示模板默认名称或旧名称
            calcFlowTemplateService.refreshXxlJobRuntimeMeta(template, task.getTaskName());

            log.info("正在触发 XXL-JOB: jobId={}, taskId={}", template.getXxlJobId(), task.getId());

            // 触发任务
            xxlJobAdminClient.triggerJob(template.getXxlJobId(), executorParam.toJSONString(), "");

            // 更新任务状态为 DISPATCHED（已分发）
            task.setRunStatus("DISPATCHED");
            task.setStartTime(new Date());  // 记录开始等待执行的时间
            baseMapper.updateById(task);

            // 创建权重计算阶段日志
            createWeightCalcStageLog(task, indicatorSystem, weightedTreeJson);

            // 创建综合计算阶段日志（初始状态为PENDING，等待XXL-JOB执行完成后再更新）
            createComprehensiveCalcPendingStageLog(task);

            // 如果配置了落库，提前创建评估结果记录（PENDING状态，供任务结果管理页面立即展示进度）
            createPendingEvalResult(task, stages, indicatorSystem);

            log.info("任务 {} 已提交到 XXL-JOB, jobId={}, 等待执行器处理", task.getId(), template.getXxlJobId());

        } catch (Exception e) {
            log.error("提交到XXL-JOB失败: taskId={}, templateId={}, xxlJobId={}",
                    task.getId(), template.getId(), template.getXxlJobId(), e);
            task.setRunStatus("FAILED");
            String errorMsg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            task.setResultSummaryJson("{\"message\":\"提交到XXL-JOB失败: " + errorMsg + "\"}");
            baseMapper.updateById(task);
            throw new ServiceException("提交到XXL-JOB失败: " + errorMsg);
        }

        return selectTaskDetail(task.getId());
    }

    /**
     * 处理 XXL-JOB 回调
     * 由执行器执行完成后调用，更新任务状态和结果
     */
    @Override
    public void handleXxlJobCallback(Long taskId, boolean success, String resultJson) {
        handleXxlJobCallbackWithReport(taskId, success, resultJson, null, null, null);
    }

    /**
     * 处理 XXL-JOB 回调（带报告URL）
     * 由执行器执行完成后调用，更新任务状态和结果
     */
    @Override
    public void handleXxlJobCallbackWithReport(Long taskId, boolean success, String resultJson,
                                               String reportUrl, String wordUrl, String wpsUrl) {
        log.info("XXL-JOB回调: taskId={}, success={}, reportUrl={}", taskId, success, reportUrl);

        CalcTask task = baseMapper.selectById(taskId);
        if (task == null) {
            log.error("回调任务不存在: taskId={}", taskId);
            return;
        }

        // 只有在 PENDING/DISPATCHED/RUNNING 状态下才处理回调
        String currentStatus = task.getRunStatus();
        if (!"PENDING".equals(currentStatus) && !"DISPATCHED".equals(currentStatus) && !"RUNNING".equals(currentStatus)) {
            log.warn("任务 {} 当前状态为 {}，忽略重复回调", taskId, currentStatus);
            return;
        }

        if (success) {
            // 执行成功
            try {
                JSONObject result = JSON.parseObject(resultJson);
                task.setRunStatus("SUCCESS");
                task.setCurrentStage("FINISHED");
                task.setProgressPercent(100);
                task.setResultSummaryJson(resultJson);
                task.setEndTime(new Date());
                task.setUpdateBy("XXL-JOB");
                task.setUpdateTime(new Date());
                baseMapper.updateById(task);

                // 创建或更新综合计算阶段日志
                createOrUpdateComprehensiveCalcStageLog(task, result);

                // 可选：生成评估结果记录（带报告URL）
                log.info("任务 {} 开始生成评估结果记录...", taskId);
                generateEvalResultWithReport(task, result, reportUrl, wordUrl, wpsUrl);
                log.info("任务 {} 评估结果记录生成完成", taskId);

                log.info("任务 {} 执行完成，状态已更新为 SUCCESS", taskId);
            } catch (Exception e) {
                log.error("处理成功回调时出错: taskId={}", taskId, e);
                task.setRunStatus("FAILED");
                task.setResultSummaryJson("{\"message\":\"回调处理失败: " + e.getMessage() + "\"}");
                task.setEndTime(new Date());
                baseMapper.updateById(task);

                // 记录失败日志
                createOrUpdateComprehensiveCalcStageLogFailed(task, "回调处理失败: " + e.getMessage());
            }
        } else {
            // 执行失败
            task.setRunStatus("FAILED");
            task.setResultSummaryJson("{\"message\":\"" + StringUtils.defaultString(resultJson, "执行失败") + "\"}");
            task.setEndTime(new Date());
            task.setUpdateBy("XXL-JOB");
            task.setUpdateTime(new Date());
            baseMapper.updateById(task);

            // 记录失败日志
            createOrUpdateComprehensiveCalcStageLogFailed(task, resultJson);

            log.info("任务 {} 执行失败，状态已更新为 FAILED", taskId);
        }
    }

    /**
     * 查询任务执行状态（供前端轮询）
     */
    @Override
    public CalcTaskAsyncResult getTaskStatus(Long taskId) {
        CalcTask task = baseMapper.selectById(taskId);
        if (task == null) {
            return null;
        }

        CalcTaskAsyncResult result = new CalcTaskAsyncResult();
        result.setTaskId(task.getId());
        result.setTaskName(task.getTaskName());
        result.setStatus(task.getRunStatus());
        result.setCurrentStage(task.getCurrentStage());
        result.setProgressPercent(task.getProgressPercent());
        result.setCreateTime(task.getCreateTime());
        result.setStartTime(task.getStartTime());
        result.setEndTime(task.getEndTime());

        // 解析结果摘要
        if (StringUtils.isNotBlank(task.getResultSummaryJson())) {
            try {
                JSONObject summary = JSON.parseObject(task.getResultSummaryJson());
                result.setResult(summary);
            } catch (Exception e) {
                result.setResult(task.getResultSummaryJson());
            }
        }

        // 如果失败，提取错误信息
        if ("FAILED".equals(task.getRunStatus())) {
            try {
                JSONObject summary = JSON.parseObject(task.getResultSummaryJson());
                result.setErrorMessage(summary.getString("message"));
            } catch (Exception e) {
                result.setErrorMessage(task.getResultSummaryJson());
            }
        }

        return result;
    }

    // ==================== 私有方法 ====================

    private boolean shouldSkipWeightRecalculationStage(JSONObject weightStage, JSONObject weightConfig) {
        if (weightStage != null && weightStage.containsKey("enabled")
                && Boolean.FALSE.equals(weightStage.getBoolean("enabled"))) {
            return true;
        }
        if (weightConfig == null) {
            return true;
        }
        String src = weightConfig.getString("weightSource");
        if (StringUtils.isBlank(src)) {
            return true;
        }
        return "INDICATOR_SYSTEM".equalsIgnoreCase(src.trim());
    }

    private void enrichComprehensiveConfigForExecutor(JSONObject comprehensiveConfig) {
        if (comprehensiveConfig == null) {
            return;
        }
        String aggregationSource = comprehensiveConfig.getString("aggregationSource");
        if (!"TEMPLATE_OVERRIDE".equalsIgnoreCase(aggregationSource)) {
            return;
        }
        Long overrideAggAlgId = comprehensiveConfig.getLong("overrideAggregationAlg");
        if (overrideAggAlgId == null || overrideAggAlgId <= 0) {
            // 兼容旧字段名
            overrideAggAlgId = comprehensiveConfig.getLong("overrideAggregationAlgId");
        }
        if (overrideAggAlgId == null || overrideAggAlgId <= 0) {
            return;
        }
        try {
            AlgorithmInfo alg = algorithmInfoService.selectAlgorithmDetail(overrideAggAlgId);
            if (alg == null) {
                return;
            }
            String algName = extractAlgorithmName(alg);
            if (StringUtils.isNotBlank(algName)) {
                comprehensiveConfig.put("overrideAggregationAlgName", algName);
            }
            if (StringUtils.isNotBlank(alg.getAlgorithmType())) {
                comprehensiveConfig.put("overrideAggregationAlgType", alg.getAlgorithmType());
            }
        } catch (Exception ex) {
            log.warn("解析覆盖聚合算法失败, algorithmId={}: {}", overrideAggAlgId, ex.getMessage());
        }
    }

    private String extractAlgorithmName(AlgorithmInfo alg) {
        if (alg == null) {
            return null;
        }
        String codeUrl = alg.getAlgorithmCodeUrl();
        if (StringUtils.isNotBlank(codeUrl)) {
            String path = codeUrl.replace('\\', '/');
            int idx = path.lastIndexOf('/');
            String file = idx >= 0 ? path.substring(idx + 1) : path;
            if (file.endsWith(".zip")) {
                file = file.substring(0, file.length() - 4);
            }
            if (StringUtils.isNotBlank(file)) {
                return file.trim();
            }
        }
        if (StringUtils.isNotBlank(alg.getAlgorithmName())) {
            return alg.getAlgorithmName().trim();
        }
        return null;
    }

    /**
     * 生成评估结果记录
     *
     * reportUrl 对应 PDF，wordUrl 对应 Word（当前执行器只生成 Word）。
     * 以 wordUrl 为主要下载链接，reportUrl 作为备选。
     */
    private void generateEvalResultWithReport(CalcTask task, JSONObject result,
                                              String reportUrl, String wordUrl, String wpsUrl) {
        try {
            log.info("开始生成评估结果记录: taskId={}, result={}", task.getId(), result != null ? result.toJSONString() : "null");

            // 从模板快照解析配置
            JSONObject configRoot = JSON.parseObject(task.getTemplateSnapshotJson());
            JSONObject stages = configRoot.getJSONObject("stages");
            JSONObject reportConfig = stageConfig(stages.getJSONObject("reportOutput"));

            // 只有配置了 RESULT_DB 才写入评估结果表
            JSONArray outputTargets = reportConfig.getJSONArray("outputTargets");
            log.info("任务 {} 输出目标配置: outputTargets={}", task.getId(), outputTargets);
            if (outputTargets == null || !outputTargets.contains("RESULT_DB")) {
                log.info("任务 {} 未配置 RESULT_DB 输出目标，跳过评估结果记录生成", task.getId());
                return;
            }

            // 查找是否已有该任务的预创建评估结果记录（任务发起时创建的 PENDING 记录）
            EvalResult evalResult = evalResultService.getOne(
                    new QueryWrapper<EvalResult>().eq("task_id", task.getId()).eq("del_flag", 0));
            boolean isExisting = (evalResult != null);
            if (!isExisting) {
                evalResult = new EvalResult();
                evalResult.setCreateBy("XXL-JOB");
                evalResult.setCreateTime(new Date());
            }

            evalResult.setTaskId(task.getId());
            evalResult.setTaskName(task.getTaskName());
            evalResult.setTemplateId(task.getCalcFlowTemplateId());
            if (task.getCalcFlowTemplateId() != null) {
                CalcFlowTemplate calcFlowTemplate = calcFlowTemplateService.selectTemplateDetail(task.getCalcFlowTemplateId());
                if (calcFlowTemplate != null) {
                    evalResult.setTemplateName(calcFlowTemplate.getTemplateName());
                }
            }

            // 从回调结果中提取字段，添加详细日志
            BigDecimal score = result.getBigDecimal("score");
            String grade = result.getString("grade");
            String conclusion = result.getString("conclusion");
            String suggestion = result.getString("suggestion");
            Long indicatorSystemId = result.getLong("indicatorSystemId");
            String indicatorSystemName = result.getString("indicatorSystemName");

            log.info("任务 {} 回调结果: score={}, grade={}, indicatorSystemId={}, indicatorSystemName={}",
                    task.getId(), score, grade, indicatorSystemId, indicatorSystemName);

            evalResult.setScore(score);
            evalResult.setGrade(grade);
            evalResult.setConclusion(conclusion);
            evalResult.setSuggestion(suggestion);
            evalResult.setIndicatorSystemId(indicatorSystemId);
            evalResult.setIndicatorSystemName(indicatorSystemName);
            evalResult.setWorkflowStatus("GATHERING");

            // 报告下载链接：优先 PDF（reportUrl），不存在时用 Word（wordUrl）
            String effectiveReportUrl = StringUtils.isNotBlank(reportUrl) ? reportUrl : wordUrl;
            if (StringUtils.isNotBlank(effectiveReportUrl)) {
                evalResult.setReportUrl(effectiveReportUrl);
            }

            // 维度得分
            List<EvalResult.DimensionScore> dimensionScores = new ArrayList<>();
            JSONArray dimensions = result.getJSONArray("dimensions");
            if (dimensions != null) {
                for (int i = 0; i < dimensions.size(); i++) {
                    JSONObject one = dimensions.getJSONObject(i);
                    EvalResult.DimensionScore dimensionScore = new EvalResult.DimensionScore();
                    dimensionScore.setLabel(one.getString("label"));
                    dimensionScore.setValue(one.getBigDecimal("value"));
                    dimensionScore.setTone(one.getString("tone"));
                    dimensionScores.add(dimensionScore);
                }
            }
            evalResult.setDimensionList(dimensionScores);

            // 报告载荷：保留全部 URL 供后续扩展使用
            JSONObject reportPayload = new JSONObject();
            reportPayload.put("outputTargets", outputTargets);
            reportPayload.put("reportTemplateId", reportConfig.getLong("reportTemplateId"));
            reportPayload.put("executionChannel", "XXL-JOB");
            if (StringUtils.isNotBlank(reportUrl)) {
                reportPayload.put("reportUrl", reportUrl);
            }
            if (StringUtils.isNotBlank(wordUrl)) {
                reportPayload.put("wordUrl", wordUrl);
            }
            if (StringUtils.isNotBlank(wpsUrl)) {
                reportPayload.put("wpsUrl", wpsUrl);
            }
            evalResult.setReportPayloadJson(reportPayload.toJSONString());

            if (isExisting) {
                evalResult.setUpdateBy("XXL-JOB");
                evalResult.setUpdateTime(new Date());
                evalResultService.updateEvalResult(evalResult);
            } else {
                evalResultService.insertEvalResult(evalResult);
            }
            log.info("任务 {} {}评估结果记录成功: resultId={}, score={}, grade={}, workflowStatus={}, reportUrl={}",
                    task.getId(), isExisting ? "更新" : "生成", evalResult.getId(),
                    evalResult.getScore(), evalResult.getGrade(), evalResult.getWorkflowStatus(), effectiveReportUrl);
        } catch (Exception e) {
            log.error("生成评估结果记录失败: taskId={}, error={}", task.getId(), e.getMessage(), e);
            // 打印更详细的错误信息
            if (e.getCause() != null) {
                log.error("根本原因: {}", e.getCause().getMessage());
            }
        }
    }

    private JSONObject parseConfig(String configJson) {
        try {
            return JSON.parseObject(configJson);
        } catch (Exception ex) {
            throw new ServiceException("template configJson is invalid");
        }
    }

    private JSONObject stageConfig(JSONObject stage) {
        if (stage == null) {
            return new JSONObject();
        }
        JSONObject config = stage.getJSONObject("config");
        return config == null ? new JSONObject() : config;
    }

    private String buildTaskName(CalcFlowTemplate template) {
        return template.getTemplateName() + "-" + System.currentTimeMillis();
    }

    // ==================== 阶段日志相关方法 ====================

    /**
     * 创建权重计算阶段日志
     */
    private void createWeightCalcStageLog(CalcTask task, EvalIndicatorSystem indicatorSystem, String weightedTreeJson) {
        try {
            CalcTaskStageLog stageLog = new CalcTaskStageLog();
            stageLog.setCalcTaskId(task.getId());
            stageLog.setStageCode("WEIGHT_CALC");
            stageLog.setStageName("权重计算");
            stageLog.setStageOrder(1);
            stageLog.setExecuteStatus("SUCCESS");
            stageLog.setBeginTime(task.getStartTime());
            stageLog.setFinishTime(new Date());

            // 构建易读的输入摘要
            StringBuilder inputSummary = new StringBuilder();
            inputSummary.append("指标体系：").append(indicatorSystem != null ? indicatorSystem.getSystemName() : "未知").append("\n");

            // 解析加权树，获取节点数量
            int nodeCount = 0;
            if (StringUtils.isNotBlank(weightedTreeJson)) {
                try {
                    JSONObject tree = JSON.parseObject(weightedTreeJson);
                    nodeCount = countTreeNodes(tree);
                } catch (Exception e) {
                    log.warn("解析加权树失败: taskId={}", task.getId(), e);
                }
            }
            inputSummary.append("指标节点数：").append(nodeCount).append("\n");
            inputSummary.append("计算方式：使用指标体系预设权重");
            stageLog.setInputSummary(inputSummary.toString());

            // 构建易读的输出摘要
            StringBuilder outputSummary = new StringBuilder();
            outputSummary.append("权重计算完成\n");
            outputSummary.append("已生成带权重的指标树，供后续综合计算使用");
            stageLog.setOutputSummary(outputSummary.toString());

            stageLogMapper.insert(stageLog);
            log.debug("创建权重计算阶段日志: taskId={}", task.getId());
        } catch (Exception e) {
            log.error("创建权重计算阶段日志失败: taskId={}", task.getId(), e);
        }
    }

    /**
     * 创建综合计算阶段日志（等待执行状态）
     */
    private void createComprehensiveCalcPendingStageLog(CalcTask task) {
        try {
            CalcTaskStageLog stageLog = new CalcTaskStageLog();
            stageLog.setCalcTaskId(task.getId());
            stageLog.setStageCode("COMPREHENSIVE_CALC");
            stageLog.setStageName("综合计算");
            stageLog.setStageOrder(2);
            stageLog.setExecuteStatus("PENDING");
            stageLog.setBeginTime(new Date());

            // 构建易读的输入摘要
            StringBuilder inputSummary = new StringBuilder();
            inputSummary.append("任务名称：").append(task.getTaskName()).append("\n");
            inputSummary.append("执行方式：XXL-JOB异步执行\n");
            inputSummary.append("状态：等待执行器处理...");
            stageLog.setInputSummary(inputSummary.toString());

            stageLogMapper.insert(stageLog);
            log.debug("创建综合计算阶段日志(PENDING): taskId={}", task.getId());
        } catch (Exception e) {
            log.error("创建综合计算阶段日志失败: taskId={}", task.getId(), e);
        }
    }

    /**
     * 更新综合计算阶段日志（执行成功）
     */
    private void createOrUpdateComprehensiveCalcStageLog(CalcTask task, JSONObject result) {
        try {
            // 先查询是否已有PENDING状态的日志
            QueryWrapper<CalcTaskStageLog> wrapper = new QueryWrapper<>();
            wrapper.eq("calc_task_id", task.getId());
            wrapper.eq("stage_code", "COMPREHENSIVE_CALC");
            CalcTaskStageLog stageLog = stageLogMapper.selectOne(wrapper);

            if (stageLog == null) {
                // 创建新日志
                stageLog = new CalcTaskStageLog();
                stageLog.setCalcTaskId(task.getId());
                stageLog.setStageCode("COMPREHENSIVE_CALC");
                stageLog.setStageName("综合计算");
                stageLog.setStageOrder(2);
                stageLog.setBeginTime(task.getStartTime());
            }

            stageLog.setExecuteStatus("SUCCESS");
            stageLog.setFinishTime(new Date());

            // 构建易读的输入摘要
            StringBuilder inputSummary = new StringBuilder();
            inputSummary.append("任务名称：").append(task.getTaskName()).append("\n");
            inputSummary.append("执行方式：XXL-JOB异步执行\n");

            // 从模板快照解析配置获取计算策略
            String nullDataPolicy = "空数据补零";
            try {
                JSONObject config = JSON.parseObject(task.getTemplateSnapshotJson());
                JSONObject comprehensiveConfig = config.getJSONObject("stages")
                        .getJSONObject("comprehensiveCalc")
                        .getJSONObject("config");
                String policy = comprehensiveConfig.getString("nullDataPolicy");
                if ("SKIP".equalsIgnoreCase(policy)) {
                    nullDataPolicy = "空数据跳过";
                } else if ("TERMINATE".equalsIgnoreCase(policy)) {
                    nullDataPolicy = "空数据终止";
                }
            } catch (Exception e) {
                log.debug("解析空数据策略失败", e);
            }
            inputSummary.append("空数据处理：").append(nullDataPolicy);
            stageLog.setInputSummary(inputSummary.toString());

            // 构建易读的输出摘要
            StringBuilder outputSummary = new StringBuilder();
            BigDecimal score = result.getBigDecimal("score");
            String grade = result.getString("grade");
            String indicatorSystemName = result.getString("indicatorSystemName");
            Integer nodeCount = result.getInteger("weightedTreeNodeCount");

            outputSummary.append("指标体系：").append(StringUtils.defaultString(indicatorSystemName, "未知")).append("\n");
            if (nodeCount != null) {
                outputSummary.append("计算节点数：").append(nodeCount).append("\n");
            }
            outputSummary.append("综合得分：").append(score != null ? score.toString() : "—").append("\n");
            outputSummary.append("评级结果：").append(StringUtils.defaultString(grade, "—"));

            // 添加维度得分
            JSONArray dimensions = result.getJSONArray("dimensions");
            if (dimensions != null && !dimensions.isEmpty()) {
                outputSummary.append("\n\n维度得分：");
                for (int i = 0; i < dimensions.size(); i++) {
                    JSONObject dim = dimensions.getJSONObject(i);
                    String label = dim.getString("label");
                    BigDecimal value = dim.getBigDecimal("value");
                    if (StringUtils.isNotBlank(label) && value != null) {
                        outputSummary.append("\n  • ").append(label).append("：").append(value);
                    }
                }
            }

            stageLog.setOutputSummary(outputSummary.toString());

            if (stageLog.getId() != null) {
                stageLogMapper.updateById(stageLog);
            } else {
                stageLogMapper.insert(stageLog);
            }
            log.debug("更新综合计算阶段日志(SUCCESS): taskId={}", task.getId());
        } catch (Exception e) {
            log.error("更新综合计算阶段日志失败: taskId={}", task.getId(), e);
        }
    }

    /**
     * 更新综合计算阶段日志（执行失败）
     */
    private void createOrUpdateComprehensiveCalcStageLogFailed(CalcTask task, String errorMessage) {
        try {
            // 先查询是否已有PENDING状态的日志
            QueryWrapper<CalcTaskStageLog> wrapper = new QueryWrapper<>();
            wrapper.eq("calc_task_id", task.getId());
            wrapper.eq("stage_code", "COMPREHENSIVE_CALC");
            CalcTaskStageLog stageLog = stageLogMapper.selectOne(wrapper);

            if (stageLog == null) {
                // 创建新日志
                stageLog = new CalcTaskStageLog();
                stageLog.setCalcTaskId(task.getId());
                stageLog.setStageCode("COMPREHENSIVE_CALC");
                stageLog.setStageName("综合计算");
                stageLog.setStageOrder(2);
                stageLog.setBeginTime(task.getStartTime());
            }

            stageLog.setExecuteStatus("FAILED");
            stageLog.setFinishTime(new Date());

            // 构建易读的输入摘要
            StringBuilder inputSummary = new StringBuilder();
            inputSummary.append("任务名称：").append(task.getTaskName()).append("\n");
            inputSummary.append("执行方式：XXL-JOB异步执行");
            stageLog.setInputSummary(inputSummary.toString());

            // 错误信息
            stageLog.setErrorMessage(StringUtils.defaultString(errorMessage, "执行失败"));

            if (stageLog.getId() != null) {
                stageLogMapper.updateById(stageLog);
            } else {
                stageLogMapper.insert(stageLog);
            }
            log.debug("更新综合计算阶段日志(FAILED): taskId={}", task.getId());
        } catch (Exception e) {
            log.error("更新综合计算阶段日志(失败状态)失败: taskId={}", task.getId(), e);
        }
    }

    /**
     * 任务发起后立即创建待计算评估结果记录（PENDING 状态）
     * 仅当模板配置了 RESULT_DB 落库目标时才创建，使结果管理页面可立即展示任务进度
     */
    private void createPendingEvalResult(CalcTask task, JSONObject stages, EvalIndicatorSystem indicatorSystem) {
        try {
            JSONObject reportConfig = stageConfig(stages.getJSONObject("reportOutput"));
            JSONArray outputTargets = reportConfig.getJSONArray("outputTargets");
            if (outputTargets == null || !outputTargets.contains("RESULT_DB")) {
                return;
            }
            // 避免重复创建
            EvalResult existing = evalResultService.getOne(
                    new QueryWrapper<EvalResult>().eq("task_id", task.getId()).eq("del_flag", 0));
            if (existing != null) {
                return;
            }
            EvalResult evalResult = new EvalResult();
            evalResult.setTaskId(task.getId());
            evalResult.setTaskName(task.getTaskName());
            evalResult.setTemplateId(task.getCalcFlowTemplateId());
            evalResult.setIndicatorSystemId(indicatorSystem.getId());
            evalResult.setIndicatorSystemName(indicatorSystem.getSystemName());
            evalResult.setWorkflowStatus("PENDING");
            evalResult.setCreateBy(task.getCreateBy());
            evalResult.setCreateTime(new Date());
            evalResultService.insertEvalResult(evalResult);
            log.info("任务 {} 预创建评估结果记录（PENDING）: resultId={}", task.getId(), evalResult.getId());
        } catch (Exception e) {
            log.error("预创建评估结果记录失败: taskId={}", task.getId(), e);
        }
    }

    /**
     * 统计指标树节点数量
     */
    private int countTreeNodes(JSONObject tree) {
        if (tree == null) return 0;
        int count = 1;
        JSONArray children = tree.getJSONArray("children");
        if (children != null) {
            for (int i = 0; i < children.size(); i++) {
                count += countTreeNodes(children.getJSONObject(i));
            }
        }
        return count;
    }
}
