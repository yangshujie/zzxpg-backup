package com.ruoyi.zhpgcalc.report;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateExceptionHandler;
import lombok.extern.slf4j.Slf4j;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.Map;

/**
 * HTML 模板渲染器
 * 使用Freemarker渲染模板
 */
@Slf4j
public class HtmlTemplateRenderer {

    private final Configuration configuration;

    public HtmlTemplateRenderer() {
        this.configuration = new Configuration(Configuration.VERSION_2_3_34);
        this.configuration.setDefaultEncoding("UTF-8");
        this.configuration.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
        this.configuration.setLogTemplateExceptions(false);
        this.configuration.setWrapUncheckedExceptions(true);
        this.configuration.setNumberFormat("0.######");
        this.configuration.setDateFormat("yyyy-MM-dd");
        this.configuration.setDateTimeFormat("yyyy-MM-dd HH:mm:ss");
    }

    /**
     * 渲染模板
     *
     * @param templateName 模板名称（用于错误提示）
     * @param templateHtml 模板HTML内容（Freemarker语法）
     * @param dataModel    数据模型
     * @return 渲染后的HTML
     */
    public String render(String templateName, String templateHtml, Map<String, Object> dataModel) {
        if (templateHtml == null || templateHtml.trim().isEmpty()) {
            throw new IllegalArgumentException("templateHtml 不能为空");
        }
        try (StringReader reader = new StringReader(templateHtml);
             StringWriter writer = new StringWriter()) {
            Template template = new Template(templateName, reader, configuration);
            template.process(dataModel, writer);
            return writer.toString();
        } catch (Exception e) {
            throw new RuntimeException("模板渲染失败: " + e.getMessage(), e);
        }
    }

    /**
     * 验证模板语法
     */
    public void validateTemplate(String templateName, String templateHtml) {
        try (StringReader reader = new StringReader(templateHtml)) {
            new Template(templateName, reader, configuration);
        } catch (Exception e) {
            throw new IllegalArgumentException("模板语法非法: " + e.getMessage(), e);
        }
    }
}
