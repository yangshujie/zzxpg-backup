package com.ruoyi.zhpgcalc.report;

import lombok.extern.slf4j.Slf4j;

import java.util.Map;

/**
 * 报告生成引擎
 * 组合模板渲染、HTML规范化和DOCX生成功能
 */
@Slf4j
public class ReportEngine {

    private final HtmlTemplateRenderer renderer = new HtmlTemplateRenderer();
    private final HtmlNormalizer normalizer = new HtmlNormalizer();
    private final DocxGenerator docxGenerator = new DocxGenerator();
    private final PlaceholderService placeholderService = new PlaceholderService();

    /**
     * 生成DOCX报告
     *
     * @param templateName 模板名称
     * @param templateHtml 模板HTML内容（使用自定义占位符语法）
     * @param data         数据模型
     * @return DOCX字节数组
     */
    public byte[] generateDocx(String templateName, String templateHtml, Map<String, Object> data) {
        // 1. 验证模板
        placeholderService.validateTemplateTokens(templateHtml);

        // 2. 转换为Freemarker模板
        String freemarkerTemplate = placeholderService.toFreemarkerTemplate(templateHtml);
        log.debug("转换后的Freemarker模板: {}", freemarkerTemplate.substring(0, Math.min(200, freemarkerTemplate.length())));

        // 3. 渲染模板
        String renderedHtml = renderer.render(templateName, freemarkerTemplate, data);

        // 4. 生成DOCX
        return generateDocxFromRenderedHtml(renderedHtml);
    }

    /**
     * 从已渲染的HTML生成DOCX
     *
     * @param renderedHtml 已渲染的HTML
     * @return DOCX字节数组
     */
    public byte[] generateDocxFromRenderedHtml(String renderedHtml) {
        // 1. 转换为XHTML
        String xhtml = normalizer.toXhtml(renderedHtml);

        // 2. 生成DOCX
        return docxGenerator.htmlToDocx(xhtml);
    }

    /**
     * 提取模板中的占位符
     */
    public java.util.List<String> extractPlaceholders(String templateHtml) {
        return placeholderService.extractPlaceholders(templateHtml);
    }
}
