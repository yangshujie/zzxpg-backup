package com.ruoyi.domain.zhpg;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
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

import java.math.BigDecimal;

/**
 * 评估指标对象 pgzc_indicator
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ToString
@Accessors(chain = true)
@TableName("pgzc_indicator")
@ApiModel(value = "评估指标", description = "评估指标对象")
public class EvalIndicator extends BaseEntity {
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    @Excel(name = "指标编号")
    @ApiModelProperty(value = "指标ID（列表与导出中的编号）")
    private Long id;

    @TableField("id_code")
    @Excel(name = "ID编码")
    @ApiModelProperty(value = "全局ID编码；从指标库导入指标体系时，树节点 id 与此一致")
    private String idCode;

    @Excel(name = "父指标ID")
    @ApiModelProperty(value = "父指标ID（0为顶级）")
    private Long parentId;

    @Excel(name = "指标名称")
    @ApiModelProperty(value = "指标名称")
    private String indicatorName;

    @Excel(name = "体系类型")
    @ApiModelProperty(value = "指标集类型/一能三性：装备作战效能、作战适用性、体系适用性、在役适用性")
    @TableField("system_type")
    private String systemType;

    @Excel(name = "指标类型", readConverterExp = "space_recon=航天侦察,space_domain_awareness=太空态势感知,space_defense=太空攻防,space_track_control=航天测运控,space_launch=航天发射,sea_based_space=海基航天,无=无")
    @ApiModelProperty(value = "指标类型（装备类型/无）")
    private String indicatorType;

    @Excel(name = "计量单位")
    @ApiModelProperty(value = "计量单位")
    private String unit;

    @ApiModelProperty(value = "取值范围下限")
    private BigDecimal valueMin;

    @ApiModelProperty(value = "取值范围上限")
    private BigDecimal valueMax;

    @ApiModelProperty(value = "权重（已废弃，由指标值类型与门限范围替代）")
    private BigDecimal weight;

    /** 指标值类型：成本型、效益型、区间效益型 */
    @TableField("value_category")
    @ApiModelProperty(value = "指标值类型")
    private String valueCategory;

    @Excel(name = "计算方法")
    @ApiModelProperty(value = "计算方法")
    private String calcMethod;

    @TableField("work_mode")
    @Excel(name = "工作模式")
    @ApiModelProperty(value = "工作模式")
    private String workMode;

    @ApiModelProperty(value = "关联算法模型ID")
    private Long algorithmId;

    @ApiModelProperty(value = "层级")
    @TableField("indicator_level")
    private Integer level;

    @ApiModelProperty(value = "排序号")
    private Integer orderNum;

    @ApiModelProperty(value = "描述")
    private String description;

    @Excel(name = "状态")
    @ApiModelProperty(value = "状态（DRAFT/PUBLISHED）")
    private String status;

    @TableLogic
    private String delFlag;

    /** 是否为底层指标（1=底层叶子节点，0=中间/根节点） */
    @TableField("is_bottom_node")
    @ApiModelProperty(value = "是否为底层指标")
    private Boolean isBottomNode;

    /** 关联算法名称（非数据库字段，用于展示） */
    @TableField(exist = false)
    @ApiModelProperty(value = "关联算法名称")
    private String algorithmName;
}
