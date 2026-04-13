package com.ruoyi.domain.zhpg.dto;

import javax.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ReportTemplateValidateRequest {
    @NotBlank(message = "htmlContent 不能为空")
    private String htmlContent;
}
