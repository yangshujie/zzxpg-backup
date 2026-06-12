package com.ruoyi.common.report;

import java.util.Map;

/**
 * 报告引擎请求参数
 * <p>
 * 封装模板渲染和 DOCX 生成的完整输入。
 * htmlContent 应为原始 HTML 模板（含 {{placeholder}} 占位符），
 * 引擎内部会自动完成占位符 → FreeMarker 转换。
 */
public class ReportEngineRequest {

    /** 模板名称（用于 FreeMarker 错误定位） */
    private String templateName;

    /** 原始 HTML 模板（含 {{placeholder}}、{{#each}}、{{#if}} 等占位符语法） */
    private String htmlContent;

    /** 渲染数据模型 */
    private Map<String, Object> dataModel;

    /** 是否自动注入中文章节编号（一、二、三…） */
    private boolean enableChapterNumbering;

    public ReportEngineRequest() {
    }

    public ReportEngineRequest(String templateName, String htmlContent, Map<String, Object> dataModel, boolean enableChapterNumbering) {
        this.templateName = templateName;
        this.htmlContent = htmlContent;
        this.dataModel = dataModel;
        this.enableChapterNumbering = enableChapterNumbering;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String templateName;
        private String htmlContent;
        private Map<String, Object> dataModel;
        private boolean enableChapterNumbering;

        Builder() {
        }

        public Builder templateName(String templateName) {
            this.templateName = templateName;
            return this;
        }

        public Builder htmlContent(String htmlContent) {
            this.htmlContent = htmlContent;
            return this;
        }

        public Builder dataModel(Map<String, Object> dataModel) {
            this.dataModel = dataModel;
            return this;
        }

        public Builder enableChapterNumbering(boolean enableChapterNumbering) {
            this.enableChapterNumbering = enableChapterNumbering;
            return this;
        }

        public ReportEngineRequest build() {
            return new ReportEngineRequest(templateName, htmlContent, dataModel, enableChapterNumbering);
        }
    }

    // -- getters / setters --

    public String getTemplateName() {
        return templateName;
    }

    public void setTemplateName(String templateName) {
        this.templateName = templateName;
    }

    public String getHtmlContent() {
        return htmlContent;
    }

    public void setHtmlContent(String htmlContent) {
        this.htmlContent = htmlContent;
    }

    public Map<String, Object> getDataModel() {
        return dataModel;
    }

    public void setDataModel(Map<String, Object> dataModel) {
        this.dataModel = dataModel;
    }

    public boolean isEnableChapterNumbering() {
        return enableChapterNumbering;
    }

    public void setEnableChapterNumbering(boolean enableChapterNumbering) {
        this.enableChapterNumbering = enableChapterNumbering;
    }
}
