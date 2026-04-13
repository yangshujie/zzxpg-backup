package com.ruoyi.domain.zhpg.dto;

/**
 * 指标树权重计算（层内归一 / 均分）结果
 */
public class WeightApplyResult {

    private final String indicatorTree;
    private final String hint;

    public WeightApplyResult(String indicatorTree, String hint) {
        this.indicatorTree = indicatorTree;
        this.hint = hint == null ? "" : hint;
    }

    public String getIndicatorTree() {
        return indicatorTree;
    }

    public String getHint() {
        return hint;
    }
}
