package com.ruoyi.common.report;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 统一报告引擎
 * <p>
 * 封装完整的报告生成流水线：
 * <pre>
 * {{placeholder}} HTML 模板
 *   → PlaceholderService 占位符校验 / 转换（{{key}} → ${key!''}）
 *   → HtmlTemplateRenderer FreeMarker 渲染
 *   → [可选] injectChapterNumbering 中文章节编号注入
 *   → HtmlNormalizer Jsoup XHTML 清洗
 *   → DocxGenerator Aspose.Words DOCX 生成
 * </pre>
 *
 * 两条路径：
 * <ul>
 *   <li>{@link #generate(ReportEngineRequest)} — 全流水线，输入原始 HTML（含 {{}} 占位符）</li>
 *   <li>{@link #generateFromRenderedHtml(String)} — 部分流水线，输入已渲染完成的 HTML，直接清洗转 DOCX</li>
 * </ul>
 */
public class ReportEngine {

    private static final Logger log = LoggerFactory.getLogger(ReportEngine.class);

    private static final String[] CN_CHAPTER_NUMBERS = {
            "一", "二", "三", "四", "五", "六", "七", "八", "九", "十"
    };

    private final PlaceholderService placeholderService;
    private final HtmlTemplateRenderer htmlTemplateRenderer;
    private final HtmlNormalizer htmlNormalizer;
    private final DocxGenerator docxGenerator;

    public ReportEngine() {
        this.placeholderService = new PlaceholderService();
        this.htmlTemplateRenderer = new HtmlTemplateRenderer();
        this.htmlNormalizer = new HtmlNormalizer();
        this.docxGenerator = new DocxGenerator();
    }

    /**
     * 全流水线：输入原始 HTML（含 {{}} 占位符），输出 DOCX 字节。
     *
     * <p>执行步骤：
     * <ol>
     *   <li>占位符校验（语法/嵌套检查）</li>
     *   <li>{{placeholder}} → FreeMarker 模板转换</li>
     *   <li>FreeMarker 渲染 HTML</li>
     *   <li>可选：中文章节编号注入</li>
     *   <li>Jsoup XHTML 清洗</li>
     *   <li>docx4j XHTML → DOCX 转换</li>
     * </ol>
     *
     * @param request 引擎请求参数（模板内容 + 数据 + 选项）
     * @return 引擎执行结果（DOCX 字节 + 渲染后 HTML + 警告）
     */
    public ReportEngineResult generate(ReportEngineRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("request must not be null");
        }
        String htmlContent = request.getHtmlContent();
        String templateName = request.getTemplateName();
        Map<String, Object> dataModel = request.getDataModel();

        if (htmlContent == null || htmlContent.trim().isEmpty()) {
            throw new IllegalArgumentException("htmlContent must not be empty");
        }
        if (templateName == null) {
            templateName = "report";
        }
        if (dataModel == null) {
            dataModel = new java.util.HashMap<>();
        }

        List<String> warnings = new ArrayList<>();

        // 1. 占位符校验
        try {
            placeholderService.validateTemplateTokens(htmlContent);
        } catch (Exception e) {
            throw new RuntimeException("Template token validation failed: " + e.getMessage(), e);
        }

        // 2. {{placeholder}} → FreeMarker 模板
        String freemarkerTemplate;
        try {
            freemarkerTemplate = placeholderService.toFreemarkerTemplate(htmlContent);
        } catch (Exception e) {
            throw new RuntimeException("Template conversion to FreeMarker failed: " + e.getMessage(), e);
        }

        // 3. FreeMarker 渲染
        String html;
        try {
            html = htmlTemplateRenderer.render(templateName, freemarkerTemplate, dataModel);
        } catch (Exception e) {
            throw new RuntimeException("Template rendering failed: " + e.getMessage(), e);
        }

        // 4. 可选：中文章节编号注入
        if (request.isEnableChapterNumbering()) {
            try {
                html = injectChapterNumbering(html);
            } catch (Exception e) {
                log.warn("Chapter numbering injection failed, continuing without it: {}", e.getMessage());
            }
        }

        // 5. → XHTML → DOCX
        byte[] docxBytes = convertToDocx(html);

        return ReportEngineResult.builder()
                .docxBytes(docxBytes)
                .warnings(warnings)
                .renderedHtml(html)
                .build();
    }

    /**
     * 部分流水线：输入已渲染完成的 HTML，跳过占位符处理直接进行 XHTML 清洗和 DOCX 生成。
     * <p>
     * 适用于编辑确认后的 HTML 直接导出（前端编辑后提交的 editedHtml）。
     *
     * @param renderedHtml 已渲染完成的 HTML（不含 {{}} 占位符）
     * @return DOCX 文件字节
     */
    public byte[] generateFromRenderedHtml(String renderedHtml) {
        if (renderedHtml == null || renderedHtml.trim().isEmpty()) {
            throw new IllegalArgumentException("renderedHtml must not be empty");
        }
        return convertToDocx(renderedHtml);
    }

    // ========== 内部方法 ==========

    private byte[] convertToDocx(String html) {
        // XHTML 清洗
        String xhtml;
        try {
            xhtml = htmlNormalizer.toXhtml(html);
        } catch (Exception e) {
            throw new RuntimeException("HTML normalization to XHTML failed: " + e.getMessage(), e);
        }

        // XHTML → DOCX
        try {
            return docxGenerator.htmlToDocx(xhtml);
        } catch (Exception e) {
            throw new RuntimeException("DOCX generation failed: " + e.getMessage(), e);
        }
    }

    /**
     * 向 HTML 中的 {@code <h2>} 章节标题注入中文章节编号（一、二、三…）。
     * <p>
     * 如果标题已包含「一、」「第X部分」等编号前缀则跳过。
     * 支持 {@code <section>} 包裹结构和直接 {@code <h2>} 两种写法。
     */
    String injectChapterNumbering(String html) {
        if (html == null || html.isEmpty()) {
            return html;
        }

        Document doc = Jsoup.parse(html);
        Elements h2Elements = doc.select("h2");

        int chapterIndex = 0;
        for (Element h2 : h2Elements) {
            String text = h2.text().trim();

            // 跳过已有编号的标题
            if (hasNumberingPrefix(text)) {
                continue;
            }

            if (chapterIndex >= CN_CHAPTER_NUMBERS.length) {
                log.warn("Chapter count exceeds predefined Chinese number count ({})", CN_CHAPTER_NUMBERS.length);
                break;
            }

            h2.prependText(CN_CHAPTER_NUMBERS[chapterIndex] + "、");
            chapterIndex++;
        }

        doc.outputSettings()
                .syntax(Document.OutputSettings.Syntax.xml)
                .prettyPrint(false);
        return doc.outerHtml();
    }

    /**
     * 检查标题文本是否已包含编号前缀。
     */
    private boolean hasNumberingPrefix(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }
        // 中文数字 + 顿号：一、 二、 ...
        if (text.matches("^[一二三四五六七八九十]+[、.．]\\s*.*")) {
            return true;
        }
        // "第X章" "第X节" "第X部分"
        if (text.matches("^第[一二三四五六七八九十]+[章节部分篇]\\s*.*")) {
            return true;
        }
        // 阿拉伯数字：1.  1.1  1.1.1
        if (text.matches("^\\d+(\\.\\d+)*[、.．\\s]\\s*.*")) {
            return true;
        }
        return false;
    }
}
