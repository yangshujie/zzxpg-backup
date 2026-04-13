package com.ruoyi.domain.zhpg.dto;

/**
 * 指标体系客观赋权（算法服务）计算结果
 */
public class ObjectiveWeightComputeResult {

    private final String indicatorTreeWeight;
    private final String hint;
    private final String mockNote;
    private final int algorithmCallCount;

    public ObjectiveWeightComputeResult(String indicatorTreeWeight, String hint, String mockNote, int algorithmCallCount) {
        this.indicatorTreeWeight = indicatorTreeWeight;
        this.hint = hint == null ? "" : hint;
        this.mockNote = mockNote == null ? "" : mockNote;
        this.algorithmCallCount = algorithmCallCount;
    }

    public String getIndicatorTreeWeight() {
        return indicatorTreeWeight;
    }

    public String getHint() {
        return hint;
    }

    public String getMockNote() {
        return mockNote;
    }

    public int getAlgorithmCallCount() {
        return algorithmCallCount;
    }
}
