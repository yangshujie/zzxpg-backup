package com.ruoyi.domain.zhpg.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel("Calc flow execution save request")
public class CalcFlowExecutionSaveRequest {

    @ApiModelProperty("runtime config json")
    private String runtimeConfigJson;

    @ApiModelProperty("step state json")
    private String stepStateJson;

    @ApiModelProperty("current step")
    private String currentStep;

    @ApiModelProperty("status")
    private String status;

    @ApiModelProperty("calc task id")
    private Long calcTaskId;

    @ApiModelProperty("eval result id")
    private Long evalResultId;

    @ApiModelProperty("latest report id")
    private Long latestReportId;
}
