package com.ruoyi.domain.zhpg.dto;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 向 xxl-job-admin /jobinfo/add 或 /jobinfo/update 提交的请求体，
 * 字段名与 XxlJobInfo 一一对应。
 */
@Data
@Accessors(chain = true)
public class XxlJobInfoRequest {

    private Integer id;
    private int jobGroup;
    private String jobDesc;
    private String author;
    private String scheduleType;
    private String scheduleConf;
    private String misfireStrategy;
    private String executorRouteStrategy;
    private String executorHandler;
    private String executorParam;
    private String executorBlockStrategy;
    private int executorTimeout;
    private int executorFailRetryCount;
    private String glueType;
    private String glueRemark;
    private String taskName;

    /**
     * 触发状态：0-停止，1-运行
     */
    private int triggerStatus;
}
