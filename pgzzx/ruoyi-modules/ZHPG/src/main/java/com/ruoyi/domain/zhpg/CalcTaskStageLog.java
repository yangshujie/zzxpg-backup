package com.ruoyi.domain.zhpg;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.Date;

/**
 * 计算任务阶段执行日志对象 pgzc_calc_task_stage_log
 */
@Data
@ToString
@Accessors(chain = true)
@TableName("pgzc_calc_task_stage_log")
@ApiModel(value = "计算任务阶段执行日志")
public class CalcTaskStageLog implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    @ApiModelProperty(value = "日志ID")
    private Long id;

    @ApiModelProperty(value = "关联计算任务ID")
    private Long calcTaskId;

    @ApiModelProperty(value = "阶段编码(SCHEDULE_CONFIG/WEIGHT_CALC/COMPREHENSIVE_CALC/REPORT_OUTPUT)")
    private String stageCode;

    @ApiModelProperty(value = "阶段名称")
    private String stageName;

    @ApiModelProperty(value = "执行顺序(1-4)")
    private Integer stageOrder;

    @ApiModelProperty(value = "执行状态(PENDING/RUNNING/SUCCESS/FAILED/SKIPPED)")
    private String executeStatus;

    @ApiModelProperty(value = "输入摘要")
    private String inputSummary;

    @ApiModelProperty(value = "输出摘要")
    private String outputSummary;

    @ApiModelProperty(value = "异常信息")
    private String errorMessage;

    @ApiModelProperty(value = "开始时间")
    private Date beginTime;

    @ApiModelProperty(value = "结束时间")
    private Date finishTime;
}
