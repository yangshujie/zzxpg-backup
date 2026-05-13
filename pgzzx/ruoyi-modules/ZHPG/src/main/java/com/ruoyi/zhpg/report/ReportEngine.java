package com.ruoyi.zhpg.report;

import com.ruoyi.common.report.DocxGenerator;
import com.ruoyi.common.report.HtmlNormalizer;
import com.ruoyi.common.report.HtmlTemplateRenderer;

import java.util.Map;

/**
 * 报告生成引擎
 */
public class ReportEngine {

    private final HtmlTemplateRenderer renderer = new HtmlTemplateRenderer();
    private final HtmlNormalizer normalizer = new HtmlNormalizer();
    private final DocxGenerator docxGenerator = new DocxGenerator();

    public byte[] generateDocx(String templateName, String templateHtml, Map<String, Object> data) {
        String renderedHtml = renderer.render(templateName, templateHtml, data);
        return generateDocxFromRenderedHtml(renderedHtml);
    }

    public byte[] generateDocxFromRenderedHtml(String renderedHtml) {
        String xhtml = normalizer.toXhtml(renderedHtml);
        return docxGenerator.htmlToDocx(xhtml);
    }
}
