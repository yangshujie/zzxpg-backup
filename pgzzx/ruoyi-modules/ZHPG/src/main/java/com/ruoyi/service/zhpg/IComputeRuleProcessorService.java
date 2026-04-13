package com.ruoyi.service.zhpg;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;

/**
 * 指标计算规则处理器：解析 computeRule，生成模拟数据并调用算法服务计算指标值
 */
public interface IComputeRuleProcessorService {

    /**
     * 处理指标树中所有叶子节点的 computeRule，计算指标值并填充到节点中
     *
     * @param indicatorTreeJson 指标树JSON
     * @param mockSampleRows    模拟数据行数（默认8）
     * @return 处理后的指标树JSON（叶子节点带有score/calculatedValue）
     */
    String processLeafNodesWithMockData(String indicatorTreeJson, int mockSampleRows);

    /**
     * 处理单个叶子节点的 computeRule
     *
     * @param leafNode       叶子节点JSON
     * @param mockSampleRows 模拟数据行数
     * @return 计算结果（指标得分），如果无法计算则返回null
     */
    Double processSingleLeafNode(JSONObject leafNode, int mockSampleRows);

    /**
     * 递归处理 computeRule 中的算法节点
     *
     * @param computeRule    computeRule JSON对象
     * @param targetId       目标节点ID
     * @param mockSampleRows 模拟数据行数
     * @return 计算结果列表
     */
    JSONArray computeRuleRecursive(JSONObject computeRule, String targetId, int mockSampleRows);
}
