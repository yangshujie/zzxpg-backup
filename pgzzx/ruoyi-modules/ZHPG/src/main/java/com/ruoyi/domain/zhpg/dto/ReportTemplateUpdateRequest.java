package com.ruoyi.domain.zhpg.dto;

import javax.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ReportTemplateUpdateRequest {
    @NotBlank(message = "templateName 不能为空")
    private String templateName;

    private String templateDescription;

    private String evaluationType;
}
