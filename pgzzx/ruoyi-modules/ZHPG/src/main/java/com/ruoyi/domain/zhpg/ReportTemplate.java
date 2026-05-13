package com.ruoyi.domain.zhpg;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.web.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 报告模板主表
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("pgzc_report_template")
public class ReportTemplate extends BaseEntity {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 模板名称 */
    private String templateName;

    /** 模板描述 */
    private String templateDescription;

    /** 评估任务类型 */
    private String evaluationType;

    /** HTML模板内容 */
    private String htmlContent;


    /** 数据库中无 remark 字段，标记为排除 */
    @TableField(exist = false)
    private String remark;
}
