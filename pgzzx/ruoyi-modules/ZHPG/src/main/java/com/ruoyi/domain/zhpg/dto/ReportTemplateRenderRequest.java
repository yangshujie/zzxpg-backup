package com.ruoyi.domain.zhpg.dto;

import java.util.Map;
import lombok.Data;

@Data
public class ReportTemplateRenderRequest {
    private Long versionId;
    private Map<String, Object> fields;
    private String editedHtml;
}
