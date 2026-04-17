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
 * 评估指标模板对象 pgzc_indicator_template
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ToString
@Accessors(chain = true)
@TableName("pgzc_indicator_template")
@ApiModel(value = "评估指标模板", description = "评估指标模板对象")
public class EvalIndicatorTemplate extends BaseEntity {
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    @ApiModelProperty(value = "模板ID")
    private Long id;

    @Excel(name = "模板编号")
    @ApiModelProperty(value = "模板编号（与主键 id 一致的字符串，如 \"12\"）")
    private String templateCode;

    @Excel(name = "模板名称")
    @ApiModelProperty(value = "模板名称")
    private String templateName;

    @Excel(name = "指标集类型")
    @ApiModelProperty(value = "指标集类型/一能三性：装备作战效能、作战适用性、体系适用性、在役适用性")
    private String systemType;

    @Excel(name = "适用装备类型", readConverterExp = "space_recon=航天侦察,space_domain_awareness=太空态势感知,space_defense=太空攻防,space_track_control=航天测运控,space_launch=航天发射,sea_based_space=海基航天,无=无")
    @ApiModelProperty(value = "适用装备类型")
    private String equipmentType;

    @TableField("work_mode")
    @Excel(name = "工作模式")
    @ApiModelProperty(value = "工作模式")
    private String workMode;

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
