package com.ruoyi.zhpgcalc.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 综合计算执行请求DTO
 */
@Data
public class CalcExecuteRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 计算任务ID
     */
    private Long taskId;

    /**
     * 任务名称
     */
    private String taskName;

    /**
     * 模板ID
     */
    private Long templateId;

    /**
     * 模板名称
     */
    private String templateName;

    /**
     * 指标体系ID
     */
    private Long indicatorSystemId;

    /**
     * 指标体系名称
     */
    private String indicatorSystemName;

    /**
     * 评估任务ID（需求ID）
     */
    private Long assessTaskId;

    /**
     * 加权后的指标树JSON
     */
    private String weightedTreeJson;

    /**
     * 综合计算阶段配置
     */
    private Object comprehensiveConfig;

    /**
     * 调度阶段配置
     */
    private Object scheduleConfig;

    /**
     * 运行时策略配置
     */
    private Object runtimePolicy;

    /**
     * 预处理批次ID
     */
    private Long batchId;
}
