package com.ruoyi.service.zhpg;

import com.ruoyi.domain.zhpg.dto.WeightApplyResult;

/**
 * 指标树权重：层内均分或按正值比例归一（不依赖外部 Python 包）
 */
public interface IIndicatorTreeWeightService {

    /**
     * @param indicatorTreeJson 指标树 JSON（推荐 {"treeData":根对象}；兼容顶层数组）
     * @param strategy          AUTO | EQUAL | RENORMALIZE
     * @param weightAssignAlgorithm 可选，AUTO 时若已选算法仅作提示用
     */
    WeightApplyResult applyWeights(String indicatorTreeJson, String strategy, String weightAssignAlgorithm);
}
