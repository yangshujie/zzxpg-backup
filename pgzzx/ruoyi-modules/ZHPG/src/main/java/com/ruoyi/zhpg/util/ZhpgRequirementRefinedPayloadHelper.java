package com.ruoyi.zhpg.util;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.ruoyi.common.core.utils.StringUtils;

import java.util.Map;

/**
 * 解析「需求分析 → 评估中心」下发的需求与细化指标一体报文（见下发至评估系统需求及对应指标数据格式）。
 * 从报文中取出指标树根，并生成描述文案；回传写入时可通过树根节点 {@code id}/{@code uid} 与库表 {@code id_code} 对齐匹配已有体系。
 */
public final class ZhpgRequirementRefinedPayloadHelper {

    private ZhpgRequirementRefinedPayloadHelper() {
    }

    /**
     * 从报文中解析指标树根节点（或树根数组），供外包一层 {@code {"treeData": ...}} 入库。
     * 优先级：顶层 treeData → indicatorSystem.treeData → indicatorTree（字符串或对象，支持内含 treeData）。
     */
    /**
     * 从已解析的树根对象得到用于匹配 {@link com.ruoyi.domain.zhpg.EvalIndicatorSystem#getIdCode()} 的编码：
     * 与 {@link ZhpgIndicatorTreeJsonHelper#extractRootIdCode(String)} 规则一致（含合成根壳时取第一子节点）。
     */
    public static String extractRootNodeIdForMatch(Object treeRoot) {
        if (treeRoot == null) {
            return null;
        }
        JSONObject wrap = new JSONObject();
        wrap.put("treeData", treeRoot);
        return ZhpgIndicatorTreeJsonHelper.extractRootIdCode(wrap.toJSONString());
    }

    public static Object resolveTreeRoot(JSONObject payload) {
        if (payload == null) {
            return null;
        }
        Object td = payload.get("treeData");
        if (td != null && !isBlankString(td)) {
            return td;
        }
        JSONObject ind = payload.getJSONObject("indicatorSystem");
        if (ind != null) {
            Object t2 = ind.get("treeData");
            if (t2 != null && !isBlankString(t2)) {
                return t2;
            }
        }
        Object it = payload.get("indicatorTree");
        if (it == null) {
            return null;
        }
        if (it instanceof String) {
            String s = ((String) it).trim();
            if (s.isEmpty()) {
                return null;
            }
            Object parsed = JSON.parse(s);
            return unwrapTreeDataIfPresent(parsed);
        }
        return unwrapTreeDataIfPresent(it);
    }

    private static Object unwrapTreeDataIfPresent(Object parsed) {
        if (parsed instanceof JSONObject) {
            JSONObject o = (JSONObject) parsed;
            if (o.containsKey("treeData")) {
                return o.get("treeData");
            }
            return o;
        }
        if (parsed instanceof Map) {
            Map<?, ?> m = (Map<?, ?>) parsed;
            if (m.containsKey("treeData")) {
                return m.get("treeData");
            }
            return parsed;
        }
        return parsed;
    }

    private static boolean isBlankString(Object o) {
        return o instanceof String && StringUtils.isEmpty(((String) o).trim());
    }

    public static String extractRootLabel(Object treeRoot) {
        if (treeRoot instanceof JSONObject) {
            return ((JSONObject) treeRoot).getString("label");
        }
        if (treeRoot instanceof Map) {
            Object l = ((Map<?, ?>) treeRoot).get("label");
            if (l == null) {
                return null;
            }
            String s = String.valueOf(l).trim();
            return StringUtils.isEmpty(s) ? null : s;
        }
        return null;
    }

    /**
     * 指标体系名称：优先报文 systemName，其次 根节点 label + 需求ID，再次仅根 label，再次 requirementName，
     * 再依次用 requirementId / projectId 生成可读占位名，最后才用时间戳（保证非空且尽量不撞名）。
     */
    public static String resolveSystemName(JSONObject payload, String rootLabel) {
        if (payload != null) {
            String explicit = payload.getString("systemName");
            if (StringUtils.isNotEmpty(explicit)) {
                return explicit.trim();
            }
        }
        Long rid = payload != null ? payload.getLong("requirementId") : null;
        if (StringUtils.isNotEmpty(rootLabel) && rid != null && rid > 0) {
            return rootLabel.trim() + "（需求" + rid + "）";
        }
        if (StringUtils.isNotEmpty(rootLabel)) {
            return rootLabel.trim();
        }
        if (payload != null) {
            String rn = payload.getString("requirementName");
            if (StringUtils.isNotEmpty(rn)) {
                return rn.trim();
            }
            Long pid = payload.getLong("projectId");
            if (rid != null && rid > 0 && pid != null && pid > 0) {
                return "外部下发指标体系-需求" + rid + "-项目" + pid;
            }
            if (rid != null && rid > 0) {
                return "外部下发指标体系-需求" + rid;
            }
            if (pid != null && pid > 0) {
                return "外部下发指标体系-项目" + pid;
            }
        }
        return "外部下发指标体系-" + System.currentTimeMillis();
    }

    /**
     * 将需求侧字段整理为指标体系描述（评估目的、需求/项目标识、装备与试验类型等）。
     */
    public static String buildDescription(JSONObject payload) {
        if (payload == null || payload.isEmpty()) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        String purpose = payload.getString("evaluationPurpose");
        if (StringUtils.isNotEmpty(purpose)) {
            sb.append("【评估目的】").append(purpose.trim());
        }
        Long requirementId = payload.getLong("requirementId");
        Long projectId = payload.getLong("projectId");
        Integer designType = payload.getInteger("designType");
        Integer status = payload.getInteger("status");
        Integer evaluationType = payload.getInteger("evaluationType");
        Integer evaluationLevel = payload.getInteger("evaluationLevel");
        StringBuilder meta = new StringBuilder();
        boolean first = true;
        first = appendKVPart(meta, first, "需求ID", requirementId);
        first = appendKVPart(meta, first, "项目ID", projectId);
        first = appendKVPart(meta, first, "设计类型", designType);
        first = appendKVPart(meta, first, "需求状态", status);
        first = appendKVPart(meta, first, "评估类型", evaluationType);
        first = appendKVPart(meta, first, "评估层级", evaluationLevel);
        String rn = payload.getString("requirementName");
        if (StringUtils.isNotEmpty(rn)) {
            meta.append(first ? "" : "；").append("需求名称：").append(rn.trim());
        }
        if (meta.length() > 0) {
            if (sb.length() > 0) {
                sb.append("\n\n");
            }
            sb.append("【需求与项目】").append(meta);
        }
        appendJsonLine(sb, "\n\n【被试装备】", payload.get("equipList"));
        appendJsonLine(sb, "\n\n【保障装备】", payload.get("supportEquip"));
        appendJsonLine(sb, "\n\n【评估目标】", payload.get("evaluationGoal"));
        appendJsonLine(sb, "\n\n【试验类型】", payload.get("testType"));
        return sb.toString();
    }

    private static boolean appendKVPart(StringBuilder sb, boolean first, String name, Object v) {
        if (v == null) {
            return first;
        }
        sb.append(first ? "" : "；").append(name).append("：").append(v);
        return false;
    }

    private static void appendJsonLine(StringBuilder sb, String title, Object arrOrAny) {
        if (arrOrAny == null) {
            return;
        }
        if (arrOrAny instanceof JSONArray && ((JSONArray) arrOrAny).isEmpty()) {
            return;
        }
        sb.append(title);
        if (arrOrAny instanceof String && StringUtils.isEmpty(((String) arrOrAny).trim())) {
            return;
        }
        sb.append(JSON.toJSONString(arrOrAny));
    }
}
