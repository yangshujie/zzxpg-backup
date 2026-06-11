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

    @ApiModelProperty("流程执行实例ID")
    private Long executionId;

    @ApiModelProperty("执行任务名称")
    private String taskName;

    @ApiModelProperty(value = "运行时指标体系ID")
    private Long indicatorSystemId;

    @ApiModelProperty("关联需求ID")
    private Long requirementId;

    @ApiModelProperty("本次执行运行时配置JSON，优先于模板configJson")
    private String runtimeConfigJson;

    @ApiModelProperty("是否跳过权重计算阶段展示（主流程第三步点击计算时使用）")
    private Boolean skipWeightLog;

    @ApiModelProperty("预处理批次ID")
    private Long batchId;
}
