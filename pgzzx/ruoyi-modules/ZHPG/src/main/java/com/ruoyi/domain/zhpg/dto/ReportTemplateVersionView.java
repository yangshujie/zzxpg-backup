package com.ruoyi.domain.zhpg.dto;

import java.util.Date;
import lombok.Data;

@Data
public class ReportTemplateVersionView {
    private Long id;
    private String versionNo;
    private String changeLog;
    private String createBy;
    private Date createTime;
}
