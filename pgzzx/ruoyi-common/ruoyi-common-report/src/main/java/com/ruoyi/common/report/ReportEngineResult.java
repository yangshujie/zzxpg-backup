package com.ruoyi.common.report;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 报告引擎执行结果
 * <p>
 * 包含生成的 DOCX 字节和渲染过程中的辅助信息。
 */
public class ReportEngineResult {

    /** DOCX 文件字节 */
    private byte[] docxBytes;

    /** 占位符校验警告（预留扩展） */
    private List<String> warnings;

    /** 渲染后的 HTML（供预览/调试） */
    private String renderedHtml;

    public ReportEngineResult() {
        this.warnings = new ArrayList<>();
    }

    public ReportEngineResult(byte[] docxBytes, List<String> warnings, String renderedHtml) {
        this.docxBytes = docxBytes;
        this.warnings = warnings != null ? warnings : new ArrayList<>();
        this.renderedHtml = renderedHtml;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private byte[] docxBytes;
        private List<String> warnings;
        private String renderedHtml;

        Builder() {
            this.warnings = new ArrayList<>();
        }

        public Builder docxBytes(byte[] docxBytes) {
            this.docxBytes = docxBytes;
            return this;
        }

        public Builder warnings(List<String> warnings) {
            this.warnings = warnings != null ? warnings : new ArrayList<>();
            return this;
        }

        public Builder addWarning(String warning) {
            this.warnings.add(warning);
            return this;
        }

        public Builder renderedHtml(String renderedHtml) {
            this.renderedHtml = renderedHtml;
            return this;
        }

        public ReportEngineResult build() {
            return new ReportEngineResult(docxBytes, warnings, renderedHtml);
        }
    }

    // -- getters / setters --

    public byte[] getDocxBytes() {
        return docxBytes;
    }

    public void setDocxBytes(byte[] docxBytes) {
        this.docxBytes = docxBytes;
    }

    public List<String> getWarnings() {
        return warnings != null ? warnings : Collections.emptyList();
    }

    public void setWarnings(List<String> warnings) {
        this.warnings = warnings;
    }

    public String getRenderedHtml() {
        return renderedHtml;
    }

    public void setRenderedHtml(String renderedHtml) {
        this.renderedHtml = renderedHtml;
    }
}
