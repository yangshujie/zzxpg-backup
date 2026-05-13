package com.ruoyi.service.impl.zhpg;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ruoyi.common.core.exception.ServiceException;
import com.ruoyi.common.report.HtmlTemplateRenderer;
import com.ruoyi.common.report.PlaceholderService;
import com.ruoyi.common.security.utils.SecurityUtils;
import com.ruoyi.domain.zhpg.ReportTemplate;
import com.ruoyi.domain.zhpg.dto.*;
import com.ruoyi.mapper.zhpg.ReportTemplateMapper;
import com.ruoyi.zhpg.report.ReportEngine;
import com.ruoyi.service.zhpg.IReportTemplateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 报告模板服务实现（简化版：HTML直接存主表，无版本管理）
 */
@Service
public class ReportTemplateServiceImpl extends ServiceImpl<ReportTemplateMapper, ReportTemplate> implements IReportTemplateService {

    private static final Pattern VARIABLE_TOKEN_PATTERN = Pattern.compile("\\{\\{\\s*([a-zA-Z0-9_\\.]+)\\s*\\}\\}");

    @Autowired
    private PlaceholderService placeholderService;

    private final HtmlTemplateRenderer htmlTemplateRenderer = new HtmlTemplateRenderer();

    @Override
    @Transactional
    public ReportTemplateDetailResponse createTemplate(ReportTemplateCreateRequest request) {
        placeholderService.validateTemplateTokens(request.getHtmlContent());

        Date now = new Date();
        ReportTemplate template = new ReportTemplate();
        template.setTemplateName(request.getTemplateName());
        template.setTemplateDescription(request.getTemplateDescription());
        template.setEvaluationType(request.getEvaluationType());
        template.setHtmlContent(request.getHtmlContent());

        template.setCreateBy(SecurityUtils.getUsername());
        template.setCreateTime(now);
        template.setUpdateTime(now);

        this.save(template);

        return getDetail(template.getId());
    }

    @Override
    public List<ReportTemplate> listTemplates() {
        LambdaQueryWrapper<ReportTemplate> query = new LambdaQueryWrapper<>();
        query.orderByDesc(ReportTemplate::getUpdateTime);
        return this.list(query);
    }

    @Override
    public ReportTemplateDetailResponse getDetail(Long templateId) {
        ReportTemplate template = findTemplate(templateId);

        ReportTemplateDetailResponse response = new ReportTemplateDetailResponse();
        response.setId(template.getId());
        response.setTemplateName(template.getTemplateName());
        response.setTemplateDescription(template.getTemplateDescription());
        response.setEvaluationType(template.getEvaluationType());
        response.setHtmlContent(template.getHtmlContent());
        response.setPlaceholders(placeholderService.extractPlaceholders(template.getHtmlContent()));
        response.setCreateTime(template.getCreateTime());
        response.setUpdateTime(template.getUpdateTime());
        return response;
    }

    @Override
    @Transactional
    public ReportTemplateDetailResponse updateMeta(Long templateId, ReportTemplateUpdateRequest request) {
        ReportTemplate template = findTemplate(templateId);
        template.setTemplateName(request.getTemplateName());
        template.setTemplateDescription(request.getTemplateDescription());
        template.setEvaluationType(request.getEvaluationType());
        template.setUpdateBy(SecurityUtils.getUsername());
        template.setUpdateTime(new Date());
        this.updateById(template);
        return getDetail(templateId);
    }

    @Override
    @Transactional
    public void deleteTemplate(Long templateId) {
        this.removeById(templateId);
    }

    @Override
    @Transactional
    public ReportTemplateDetailResponse updateTemplateContent(Long templateId, String htmlContent) {
        ReportTemplate template = findTemplate(templateId);
        placeholderService.validateTemplateTokens(htmlContent);

        template.setHtmlContent(htmlContent);
        template.setUpdateBy(SecurityUtils.getUsername());
        template.setUpdateTime(new Date());
        this.updateById(template);

        return getDetail(templateId);
    }

    @Override
    public Map<String, Object> validateTemplate(ReportTemplateValidateRequest request) {
        placeholderService.validateTemplateTokens(request.getHtmlContent());
        String freemarkerTemplate = placeholderService.toFreemarkerTemplate(request.getHtmlContent());
        htmlTemplateRenderer.validateTemplate("template-validate", freemarkerTemplate);
        Map<String, Object> result = new HashMap<>();
        result.put("valid", true);
        result.put("placeholders", placeholderService.extractPlaceholders(request.getHtmlContent()));
        return result;
    }

    @Override
    public String preview(Long templateId, ReportTemplateRenderRequest request) {
        ReportTemplate template = findTemplate(templateId);
        return renderPreviewHtml(String.valueOf(templateId), template.getHtmlContent(), request.getFields());
    }

    @Override
    public ReportDownloadData renderHtmlDownload(Long templateId, ReportTemplateRenderRequest request) {
        ReportTemplate template = findTemplate(templateId);
        String renderedHtml;
        String fileName;
        if (hasEditedHtml(request)) {
            renderedHtml = request.getEditedHtml();
            fileName = templateId + "-edited.html";
        } else {
            renderedHtml = renderHtml(String.valueOf(templateId), template.getHtmlContent(), request.getFields());
            fileName = templateId + ".html";
        }
        return new ReportDownloadData(fileName, "text/html;charset=UTF-8", renderedHtml.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public ReportDownloadData renderDocxDownload(Long templateId, ReportTemplateRenderRequest request) {
        ReportTemplate template = findTemplate(templateId);
        String freemarkerTemplate = placeholderService.toFreemarkerTemplate(template.getHtmlContent());
        try {
            ReportEngine engine = new ReportEngine();
            byte[] docxBytes;
            String fileName;
            if (hasEditedHtml(request)) {
                docxBytes = engine.generateDocxFromRenderedHtml(request.getEditedHtml());
                fileName = templateId + "-edited.docx";
            } else {
                docxBytes = engine.generateDocx(String.valueOf(templateId), freemarkerTemplate,
                        request.getFields() == null ? new HashMap<>() : request.getFields());
                fileName = templateId + ".docx";
            }
            return new ReportDownloadData(fileName, "application/vnd.openxmlformats-officedocument.wordprocessingml.document", docxBytes);
        } catch (Exception e) {
            throw new ServiceException("DOCX导出失败: " + e.getMessage());
        }
    }

    private String renderHtml(String templateName, String htmlContent, Map<String, Object> fields) {
        String freemarkerTemplate = placeholderService.toFreemarkerTemplate(htmlContent);
        try {
            return htmlTemplateRenderer.render(templateName, freemarkerTemplate,
                    fields == null ? new HashMap<>() : fields);
        } catch (Exception e) {
            throw new ServiceException("模板渲染失败: " + e.getMessage());
        }
    }

    private String renderPreviewHtml(String templateName, String htmlContent, Map<String, Object> fields) {
        String previewHtmlContent = buildPreviewEditableHtmlContent(htmlContent);
        String freemarkerTemplate = placeholderService.toFreemarkerTemplate(previewHtmlContent);
        try {
            return htmlTemplateRenderer.render(templateName + "-preview", freemarkerTemplate,
                    fields == null ? new HashMap<>() : fields);
        } catch (Exception e) {
            throw new ServiceException("预览渲染失败: " + e.getMessage());
        }
    }

    private String buildPreviewEditableHtmlContent(String htmlContent) {
        Matcher matcher = VARIABLE_TOKEN_PATTERN.matcher(htmlContent == null ? "" : htmlContent);
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            String key = matcher.group(1);
            if (key.startsWith("item.")) {
                matcher.appendReplacement(buffer, Matcher.quoteReplacement(matcher.group(0)));
                continue;
            }
            String replacement = "<span data-field-key=\"" + key + "\">{{" + key + "}}</span>";
            matcher.appendReplacement(buffer, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(buffer);
        return buffer.toString();
    }

    private ReportTemplate findTemplate(Long templateId) {
        ReportTemplate template = this.getById(templateId);
        if (template == null) {
            throw new ServiceException("模板不存在");
        }
        return template;
    }

    private boolean hasEditedHtml(ReportTemplateRenderRequest request) {
        return request != null && request.getEditedHtml() != null && !request.getEditedHtml().trim().isEmpty();
    }
}
