package com.ruoyi.domain.zhpg;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.ruoyi.common.core.annotation.Excel;
import com.ruoyi.common.core.web.domain.BaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AccessLevel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

import javax.validation.constraints.NotBlank;
import java.math.BigDecimal;

/**
 * 评估准则明细对象 pgzc_eval_criterion
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ToString
@Accessors(chain = true)
@TableName("pgzc_eval_criterion")
@ApiModel(value = "评估准则")
public class EvalCriterion extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    @ApiModelProperty(value = "主键ID")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    @Excel(name = "准则编号")
    @ApiModelProperty(value = "准则编号(唯一)")
    private String criterionCode;

    @Excel(name = "准则名称")
    @NotBlank(message = "准则名称不能为空")
    @ApiModelProperty(value = "准则名称")
    private String criterionName;

    @ApiModelProperty(value = "准则集ID")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long setId;

    @Excel(name = "装备类型")
    @ApiModelProperty(value = "装备类型")
    private String equipmentType;

    @ApiModelProperty(value = "绑定指标ID")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long indicatorId;

    @ApiModelProperty(value = "指标层级")
    private Integer indicatorLevel;

    @ApiModelProperty(value = "关联指标体系ID")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long indicatorSystemId;

    @ApiModelProperty(value = "构建方式(CUSTOM/SCRIPT)")
    private String buildMode;

    @Excel(name = "规则类型")
    @NotBlank(message = "规则类型不能为空")
    @ApiModelProperty(value = "规则类型(THRESHOLD/LEVEL_MAP/CONDITION/TREND_STABILITY/DYNAMIC_ENVELOPE/ROBUSTNESS/RELATIVE_COMPARE/PROBABILITY_CONFIDENCE/INTERVAL_OVERLAP/MONOTONICITY/SHORT_BOARD_COMP/VETO_CONTROL)")
    private String ruleType;

    @ApiModelProperty(value = "规则定义JSON")
    private String ruleJson;

    @Excel(name = "指标值类型")
    @ApiModelProperty(value = "指标值类型(成本型/效益型/区间效益型)")
    private String valueCategory;

    @ApiModelProperty(value = "最佳值")
    private BigDecimal bestValue;

    @ApiModelProperty(value = "最差值")
    private BigDecimal worstValue;

    @Excel(name = "结论文案模板")
    @ApiModelProperty(value = "结论文案模板")
    private String conclusionTemplate;

    @ApiModelProperty(value = "优先级")
    private Integer priority;

    @ApiModelProperty(value = "是否是一票否决(0-否，1-是)")
    private Integer isMandatory;

    @ApiModelProperty(value = "否决后执行动作(DIRECT_ZERO/DIRECT_FAIL/DOWNGRADE)")
    private String vetoAction;

    @Excel(name = "状态")
    @ApiModelProperty(value = "状态")
    private String status;

    @ApiModelProperty(value = "逻辑删除(0否1是)")
    private Integer deleted;

    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    @TableField(exist = false)
    private String remark;
}
