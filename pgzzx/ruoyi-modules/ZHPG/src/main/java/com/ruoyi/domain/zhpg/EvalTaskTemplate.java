package com.ruoyi.domain.zhpg;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.annotation.Excel;
import com.ruoyi.common.core.web.domain.BaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.Accessors;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * 评估任务模板对象 pgzc_eval_task_template
 *
 * 评估任务构建分系统(RWGJ)核心实体：在已有指标体系/计算流程模板/报告模板/评估准则之上
 * 聚合为一个"评估任务模板"，按 templateType 区分评估方法类型，配置落在 typeConfigJson。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ToString
@Accessors(chain = true)
@TableName("pgzc_eval_task_template")
@ApiModel(value = "评估任务模板")
public class EvalTaskTemplate extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    @ApiModelProperty(value = "模板ID")
    private Long id;

    @Excel(name = "模板编号")
    @ApiModelProperty(value = "模板编号(系统生成)")
    private String templateCode;

    @Excel(name = "模板名称")
    @NotBlank(message = "模板名称不能为空")
    @Size(max = 200, message = "模板名称长度不能超过200")
    @ApiModelProperty(value = "模板名称")
    private String templateName;

    @Excel(name = "评估方法类型")
    @NotBlank(message = "评估方法类型不能为空")
    @ApiModelProperty(value = "评估方法类型(EQUIP_EFFECTIVENESS/SYSTEM_CONTRIBUTION/TASK_SATISFACTION/SPECIAL_CAPABILITY/KEY_FACTOR)")
    private String templateType;

    @Excel(name = "模板分类")
    @ApiModelProperty(value = "模板分类(GENERAL通用/SPECIFIC专用)")
    private String classification;

    @Excel(name = "装备类型")
    @ApiModelProperty(value = "装备类型")
    private String equipmentType;

    @Excel(name = "评估粒度")
    @ApiModelProperty(value = "评估粒度")
    private String calcGranularity;

    @ApiModelProperty(value = "引用指标体系ID")
    private Long indicatorSystemId;

    @ApiModelProperty(value = "引用计算流程模板ID")
    private Long calcFlowTemplateId;

    @ApiModelProperty(value = "引用评估准则集ID")
    private Long criterionSetId;

    @ApiModelProperty(value = "引用报告模板ID")
    private Long reportTemplateId;

    @ApiModelProperty(value = "类型差异化配置JSON")
    private String typeConfigJson;

    @Excel(name = "状态")
    @ApiModelProperty(value = "状态(PUBLISHED/DISABLED)")
    private String status;

    @ApiModelProperty(value = "模板说明")
    private String description;
}
