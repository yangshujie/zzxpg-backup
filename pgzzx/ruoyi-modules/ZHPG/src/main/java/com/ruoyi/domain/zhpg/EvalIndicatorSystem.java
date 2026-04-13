package com.ruoyi.domain.zhpg;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.annotation.Excel;
import com.ruoyi.common.core.web.domain.BaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.util.Date;

/**
 * 评估指标体系对象 pgzc_indicator_system
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ToString
@Accessors(chain = true)
@TableName("pgzc_indicator_system")
@ApiModel(value = "评估指标体系", description = "评估指标体系对象")
public class EvalIndicatorSystem extends BaseEntity {
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    @Excel(name = "指标体系ID")
    @ApiModelProperty(value = "指标体系ID")
    private Long id;

    @Excel(name = "指标体系名称")
    @ApiModelProperty(value = "指标体系名称")
    private String systemName;

    @TableField("id_code")
    @Excel(name = "ID编码")
    @ApiModelProperty(value = "全局ID编码，与指标树根节点 JSON 的 id（或 uid）一致")
    private String idCode;

    @TableField("work_mode")
    @Excel(name = "工作模式")
    @ApiModelProperty(value = "工作模式")
    private String workMode;

    @ApiModelProperty(value = "原始指标树结构JSON（主分协同下为粗建/未回传版本，不被回传覆盖）")
    private String indicatorTree;

    @TableField("refined_indicator_tree")
    @ApiModelProperty(value = "主分协同下分系统回传的细化指标树JSON；权重计算以此为准（非空时）。内部流转通常为空。")
    private String refinedIndicatorTree;

    @ApiModelProperty(value = "含权重的指标树JSON")
    private String indicatorTreeWeight;

    @Excel(name = "权重分配算法")
    @ApiModelProperty(value = "权重分配算法")
    private String weightAssignAlgorithm;

    @ApiModelProperty(value = "权重分配算法参数JSON")
    private String weightAssignParams;

    @TableField("conduction_config")
    @ApiModelProperty(value = "默认传导算法配置JSON（串联/并联/热备/冷备/表决等）")
    private String conductionConfig;

    @ApiModelProperty(value = "是否启用（0=测试中 1=已启用）")
    private Integer isApplied;

    @ApiModelProperty(value = "来源模板ID")
    private Long templateId;

    @ApiModelProperty(value = "描述")
    private String description;

    @Excel(name = "状态")
    @ApiModelProperty(value = "状态（DRAFT/PUBLISHED）")
    private String status;

    @TableField(value = "build_phase", updateStrategy = FieldStrategy.IGNORED)
    @Excel(name = "构建阶段")
    @ApiModelProperty(value = "构建阶段：INITIAL_DRAFT=初始粗建，REFINED=已回传细化；仅主分协同使用。历史可能含 SUBMITTED、FINALIZED")
    private String buildPhase;

    @TableField("source_subsystem")
    @ApiModelProperty(value = "回传来源分系统标识")
    private String sourceSubsystem;

    @TableField("requirement_id")
    @ApiModelProperty(value = "关联需求ID（需求分析等外部系统下发时传入）")
    private Long requirementId;

    @TableField("refined_time")
    @ApiModelProperty(value = "分系统回传细化时间")
    private Date refinedTime;

    @TableLogic
    private String delFlag;
}
