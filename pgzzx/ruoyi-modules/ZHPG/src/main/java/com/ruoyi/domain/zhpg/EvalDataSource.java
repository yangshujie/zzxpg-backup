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

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.util.Date;

/**
 * 评估数据源配置对象 pgzc_eval_data_source
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ToString
@Accessors(chain = true)
@TableName("pgzc_eval_data_source")
@ApiModel(value = "评估数据源配置")
public class EvalDataSource extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    @ApiModelProperty(value = "数据源ID")
    private Long id;

    @Excel(name = "数据源编号")
    @ApiModelProperty(value = "数据源编号")
    private String sourceCode;

    @Excel(name = "数据源名称")
    @NotBlank(message = "数据源名称不能为空")
    @Size(max = 200, message = "数据源名称长度不能超过200个字符")
    @ApiModelProperty(value = "数据源名称")
    private String sourceName;

    @Excel(name = "数据源类型")
    @NotBlank(message = "数据源类型不能为空")
    @ApiModelProperty(value = "数据源类型")
    private String sourceType;

    @ApiModelProperty(value = "主机地址")
    private String host;

    @ApiModelProperty(value = "端口")
    private Integer port;

    @ApiModelProperty(value = "数据库/服务名称")
    private String databaseName;

    @ApiModelProperty(value = "模式/schema")
    private String schemaName;

    @ApiModelProperty(value = "表名/视图名")
    private String tableName;

    @ApiModelProperty(value = "REST接口地址")
    private String apiUrl;

    @ApiModelProperty(value = "请求方法")
    private String requestMethod;

    @ApiModelProperty(value = "文件路径")
    private String filePath;

    @ApiModelProperty(value = "用户名")
    private String username;

    /**
     * 字段名故意使用 password，避免操作日志输出敏感值。
     */
    @ApiModelProperty(value = "密码")
    private String password;

    @ApiModelProperty(value = "关联字段，逗号分隔")
    private String fieldNames;

    @ApiModelProperty(value = "请求参数(JSON或文本)")
    private String requestParams;

    @ApiModelProperty(value = "描述")
    private String description;

    @Excel(name = "状态")
    @ApiModelProperty(value = "状态(ENABLED/DISABLED)")
    private String status;

    @ApiModelProperty(value = "最近测试状态(SUCCESS/FAILED)")
    private String lastTestStatus;

    @ApiModelProperty(value = "最近测试结果")
    private String lastTestMessage;

    @ApiModelProperty(value = "最近测试时间")
    private Date lastTestTime;


    /** 数据库中无 remark 字段，排除 BaseEntity 继承字段 */
    @TableField(exist = false)
    private String remark;

    @Override
    public String getRemark() {
        return this.remark;
    }

    @Override
    public void setRemark(String remark) {
        this.remark = remark;
    }
}
