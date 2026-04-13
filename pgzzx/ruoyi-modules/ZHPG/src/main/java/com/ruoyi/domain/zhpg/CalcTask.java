package com.ruoyi.domain.zhpg;

import com.baomidou.mybatisplus.annotation.*;
import com.ruoyi.common.core.annotation.Excel;
import com.ruoyi.common.core.web.domain.BaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.util.Date;
import java.util.List;

/**
 * 评估计算任务对象 pgzc_calc_task
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ToString
@Accessors(chain = true)
@TableName("pgzc_calc_task")
@ApiModel(value = "评估计算任务")
public class CalcTask extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    @ApiModelProperty(value = "计算任务ID")
    private Long id;

    @Excel(name = "计算任务名称")
    @ApiModelProperty(value = "计算任务名称")
    private String taskName;

    @ApiModelProperty(value = "关联评估任务ID")
    private Long assessTaskId;

    @ApiModelProperty(value = "关联流程模板ID")
    private Long calcFlowTemplateId;

    @ApiModelProperty(value = "执行时模板快照")
    private String templateSnapshotJson;

    @Excel(name = "运行状态")
    @ApiModelProperty(value = "运行状态(WAITING/RUNNING/PAUSED/SUCCESS/FAILED/TERMINATED)")
    private String runStatus;

    @ApiModelProperty(value = "当前执行阶段编码")
    private String currentStage;

    @ApiModelProperty(value = "执行进度(0-100)")
    private Integer progressPercent;

    @ApiModelProperty(value = "结果摘要JSON")
    private String resultSummaryJson;

    @ApiModelProperty(value = "日志跟踪号")
    private String logTraceId;

    @ApiModelProperty(value = "开始时间")
    private Date startTime;

    @ApiModelProperty(value = "结束时间")
    private Date endTime;

    @ApiModelProperty(value = "综合计算异步状态(PENDING/SUCCESS/FAIL)，XXL-JOB链路写回")
    private String comprehensiveAsyncState;

    @ApiModelProperty(value = "综合计算异步结果JSON或失败信息")
    private String comprehensiveAsyncJson;

    @TableLogic
    private String delFlag;

    /** 阶段日志列表（非数据库字段） */
    @TableField(exist = false)
    @ApiModelProperty(value = "阶段日志列表")
    private List<CalcTaskStageLog> stageLogs;
}
