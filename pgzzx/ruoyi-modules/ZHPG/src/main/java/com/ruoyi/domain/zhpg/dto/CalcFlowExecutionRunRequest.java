package com.ruoyi.domain.zhpg.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel("Calc flow execution run request")
public class CalcFlowExecutionRunRequest {

    @ApiModelProperty("task name")
    private String taskName;

    @ApiModelProperty("runtime config json")
    private String runtimeConfigJson;

    @ApiModelProperty("step state json")
    private String stepStateJson;

    @ApiModelProperty("current step")
    private String currentStep;

    @ApiModelProperty("skip weight log")
    private Boolean skipWeightLog;
}
