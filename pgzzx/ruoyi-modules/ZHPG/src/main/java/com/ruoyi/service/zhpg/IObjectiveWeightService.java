package com.ruoyi.service.zhpg;

import com.ruoyi.domain.zhpg.dto.ObjectiveWeightComputeResult;
import com.alibaba.fastjson2.JSONObject;

/**
 * 指标体系客观赋权：模拟样本数据 + zgpg_algs 权重算法
 */
public interface IObjectiveWeightService {

    /**
     * 对指定指标体系计算客观权重并可选写库。
     *
     * @param systemId 指标体系主键
     * @param options  可选：persist（默认 true）、mockSampleRows（默认 8）、mockSeed（可选 Long）
     * @param operator  写库时的更新人（persist=true 时建议非空）
     */
    ObjectiveWeightComputeResult computeForSystem(Long systemId, JSONObject options, String operator);
}
