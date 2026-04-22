package com.ruoyi.zhpg.util;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.ruoyi.common.core.utils.StringUtils;

/**
 * Indicator tree JSON helper.
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
            case "space_recon":
            case "SPACE_RECON":
            case "航天侦察":
                return "space_recon";
            case "space_domain_awareness":
            case "SPACE_SITUATIONAL_AWARENESS":
            case "SPACE_AWARENESS":
            case "SPACE_SA":
            case "太空态势感知":
                return "space_domain_awareness";
            case "space_defense":
            case "SPACE_OFFENSE_DEFENSE":
            case "SPACE_AD":
            case "SPACE_COMBAT":
            case "太空攻防":
                return "space_defense";
            case "space_track_control":
            case "SPACE_TTC":
            case "COMM_COUNTER":
            case "航天测运控":
                return "space_track_control";
            case "space_launch":
            case "SPACE_LAUNCH":
            case "航天发射":
                return "space_launch";
            case "sea_based_space":
            case "SEA_BASED_SPACE":
            case "SEA_BASED":
            case "NAV_POSITION":
            case "海基航天":
                return "sea_based_space";
            case "SYSTEM_AGGREGATION":
            case "无":
                return "无";
            default:
                return v;
        }
    }

    private static String normalizeSourceCenterCode(String raw) {
        return normalizeIndicatorTypeCode(raw);
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
            if (flowNode.containsKey("fields")
                    && flowNode.getJSONArray("fields") != null
                    && !flowNode.getJSONArray("fields").isEmpty()) {
                flowNode.remove("field");
            }
            String source = flowNode.getString("source");
            if (StringUtils.isNotEmpty(source)) {
                flowNode.put("source", normalizeSourceCenterCode(source));
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
     * Normalize workMode / indicatorType / valueCategory(type) / start.source in indicator tree JSON.
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
     * Parsed tree metadata for weight calculation.
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
     * Parse indicator tree JSON to root array used by weight calculation.
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
     * Serialize weighted roots back to the original outer shape.
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
     * Extract workMode from indicator tree JSON.
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
     * Prefer business id in `id`, fallback to `uid`.
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
     * Extract root id code from indicator tree JSON.
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
