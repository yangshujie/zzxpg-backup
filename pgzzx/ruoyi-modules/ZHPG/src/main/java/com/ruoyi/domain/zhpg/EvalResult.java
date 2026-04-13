package com.ruoyi.domain.zhpg;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.web.domain.BaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 评估结果对象 pgzc_eval_result
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ToString
@Accessors(chain = true)
@TableName("pgzc_eval_result")
@ApiModel(value = "EvalResult", description = "评估结果对象")
public class EvalResult extends BaseEntity {
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    @ApiModelProperty(value = "结果ID")
    private Long id;

    @ApiModelProperty(value = "结果编号（唯一标识）")
    private String resultCode;

    @ApiModelProperty(value = "关联计算任务ID（pgzc_calc_task.id）")
    private Long taskId;

    @ApiModelProperty(value = "任务名称")
    private String taskName;

    @ApiModelProperty(value = "指标体系ID")
    private Long indicatorSystemId;

    @ApiModelProperty(value = "指标体系名称")
    private String indicatorSystemName;

    @ApiModelProperty(value = "关联评估模板ID")
    private Long templateId;

    @ApiModelProperty(value = "评估模板名称")
    private String templateName;

    @ApiModelProperty(value = "综合得分（0-100）")
    private BigDecimal score;

    @ApiModelProperty(value = "等级（A/B/C/D）")
    private String grade;

    @ApiModelProperty(value = "关键结论")
    private String conclusion;

    @ApiModelProperty(value = "整改建议")
    private String suggestion;

    @ApiModelProperty(value = "各维度得分JSON")
    private String dimensions;

    @ApiModelProperty(value = "工作流状态（PENDING/GATHERING/REVIEWING/PUBLISHED/ARCHIVED）")
    private String workflowStatus;

    @ApiModelProperty(value = "发布时间")
    private Date publishTime;

    @ApiModelProperty(value = "关联报告URL")
    private String reportUrl;

    @ApiModelProperty(value = "报告与流水线载荷 JSON（含 reportDataContext、outputTargets、executionChain 等）")
    private String reportPayloadJson;

    @ApiModelProperty(value = "逻辑删除")
    private Integer delFlag;

    /** 维度得分列表（非数据库字段） */
    @TableField(exist = false)
    @ApiModelProperty(value = "维度得分列表")
    private List<DimensionScore> dimensionList;

    /** 关联计算任务信息（非数据库字段） */
    @TableField(exist = false)
    @ApiModelProperty(value = "关联计算任务运行状态")
    private String taskRunStatus;

    @TableField(exist = false)
    @ApiModelProperty(value = "关联计算任务进度(0-100)")
    private Integer taskProgressPercent;

    @TableField(exist = false)
    @ApiModelProperty(value = "关联计算任务当前阶段")
    private String taskCurrentStage;

    /** 评分等级配置（来自计算任务快照中的 comprehensiveCalc.config.scoreLevels，非数据库字段） */
    @TableField(exist = false)
    @ApiModelProperty(value = "评分等级配置列表，每项含 name/threshold")
    private List<Map<String, Object>> scoreLevels;

    /**
     * 维度得分内部类
     */
    @Data
    public static class DimensionScore {
        private String label;
        private BigDecimal value;
        private String tone;
    }
}
