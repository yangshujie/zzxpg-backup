package com.ruoyi.service.impl.zhpg;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ruoyi.common.core.exception.ServiceException;
import com.ruoyi.common.core.utils.StringUtils;
import com.ruoyi.common.security.utils.SecurityUtils;
import com.ruoyi.domain.zhpg.CalcFlowExecution;
import com.ruoyi.domain.zhpg.CalcFlowTemplate;
import com.ruoyi.domain.zhpg.CalcTask;
import com.ruoyi.domain.zhpg.EvalIndicatorSystem;
import com.ruoyi.domain.zhpg.dto.CalcExecutionRequest;
import com.ruoyi.domain.zhpg.dto.CalcFlowExecutionInitRequest;
import com.ruoyi.domain.zhpg.dto.CalcFlowExecutionRunRequest;
import com.ruoyi.domain.zhpg.dto.CalcFlowExecutionSaveRequest;
import com.ruoyi.mapper.zhpg.CalcFlowExecutionMapper;
import com.ruoyi.service.zhpg.ICalcFlowExecutionService;
import com.ruoyi.service.zhpg.ICalcFlowTemplateService;
import com.ruoyi.service.zhpg.ICalcTaskService;
import com.ruoyi.service.zhpg.IEvalIndicatorSystemService;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
public class CalcFlowExecutionServiceImpl extends ServiceImpl<CalcFlowExecutionMapper, CalcFlowExecution>
        implements ICalcFlowExecutionService {

    private final ICalcFlowTemplateService templateService;
    private final ICalcTaskService calcTaskService;
    private final IEvalIndicatorSystemService indicatorSystemService;

    public CalcFlowExecutionServiceImpl(CalcFlowExecutionMapper baseMapper,
                                        ICalcFlowTemplateService templateService,
                                        ICalcTaskService calcTaskService,
                                        IEvalIndicatorSystemService indicatorSystemService) {
        this.baseMapper = baseMapper;
        this.templateService = templateService;
        this.calcTaskService = calcTaskService;
        this.indicatorSystemService = indicatorSystemService;
    }

    @Override
    public CalcFlowExecution initExecution(CalcFlowExecutionInitRequest request) {
        EvalIndicatorSystem indicatorSystem = resolveIndicatorSystem(request);
        CalcFlowExecution existing = latest(request.getTemplateId(), indicatorSystem.getId());
        if (existing != null) {
            return existing;
        }

        CalcFlowTemplate template = templateService.selectTemplateDetail(request.getTemplateId());
        if (template == null) {
            throw new ServiceException("calc flow template not found");
        }

        CalcFlowExecution execution = new CalcFlowExecution();
        execution.setExecutionCode("FLOW-" + System.currentTimeMillis());
        execution.setTemplateId(request.getTemplateId());
        execution.setIndicatorSystemId(indicatorSystem.getId());
        execution.setExecutionName(StringUtils.isNotBlank(request.getExecutionName())
                ? request.getExecutionName() : template.getTemplateName());
        execution.setRuntimeConfigJson(template.getConfigJson());
        execution.setTemplateSnapshotJson(template.getConfigJson());
        execution.setStepStateJson(defaultStepStateJson());
        execution.setCurrentStep("scheduleConfig");
        execution.setStatus("DRAFT");
        execution.setCreateBy(currentUsername());
        execution.setCreateTime(new Date());
        baseMapper.insert(execution);
        return execution;
    }

    @Override
    public CalcFlowExecution latest(Long templateId, Long indicatorSystemId) {
        if (templateId == null || indicatorSystemId == null) {
            return null;
        }
        QueryWrapper<CalcFlowExecution> wrapper = new QueryWrapper<>();
        wrapper.eq("template_id", templateId)
                .eq("indicator_system_id", indicatorSystemId)
                .orderByDesc("update_time", "create_time", "id")
                .last("limit 1");
        return baseMapper.selectOne(wrapper);
    }

    @Override
    public CalcFlowExecution saveRuntimeConfig(Long id, CalcFlowExecutionSaveRequest request) {
        CalcFlowExecution execution = baseMapper.selectById(id);
        if (execution == null) {
            throw new ServiceException("calc flow execution not found");
        }
        if (StringUtils.isNotBlank(request.getRuntimeConfigJson())) {
            execution.setRuntimeConfigJson(request.getRuntimeConfigJson());
        }
        if (StringUtils.isNotBlank(request.getStepStateJson())) {
            execution.setStepStateJson(request.getStepStateJson());
        }
        if (StringUtils.isNotBlank(request.getCurrentStep())) {
            execution.setCurrentStep(request.getCurrentStep());
        }
        if (StringUtils.isNotBlank(request.getStatus())) {
            execution.setStatus(request.getStatus());
        } else if ("DRAFT".equals(execution.getStatus())) {
            execution.setStatus("CONFIGURING");
        }
        if (request.getCalcTaskId() != null) {
            execution.setCalcTaskId(request.getCalcTaskId());
        }
        if (request.getEvalResultId() != null) {
            execution.setEvalResultId(request.getEvalResultId());
        }
        if (request.getLatestReportId() != null) {
            execution.setLatestReportId(request.getLatestReportId());
        }
        execution.setUpdateBy(currentUsername());
        execution.setUpdateTime(new Date());
        baseMapper.updateById(execution);
        return execution;
    }

    @Override
    public CalcTask runExecution(Long id, CalcFlowExecutionRunRequest request) {
        CalcFlowExecution execution = baseMapper.selectById(id);
        if (execution == null) {
            throw new ServiceException("calc flow execution not found");
        }

        CalcFlowExecutionSaveRequest saveRequest = new CalcFlowExecutionSaveRequest();
        saveRequest.setRuntimeConfigJson(request.getRuntimeConfigJson());
        saveRequest.setStepStateJson(request.getStepStateJson());
        saveRequest.setCurrentStep(StringUtils.isNotBlank(request.getCurrentStep())
                ? request.getCurrentStep() : "comprehensiveCalc");
        saveRequest.setStatus("CONFIGURING");
        execution = saveRuntimeConfig(id, saveRequest);

        CalcExecutionRequest calcRequest = new CalcExecutionRequest();
        calcRequest.setExecutionId(execution.getId());
        calcRequest.setCalcFlowTemplateId(execution.getTemplateId());
        calcRequest.setIndicatorSystemId(execution.getIndicatorSystemId());
        calcRequest.setTaskName(request.getTaskName());
        calcRequest.setRuntimeConfigJson(execution.getRuntimeConfigJson());
        calcRequest.setSkipWeightLog(request.getSkipWeightLog());

        CalcTask task = calcTaskService.run(calcRequest);
        execution.setCalcTaskId(task.getId());
        execution.setStatus("RUNNING");
        execution.setCurrentStep("comprehensiveCalc");
        execution.setUpdateBy(currentUsername());
        execution.setUpdateTime(new Date());
        baseMapper.updateById(execution);
        return task;
    }

    private String defaultStepStateJson() {
        return "{\"scheduleConfig\":{\"status\":\"pending\"},"
                + "\"weightCalc\":{\"status\":\"pending\"},"
                + "\"comprehensiveCalc\":{\"status\":\"pending\"},"
                + "\"reportOutput\":{\"status\":\"pending\"}}";
    }

    private EvalIndicatorSystem resolveIndicatorSystem(CalcFlowExecutionInitRequest request) {
        if (request.getIndicatorSystemId() != null) {
            EvalIndicatorSystem indicatorSystem = indicatorSystemService.getById(request.getIndicatorSystemId());
            if (indicatorSystem == null) {
                throw new ServiceException("indicator system not found");
            }
            if (request.getRequirementId() != null
                    && !request.getRequirementId().equals(indicatorSystem.getRequirementId())) {
                throw new ServiceException("所选指标体系与需求ID不匹配");
            }
            return indicatorSystem;
        }
        if (request.getRequirementId() == null) {
            throw new ServiceException("indicatorSystemId or requirementId is required");
        }
        EvalIndicatorSystem indicatorSystem = indicatorSystemService.getByRequirementId(request.getRequirementId());
        if (indicatorSystem == null) {
            throw new ServiceException("未找到与需求ID关联的指标体系");
        }
        request.setIndicatorSystemId(indicatorSystem.getId());
        return indicatorSystem;
    }

    private String currentUsername() {
        try {
            return SecurityUtils.getUsername();
        } catch (Exception ignored) {
            return null;
        }
    }
}
