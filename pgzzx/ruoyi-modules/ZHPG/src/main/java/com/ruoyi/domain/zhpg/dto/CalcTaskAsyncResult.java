package com.ruoyi.domain.zhpg.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.Date;

/**
 * 计算任务异步执行结果（供前端轮询查询）
 */
@Data
@ApiModel("计算任务执行状态")
public class CalcTaskAsyncResult {

    @ApiModelProperty("任务ID")
    private Long taskId;

    @ApiModelProperty("任务名称")
    private String taskName;

    @ApiModelProperty("任务状态: PENDING(待执行)/DISPATCHED(已分发)/RUNNING(执行中)/SUCCESS(成功)/FAILED(失败)")
    private String status;

    @ApiModelProperty("当前执行阶段")
    private String currentStage;

    @ApiModelProperty("执行进度 0-100")
    private Integer progressPercent;

    @ApiModelProperty("XXL-JOB执行日志ID")
    private Long xxlJobLogId;

    @ApiModelProperty("执行结果摘要（成功时）")
    private Object result;

    @ApiModelProperty("错误信息（失败时）")
    private String errorMessage;

    @ApiModelProperty("创建时间")
    private Date createTime;

    @ApiModelProperty("开始执行时间")
    private Date startTime;

    @ApiModelProperty("结束时间")
    private Date endTime;
}
