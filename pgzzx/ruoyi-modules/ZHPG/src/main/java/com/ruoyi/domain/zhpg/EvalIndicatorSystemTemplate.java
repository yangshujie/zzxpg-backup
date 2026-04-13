package com.ruoyi.domain.zhpg;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
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

/**
 * 指标体系模板对象 pgzc_indicator_system_template
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ToString
@Accessors(chain = true)
@TableName("pgzc_indicator_system_template")
@ApiModel(value = "指标体系模板", description = "指标体系模板对象")
public class EvalIndicatorSystemTemplate extends BaseEntity {
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    @ApiModelProperty(value = "模板ID")
    private Long id;

    @Excel(name = "模板编号")
    @ApiModelProperty(value = "模板编号")
    private String templateCode;

    @Excel(name = "模板名称")
    @ApiModelProperty(value = "模板名称")
    private String templateName;

    @Excel(name = "指标集类型")
    @ApiModelProperty(value = "指标集类型/一能三性：装备作战效能、作战适用性、体系适用性、在役适用性")
    private String systemType;

    @Excel(name = "装备类型")
    @ApiModelProperty(value = "装备类型（不少于6类，单装指标体系库维度）")
    private String equipmentType;

    @TableField("work_mode")
    @Excel(name = "工作模式")
    @ApiModelProperty(value = "工作模式")
    private String workMode;

    @ApiModelProperty(value = "指标树结构JSON")
    private String indicatorTree;

    @ApiModelProperty(value = "模板配置JSON")
    private String configJson;

    @ApiModelProperty(value = "模板描述")
    private String description;

    @Excel(name = "状态")
    @ApiModelProperty(value = "状态（DRAFT/PUBLISHED）")
    private String status;

    @TableLogic
    private String delFlag;
}
