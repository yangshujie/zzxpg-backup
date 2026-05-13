package com.ruoyi.domain.zhpg;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
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

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

@Data
@EqualsAndHashCode(callSuper = true)
@ToString
@Accessors(chain = true)
@TableName("pgzc_calc_flow_template")
@ApiModel(value = "CalcFlowTemplate")
public class CalcFlowTemplate extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    @ApiModelProperty(value = "template id")
    private Long id;

    @Excel(name = "templateCode")
    @ApiModelProperty(value = "template code")
    private String templateCode;

    @Excel(name = "templateName")
    @NotBlank(message = "templateName is required")
    @Size(max = 200, message = "templateName length must be <= 200")
    @ApiModelProperty(value = "template name")
    private String templateName;

    @Excel(name = "taskType")
    @NotBlank(message = "taskType is required")
    @ApiModelProperty(value = "task type")
    private String taskType;

    @Excel(name = "equipmentType")
    @ApiModelProperty(value = "equipment type")
    private String equipmentType;

    @Excel(name = "calcGranularity")
    @ApiModelProperty(value = "calc granularity")
    private String calcGranularity;

    @TableField(updateStrategy = FieldStrategy.IGNORED)
    @ApiModelProperty(value = "可选遗留/展示字段；发起计算不使用，请在 CalcExecutionRequest 中传 indicatorSystemId")
    private Long indicatorSystemId;

    @TableField(updateStrategy = FieldStrategy.IGNORED)
    @ApiModelProperty(value = "与 indicatorSystemId 配套的展示名；不参与计算路由")
    private String indicatorSystemName;

    @ApiModelProperty(value = "data plan id")
    private Long dataPlanId;

    @ApiModelProperty(value = "data plan name")
    private String dataPlanName;

    @ApiModelProperty(value = "config json")
    private String configJson;

    @Excel(name = "versionNo")
    @ApiModelProperty(value = "version no")
    private String versionNo;

    @Excel(name = "status")
    @ApiModelProperty(value = "status")
    private String status;

    @ApiModelProperty(value = "description")
    private String description;

    @ApiModelProperty(value = "XXL-JOB admin 中对应的 jobInfo 主键；发布时自动注册写入")
    private Integer xxlJobId;

}
