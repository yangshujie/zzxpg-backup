package com.ruoyi.service.impl.zhpg;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ruoyi.common.core.exception.ServiceException;
import com.ruoyi.common.security.utils.SecurityUtils;
import com.ruoyi.domain.zhpg.CalcFlowTemplate;
import com.ruoyi.domain.zhpg.EvalIndicatorSystem;
import com.ruoyi.domain.zhpg.dto.XxlJobInfoRequest;
import com.ruoyi.mapper.zhpg.CalcFlowTemplateMapper;
import com.ruoyi.mapper.zhpg.XxlJobAdminMapper;
import com.ruoyi.service.zhpg.ICalcFlowTemplateService;
import com.ruoyi.service.zhpg.IEvalIndicatorSystemService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Service
public class CalcFlowTemplateServiceImpl extends ServiceImpl<CalcFlowTemplateMapper, CalcFlowTemplate>
        implements ICalcFlowTemplateService {

    private static final AtomicLong CODE_SEQ = new AtomicLong(System.currentTimeMillis() % 100000);
    private static final String DEFAULT_EXECUTOR_NAME = "zhpgCalcHandler";

    private final IEvalIndicatorSystemService indicatorSystemService;
    private final XxlJobAdminClient xxlJobAdminClient;
    private final XxlJobAdminMapper xxlJobAdminMapper;

    @Value("${zhpg.calc.xxl-job.executor-app-name:zhpg-calc-executor}")
    private String executorAppName;

    @Value("${zhpg.calc.xxl-job.default-job-group:0}")
    private int defaultJobGroup;

    public CalcFlowTemplateServiceImpl(IEvalIndicatorSystemService indicatorSystemService,
                                       XxlJobAdminClient xxlJobAdminClient,
                                       XxlJobAdminMapper xxlJobAdminMapper) {
        this.indicatorSystemService = indicatorSystemService;
        this.xxlJobAdminClient = xxlJobAdminClient;
        this.xxlJobAdminMapper = xxlJobAdminMapper;
    }

    @Override
    public Page<CalcFlowTemplate> selectTemplatePage(Page<?> page, CalcFlowTemplate query) {
        QueryWrapper<CalcFlowTemplate> wrapper = buildQueryWrapper(query);
        wrapper.orderByDesc("update_time");
        return baseMapper.selectPage((Page<CalcFlowTemplate>) page, wrapper);
    }

    @Override
    public List<CalcFlowTemplate> selectTemplateList(CalcFlowTemplate query) {
        QueryWrapper<CalcFlowTemplate> wrapper = buildQueryWrapper(query);
        wrapper.orderByDesc("update_time");
        return baseMapper.selectList(wrapper);
    }

    @Override
    public CalcFlowTemplate selectTemplateDetail(Long id) {
        return baseMapper.selectById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int insertTemplate(CalcFlowTemplate template) {
        validateTemplateName(template.getTemplateName(), null);
        enrichTemplateFields(template, null);
        if (StringUtils.isBlank(template.getTemplateCode())) {
            template.setTemplateCode("CFT" + String.format("%06d", CODE_SEQ.incrementAndGet()));
        }
        if (StringUtils.isBlank(template.getStatus())) {
            template.setStatus("DRAFT");
        }
        if (StringUtils.isBlank(template.getVersionNo())) {
            template.setVersionNo("V1.0");
        }
        template.setConfigJson(normalizeConfigJson(template.getConfigJson()));
        return baseMapper.insert(template);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int updateTemplate(CalcFlowTemplate template) {
        CalcFlowTemplate existing = baseMapper.selectById(template.getId());
        if (existing == null) {
            throw new ServiceException("calc flow template not found");
        }
        validateTemplateName(template.getTemplateName(), template.getId());
        enrichTemplateFields(template, existing);
        template.setConfigJson(normalizeConfigJson(template.getConfigJson()));
        return baseMapper.updateById(template);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteTemplateByIds(Long[] ids) {
        // 删除模板时同步删除 xxl-job 任务
        if (xxlJobAdminClient.isAvailable()) {
            for (Long tid : ids) {
                CalcFlowTemplate t = baseMapper.selectById(tid);
                if (t != null && t.getXxlJobId() != null && t.getXxlJobId() > 0) {
                    try {
                        xxlJobAdminClient.removeJob(t.getXxlJobId());
                    } catch (Exception e) {
                        log.warn("删除模板时移除 xxl-job 失败, jobId={}: {}", t.getXxlJobId(), e.getMessage());
                    }
                }
            }
        }
        return baseMapper.deleteBatchIds(Arrays.asList(ids));
    }

    @Override
    public int submitTest(Long id) {
        CalcFlowTemplate template = baseMapper.selectById(id);
        if (template == null) {
            throw new ServiceException("calc flow template not found");
        }
        if (!"DRAFT".equals(template.getStatus())) {
            throw new ServiceException("only draft template can be submitted for test");
        }
        validateBeforePublish(template);
        template.setStatus("TESTING");
        template.setUpdateBy(SecurityUtils.getUsername());
        template.setUpdateTime(new Date());
        return baseMapper.updateById(template);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int publishTemplate(Long id) {
        CalcFlowTemplate template = baseMapper.selectById(id);
        if (template == null) {
            throw new ServiceException("calc flow template not found");
        }
        if (!"TESTING".equals(template.getStatus())) {
            throw new ServiceException("only testing template can be published");
        }
        template.setStatus("PUBLISHED");
        template.setUpdateBy(SecurityUtils.getUsername());
        template.setUpdateTime(new Date());
        // 发布时自动注册/更新 xxl-job
        syncXxlJobSafe(template);
        return baseMapper.updateById(template);
    }

    @Override
    public int disableTemplate(Long id) {
        CalcFlowTemplate template = baseMapper.selectById(id);
        if (template == null) {
            throw new ServiceException("calc flow template not found");
        }
        if (!"PUBLISHED".equals(template.getStatus())) {
            throw new ServiceException("only published template can be disabled");
        }
        template.setStatus("DISABLED");
        template.setUpdateBy(SecurityUtils.getUsername());
        template.setUpdateTime(new Date());
        // 停用时停止 xxl-job 调度
        if (template.getXxlJobId() != null && template.getXxlJobId() > 0 && xxlJobAdminClient.isAvailable()) {
            try {
                xxlJobAdminClient.stopJob(template.getXxlJobId());
            } catch (Exception e) {
                log.warn("停用模板时停止 xxl-job 失败, jobId={}: {}", template.getXxlJobId(), e.getMessage());
            }
        }
        return baseMapper.updateById(template);
    }

    @Override
    public int enableTemplate(Long id) {
        CalcFlowTemplate template = baseMapper.selectById(id);
        if (template == null) {
            throw new ServiceException("calc flow template not found");
        }
        if (!"DISABLED".equals(template.getStatus()) && !"TESTING".equals(template.getStatus())) {
            throw new ServiceException("only disabled or testing template can be enabled");
        }
        template.setStatus("PUBLISHED");
        template.setUpdateBy(SecurityUtils.getUsername());
        template.setUpdateTime(new Date());
        // 启用时恢复 xxl-job 调度
        if (template.getXxlJobId() != null && template.getXxlJobId() > 0 && xxlJobAdminClient.isAvailable()) {
            try {
                xxlJobAdminClient.startJob(template.getXxlJobId());
            } catch (Exception e) {
                log.warn("启用模板时启动 xxl-job 失败, jobId={}: {}", template.getXxlJobId(), e.getMessage());
            }
        }
        return baseMapper.updateById(template);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CalcFlowTemplate copyVersion(Long id) {
        CalcFlowTemplate source = baseMapper.selectById(id);
        if (source == null) {
            throw new ServiceException("source template not found");
        }
        CalcFlowTemplate copy = new CalcFlowTemplate();
        copy.setTemplateCode("CFT" + String.format("%06d", CODE_SEQ.incrementAndGet()));
        copy.setTemplateName(source.getTemplateName() + " (copy)");
        copy.setTaskType(source.getTaskType());
        copy.setEquipmentType(source.getEquipmentType());
        copy.setCalcGranularity(source.getCalcGranularity());
        copy.setIndicatorSystemId(source.getIndicatorSystemId());
        copy.setIndicatorSystemName(source.getIndicatorSystemName());
        copy.setDataPlanId(source.getDataPlanId());
        copy.setDataPlanName(source.getDataPlanName());
        copy.setConfigJson(source.getConfigJson());
        copy.setVersionNo(incrementVersion(source.getVersionNo()));
        copy.setStatus("DRAFT");
        copy.setDescription(source.getDescription());
        copy.setCreateBy(SecurityUtils.getUsername());
        copy.setCreateTime(new Date());
        baseMapper.insert(copy);
        return copy;
    }

    private QueryWrapper<CalcFlowTemplate> buildQueryWrapper(CalcFlowTemplate query) {
        QueryWrapper<CalcFlowTemplate> wrapper = new QueryWrapper<>();
        if (query != null) {
            if (StringUtils.isNotBlank(query.getTemplateName())) {
                wrapper.like("template_name", query.getTemplateName());
            }
            if (StringUtils.isNotBlank(query.getTaskType())) {
                wrapper.eq("task_type", query.getTaskType());
            }
            if (StringUtils.isNotBlank(query.getEquipmentType())) {
                wrapper.eq("equipment_type", query.getEquipmentType());
            }
            if (StringUtils.isNotBlank(query.getCalcGranularity())) {
                wrapper.eq("calc_granularity", query.getCalcGranularity());
            }
            if (StringUtils.isNotBlank(query.getStatus())) {
                if ("NOT_PUBLISHED".equalsIgnoreCase(query.getStatus())) {
                    wrapper.ne("status", "PUBLISHED");
                } else {
                    wrapper.eq("status", query.getStatus());
                }
            }
        }
        return wrapper;
    }

    private void validateTemplateName(String name, Long excludeId) {
        if (StringUtils.isBlank(name)) {
            return;
        }
        QueryWrapper<CalcFlowTemplate> wrapper = new QueryWrapper<>();
        wrapper.eq("template_name", name);
        if (excludeId != null) {
            wrapper.ne("id", excludeId);
        }
        if (baseMapper.selectCount(wrapper) > 0) {
            throw new ServiceException("templateName already exists: " + name);
        }
    }

    private void validateBeforePublish(CalcFlowTemplate template) {
        if (StringUtils.isBlank(template.getTaskType())) {
            throw new ServiceException("taskType is required");
        }
        if (StringUtils.isBlank(template.getConfigJson())) {
            throw new ServiceException("configJson is required");
        }
        JSONObject root = JSON.parseObject(normalizeConfigJson(template.getConfigJson()));
        JSONObject stages = root.getJSONObject("stages");
        if (stages == null) {
            throw new ServiceException("configJson.stages is required");
        }
        JSONObject scheduleConfig = stages.getJSONObject("scheduleConfig");
        if (scheduleConfig == null || scheduleConfig.getJSONObject("config") == null) {
            throw new ServiceException("scheduleConfig is required");
        }
        JSONObject config = scheduleConfig.getJSONObject("config");
        if (StringUtils.isBlank(config.getString("executorName"))) {
            throw new ServiceException("scheduleConfig.config.executorName is required");
        }
    }

    private void enrichTemplateFields(CalcFlowTemplate template, CalcFlowTemplate existing) {
        if (template == null) {
            return;
        }
        Long sysId = template.getIndicatorSystemId();
        if (sysId != null && sysId <= 0) {
            sysId = null;
            template.setIndicatorSystemId(null);
        }
        if (sysId != null) {
            EvalIndicatorSystem indicatorSystem = indicatorSystemService.getById(sysId);
            if (indicatorSystem == null) {
                // 流程模板已与指标体系解耦：无效/已删的 ID 直接清空，不再阻断保存
                template.setIndicatorSystemId(null);
                template.setIndicatorSystemName(null);
            } else {
                if (StringUtils.isBlank(template.getIndicatorSystemName())) {
                    template.setIndicatorSystemName(indicatorSystem.getSystemName());
                }
            }
        } else {
            template.setIndicatorSystemName(null);
            if (existing != null && StringUtils.isBlank(template.getEquipmentType())) {
                template.setEquipmentType(existing.getEquipmentType());
            }
        }
        if (StringUtils.isBlank(template.getCalcGranularity())) {
            template.setCalcGranularity(resolveCalcGranularity(template.getTaskType()));
        }
        if (existing != null) {
            if (template.getDataPlanId() == null) {
                template.setDataPlanId(existing.getDataPlanId());
            }
            if (StringUtils.isBlank(template.getDataPlanName())) {
                template.setDataPlanName(existing.getDataPlanName());
            }
        }
    }

    private String resolveCalcGranularity(String taskType) {
        if ("SYSTEM_TASK".equals(taskType)) {
            return "SYSTEM_TASK";
        }
        if ("PERFORMANCE_TEST".equals(taskType)) {
            return "PERFORMANCE";
        }
        return "EQUIP_EFFECTIVENESS";
    }

    private String incrementVersion(String versionNo) {
        if (StringUtils.isBlank(versionNo)) {
            return "V1.0";
        }
        try {
            String num = versionNo.replaceAll("[^0-9.]", "");
            String[] parts = num.split("\\.");
            int major = Integer.parseInt(parts[0]);
            int minor = parts.length > 1 ? Integer.parseInt(parts[1]) : 0;
            return "V" + major + "." + (minor + 1);
        } catch (Exception e) {
            return versionNo + ".1";
        }
    }

    private String buildDefaultConfigJson() {
        return "{\n"
                + "  \"stages\": {\n"
                + "    \"scheduleConfig\": {\n"
                + "      \"stageCode\": \"SCHEDULE_CONFIG\",\n"
                + "      \"stageName\": \"调度策略选配\",\n"
                + "      \"stageOrder\": 1,\n"
                + "      \"enabled\": true,\n"
                + "      \"config\": {\n"
                + "        \"routeStrategy\": \"FIRST\",\n"
                + "        \"executorName\": \"zhpgCalcHandler\",\n"
                + "        \"timeoutSeconds\": 300,\n"
                + "        \"retryTimes\": 0\n"
                + "      }\n"
                + "    },\n"
                + "    \"comprehensiveCalc\": {\n"
                + "      \"stageCode\": \"COMPREHENSIVE_CALC\",\n"
                + "      \"stageName\": \"comprehensiveCalc\",\n"
                + "      \"stageOrder\": 2,\n"
                + "      \"enabled\": true,\n"
                + "      \"config\": {\n"
                + "        \"conductionMethodOverride\": \"\",\n"
                + "        \"nullDataPolicy\": \"ZERO_FILL\",\n"
                + "        \"scoreLevelRule\": \"AUTO\",\n"
                + "        \"scoreLevelConfig\": {\n"
                + "          \"EXCELLENT\": 90,\n"
                + "          \"GOOD\": 75,\n"
                + "          \"PASS\": 60\n"
                + "        },\n"
                + "        \"intermediateResultOutput\": false,\n"
                + "        \"failStrategy\": \"MANUAL_CONFIRM\",\n"
                + "        \"timeoutSeconds\": 900\n"
                + "      }\n"
                + "    },\n"
                + "    \"reportOutput\": {\n"
                + "      \"stageCode\": \"REPORT_OUTPUT\",\n"
                + "      \"stageName\": \"reportOutput\",\n"
                + "      \"stageOrder\": 3,\n"
                + "      \"enabled\": true,\n"
                + "      \"config\": {\n"
                + "        \"reportTemplateId\": null,\n"
                + "        \"outputTargets\": [\"RESULT_DB\"],\n"
                + "        \"resultFormat\": \"STRUCTURED\",\n"
                + "        \"includeSubScores\": true,\n"
                + "        \"includeConclusion\": true,\n"
                + "        \"autoArchive\": false\n"
                + "      }\n"
                + "    }\n"
                + "  },\n"
                + "  \"runtimePolicy\": {\n"
                + "    \"blockStrategy\": \"SERIAL_EXECUTION\",\n"
                + "    \"misfireStrategy\": \"DO_NOTHING\"\n"
                + "  }\n"
                + "}";
    }

    private String normalizeConfigJson(String configJson) {
        JSONObject defaultRoot = JSON.parseObject(buildDefaultConfigJson());
        if (StringUtils.isBlank(configJson)) {
            return defaultRoot.toJSONString();
        }
        try {
            JSONObject root = JSON.parseObject(configJson);
            mergeObject(defaultRoot, root);
            removeWeightCalcStage(defaultRoot);

            JSONObject scheduleConfig = defaultRoot.getJSONObject("stages")
                    .getJSONObject("scheduleConfig")
                    .getJSONObject("config");
            // 设置默认执行器名称
            if (StringUtils.isBlank(scheduleConfig.getString("executorName"))) {
                scheduleConfig.put("executorName", DEFAULT_EXECUTOR_NAME);
            }
            return defaultRoot.toJSONString();
        } catch (Exception ex) {
            throw new ServiceException("configJson is invalid");
        }
    }

    private void removeWeightCalcStage(JSONObject root) {
        if (root == null) {
            return;
        }
        JSONObject stages = root.getJSONObject("stages");
        if (stages != null) {
            stages.remove("weightCalc");
        }
    }

    // ======================== xxl-job 注册 ========================

    @Override
    public int ensureXxlJobRegistered(CalcFlowTemplate template) {
        if (!xxlJobAdminClient.isAvailable()) {
            return 0;
        }
        XxlJobInfoRequest req = buildXxlJobRequest(template);
        if (template.getXxlJobId() != null && template.getXxlJobId() > 0) {
            // 已注册，同步更新路由策略等
            try {
                req.setId(template.getXxlJobId());
                xxlJobAdminClient.updateJob(req);
            } catch (Exception e) {
                log.warn("同步更新 xxl-job 失败, jobId={}: {}", template.getXxlJobId(), e.getMessage());
            }
            return template.getXxlJobId();
        }
        Integer existingJobId = findExistingJobId(req.getTaskName());
        if (existingJobId != null && existingJobId > 0) {
            template.setXxlJobId(existingJobId);
            baseMapper.updateById(template);
            try {
                req.setId(existingJobId);
                xxlJobAdminClient.updateJob(req);
            } catch (Exception e) {
                log.warn("回填已有 xxl-job 后同步更新失败, jobId={}: {}", existingJobId, e.getMessage());
            }
            log.info("模板 {} 复用已有 xxl-job, jobId={}", template.getTemplateCode(), existingJobId);
            return existingJobId;
        }
        // 新注册
        int jobId = xxlJobAdminClient.addJob(req);
        template.setXxlJobId(jobId);
        baseMapper.updateById(template);
        log.info("模板 {} 已注册 xxl-job, jobId={}", template.getTemplateCode(), jobId);
        return jobId;
    }

    @Override
    public void refreshXxlJobRuntimeMeta(CalcFlowTemplate template, String runtimeTaskName) {
        if (!xxlJobAdminClient.isAvailable() || template == null) {
            return;
        }
        int jobId = ensureXxlJobRegistered(template);
        if (jobId <= 0) {
            return;
        }
        XxlJobInfoRequest req = buildXxlJobRequest(template);
        req.setId(jobId);
        if (StringUtils.isNotBlank(runtimeTaskName)) {
            req.setJobDesc("[ZHPG] " + runtimeTaskName.trim());
        }
        xxlJobAdminClient.updateJob(req);
    }

    private void syncXxlJobSafe(CalcFlowTemplate template) {
        try {
            ensureXxlJobRegistered(template);
        } catch (Exception e) {
            log.warn("xxl-job 注册/同步失败（不阻断发布）: {}", e.getMessage());
        }
    }

    private XxlJobInfoRequest buildXxlJobRequest(CalcFlowTemplate template) {
        JSONObject root = JSON.parseObject(normalizeConfigJson(template.getConfigJson()));
        JSONObject schedCfg = root.getJSONObject("stages")
                .getJSONObject("scheduleConfig")
                .getJSONObject("config");
        JSONObject runtimePolicy = root.getJSONObject("runtimePolicy");

        String routeStrategy = schedCfg.getString("routeStrategy");
        if (StringUtils.isBlank(routeStrategy)) {
            routeStrategy = "FIRST";
        }
        String executorHandler = schedCfg.getString("executorName");
        if (StringUtils.isBlank(executorHandler)) {
            executorHandler = DEFAULT_EXECUTOR_NAME;
        }
        int timeout = schedCfg.getIntValue("timeoutSeconds");
        int retryTimes = schedCfg.getIntValue("retryTimes");

        String blockStrategy = runtimePolicy != null ? runtimePolicy.getString("blockStrategy") : null;
        if (StringUtils.isBlank(blockStrategy)) {
            blockStrategy = "SERIAL_EXECUTION";
        }
        String misfireStrategy = runtimePolicy != null ? runtimePolicy.getString("misfireStrategy") : null;
        if (StringUtils.isBlank(misfireStrategy)) {
            misfireStrategy = "DO_NOTHING";
        }

        XxlJobInfoRequest req = new XxlJobInfoRequest();
        req.setJobGroup(resolveJobGroup());
        req.setJobDesc("[ZHPG] " + template.getTemplateName());
        req.setAuthor("ZHPG");
        req.setScheduleType("NONE");
        req.setScheduleConf("");
        req.setMisfireStrategy(misfireStrategy);
        req.setExecutorRouteStrategy(routeStrategy);
        req.setExecutorHandler(executorHandler);
        req.setExecutorParam("");
        req.setExecutorBlockStrategy(blockStrategy);
        req.setExecutorTimeout(timeout);
        req.setExecutorFailRetryCount(retryTimes);
        req.setGlueType("BEAN");
        req.setGlueRemark("GLUE代码初始化");
        // task_name 在 admin 侧全局唯一校验；附带模板主键避免与历史/手工任务同 code 冲突
        req.setTaskName(template.getTemplateCode() + "_" + template.getId());
        return req;
    }

    private int resolveJobGroup() {
        try {
            Integer groupId = xxlJobAdminMapper.selectJobGroupIdByAppName(executorAppName);
            if (groupId != null && groupId > 0) {
                return groupId;
            }
        } catch (Exception e) {
            log.warn("跨 schema 查询 xxl_job_group 失败: {}", e.getMessage());
        }
        if (defaultJobGroup > 0) {
            return defaultJobGroup;
        }
        return 1;
    }

    private Integer findExistingJobId(String taskName) {
        if (StringUtils.isBlank(taskName)) {
            return null;
        }
        try {
            return xxlJobAdminMapper.selectJobIdByTaskName(taskName);
        } catch (Exception e) {
            log.warn("跨 schema 查询 xxl_job_info 失败: {}", e.getMessage());
            return null;
        }
    }

    private void mergeObject(JSONObject target, JSONObject source) {
        if (target == null || source == null) {
            return;
        }
        for (String key : source.keySet()) {
            Object sourceValue = source.get(key);
            Object targetValue = target.get(key);
            if (sourceValue instanceof JSONObject && targetValue instanceof JSONObject) {
                mergeObject((JSONObject) targetValue, (JSONObject) sourceValue);
            } else {
                target.put(key, sourceValue);
            }
        }
    }

}
