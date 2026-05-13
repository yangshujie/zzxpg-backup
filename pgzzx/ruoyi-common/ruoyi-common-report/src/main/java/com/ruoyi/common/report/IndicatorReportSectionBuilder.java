package com.ruoyi.common.report;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class IndicatorReportSectionBuilder {

    private IndicatorReportSectionBuilder() {
    }

    public static List<Section> buildSections(Object scoredIndicatorTree) {
        JSONArray roots = toRoots(scoredIndicatorTree);
        List<Section> sections = new ArrayList<>();
        for (int i = 0; i < roots.size(); i++) {
            appendNode(roots.getJSONObject(i), String.valueOf(i + 1), 2, sections);
        }
        return sections;
    }

    public static String cleanTaskName(String raw) {
        if (raw == null) return "";
        return raw.trim().replaceFirst("-\\d{10,}$", "").trim();
    }

    public static String cleanIndicatorSystemName(String raw) {
        if (raw == null) return "";
        return raw.trim()
                .replaceAll("[（(][^）)]*[）)]", "")
                .replaceAll("\\s+", " ")
                .trim();
    }

    public static String buildHierarchicalSummaryTable(JSONArray roots) {
        if (roots == null || roots.isEmpty()) {
            return "<p>暂无能力维度得分数据。</p>";
        }
        List<List<JSONObject>> paths = new ArrayList<>();
        for (int i = 0; i < roots.size(); i++) {
            collectLeafPaths(roots.getJSONObject(i), new ArrayList<>(), paths);
        }
        if (paths.isEmpty()) {
            return "<p>暂无能力维度得分数据。</p>";
        }

        int maxDepth = 1;
        for (List<JSONObject> path : paths) {
            maxDepth = Math.max(maxDepth, Math.min(path.size(), 5));
        }

        StringBuilder html = new StringBuilder();
        html.append("<table border=\"1\" cellspacing=\"0\" cellpadding=\"6\" style=\"width:100%;border-collapse:collapse;border:1px solid #dcdfe6;font-family:Arial,'Microsoft YaHei',sans-serif;font-size:13px;text-align:center;\">");
        html.append("<thead><tr style=\"background:#f5f7fa;color:#303133;\">");
        String[] levelNames = {"一级指标", "二级指标", "三级指标", "四级指标", "五级指标"};
        for (int i = 0; i < maxDepth; i++) {
            html.append("<th style=\"border:1px solid #dcdfe6;padding:8px;\">").append(levelNames[i]).append("</th>");
        }
        html.append("<th style=\"border:1px solid #dcdfe6;padding:8px;\">指标得分</th>")
                .append("<th style=\"border:1px solid #dcdfe6;padding:8px;\">评定</th>")
                .append("<th style=\"border:1px solid #dcdfe6;padding:8px;\">实测值</th>")
                .append("<th style=\"border:1px solid #dcdfe6;padding:8px;\">参考阈值</th>")
                .append("</tr></thead><tbody>");

        for (int row = 0; row < paths.size(); row++) {
            List<JSONObject> path = paths.get(row);
            JSONObject leaf = path.get(path.size() - 1);
            html.append("<tr>");
            for (int depth = 0; depth < maxDepth; depth++) {
                JSONObject node = depth < path.size() ? path.get(depth) : null;
                if (node == null) {
                    html.append("<td style=\"border:1px solid #dcdfe6;padding:8px;\">-</td>");
                    continue;
                }
                if (row > 0 && depth < paths.get(row - 1).size() && paths.get(row - 1).get(depth) == node) {
                    continue;
                }
                int rowspan = 1;
                for (int next = row + 1; next < paths.size(); next++) {
                    if (depth < paths.get(next).size() && paths.get(next).get(depth) == node) {
                        rowspan++;
                    } else {
                        break;
                    }
                }
                html.append("<td");
                if (rowspan > 1) {
                    html.append(" rowspan=\"").append(rowspan).append("\"");
                }
                html.append(" style=\"border:1px solid #dcdfe6;padding:8px;text-align:left;vertical-align:middle;\">")
                        .append(escape(firstText(node, "label", "name", "title", "indicatorName")))
                        .append("</td>");
            }
            String unit = leaf.getString("unit");
            html.append("<td style=\"border:1px solid #dcdfe6;padding:8px;\">").append(format(firstNumber(leaf, "score", "calculatedValue", "evalScore"))).append("</td>")
                    .append("<td style=\"border:1px solid #dcdfe6;padding:8px;\">").append(escape(formatTone(firstText(leaf, "tone", "grade", "status")))).append("</td>")
                    .append("<td style=\"border:1px solid #dcdfe6;padding:8px;\">").append(escape(formatValue(firstNumber(leaf, "evalValue", "calculatedValue", "metricValue", "value"), unit))).append("</td>")
                    .append("<td style=\"border:1px solid #dcdfe6;padding:8px;\">").append(escape(formatValue(firstNumber(leaf, "referenceValue", "threshold", "valueMax", "valueMin"), unit))).append("</td>")
                    .append("</tr>");
        }
        html.append("</tbody></table>");
        return html.toString();
    }

    private static void collectLeafPaths(JSONObject node, List<JSONObject> path, List<List<JSONObject>> paths) {
        if (node == null) return;
        path.add(node);
        JSONArray children = node.getJSONArray("children");
        if (children == null || children.isEmpty()) {
            paths.add(new ArrayList<>(path));
        } else {
            for (int i = 0; i < children.size(); i++) {
                collectLeafPaths(children.getJSONObject(i), path, paths);
            }
        }
        path.remove(path.size() - 1);
    }

    private static void appendNode(JSONObject node, String numbering, int level, List<Section> sections) {
        if (node == null) return;
        JSONArray children = node.getJSONArray("children");
        boolean leaf = children == null || children.isEmpty();

        Section section = new Section();
        section.setNumbering(numbering);
        section.setLevel(Math.max(2, Math.min(level, 5)));
        section.setTitle(firstText(node, "label", "name", "title", "indicatorName"));
        section.setLeaf(leaf);
        section.setScore(firstNumber(node, "score", "calculatedValue", "evalScore", "evalValue"));
        section.setEvalValue(firstNumber(node, "evalValue", "calculatedValue", "metricValue", "value"));
        section.setReferenceValue(firstNumber(node, "referenceValue", "threshold", "valueMax", "valueMin"));
        section.setUnit(node.getString("unit"));
        section.setTone(firstText(node, "tone", "grade", "status"));
        if (section.getTone() == null && section.getScore() != null) {
            section.setTone(resolveTone(section.getScore()));
        }
        if (leaf) {
            section.setSummary(buildLeafSummary(section, node));
            section.setEvalTable(buildEvalTable(section, node));
        }
        sections.add(section);

        if (!leaf) {
            for (int i = 0; i < children.size(); i++) {
                appendNode(children.getJSONObject(i), numbering + "." + (i + 1), level + 1, sections);
            }
        }
    }

    private static JSONArray toRoots(Object raw) {
        if (raw == null) return new JSONArray();
        Object parsed = raw;
        if (raw instanceof String) {
            String text = ((String) raw).trim();
            if (text.isEmpty()) return new JSONArray();
            parsed = JSON.parse(text);
        }
        if (parsed instanceof JSONArray) {
            return (JSONArray) parsed;
        }
        if (parsed instanceof JSONObject) {
            JSONObject object = (JSONObject) parsed;
            JSONObject treeData = object.getJSONObject("treeData");
            if (treeData != null) {
                return toRoots(treeData);
            }
            JSONArray children = object.getJSONArray("children");
            if (children != null && !children.isEmpty()) {
                return children;
            }
            JSONArray array = new JSONArray();
            array.add(object);
            return array;
        }
        return JSON.parseArray(JSON.toJSONString(parsed));
    }

    private static String buildLeafSummary(Section section, JSONObject node) {
        String score = format(section.getScore());
        String tone = section.getTone() == null ? "-" : section.getTone();
        String evalValue = formatValue(firstNumber(node, "evalValue", "calculatedValue", "metricValue", "value"), node.getString("unit"));
        String referenceValue = formatValue(firstNumber(node, "referenceValue", "threshold", "valueMax", "valueMin"), node.getString("unit"));
        return "该指标得分 " + score + "，评定结果为 " + tone + "。实测值 " + evalValue + "，参考阈值 " + referenceValue + "。";
    }

    private static String buildEvalTable(Section section, JSONObject node) {
        String unit = node.getString("unit");
        String evalValue = formatValue(firstNumber(node, "evalValue", "calculatedValue", "metricValue", "value"), unit);
        String referenceValue = formatValue(firstNumber(node, "referenceValue", "threshold", "valueMax", "valueMin"), unit);
        return "<table border=\"1\" cellspacing=\"0\" cellpadding=\"6\" style=\"border-collapse:collapse;width:100%;\">"
                + "<tr><th>指标</th><th>得分</th><th>评定</th><th>实测值</th><th>参考阈值</th></tr>"
                + "<tr><td>" + escape(section.getTitle()) + "</td><td>" + format(section.getScore()) + "</td><td>"
                + escape(section.getTone()) + "</td><td>" + escape(evalValue) + "</td><td>" + escape(referenceValue)
                + "</td></tr></table>";
    }

    private static String firstText(JSONObject node, String... keys) {
        for (String key : keys) {
            String value = node.getString(key);
            if (value != null && !value.trim().isEmpty()) return value.trim();
        }
        return "";
    }

    private static BigDecimal firstNumber(JSONObject node, String... keys) {
        for (String key : keys) {
            BigDecimal value = node.getBigDecimal(key);
            if (value != null) return value;
        }
        return null;
    }

    private static String resolveTone(BigDecimal score) {
        if (score.compareTo(new BigDecimal("90")) >= 0) return "excellent";
        if (score.compareTo(new BigDecimal("75")) >= 0) return "good";
        if (score.compareTo(new BigDecimal("60")) >= 0) return "pass";
        return "risk";
    }

    private static String formatTone(String tone) {
        if (tone == null || tone.trim().isEmpty()) return "-";
        switch (tone.trim().toLowerCase()) {
            case "excellent":
                return "优秀";
            case "good":
                return "良好";
            case "pass":
                return "合格";
            case "risk":
                return "风险";
            default:
                return tone.trim();
        }
    }

    private static String formatValue(BigDecimal value, String unit) {
        if (value == null) return "-";
        return format(value) + (unit == null ? "" : unit);
    }

    private static String format(BigDecimal value) {
        if (value == null) return "-";
        return value.stripTrailingZeros().toPlainString();
    }

    private static String escape(String value) {
        if (value == null) return "";
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    public static class Section {
        private String numbering;
        private Integer level;
        private String title;
        private boolean leaf;
        private BigDecimal score;
        private BigDecimal evalValue;
        private BigDecimal referenceValue;
        private String unit;
        private String tone;
        private String summary;
        private String evalTable;
        private String chartImg;

        public String getNumbering() {
            return numbering;
        }

        public void setNumbering(String numbering) {
            this.numbering = numbering;
        }

        public Integer getLevel() {
            return level;
        }

        public void setLevel(Integer level) {
            this.level = level;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public boolean isLeaf() {
            return leaf;
        }

        public void setLeaf(boolean leaf) {
            this.leaf = leaf;
        }

        public BigDecimal getScore() {
            return score;
        }

        public void setScore(BigDecimal score) {
            this.score = score;
        }

        public BigDecimal getEvalValue() {
            return evalValue;
        }

        public void setEvalValue(BigDecimal evalValue) {
            this.evalValue = evalValue;
        }

        public BigDecimal getReferenceValue() {
            return referenceValue;
        }

        public void setReferenceValue(BigDecimal referenceValue) {
            this.referenceValue = referenceValue;
        }

        public String getUnit() {
            return unit;
        }

        public void setUnit(String unit) {
            this.unit = unit;
        }

        public String getTone() {
            return tone;
        }

        public void setTone(String tone) {
            this.tone = tone;
        }

        public String getSummary() {
            return summary;
        }

        public void setSummary(String summary) {
            this.summary = summary;
        }

        public String getEvalTable() {
            return evalTable;
        }

        public void setEvalTable(String evalTable) {
            this.evalTable = evalTable;
        }

        public String getChartImg() {
            return chartImg;
        }

        public void setChartImg(String chartImg) {
            this.chartImg = chartImg;
        }
    }
}
