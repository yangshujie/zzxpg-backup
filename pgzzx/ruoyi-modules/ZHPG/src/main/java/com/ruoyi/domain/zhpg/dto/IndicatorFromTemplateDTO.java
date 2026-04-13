package com.ruoyi.domain.zhpg.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel("从模板生成指标请求")
public class IndicatorFromTemplateDTO {

    @ApiModelProperty(value = "模板ID", required = true)
    private Long templateId;

    @ApiModelProperty(value = "父节点ID（0或空表示根节点）")
    private Long parentId;
}

