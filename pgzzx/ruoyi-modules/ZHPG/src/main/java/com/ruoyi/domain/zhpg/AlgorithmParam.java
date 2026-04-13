package com.ruoyi.domain.zhpg;

import com.baomidou.mybatisplus.annotation.*;
import com.ruoyi.common.core.web.domain.BaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.Accessors;

import javax.validation.constraints.Size;

/**
 * 算法参数实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ToString
@Accessors(chain = true)
@TableName("pgzc_algorithm_param")
@ApiModel(value = "算法参数")
public class AlgorithmParam extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    @ApiModelProperty(value = "参数ID")
    private Long id;

    @ApiModelProperty(value = "所属算法ID")
    private Long algorithmId;

    @ApiModelProperty(value = "参数类别(INPUT输入参数 OUTPUT输出参数 CONFIG配置参数)")
    private String paramCategory;

    @Size(max = 100, message = "参数名称不能超过100个字符")
    @ApiModelProperty(value = "参数显示名称")
    private String paramName;

    @Size(max = 100, message = "参数字段名不能超过100个字符")
    @ApiModelProperty(value = "参数字段名(英文标识)")
    private String paramField;

    @ApiModelProperty(value = "参数类型(string/number/array/dictionary/date/tel/template)")
    private String paramType;

    @ApiModelProperty(value = "默认值")
    private String defaultValue;

    @Size(max = 500, message = "参数描述不能超过500个字符")
    @ApiModelProperty(value = "参数描述")
    @TableField("param_desc")
    private String paramDesc;

    @ApiModelProperty(value = "是否必填(0否 1是)")
    private Integer requiredFlag;

    @ApiModelProperty(value = "排序号")
    private Integer sortOrder;

    @TableLogic
    private String delFlag;
}
