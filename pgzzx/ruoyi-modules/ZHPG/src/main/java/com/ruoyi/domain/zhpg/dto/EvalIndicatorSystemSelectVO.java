package com.ruoyi.domain.zhpg.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * 指标体系选择VO - 用于外部系统下拉选择
 */
@Data
@ApiModel(description = "指标体系选择VO")
public class EvalIndicatorSystemSelectVO implements Serializable {
    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "指标体系ID")
    private Long indicatorSystemId;

    @ApiModelProperty(value = "指标体系名称")
    private String indicatorSystemName;

    @ApiModelProperty(value = "指标树根节点：有回传细化(refined_indicator_tree)时用回传，否则用原始(indicator_tree)；与库内 JSON 中 treeData 形态一致，返回时已解包不再套外层")
    private Object treeData;

    @ApiModelProperty(value = "描述")
    private String description;
}
