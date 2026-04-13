package com.ruoyi.zhpgcalc.dto;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 综合计算执行响应DTO
 */
@Data
public class CalcExecuteResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 计算得分
     */
    private BigDecimal score;

    /**
     * 等级
     */
    private String grade;

    /**
     * 评估结论
     */
    private String conclusion;

    /**
     * 建议
     */
    private String suggestion;

    /**
     * 维度得分列表
     */
    private Object dimensions;

    /**
     * 执行器代码
     */
    private String executorCode;

    /**
     * 加权树节点数量
     */
    private Integer weightedTreeNodeCount;

    /**
     * 指标体系ID
     */
    private Long indicatorSystemId;

    /**
     * 指标体系名称
     */
    private String indicatorSystemName;

    /**
     * 综合计算后带各节点 score/calculatedValue 的指标树（与入参 treeData/children 结构一致，供报告阶段使用）
     * 使用 Object 类型避免双重序列化，实际类型为 JSONObject 或 JSONArray
     */
    private Object scoredIndicatorTree;

    /**
     * 中间计算结果（仅在模板开启 intermediateResultOutput 时返回）
     */
    private Object intermediateResults;
}
