package com.ruoyi.domain.zhpg.dto;

/**
 * 指标体系主观赋权计算结果
 *
 * 调用方根据每个父节点上保存的 weightAssignAlgorithm（不校验 / 相似度法 / 校验 / 理论证据法
 * / 连环比率法 / 层次分析）以及对应子节点参数（importance / weightTemp / weightTemp1 /
 * expertWeight / bz / jz）逐层算出权重，写回 weight 字段。
 */
public class SubjectiveWeightComputeResult {

    private final String indicatorTreeWeight;
    private final String hint;
    private final int parentNodeCount;
    private final int ahpFailCount;

    public SubjectiveWeightComputeResult(String indicatorTreeWeight, String hint, int parentNodeCount, int ahpFailCount) {
        this.indicatorTreeWeight = indicatorTreeWeight;
        this.hint = hint == null ? "" : hint;
        this.parentNodeCount = parentNodeCount;
        this.ahpFailCount = ahpFailCount;
    }

    public String getIndicatorTreeWeight() {
        return indicatorTreeWeight;
    }

    public String getHint() {
        return hint;
    }

    public int getParentNodeCount() {
        return parentNodeCount;
    }

    public int getAhpFailCount() {
        return ahpFailCount;
    }
}
