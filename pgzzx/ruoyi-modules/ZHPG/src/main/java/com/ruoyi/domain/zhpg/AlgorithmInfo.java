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
 * 算法信息实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ToString
@Accessors(chain = true)
@TableName("pgzc_algorithm")
@ApiModel(value = "算法信息")
public class AlgorithmInfo extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    @ApiModelProperty(value = "算法ID")
    private Long id;

    @Excel(name = "算法名称")
    @NotBlank(message = "算法名称不能为空")
    @Size(max = 200, message = "算法名称不能超过200个字符")
    @ApiModelProperty(value = "算法名称")
    private String algorithmName;

    @Excel(name = "算法类型")
    @ApiModelProperty(value = "算法类型")
    private String algorithmType;

    @Excel(name = "装备类型")
    @ApiModelProperty(value = "装备类型")
    private String equipmentType;

    @ApiModelProperty(value = "算法描述")
    @TableField("algorithm_desc")
    private String algorithmDesc;

    @ApiModelProperty(value = "算法版本（暂不使用，保留字段）")
    private String algorithmVersion;

    @ApiModelProperty(value = "算法代码文件路径")
    private String algorithmCodeUrl;

    @ApiModelProperty(value = "是否需要基准数据(0否 1是)")
    private Integer baseFlag;

    @Excel(name = "发布状态")
    @ApiModelProperty(value = "发布状态(DRAFT草稿 PUBLISHED已发布)")
    private String publishStatus;

    @Excel(name = "状态")
    @ApiModelProperty(value = "状态(ENABLED启用 DISABLED停用)")
    private String status;

    @TableLogic
    private String delFlag;

    /** 算法参数列表（非数据库字段；字段名避免与 BaseEntity#params 冲突，JSON 仍用 params） */
    @JsonProperty("params")
    @TableField(exist = false)
    @ApiModelProperty(value = "算法参数列表")
    private List<AlgorithmParam> algorithmParams;
}
