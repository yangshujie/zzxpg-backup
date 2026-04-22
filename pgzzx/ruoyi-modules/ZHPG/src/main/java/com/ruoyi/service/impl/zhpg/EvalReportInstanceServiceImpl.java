package com.ruoyi.service.impl.zhpg;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.aspose.words.Document;
import com.aspose.words.License;
import com.aspose.words.SaveFormat;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ruoyi.common.core.constant.HttpStatus;
import com.ruoyi.common.core.exception.ServiceException;
import com.ruoyi.common.core.web.domain.AjaxResult;
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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class EvalReportInstanceServiceImpl extends ServiceImpl<EvalReportInstanceMapper, EvalReportInstance>
        implements IEvalReportInstanceService {

    private static volatile boolean ASPOSE_LICENSE_LOADED = false;

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
        if (result == null || (result.getDelFlag() != null && result.getDelFlag() == 1)) {
            throw new ServiceException("评估结果不存在");
        }

        ReportTemplate template = reportTemplateService.getById(request.getReportTemplateId());
        if (template == null || (template.getDeleted() != null && template.getDeleted() == 1)) {
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
            byte[] pdfBytes = convertWordToPdf(docx.getBytes());
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
        report.setDelFlag(0);
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
                .eq("del_flag", 0)
                .orderByDesc("generation_no")
                .orderByDesc("create_time");
        return reportInstanceMapper.selectList(wrapper);
    }

    @Override
    public Map<String, Object> getReportLinks(Long reportId) {
        EvalReportInstance report = reportInstanceMapper.selectById(reportId);
        if (report == null || (report.getDelFlag() != null && report.getDelFlag() == 1)) {
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

    private int nextGenerationNo(Long evalResultId) {
        QueryWrapper<EvalReportInstance> wrapper = new QueryWrapper<>();
        wrapper.eq("eval_result_id", evalResultId).eq("del_flag", 0);
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
        fields.put("dimensions", result.getDimensionList());
        if (request.getFields() != null) {
            fields.putAll(request.getFields());
        }
        return fields;
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

    private byte[] convertWordToPdf(byte[] wordBytes) {
        try {
            ensureAsposeLicense();
            try (ByteArrayInputStream input = new ByteArrayInputStream(wordBytes);
                 ByteArrayOutputStream output = new ByteArrayOutputStream()) {
                Document document = new Document(input);
                document.save(output, SaveFormat.PDF);
                return output.toByteArray();
            }
        } catch (Exception e) {
            throw new RuntimeException("Word转PDF失败: " + e.getMessage(), e);
        }
    }

    private void ensureAsposeLicense() {
        if (ASPOSE_LICENSE_LOADED) {
            return;
        }
        synchronized (EvalReportInstanceServiceImpl.class) {
            if (ASPOSE_LICENSE_LOADED) {
                return;
            }
            String xml = "<License>\n"
                    + "    <Data>\n"
                    + "        <Products>\n"
                    + "            <Product>Aspose.Total for Java</Product>\n"
                    + "            <Product>Aspose.Words for Java</Product>\n"
                    + "        </Products>\n"
                    + "        <EditionType>Enterprise</EditionType>\n"
                    + "        <SubscriptionExpiry>20991231</SubscriptionExpiry>\n"
                    + "        <LicenseExpiry>20991231</LicenseExpiry>\n"
                    + "        <SerialNumber>8bfe198c-7f0c-4ef8-8ff0-acc3237bf0d7</SerialNumber>\n"
                    + "    </Data>\n"
                    + "    <Signature>\n"
                    + "        sNLLKGMUdF0r8O1kKilWAGdgfs2BvJb/2Xp8p5iuDVfZXmhppo+d0Ran1P9TKdjV4ABwAgKXxJ3jcQTqE/2IRfqwnPf8itN8aFZlV3TJPYeD3yWE7IT55Gz6EijUpC7aKeoohTb4w2fpox58wWoF3SNp6sK6jDfiAUGEHYJ9pjU=\n"
                    + "    </Signature>\n"
                    + "</License>";
            try (InputStream inputStream = new ByteArrayInputStream(xml.getBytes())) {
                License license = new License();
                license.setLicense(inputStream);
            } catch (Exception e) {
                log.warn("Aspose 许可加载失败: {}", e.getMessage());
            }
            ASPOSE_LICENSE_LOADED = true;
        }
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
