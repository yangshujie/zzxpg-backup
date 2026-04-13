package com.ruoyi.domain.zhpg.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.Date;

@Data
@ApiModel(value = "数据源测试结果")
public class DataSourceTestResult {

    @ApiModelProperty(value = "是否成功")
    private Boolean success;

    @ApiModelProperty(value = "测试状态")
    private String status;

    @ApiModelProperty(value = "结果消息")
    private String message;

    @ApiModelProperty(value = "测试时间")
    private Date testTime;
}
