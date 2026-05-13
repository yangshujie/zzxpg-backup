package com.ruoyi.domain.zhpg;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonProperty;
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
import java.util.List;

/**
 * 算法需求实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ToString
@Accessors(chain = true)
@TableName("pgzc_algorithm_requirement")
@ApiModel(value = "算法需求")
public class AlgorithmRequirement extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    @ApiModelProperty(value = "需求ID")
    private Long id;

    @Excel(name = "需求编号")
    @ApiModelProperty(value = "需求编号")
    private String code;

    @Excel(name = "需求名称")
    @NotBlank(message = "需求名称不能为空")
    @Size(max = 200, message = "需求名称不能超过200个字符")
    @ApiModelProperty(value = "需求名称")
    private String title;

    @Excel(name = "需求类型")
    @ApiModelProperty(value = "需求类型：新增算法、算法改造、参数调优")
    private String type;

    @ApiModelProperty(value = "需求来源")
    private String source;

    @ApiModelProperty(value = "关联模板")
    private String template;

    @ApiModelProperty(value = "关键输入")
    private String input;

    @ApiModelProperty(value = "目标输出")
    private String output;

    @ApiModelProperty(value = "需求说明")
    private String summary;

    @ApiModelProperty(value = "验收方式")
    private String acceptance;

    @ApiModelProperty(value = "特殊配置要求")
    private String specialConfig;

    @Excel(name = "状态")
    @ApiModelProperty(value = "状态：待构建、构建中、已构建")
    private String status;

    @ApiModelProperty(value = "关联算法ID")
    private Long algorithmId;

    @ApiModelProperty(value = "关联算法名称")
    private String algorithmName;

    @ApiModelProperty(value = "通知状态：待通知、已通知、通知失败")
    private String notifyStatus;

    @ApiModelProperty(value = "通知时间")
    private String notifyTime;

    @ApiModelProperty(value = "通知方式：HTTP、MQ、WEBHOOK")
    private String notifyType;

    @ApiModelProperty(value = "通知目标地址")
    private String notifyTarget;

    @Excel(name = "状态")
    @ApiModelProperty(value = "数据状态：ENABLED、DISABLED")
    private String dataStatus;

    /** 需求参数要求列表（非数据库字段，避免与BaseEntity.params冲突） */
    @JsonProperty("params")
    @TableField(exist = false)
    @ApiModelProperty(value = "需求参数要求列表")
    private List<AlgorithmRequirementParam> requirementParams;
}
