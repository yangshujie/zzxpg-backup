package com.ruoyi.domain.zhpg.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
@ApiModel("综合分析计算执行请求")
public class CalcExecutionRequest {

    @NotNull(message = "calcFlowTemplateId is required")
    @ApiModelProperty(value = "流程模板ID", required = true)
    private Long calcFlowTemplateId;

    @ApiModelProperty("执行任务名称")
    private String taskName;

    @ApiModelProperty("关联评估任务ID")
    private Long assessTaskId;

    @NotNull(message = "indicatorSystemId is required")
    @ApiModelProperty(value = "运行时指标体系ID", required = true)
    private Long indicatorSystemId;
}
