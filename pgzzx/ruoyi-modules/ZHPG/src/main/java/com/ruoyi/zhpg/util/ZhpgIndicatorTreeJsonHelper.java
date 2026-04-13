package com.ruoyi.zhpg.util;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.ruoyi.common.core.utils.StringUtils;

/**
 * 指标体系 indicator_tree：与原先评估项目一致，推荐形态为 {@code {"treeData": { ... 根节点 ... }}}。
 * 仍兼容顶层为 JSON 数组的历史数据。
 */
public final class ZhpgIndicatorTreeJsonHelper {

    public static final String FOREST_ROOT_UID = "__zhpg_forest_root__";

    private ZhpgIndicatorTreeJsonHelper() {
    }

    public static String normalizeWorkModeCode(String raw, String fallback) {
        if (StringUtils.isEmpty(raw)) {
            return fallback;
        }
        String v = raw.trim();
        if ("MAIN_BRANCH_COLLAB".equals(v) || "主分协同".equals(v)) {
            return "主分协同";
        }
        if ("INTERNAL_CIRCULATION".equals(v) || "内部流转".equals(v)) {
            return "内部流转";
        }
        return fallback;
    }

    private static String normalizeIndicatorTypeCode(String raw) {
        if (StringUtils.isEmpty(raw)) {
            return raw;
        }
        String v = raw.trim();
        switch (v) {
            case "SPACE_RECON":
            case "航天侦察":
                return "航天侦察";
            case "SPACE_SITUATIONAL_AWARENESS":
            case "SPACE_AWARENESS":
            case "SPACE_SA":
            case "太空态势感知":
                return "太空态势感知";
            case "SPACE_OFFENSE_DEFENSE":
            case "SPACE_AD":
            case "SPACE_COMBAT":
            case "太空攻防":
                return "太空攻防";
            case "SPACE_TTC":
            case "COMM_COUNTER":
            case "航天测运控":
                return "航天测运控";
            case "SPACE_LAUNCH":
            case "航天发射":
                return "航天发射";
            case "SEA_BASED_SPACE":
            case "SEA_BASED":
            case "NAV_POSITION":
            case "海基航天":
                return "海基航天";
            case "SYSTEM_AGGREGATION":
            case "无":
                return "无";
            default:
                return v;
        }
    }

    private static String normalizeValueCategoryCode(String raw) {
        if (StringUtils.isEmpty(raw)) {
            return raw;
        }
        String v = raw.trim();
        switch (v) {
            case "COST":
            case "成本型":
                return "成本型";
            case "BENEFIT":
            case "效益型":
                return "效益型";
            case "INTERVAL_BENEFIT":
            case "区间效益型":
                return "区间效益型";
            default:
                return v;
        }
    }

    private static void normalizeComputeRuleStartFields(JSONObject node) {
        if (node == null) {
            return;
        }
        JSONObject computeRule = node.getJSONObject("computeRule");
        if (computeRule == null) {
            return;
        }
        JSONObject method = computeRule.getJSONObject("method");
        if (method == null) {
            return;
        }
        JSONArray methodNodes = method.getJSONArray("node");
        if (methodNodes == null || methodNodes.isEmpty()) {
            return;
        }
        for (int i = 0; i < methodNodes.size(); i++) {
            JSONObject flowNode = methodNodes.getJSONObject(i);
            if (flowNode == null) {
                continue;
            }
            if (!"start".equals(flowNode.getString("type"))) {
                continue;
            }
            JSONArray fields = flowNode.getJSONArray("fields");
            if (fields == null || fields.isEmpty()) {
                String singleField = flowNode.getString("field");
                if (StringUtils.isNotEmpty(singleField)) {
                    JSONArray one = new JSONArray();
                    one.add(singleField);
                    flowNode.put("fields", one);
                }
            }
            if (flowNode.containsKey("fields") && flowNode.getJSONArray("fields") != null
                    && !flowNode.getJSONArray("fields").isEmpty()) {
                flowNode.remove("field");
            }
        }
    }

    private static void normalizeTreeNode(JSONObject node) {
        if (node == null) {
            return;
        }
        String indicatorType = node.getString("indicatorType");
        if (StringUtils.isNotEmpty(indicatorType)) {
            node.put("indicatorType", normalizeIndicatorTypeCode(indicatorType));
        }

        String workMode = node.getString("workMode");
        if (StringUtils.isNotEmpty(workMode)) {
            node.put("workMode", normalizeWorkModeCode(workMode, "内部流转"));
        }

        String valueCategory = node.getString("valueCategory");
        if (StringUtils.isEmpty(valueCategory)) {
            valueCategory = node.getString("type");
        }
        if (StringUtils.isNotEmpty(valueCategory)) {
            node.put("valueCategory", normalizeValueCategoryCode(valueCategory));
        }

        normalizeComputeRuleStartFields(node);

        JSONArray children = node.getJSONArray("children");
        if (children == null || children.isEmpty()) {
            return;
        }
        for (int i = 0; i < children.size(); i++) {
            JSONObject child = children.getJSONObject(i);
            normalizeTreeNode(child);
        }
    }

    /**
     * 规范化指标树中的 workMode / indicatorType / valueCategory(type) 字段编码。
     * 若输入无法解析为 JSON，则原样返回。
     */
    public static String normalizeIndicatorTreeTypes(String indicatorTreeJson) {
        if (StringUtils.isEmpty(indicatorTreeJson)) {
            return indicatorTreeJson;
        }
        try {
            String trim = indicatorTreeJson.trim();
            Object o = JSON.parse(trim);
            if (o instanceof JSONArray) {
                JSONArray arr = (JSONArray) o;
                for (int i = 0; i < arr.size(); i++) {
                    normalizeTreeNode(arr.getJSONObject(i));
                }
                return arr.toJSONString();
            }
            if (o instanceof JSONObject) {
                JSONObject obj = (JSONObject) o;
                if (obj.containsKey("treeData")) {
                    Object td = obj.get("treeData");
                    if (td instanceof JSONObject) {
                        normalizeTreeNode((JSONObject) td);
                    } else if (td instanceof JSONArray) {
                        JSONArray arr = (JSONArray) td;
                        for (int i = 0; i < arr.size(); i++) {
                            normalizeTreeNode(arr.getJSONObject(i));
                        }
                    }
                    return obj.toJSONString();
                }
                normalizeTreeNode(obj);
                return obj.toJSONString();
            }
            return indicatorTreeJson;
        } catch (Exception ignored) {
            return indicatorTreeJson;
        }
    }

    /**
     * 供权重遍历使用的根列表，以及写回时是否包一层 treeData。
     */
    public static final class ParsedTree {
        private final JSONArray rootsForWeight;
        private final boolean serializeAsTreeData;

        public ParsedTree(JSONArray rootsForWeight, boolean serializeAsTreeData) {
            this.rootsForWeight = rootsForWeight != null ? rootsForWeight : new JSONArray();
            this.serializeAsTreeData = serializeAsTreeData;
        }

        public JSONArray getRootsForWeight() {
            return rootsForWeight;
        }

        public boolean isSerializeAsTreeData() {
            return serializeAsTreeData;
        }
    }

    /**
     * 解析为权重算法使用的根数组（单根 treeData 时为含一个元素的数组）。
     */
    public static ParsedTree parseForWeight(String indicatorTreeJson) {
        if (StringUtils.isEmpty(indicatorTreeJson)) {
            return new ParsedTree(new JSONArray(), true);
        }
        String trim = indicatorTreeJson.trim();
        Object o = JSON.parse(trim);
        if (o instanceof JSONArray) {
            return new ParsedTree((JSONArray) o, false);
        }
        if (o instanceof JSONObject) {
            JSONObject obj = (JSONObject) o;
            if (obj.containsKey("treeData")) {
                Object td = obj.get("treeData");
                JSONArray one = new JSONArray();
                if (td instanceof JSONObject) {
                    one.add(td);
                    return new ParsedTree(one, true);
                }
                if (td instanceof JSONArray) {
                    return new ParsedTree((JSONArray) td, true);
                }
                return new ParsedTree(new JSONArray(), true);
            }
            JSONArray one = new JSONArray();
            one.add(obj);
            return new ParsedTree(one, false);
        }
        return new ParsedTree(new JSONArray(), true);
    }

    /**
     * 权重计算后写回字符串，保持与入参相同的外层形态。
     */
    public static String serializeAfterWeight(ParsedTree meta, JSONArray mutatedRoots) {
        if (mutatedRoots == null) {
            mutatedRoots = new JSONArray();
        }
        if (!meta.isSerializeAsTreeData()) {
            return JSON.toJSONString(mutatedRoots);
        }
        JSONObject out = new JSONObject();
        if (mutatedRoots.size() == 1) {
            out.put("treeData", mutatedRoots.get(0));
        } else {
            JSONObject forest = new JSONObject();
            forest.put("uid", FOREST_ROOT_UID);
            forest.put("label", "指标体系");
            forest.put("indicatorType", "无");
            forest.put("children", mutatedRoots);
            out.put("treeData", forest);
        }
        return out.toJSONString();
    }

    /**
     * 从指标树 JSON 读取 workMode（根节点或合成多根壳上的字段）。
     */
    public static String extractWorkMode(String indicatorTreeJson, String defaultMode) {
        if (StringUtils.isEmpty(indicatorTreeJson)) {
            return defaultMode;
        }
        try {
            ParsedTree p = parseForWeight(indicatorTreeJson.trim());
            JSONArray roots = p.getRootsForWeight();
            if (roots.isEmpty()) {
                return defaultMode;
            }
            JSONObject root = roots.getJSONObject(0);
            if (root == null) {
                return defaultMode;
            }
            if (FOREST_ROOT_UID.equals(root.getString("uid"))) {
                String wm = root.getString("workMode");
                if (StringUtils.isNotEmpty(wm)) {
                    return normalizeWorkModeCode(wm, defaultMode);
                }
                JSONArray ch = root.getJSONArray("children");
                if (ch != null && !ch.isEmpty()) {
                    JSONObject c0 = ch.getJSONObject(0);
                    if (c0 != null) {
                        wm = c0.getString("workMode");
                        if (StringUtils.isNotEmpty(wm)) {
                            return normalizeWorkModeCode(wm, defaultMode);
                        }
                    }
                }
                return defaultMode;
            }
            String wm = root.getString("workMode");
            return StringUtils.isNotEmpty(wm) ? normalizeWorkModeCode(wm, defaultMode) : defaultMode;
        } catch (Exception ignored) {
            return defaultMode;
        }
    }

    private static boolean isForestShellNode(JSONObject root) {
        if (root == null) {
            return false;
        }
        if (FOREST_ROOT_UID.equals(root.getString("uid"))) {
            return true;
        }
        return FOREST_ROOT_UID.equals(root.getString("id"));
    }

    /**
     * 读取节点在 JSON 中的业务 id：优先 id，其次 uid（与前端工作台一致）。
     */
    public static String nodeBusinessId(JSONObject node) {
        if (node == null) {
            return null;
        }
        String id = node.getString("id");
        if (StringUtils.isNotEmpty(id)) {
            return id.trim();
        }
        String uid = node.getString("uid");
        return StringUtils.isNotEmpty(uid) ? uid.trim() : null;
    }

    /**
     * 从指标树 JSON 解析「根节点」的全局 id 编码：单根取该根；多根壳（合成根）取第一个真实子节点。
     */
    public static String extractRootIdCode(String indicatorTreeJson) {
        if (StringUtils.isEmpty(indicatorTreeJson)) {
            return null;
        }
        try {
            ParsedTree p = parseForWeight(indicatorTreeJson.trim());
            JSONArray roots = p.getRootsForWeight();
            if (roots == null || roots.isEmpty()) {
                return null;
            }
            JSONObject root = roots.getJSONObject(0);
            if (root == null) {
                return null;
            }
            if (isForestShellNode(root)) {
                JSONArray ch = root.getJSONArray("children");
                if (ch != null && !ch.isEmpty()) {
                    return nodeBusinessId(ch.getJSONObject(0));
                }
                return null;
            }
            return nodeBusinessId(root);
        } catch (Exception ignored) {
            return null;
        }
    }
}
