package com.ruoyi.domain.zhpg;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.web.domain.BaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 算法需求参数要求实体
 */
@Data
@Accessors(chain = true)
@TableName("pgzc_algorithm_requirement_param")
@ApiModel(value = "算法需求参数要求")
public class AlgorithmRequirementParam extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    @ApiModelProperty(value = "参数ID")
    private Long id;

    @ApiModelProperty(value = "需求ID")
    private Long requirementId;

    @ApiModelProperty(value = "参数名称")
    private String paramField;

    @ApiModelProperty(value = "参数类型：string、number、array、dictionary、date")
    private String paramType;

    @ApiModelProperty(value = "默认值")
    private String defaultValue;

    @ApiModelProperty(value = "是否必填：0否、1是")
    private Integer requiredFlag;

    @ApiModelProperty(value = "参数说明")
    private String paramDesc;

    @ApiModelProperty(value = "参数顺序")
    private Integer sortOrder;
}
