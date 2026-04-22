package com.ruoyi.domain.zhpg.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

@Data
@ApiModel(description = "指标体系下拉选择项")
public class EvalIndicatorSystemSelectVO implements Serializable {
    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "指标体系ID")
    private Long indicatorSystemId;

    @ApiModelProperty(value = "指标体系名称")
    private String indicatorSystemName;

    @ApiModelProperty(value = "关联需求ID")
    private Long requirementId;

    @ApiModelProperty(value = "指标树数据")
    private Object treeData;

    @ApiModelProperty(value = "描述")
    private String description;
}
