package com.ruoyi.domain.zhpg.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;

@Data
public class EvalReportGenerateRequest {
    @NotNull(message = "reportTemplateId is required")
    private Long reportTemplateId;

    private Map<String, Object> fields;

    private List<Map<String, Object>> mappings;

    private String editedHtml;
}
