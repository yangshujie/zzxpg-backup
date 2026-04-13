package com.ruoyi.domain.zhpg.dto;

import java.util.Date;
import java.util.List;
import lombok.Data;

@Data
public class ReportTemplateDetailResponse {
    private Long id;

    private String templateName;

    private String templateDescription;

    private String evaluationType;

    private String htmlContent;

    private List<String> placeholders;

    private Date createTime;

    private Date updateTime;
}
