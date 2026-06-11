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
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 * 评估准则集对象 pgzc_eval_criterion_set
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ToString
@Accessors(chain = true)
@TableName("pgzc_eval_criterion_set")
@ApiModel(value = "评估准则集")
public class EvalCriterionSet extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    @ApiModelProperty(value = "主键ID")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    @Excel(name = "准则集编号")
    @ApiModelProperty(value = "准则集编号(唯一)")
    private String setCode;

    @Excel(name = "准则集名称")
    @NotBlank(message = "准则集名称不能为空")
    @Size(max = 200, message = "准则集名称长度不能超过200")
    @ApiModelProperty(value = "准则集名称")
    private String setName;

    @NotNull(message = "绑定的指标体系ID不能为空")
    @ApiModelProperty(value = "绑定的指标体系ID")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long indicatorSystemId;

    @Excel(name = "装备类型")
    @ApiModelProperty(value = "装备类型")
    private String equipmentType;

    @Excel(name = "状态")
    @ApiModelProperty(value = "状态(DRAFT/PUBLISHED/DISABLED)")
    private String status;

    @ApiModelProperty(value = "准则集说明")
    private String description;

    @ApiModelProperty(value = "逻辑删除(0否1是)")
    private Integer deleted;

    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    @TableField(exist = false)
    private String remark;
}
