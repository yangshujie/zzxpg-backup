package com.ruoyi.service.impl.zhpg;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ruoyi.common.core.constant.HttpStatus;
import com.ruoyi.common.core.exception.ServiceException;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.report.ChartRenderer;
import com.ruoyi.common.report.IndicatorReportSectionBuilder;
import com.ruoyi.common.report.PdfConverter;
import com.ruoyi.common.security.utils.SecurityUtils;
import com.ruoyi.domain.zhpg.EvalReportInstance;
import com.ruoyi.domain.zhpg.EvalResult;
import com.ruoyi.domain.zhpg.ReportTemplate;
import com.ruoyi.domain.zhpg.dto.EvalReportGenerateRequest;
import com.ruoyi.domain.zhpg.dto.ReportDownloadData;
import com.ruoyi.domain.zhpg.dto.ReportTemplateRenderRequest;
import com.ruoyi.mapper.zhpg.EvalReportInstanceMapper;
import com.ruoyi.mapper.zhpg.EvalResultMapper;
import com.ruoyi.service.zhpg.IEvalReportInstanceService;
import com.ruoyi.service.zhpg.IReportTemplateService;
import com.ruoyi.system.api.RemoteFileService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
public class EvalReportInstanceServiceImpl extends ServiceImpl<EvalReportInstanceMapper, EvalReportInstance>
        implements IEvalReportInstanceService {

    private final ChartRenderer chartRenderer = new ChartRenderer();

    private final EvalReportInstanceMapper reportInstanceMapper;
    private final EvalResultMapper evalResultMapper;
    private final IReportTemplateService reportTemplateService;
    private final EvalReportFileStorageService fileStorageService;
    private final RemoteFileService remoteFileService;

    public EvalReportInstanceServiceImpl(EvalReportInstanceMapper reportInstanceMapper,
                                         EvalResultMapper evalResultMapper,
                                         IReportTemplateService reportTemplateService,
                                         EvalReportFileStorageService fileStorageService,
                                         RemoteFileService remoteFileService) {
        this.reportInstanceMapper = reportInstanceMapper;
        this.evalResultMapper = evalResultMapper;
        this.reportTemplateService = reportTemplateService;
        this.fileStorageService = fileStorageService;
        this.remoteFileService = remoteFileService;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public EvalReportInstance generateForResult(Long evalResultId, EvalReportGenerateRequest request) {
        if (evalResultId == null) {
            throw new ServiceException("evalResultId is required");
        }
        if (request == null || request.getReportTemplateId() == null) {
            throw new ServiceException("reportTemplateId is required");
        }

        EvalResult result = evalResultMapper.selectById(evalResultId);
        if (result == null) {
            throw new ServiceException("评估结果不存在");
        }

        ReportTemplate template = reportTemplateService.getById(request.getReportTemplateId());
        if (template == null) {
            throw new ServiceException("报告模板不存在");
        }

        int generationNo = nextGenerationNo(evalResultId);
        String resultCode = StringUtils.defaultIfBlank(result.getResultCode(), "RES-" + evalResultId);
        String reportCode = resultCode + "_v" + generationNo;

        ReportTemplateRenderRequest renderRequest = new ReportTemplateRenderRequest();
        renderRequest.setFields(buildRenderFields(result, request));
        renderRequest.setEditedHtml(request.getEditedHtml());
        ReportDownloadData docx = reportTemplateService.renderDocxDownload(template.getId(), renderRequest);
        String wordUrl = fileStorageService.uploadEvalReport(
                result.getTaskId() == null ? 0L : result.getTaskId(),
                reportCode,
                "docx",
                docx.getBytes()
        );

        // 生成 PDF 并上传
        String pdfUrl = null;
        try {
            byte[] pdfBytes = PdfConverter.getInstance().wordToPdf(docx.getBytes());
            pdfUrl = fileStorageService.uploadEvalReport(
                    result.getTaskId() == null ? 0L : result.getTaskId(),
                    reportCode,
                    "pdf",
                    pdfBytes
            );
        } catch (Exception e) {
            log.error("Word转PDF失败: {}", e.getMessage(), e);
        }

        EvalReportInstance report = new EvalReportInstance();
        report.setEvalResultId(evalResultId);
        report.setCalcTaskId(result.getTaskId());
        report.setReportCode(reportCode);
        report.setGenerationNo(generationNo);
        report.setReportTemplateId(template.getId());
        report.setReportTemplateName(template.getTemplateName());
        report.setMappingJson(buildMappingJson(request, renderRequest.getFields()));
        report.setRenderStatus("SUCCESS");
        // reportUrl 设为 PDF URL，用于浏览器预览
        report.setReportUrl(pdfUrl != null ? pdfUrl : wordUrl);
        report.setWordUrl(wordUrl);
        report.setPdfUrl(pdfUrl);
        report.setFileFormat("DOCX");

        report.setCreateBy(safeUsername());
        report.setCreateTime(new Date());
        reportInstanceMapper.insert(report);

        updateLatestReportOnEvalResult(result, report);
        return report;
    }

    @Override
    public List<EvalReportInstance> listByResult(Long evalResultId) {
        QueryWrapper<EvalReportInstance> wrapper = new QueryWrapper<>();
        wrapper.eq("eval_result_id", evalResultId)
                .orderByDesc("generation_no")
                .orderByDesc("create_time");
        return reportInstanceMapper.selectList(wrapper);
    }

    @Override
    public Page<EvalReportInstance> selectReportPage(Page<EvalReportInstance> page, EvalReportInstance query) {
        QueryWrapper<EvalReportInstance> wrapper = new QueryWrapper<>();
        if (query != null) {
            Set<Long> filteredEvalResultIds = findEvalResultIdsForReportQuery(query);
            if (filteredEvalResultIds != null && filteredEvalResultIds.isEmpty()) {
                page.setRecords(Collections.emptyList());
                page.setTotal(0);
                return page;
            }
            wrapper.like(StringUtils.isNotBlank(query.getReportCode()), "report_code", query.getReportCode())
                    .like(StringUtils.isNotBlank(query.getReportTemplateName()), "report_template_name", query.getReportTemplateName())
                    .eq(StringUtils.isNotBlank(query.getRenderStatus()), "render_status", query.getRenderStatus())
                    .eq(query.getEvalResultId() != null, "eval_result_id", query.getEvalResultId())
                    .eq(query.getCalcTaskId() != null, "calc_task_id", query.getCalcTaskId())
                    .in(filteredEvalResultIds != null, "eval_result_id", filteredEvalResultIds);
        }
        wrapper.orderByDesc("generation_no")
                .orderByDesc("create_time");
        Page<EvalReportInstance> result = reportInstanceMapper.selectPage(page, wrapper);
        enrichEvalResultFields(result.getRecords());
        return result;
    }

    private Set<Long> findEvalResultIdsForReportQuery(EvalReportInstance query) {
        boolean hasEvalResultFilter = StringUtils.isNotBlank(query.getEvalResultCode())
                || StringUtils.isNotBlank(query.getTaskName())
                || StringUtils.isNotBlank(query.getIndicatorSystemName());
        if (!hasEvalResultFilter) {
            return null;
        }
        QueryWrapper<EvalResult> wrapper = new QueryWrapper<>();
        wrapper.like(StringUtils.isNotBlank(query.getEvalResultCode()), "result_code", query.getEvalResultCode())
                .like(StringUtils.isNotBlank(query.getTaskName()), "task_name", query.getTaskName())
                .like(StringUtils.isNotBlank(query.getIndicatorSystemName()), "indicator_system_name", query.getIndicatorSystemName());
        return evalResultMapper.selectList(wrapper).stream()
                .map(EvalResult::getId)
                .collect(Collectors.toSet());
    }

    private void enrichEvalResultFields(List<EvalReportInstance> reports) {
        if (reports == null || reports.isEmpty()) {
            return;
        }
        List<Long> evalResultIds = reports.stream()
                .map(EvalReportInstance::getEvalResultId)
                .filter(id -> id != null)
                .distinct()
                .collect(Collectors.toList());
        if (evalResultIds.isEmpty()) {
            return;
        }
        Map<Long, EvalResult> resultMap = evalResultMapper.selectBatchIds(evalResultIds).stream()
                .collect(Collectors.toMap(EvalResult::getId, Function.identity(), (a, b) -> a));
        reports.forEach(report -> {
            EvalResult result = resultMap.get(report.getEvalResultId());
            if (result == null) {
                return;
            }
            report.setEvalResultCode(result.getResultCode());
            report.setTaskName(result.getTaskName());
            report.setIndicatorSystemName(result.getIndicatorSystemName());
            report.setScore(result.getScore());
            report.setGrade(result.getGrade());
        });
    }

    @Override
    public Map<String, Object> getReportLinks(Long reportId) {
        EvalReportInstance report = reportInstanceMapper.selectById(reportId);
        if (report == null) {
            throw new ServiceException("报告不存在");
        }
        Map<String, Object> links = new HashMap<>();
        links.put("reportUrl", report.getReportUrl());
        links.put("wordUrl", report.getWordUrl());
        links.put("pdfUrl", report.getPdfUrl());
        String previewTarget = StringUtils.isNotBlank(report.getPdfUrl()) ? report.getPdfUrl() : report.getReportUrl();
        if (remoteFileService != null && StringUtils.isNotBlank(previewTarget)) {
            AjaxResult fileResult = remoteFileService.getPreviewUrl(previewTarget);
            if (isFileServiceAjaxSuccess(fileResult)) {
                links.put("previewUrl", fileResult.get(AjaxResult.DATA_TAG));
            }
        }
        return links;
    }

    @Override
    public int deleteReportByIds(Long[] ids) {
        if (ids == null || ids.length == 0) {
            return 0;
        }
        return reportInstanceMapper.deleteBatchIds(Arrays.asList(ids));
    }

    private int nextGenerationNo(Long evalResultId) {
        QueryWrapper<EvalReportInstance> wrapper = new QueryWrapper<>();
        wrapper.eq("eval_result_id", evalResultId);
        Long count = reportInstanceMapper.selectCount(wrapper);
        return (count == null ? 0 : count.intValue()) + 1;
    }

    private Map<String, Object> buildRenderFields(EvalResult result, EvalReportGenerateRequest request) {
        Map<String, Object> fields = new HashMap<>();
        fields.put("taskName", result.getTaskName());
        fields.put("score", result.getScore());
        fields.put("grade", result.getGrade());
        fields.put("conclusion", result.getConclusion());
        fields.put("suggestion", result.getSuggestion());
        fields.put("indicatorSystemName", result.getIndicatorSystemName());
        List<EvalResult.DimensionScore> dimensions = resolveDimensions(result);
        fields.put("dimensions", dimensions);
        List<Map<String, Object>> chartDimensions = toChartDimensions(dimensions);
        String radarChart = renderRadarChart(chartDimensions);
        fields.put("CapabilityRadarChart", radarChart);
        fields.put("capabilityRadarChart", radarChart);
        if (request.getFields() != null) {
            fields.putAll(request.getFields());
        }
        if (isBlankField(fields.get("CapabilityRadarChart"))) {
            fields.put("CapabilityRadarChart", radarChart);
        }
        if (isBlankField(fields.get("capabilityRadarChart"))) {
            fields.put("capabilityRadarChart", fields.get("CapabilityRadarChart"));
        }
        Object scoredIndicatorTree = findScoredIndicatorTree(result, fields);
        List<IndicatorReportSectionBuilder.Section> indicatorSections =
                IndicatorReportSectionBuilder.buildSections(scoredIndicatorTree);
        enrichSectionCharts(indicatorSections);
        fields.put("IndicatorSections", indicatorSections);
        fields.put("indicatorSections", indicatorSections);
        return fields;
    }

    private List<Map<String, Object>> toChartDimensions(List<EvalResult.DimensionScore> dimensions) {
        List<Map<String, Object>> values = new ArrayList<>();
        if (dimensions == null) {
            return values;
        }
        for (EvalResult.DimensionScore dimension : dimensions) {
            if (dimension == null) {
                continue;
            }
            Map<String, Object> row = new HashMap<>();
            row.put("label", dimension.getLabel());
            row.put("score", dimension.getValue());
            row.put("value", dimension.getValue());
            row.put("tone", dimension.getTone());
            values.add(row);
        }
        return values;
    }

    private List<EvalResult.DimensionScore> resolveDimensions(EvalResult result) {
        if (result.getDimensionList() != null && !result.getDimensionList().isEmpty()) {
            return result.getDimensionList();
        }
        if (StringUtils.isBlank(result.getDimensions())) {
            return Collections.emptyList();
        }
        try {
            return JSON.parseArray(result.getDimensions(), EvalResult.DimensionScore.class);
        } catch (Exception e) {
            log.warn("解析评估结果维度得分失败: resultId={}, error={}", result.getId(), e.getMessage());
            return Collections.emptyList();
        }
    }

    private boolean isBlankField(Object value) {
        return value == null || StringUtils.isBlank(String.valueOf(value));
    }

    private Object findScoredIndicatorTree(EvalResult result, Map<String, Object> fields) {
        Object fromFields = fields.get("scoredIndicatorTree");
        if (fromFields == null) {
            fromFields = fields.get("ScoredIndicatorTree");
        }
        if (fromFields == null) {
            fromFields = fields.get("indicatorTree");
        }
        if (fromFields != null) {
            return fromFields;
        }
        if (StringUtils.isBlank(result.getReportPayloadJson())) {
            return null;
        }
        try {
            JSONObject payload = JSON.parseObject(result.getReportPayloadJson());
            Object tree = payload.get("scoredIndicatorTree");
            if (tree == null) {
                tree = payload.get("ScoredIndicatorTree");
            }
            if (tree == null) {
                tree = payload.get("indicatorTree");
            }
            return tree;
        } catch (Exception e) {
            log.warn("解析评估结果报告载荷失败: resultId={}, error={}", result.getId(), e.getMessage());
            return null;
        }
    }

    private String renderRadarChart(List<Map<String, Object>> dimensions) {
        if (dimensions == null || dimensions.isEmpty()) {
            return "";
        }
        try {
            return chartRenderer.renderRadar(dimensions);
        } catch (Exception e) {
            log.warn("渲染能力雷达图失败: {}", e.getMessage());
            return "";
        }
    }

    private void enrichSectionCharts(List<IndicatorReportSectionBuilder.Section> sections) {
        if (sections == null || sections.isEmpty()) {
            return;
        }
        for (IndicatorReportSectionBuilder.Section section : sections) {
            if (section != null && section.isLeaf()) {
                try {
                    BigDecimal evalValue = section.getEvalValue() != null ? section.getEvalValue() : section.getScore();
                    section.setChartImg(chartRenderer.renderIndicatorBar(
                            section.getTitle(), evalValue, section.getReferenceValue()));
                } catch (Exception e) {
                    log.warn("渲染指标图表失败: title={}, error={}", section.getTitle(), e.getMessage());
                }
            }
        }
    }

    private String buildMappingJson(EvalReportGenerateRequest request, Map<String, Object> fields) {
        JSONObject payload = new JSONObject();
        payload.put("mappings", request.getMappings());
        payload.put("fields", fields);
        return JSON.toJSONString(payload);
    }

    private void updateLatestReportOnEvalResult(EvalResult result, EvalReportInstance report) {
        EvalResult update = new EvalResult();
        update.setId(result.getId());
        update.setReportUrl(report.getReportUrl());
        JSONObject payload = new JSONObject();
        payload.put("latestReportId", report.getId());
        payload.put("latestReportCode", report.getReportCode());
        payload.put("wordUrl", report.getWordUrl());
        payload.put("pdfUrl", report.getPdfUrl());
        payload.put("reportUrl", report.getReportUrl());
        payload.put("reportTemplateId", report.getReportTemplateId());
        payload.put("reportTemplateName", report.getReportTemplateName());
        update.setReportPayloadJson(payload.toJSONString());
        update.setUpdateBy(safeUsername());
        update.setUpdateTime(new Date());
        evalResultMapper.updateById(update);
    }

    private static boolean isFileServiceAjaxSuccess(AjaxResult result) {
        if (result == null) {
            return false;
        }
        Object code = result.get(AjaxResult.CODE_TAG);
        return code instanceof Number && ((Number) code).intValue() == HttpStatus.SUCCESS;
    }

    private String safeUsername() {
        try {
            return SecurityUtils.getUsername();
        } catch (Exception e) {
            return "system";
        }
    }
}
