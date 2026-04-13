package com.xxl.job.admin.core.model;

import com.alibaba.fastjson2.JSONArray;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.annotation.Excel;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.util.Date;

/**
 * xxl-job info
 *
 * @author xuxueli  2016-1-12 18:25:49
 */
@ApiModel(value="任务对象",description="任务对象")
@Data
@NoArgsConstructor
@ToString           //自动override toString方法
@Accessors(chain = true)  //允许链式访问，User user=new User().setAge(31).setName("pollyduan");//返回对象
@TableName("xxl_job_info")
public class XxlJobInfo {

    @ApiModelProperty(value="任务主键")
    @Excel(name = "任务主键", cellType = Excel.ColumnType.NUMERIC)
    @TableId(value = "id", type = IdType.AUTO)
    private int id;                // 主键ID

    @ApiModelProperty(value="执行器主键ID，评估任务1，采集任务2")
    private int jobGroup;        // 执行器主键ID

    @ApiModelProperty(value="任务描述")
    private String jobDesc;

    @ApiModelProperty(value="添加时间（不传）")
    private Date addTime;

    @ApiModelProperty(value="更新时间（不传）")
    private Date updateTime;

    @ApiModelProperty(value="创建人")
    @Excel(name = "创建人")
    private String author;

    @ApiModelProperty(value="报警邮件（不传）")
    private String alarmEmail;    // 报警邮件

    @ApiModelProperty(value="调度类型，固定CRON")
    private String scheduleType;            // 调度类型

    @ApiModelProperty(value="cron表达式")
    @Excel(name = "cron表达式")
    private String scheduleConf;            // 调度配置，值含义取决于调度类型

    @ApiModelProperty(value="调度过期策略，固定 DO_NOTHING")
    private String misfireStrategy;            // 调度过期策略

    @ApiModelProperty(value="执行器路由策略")
    @Excel(name = "执行器路由策略")
    private String executorRouteStrategy;    // 执行器路由策略

    @ApiModelProperty(value="执行器，任务Handler名称")
    @Excel(name = "执行器，任务Handler名称")
    private String executorHandler;            // 执行器，任务Handler名称

    @ApiModelProperty(value="执行器，任务参数，（不传）")
    @Excel(name = "执行器，任务参数")
    private String executorParam;            // 执行器，任务参数

    @ApiModelProperty(value="阻塞处理策略")
    @Excel(name = "阻塞处理策略")
    private String executorBlockStrategy;    // 阻塞处理策略

    @ApiModelProperty(value="任务执行超时时间，固定0")
    private int executorTimeout;            // 任务执行超时时间，单位秒

    @ApiModelProperty(value="失败重试次数，固定0")
    private int executorFailRetryCount;        // 失败重试次数


    @ApiModelProperty(value="GLUE类型，固定BEAN")
    private String glueType;        // GLUE类型	#com.xxl.job.core.glue.GlueTypeEnum

    @ApiModelProperty(value="GLUE源代码，不传")
    private String glueSource;        // GLUE源代码

    @ApiModelProperty(value="GLUE备注，固定 GLUE代码初始化")
    private String glueRemark;        // GLUE备注

    @ApiModelProperty(value="GLUE更新时间，不传")
    private Date glueUpdatetime;    // GLUE更新时间


    @ApiModelProperty(value="子任务ID，多个逗号分隔，不传")
    private String childJobId;        // 子任务ID，多个逗号分隔

    @ApiModelProperty(value="调度状态：0-停止，1-运行，固定 0")
    private int triggerStatus;        // 调度状态：0-停止，1-运行

    @ApiModelProperty(value="上次调度时间，固定 0")
    private long triggerLastTime;    // 上次调度时间

    @ApiModelProperty(value="下次调度时间，固定 0")
    private long triggerNextTime;    // 下次调度时间


    ////////////////////////////////////////////
    @ApiModelProperty(value="任务名称")
    @Excel(name = "任务名称")
    private String taskName;

    @ApiModelProperty(value="评估对象类型")
    @Excel(name = "评估对象类型")
    private String assessObjectType; // 星座、星间链路、卫星（北斗、墨子）、分系统、单机

    @ApiModelProperty(value="评估对象")
    @Excel(name = "评估对象")
    private String assessObject; // 具体星座、具体卫星（BD3G01）、分系统、单机，比如 北斗

    @ApiModelProperty(value="数据开始时间")
    @Excel(name = "数据开始时间")
    private String startTime;

    @ApiModelProperty(value="数据结束时间")
    @Excel(name = "数据结束时间")
    private String endTime;

    @ApiModelProperty(value="指标体系id")
    @Excel(name = "指标体系id")
    private Long indicatorSystemId;

    @ApiModelProperty(value = "是否为周期任务1：是   0：否")
    private Integer taskExecType;

    @ApiModelProperty("评估周期时长(单位：天/周/月/年)")
    private Integer assessDuration;

    @ApiModelProperty("任务类型")
    private String firstLevel;


    @ApiModelProperty("外部任务ip")
    private String ip;

    @ApiModelProperty("剔野算法id")
    @TableField(exist = false)
    private Long tyAlgId;

    @ApiModelProperty("填充算法id")
    @TableField(exist = false)
    private Long tcAlgId;

    @ApiModelProperty("级联关系选择列表")
    @TableField(exist = false)
    private String selectList;

    public Long getTyAlgId() {
        return tyAlgId;
    }

    public void setTyAlgId(Long tyAlgId) {
        this.tyAlgId = tyAlgId;
    }

    public Long getTcAlgId() {
        return tcAlgId;
    }

    public void setTcAlgId(Long tcAlgId) {
        this.tcAlgId = tcAlgId;
    }

    public Integer getAssessDuration() {
        return assessDuration;
    }

    public void setAssessDuration(Integer assessDuration) {
        this.assessDuration = assessDuration;
    }

    public String getAssessObjectType() {
        return assessObjectType;
    }

    public void setAssessObjectType(String assessObjectType) {
        this.assessObjectType = assessObjectType;
    }

    public String getAssessObject() {
        return assessObject;
    }

    public void setAssessObject(String assessObject) {
        this.assessObject = assessObject;
    }

    public String getStartTime() {
        return startTime;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public String getEndTime() {
        return endTime;
    }

    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }

    public Long getIndicatorSystemId() {
        return indicatorSystemId;
    }

    public void setIndicatorSystemId(Long indicatorSystemId) {
        this.indicatorSystemId = indicatorSystemId;
    }

    public Integer getTaskExecType() {
        return taskExecType;
    }

    public void setTaskExecType(Integer taskExecType) {
        this.taskExecType = taskExecType;
    }

    public String getFirstLevel() {
        return firstLevel;
    }

    public void setFirstLevel(String firstLevel) {
        this.firstLevel = firstLevel;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }
//    public int getId() {
//        return id;
//    }
//
//    public void setId(int id) {
//        this.id = id;
//    }
//
//    public int getJobGroup() {
//        return jobGroup;
//    }
//
//    public void setJobGroup(int jobGroup) {
//        this.jobGroup = jobGroup;
//    }
//
//    public String getJobDesc() {
//        return jobDesc;
//    }
//
//    public void setJobDesc(String jobDesc) {
//        this.jobDesc = jobDesc;
//    }
//
//    public Date getAddTime() {
//        return addTime;
//    }
//
//    public void setAddTime(Date addTime) {
//        this.addTime = addTime;
//    }
//
//    public Date getUpdateTime() {
//        return updateTime;
//    }
//
//    public void setUpdateTime(Date updateTime) {
//        this.updateTime = updateTime;
//    }
//
//    public String getAuthor() {
//        return author;
//    }
//
//    public void setAuthor(String author) {
//        this.author = author;
//    }
//
//    public String getAlarmEmail() {
//        return alarmEmail;
//    }
//
//    public void setAlarmEmail(String alarmEmail) {
//        this.alarmEmail = alarmEmail;
//    }
//
//    public String getScheduleType() {
//        return scheduleType;
//    }
//
//    public void setScheduleType(String scheduleType) {
//        this.scheduleType = scheduleType;
//    }
//
//    public String getScheduleConf() {
//        return scheduleConf;
//    }
//
//    public void setScheduleConf(String scheduleConf) {
//        this.scheduleConf = scheduleConf;
//    }
//
//    public String getMisfireStrategy() {
//        return misfireStrategy;
//    }
//
//    public void setMisfireStrategy(String misfireStrategy) {
//        this.misfireStrategy = misfireStrategy;
//    }
//
//    public String getExecutorRouteStrategy() {
//        return executorRouteStrategy;
//    }
//
//    public void setExecutorRouteStrategy(String executorRouteStrategy) {
//        this.executorRouteStrategy = executorRouteStrategy;
//    }
//
//    public String getExecutorHandler() {
//        return executorHandler;
//    }
//
//    public void setExecutorHandler(String executorHandler) {
//        this.executorHandler = executorHandler;
//    }
//
//    public String getExecutorParam() {
//        return executorParam;
//    }
//
//    public void setExecutorParam(String executorParam) {
//        this.executorParam = executorParam;
//    }
//
//    public String getExecutorBlockStrategy() {
//        return executorBlockStrategy;
//    }
//
//    public void setExecutorBlockStrategy(String executorBlockStrategy) {
//        this.executorBlockStrategy = executorBlockStrategy;
//    }
//
//    public int getExecutorTimeout() {
//        return executorTimeout;
//    }
//
//    public void setExecutorTimeout(int executorTimeout) {
//        this.executorTimeout = executorTimeout;
//    }
//
//    public int getExecutorFailRetryCount() {
//        return executorFailRetryCount;
//    }
//
//    public void setExecutorFailRetryCount(int executorFailRetryCount) {
//        this.executorFailRetryCount = executorFailRetryCount;
//    }
//
//    public String getGlueType() {
//        return glueType;
//    }
//
//    public void setGlueType(String glueType) {
//        this.glueType = glueType;
//    }
//
//    public String getGlueSource() {
//        return glueSource;
//    }
//
//    public void setGlueSource(String glueSource) {
//        this.glueSource = glueSource;
//    }
//
//    public String getGlueRemark() {
//        return glueRemark;
//    }
//
//    public void setGlueRemark(String glueRemark) {
//        this.glueRemark = glueRemark;
//    }
//
//    public Date getGlueUpdatetime() {
//        return glueUpdatetime;
//    }
//
//    public void setGlueUpdatetime(Date glueUpdatetime) {
//        this.glueUpdatetime = glueUpdatetime;
//    }
//
//    public String getChildJobId() {
//        return childJobId;
//    }
//
//    public void setChildJobId(String childJobId) {
//        this.childJobId = childJobId;
//    }
//
//    public int getTriggerStatus() {
//        return triggerStatus;
//    }
//
//    public void setTriggerStatus(int triggerStatus) {
//        this.triggerStatus = triggerStatus;
//    }
//
//    public long getTriggerLastTime() {
//        return triggerLastTime;
//    }
//
//    public void setTriggerLastTime(long triggerLastTime) {
//        this.triggerLastTime = triggerLastTime;
//    }
//
//    public long getTriggerNextTime() {
//        return triggerNextTime;
//    }
//
//    public void setTriggerNextTime(long triggerNextTime) {
//        this.triggerNextTime = triggerNextTime;
//    }
}
