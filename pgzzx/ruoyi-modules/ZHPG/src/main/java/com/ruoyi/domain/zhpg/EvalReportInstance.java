package com.ruoyi.domain.zhpg;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.web.domain.BaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.Accessors;

@Data
@EqualsAndHashCode(callSuper = true)
@ToString
@Accessors(chain = true)
@TableName("pgzc_eval_report_instance")
@ApiModel(value = "EvalReportInstance", description = "评估报告生成实例")
public class EvalReportInstance extends BaseEntity {
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    @ApiModelProperty(value = "报告实例ID")
    private Long id;

    @ApiModelProperty(value = "关联评估结果ID")
    private Long evalResultId;

    @ApiModelProperty(value = "关联计算任务ID")
    private Long calcTaskId;

    @ApiModelProperty(value = "报告编号")
    private String reportCode;

    @ApiModelProperty(value = "同一评估结果下的生成序号")
    private Integer generationNo;

    @ApiModelProperty(value = "报告模板ID")
    private Long reportTemplateId;

    @ApiModelProperty(value = "报告模板名称")
    private String reportTemplateName;

    @ApiModelProperty(value = "匹配规则与渲染字段JSON")
    private String mappingJson;

    @ApiModelProperty(value = "渲染状态(PENDING/SUCCESS/FAILED)")
    private String renderStatus;

    @ApiModelProperty(value = "报告主URL")
    private String reportUrl;

    @ApiModelProperty(value = "Word报告URL")
    private String wordUrl;

    @ApiModelProperty(value = "PDF报告URL")
    private String pdfUrl;

    @ApiModelProperty(value = "文件格式")
    private String fileFormat;

    @ApiModelProperty(value = "错误信息")
    private String errorMessage;

    @ApiModelProperty(value = "逻辑删除")
    private Integer delFlag;
}
