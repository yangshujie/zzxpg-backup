package com.ruoyi.domain.ooda;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.annotation.Excel;
import com.ruoyi.common.core.web.domain.BaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.ToString;
import lombok.experimental.Accessors;

@ApiModel(value="测试表对象",description="测试表对象")
@Data
@Accessors(chain = true)//允许链式访问
@ToString
@TableName("table_one")
public class TableOne
{
    @ApiModelProperty(value="ID")
    @Excel(name = "id", cellType = Excel.ColumnType.NUMERIC)
    @TableId(value = "id", type = IdType.AUTO)
    private String id;

    @ApiModelProperty(value="名称")
    @Excel(name = "名称")
    private String name;
}
