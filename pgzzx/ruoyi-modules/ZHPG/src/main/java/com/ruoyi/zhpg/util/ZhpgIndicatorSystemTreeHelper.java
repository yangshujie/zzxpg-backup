package com.ruoyi.zhpg.util;

import com.ruoyi.common.core.utils.StringUtils;
import com.ruoyi.domain.zhpg.EvalIndicatorSystem;

/**
 * 指标体系多份树 JSON 的选用规则：原始 {@code indicator_tree}、结果/带权重 {@code indicator_tree_weight}。
 */
public final class ZhpgIndicatorSystemTreeHelper {

    private ZhpgIndicatorSystemTreeHelper() {
    }

    public static boolean isMainBranchCollaboration(EvalIndicatorSystem system) {
        if (system == null) {
            return false;
        }
        return "主分协同".equals(ZhpgIndicatorTreeJsonHelper.normalizeWorkModeCode(system.getWorkMode(), ""));
    }

    /**
     * 业务主用树选用规则：优先使用包含回传细化或权重结果的 {@code indicator_tree_weight}，否则用原始 {@code indicator_tree}。
     */
    public static String jsonForWeightCalculation(EvalIndicatorSystem system) {
        if (system == null) {
            return null;
        }
        if (StringUtils.isNotEmpty(system.getIndicatorTreeWeight())) {
            return system.getIndicatorTreeWeight();
        }
        return system.getIndicatorTree();
    }

    /**
     * 与 {@link #jsonForWeightCalculation(EvalIndicatorSystem)} 一致：根节点 id 编码与业务主用树对齐。
     */
    public static String jsonForRootIdCodeSync(EvalIndicatorSystem system) {
        return jsonForWeightCalculation(system);
    }

    /**
     * 解析工作模式时的 JSON 回退顺序：优先原始树，其次结果树。
     */
    public static String jsonForWorkModeExtraction(EvalIndicatorSystem system) {
        if (system == null) {
            return null;
        }
        if (StringUtils.isNotEmpty(system.getIndicatorTree())) {
            return system.getIndicatorTree();
        }
        return system.getIndicatorTreeWeight();
    }
}
