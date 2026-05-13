package com.ruoyi.domain.zhpg;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.web.domain.BaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.Accessors;

@Data
@EqualsAndHashCode(callSuper = true)
@ToString
@Accessors(chain = true)
@TableName("pgzc_calc_flow_execution")
@ApiModel(value = "CalcFlowExecution")
public class CalcFlowExecution extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @ApiModelProperty("execution code")
    private String executionCode;

    @ApiModelProperty("template id")
    private Long templateId;

    @ApiModelProperty("indicator system id")
    private Long indicatorSystemId;

    @ApiModelProperty("execution name")
    private String executionName;

    @ApiModelProperty("runtime config json")
    private String runtimeConfigJson;

    @ApiModelProperty("step state json")
    private String stepStateJson;

    @ApiModelProperty("template snapshot json")
    private String templateSnapshotJson;

    @ApiModelProperty("current step")
    private String currentStep;

    @ApiModelProperty("calc task id")
    private Long calcTaskId;

    @ApiModelProperty("eval result id")
    private Long evalResultId;

    @ApiModelProperty("latest report id")
    private Long latestReportId;

    @ApiModelProperty("DRAFT/CONFIGURING/RUNNING/SUCCESS/FAILED/REPORT_READY")
    private String status;

}
