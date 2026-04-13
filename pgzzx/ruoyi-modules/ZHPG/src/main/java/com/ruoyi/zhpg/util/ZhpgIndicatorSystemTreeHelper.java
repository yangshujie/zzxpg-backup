package com.ruoyi.zhpg.util;

import com.ruoyi.common.core.utils.StringUtils;
import com.ruoyi.domain.zhpg.EvalIndicatorSystem;

/**
 * 指标体系多份树 JSON 的选用规则：原始 {@code indicator_tree}、回传细化 {@code refined_indicator_tree}、带权重 {@code indicator_tree_weight}。
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
     * 权重计算输入：主分协同且已有回传细化树时用回传树，否则用原始指标树（内部流转仅有原始树）。
     */
    public static String jsonForWeightCalculation(EvalIndicatorSystem system) {
        if (system == null) {
            return null;
        }
        if (isMainBranchCollaboration(system) && StringUtils.isNotEmpty(system.getRefinedIndicatorTree())) {
            return system.getRefinedIndicatorTree();
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
     * 解析工作模式时的 JSON 回退顺序：优先原始树，其次回传树。
     */
    public static String jsonForWorkModeExtraction(EvalIndicatorSystem system) {
        if (system == null) {
            return null;
        }
        if (StringUtils.isNotEmpty(system.getIndicatorTree())) {
            return system.getIndicatorTree();
        }
        return system.getRefinedIndicatorTree();
    }
}
