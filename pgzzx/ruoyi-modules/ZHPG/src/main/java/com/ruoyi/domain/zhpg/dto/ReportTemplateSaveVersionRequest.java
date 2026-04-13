package com.ruoyi.domain.zhpg.dto;

import javax.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ReportTemplateSaveVersionRequest {
    @NotBlank(message = "htmlContent 不能为空")
    private String htmlContent;
    private String dslJson;
    private String changeLog;
}
