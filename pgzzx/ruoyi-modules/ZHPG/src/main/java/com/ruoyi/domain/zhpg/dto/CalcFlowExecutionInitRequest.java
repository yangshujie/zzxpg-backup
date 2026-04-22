package com.ruoyi.domain.zhpg.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
@ApiModel("Calc flow execution init request")
public class CalcFlowExecutionInitRequest {

    @NotNull(message = "templateId is required")
    @ApiModelProperty(value = "template id", required = true)
    private Long templateId;

    @ApiModelProperty(value = "indicator system id")
    private Long indicatorSystemId;

    @ApiModelProperty("requirement id")
    private Long requirementId;

    @ApiModelProperty("execution name")
    private String executionName;
}
