package com.ruoyi.service.zhpg;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.ruoyi.domain.zhpg.dto.SubjectiveWeightComputeResult;
import com.ruoyi.domain.zhpg.dto.WeightApplyResult;

/**
 * 指标体系主观赋权：根据各父节点上保存的算法（不校验 / 相似度法 / 校验 / 理论证据法
 * / 连环比率法 / 层次分析）以及子节点参数计算权重。
 *
 * 与 {@link IObjectiveWeightService} 平行：客观赋权依赖外部算法服务（zgpg_algs）以
 * 模拟样本走 cal_weight / ahp；主观赋权全部由本地 Java 实现，不依赖外部服务，参数完全
 * 来自前端 6 种交互输入。
 */
public interface ISubjectiveWeightService {

    /**
     * AHP 一致性校验：CR = CI / RI ≤ 0.1 时返回 true。
     *
     * 输入为 n×n 互反判断矩阵（元素可为数字或 "a/b" 字符串）。
     *
     * @param matrix 比值矩阵
     * @return CR 值，<= 0.1 视为通过
     */
    double calcAhpCr(JSONArray matrix);

    /** 便捷判断：CR <= 0.1。 */
    boolean checkAHP(JSONArray matrix);

    /**
     * 主观赋权：读取指标体系上保存的带每节点算法配置的指标树，按层级计算各子节点权重并归一。
     *
     * @param systemId 指标体系主键
     * @param options  可选：persist（默认 true）
     * @param operator 写库 update_by（persist=true 时建议非空）
     */
    SubjectiveWeightComputeResult computeForSystem(Long systemId, JSONObject options, String operator);

    /**
     * 直接基于传入的指标树 JSON（前端已编辑好的整树）做主观赋权计算，不读库。
     *
     * 主要用于 CalcTaskServiceImpl 在不持久化的情况下，每次计算前临时算一份带权重的树。
     *
     * @param indicatorTreeJson 指标树 JSON（推荐 {"treeData":根对象}）
     */
    SubjectiveWeightComputeResult computeForTreeJson(String indicatorTreeJson);

    /**
     * 保存用户手动编辑后的指标树权重（对应前端「权重分配调优」对话框「完成」操作）。
     *
     * 流程：归一化（层内 RENORMALIZE） → 写入 pgzc_indicator_system.indicator_tree_weight。
     */
    WeightApplyResult applyUserEditedTreeWeights(Long systemId, String userEditedTreeJson, String operator);

    /**
     * 对单个父节点执行主观赋权计算（不递归）。直接修改 children 数组中的节点权重。
     * @param stat 用于回传统计信息（如 ahpFailCount）
     */
    void computeSingleNode(JSONObject parent, JSONArray children, JSONObject stat);
}
