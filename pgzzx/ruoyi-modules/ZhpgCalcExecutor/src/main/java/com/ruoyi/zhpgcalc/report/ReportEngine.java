package com.ruoyi.zhpgcalc.report;

import com.ruoyi.common.report.DocxGenerator;
import com.ruoyi.common.report.HtmlNormalizer;
import com.ruoyi.common.report.HtmlTemplateRenderer;
import com.ruoyi.common.report.PlaceholderService;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

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
        renderedHtml = injectChapterNumbering(renderedHtml);

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

    private String injectChapterNumbering(String renderedHtml) {
        Document doc = Jsoup.parseBodyFragment(renderedHtml == null ? "" : renderedHtml);
        String[] chineseNumbers = {"一、", "二、", "三、", "四、", "五、", "六、", "七、", "八、", "九、", "十、"};
        int index = 0;
        for (Element section : doc.select("section")) {
            Element heading = null;
            for (Element child : section.children()) {
                if ("h2".equalsIgnoreCase(child.tagName())) {
                    heading = child;
                    break;
                }
                if (!child.tagName().matches("(?i)div|p")) {
                    break;
                }
            }
            if (heading == null) {
                continue;
            }
            String text = heading.text().trim();
            if (text.matches("^[一二三四五六七八九十]+、.*") || text.matches("^\\d+(\\.\\d+)*\\s+.*")) {
                continue;
            }
            String prefix = index < chineseNumbers.length ? chineseNumbers[index] : "第" + (index + 1) + "、";
            heading.text(prefix + text);
            index++;
        }
        doc.outputSettings().prettyPrint(false);
        return doc.body().html();
    }
}
