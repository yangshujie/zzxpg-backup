package com.ruoyi.zhpgcalc.service;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.aspose.words.Document;
import com.aspose.words.License;
import com.aspose.words.SaveFormat;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.common.report.ChartRenderer;
import com.ruoyi.common.report.IndicatorReportSectionBuilder;
import com.ruoyi.system.api.RemoteFileService;
import com.ruoyi.system.api.domain.SysFile;
import com.ruoyi.zhpgcalc.dto.CalcExecuteResponse;
import com.ruoyi.zhpgcalc.report.ReportEngine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 评估报告生成服务
 */
@Slf4j
@Service
public class ReportGenerationService {

    private static final String STORAGE_ROOT = "zhpg/evalReport/";
    private static final String WORD_CONTENT_TYPE =
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
    private static volatile boolean ASPOSE_LICENSE_LOADED = false;

    @Value("${custom-config.minio.bucketName:test}")
    private String bucketName;

    @Value("${zhpg.report.local-save-dir:D:/zzxpg/report-output}")
    private String localReportSaveDir;

    private final RemoteFileService remoteFileService;
    private final ReportEngine reportEngine = new ReportEngine();
    private final ChartRenderer chartRenderer = new ChartRenderer();

    public ReportGenerationService(RemoteFileService remoteFileService) {
        this.remoteFileService = remoteFileService;
    }

    /**
     * 生成评估报告并上传到 MinIO
     */
    public ReportUrls generateAndUploadReport(Long taskId, CalcExecuteResponse response, String requestJson) {
        try {
            log.info("开始生成评估报告, taskId={}", taskId);

            JSONObject request = JSON.parseObject(requestJson);
            JSONObject reportTemplate = request.getJSONObject("reportTemplate");
            if (reportTemplate == null) {
                log.warn("任务 {} 未配置报告模板，无法生成报告", taskId);
                return null;
            }

            String templateName = reportTemplate.getString("templateName");
            String htmlContent = reportTemplate.getString("htmlContent");
            if (htmlContent == null || htmlContent.trim().isEmpty()) {
                log.error("任务 {} 报告模板内容为空", taskId);
                return null;
            }

            Map<String, Object> templateData = buildTemplateData(taskId, response, request);
            byte[] wordBytes = reportEngine.generateDocx(
                    templateName != null ? templateName : "report",
                    htmlContent,
                    templateData
            );
            byte[] pdfBytes = convertWordToPdf(wordBytes);

            String datePath = new SimpleDateFormat("yyyyMM").format(new Date());
            String wordObjectName = STORAGE_ROOT + datePath + "/calc" + taskId + "_report.docx";
            String pdfObjectName = STORAGE_ROOT + datePath + "/calc" + taskId + "_report.pdf";

            try {
                String wordUrl = uploadToMinIO(wordBytes, wordObjectName, WORD_CONTENT_TYPE);
                String pdfUrl = uploadToMinIO(pdfBytes, pdfObjectName, "application/pdf");
                log.info("报告生成完成, taskId={}, pdfUrl={}, wordUrl={}", taskId, pdfUrl, wordUrl);
                return new ReportUrls(pdfUrl, wordUrl, null);
            } catch (Exception uploadEx) {
                LocalReportPaths localPaths = saveToLocalFiles(taskId, wordBytes, pdfBytes);
                log.warn("任务 {} 报告上传失败，已保存到本地 word={}, pdf={}, error={}",
                        taskId, localPaths.getWordPath(), localPaths.getPdfPath(), uploadEx.getMessage(), uploadEx);
                return new ReportUrls(localPaths.getPdfPath(), localPaths.getWordPath(), null);
            }
        } catch (Exception e) {
            log.error("报告处理失败, taskId={}", taskId, e);
            throw new RuntimeException("报告处理失败: " + e.getMessage(), e);
        }
    }

    private LocalReportPaths saveToLocalFiles(Long taskId, byte[] wordBytes, byte[] pdfBytes) throws IOException {
        String datePath = new SimpleDateFormat("yyyyMMdd").format(new Date());
        Path dir = Paths.get(localReportSaveDir, datePath);
        Files.createDirectories(dir);

        Path wordFile = dir.resolve("calc" + taskId + "_report.docx");
        Path pdfFile = dir.resolve("calc" + taskId + "_report.pdf");
        Files.write(wordFile, wordBytes);
        Files.write(pdfFile, pdfBytes);
        return new LocalReportPaths(wordFile.toAbsolutePath().toString(), pdfFile.toAbsolutePath().toString());
    }

    private byte[] convertWordToPdf(byte[] wordBytes) {
        try {
            ensureAsposeLicense();
            try (ByteArrayInputStream input = new ByteArrayInputStream(wordBytes);
                 ByteArrayOutputStream output = new ByteArrayOutputStream()) {
                Document document = new Document(input);
                document.save(output, SaveFormat.PDF);
                return output.toByteArray();
            }
        } catch (Exception e) {
            throw new RuntimeException("Word转PDF失败: " + e.getMessage(), e);
        }
    }

    private void ensureAsposeLicense() {
        if (ASPOSE_LICENSE_LOADED) {
            return;
        }
        synchronized (ReportGenerationService.class) {
            if (ASPOSE_LICENSE_LOADED) {
                return;
            }
            String xml = "<License>\n"
                    + "    <Data>\n"
                    + "        <Products>\n"
                    + "            <Product>Aspose.Total for Java</Product>\n"
                    + "            <Product>Aspose.Words for Java</Product>\n"
                    + "        </Products>\n"
                    + "        <EditionType>Enterprise</EditionType>\n"
                    + "        <SubscriptionExpiry>20991231</SubscriptionExpiry>\n"
                    + "        <LicenseExpiry>20991231</LicenseExpiry>\n"
                    + "        <SerialNumber>8bfe198c-7f0c-4ef8-8ff0-acc3237bf0d7</SerialNumber>\n"
                    + "    </Data>\n"
                    + "    <Signature>\n"
                    + "        sNLLKGMUdF0r8O1kKilWAGdgfs2BvJb/2Xp8p5iuDVfZXmhppo+d0Ran1P9TKdjV4ABwAgKXxJ3jcQTqE/2IRfqwnPf8itN8aFZlV3TJPYeD3yWE7IT55Gz6EijUpC7aKeoohTb4w2fpox58wWoF3SNp6sK6jDfiAUGEHYJ9pjU=\n"
                    + "    </Signature>\n"
                    + "</License>";
            try (InputStream inputStream = new ByteArrayInputStream(xml.getBytes())) {
                License license = new License();
                license.setLicense(inputStream);
            } catch (Exception e) {
                log.warn("Aspose 许可加载失败，继续尝试转换: {}", e.getMessage());
            }
            ASPOSE_LICENSE_LOADED = true;
        }
    }

    /**
     * 构建模板数据模型
     */
    private Map<String, Object> buildTemplateData(Long taskId, CalcExecuteResponse response, JSONObject request) {
        Map<String, Object> data = new HashMap<>();

        // ── 基础数据（向后兼容，保留原有 key） ──
        data.put("taskId", taskId);
        data.put("taskName", request.getString("taskName"));
        data.put("templateName", request.getString("templateName"));
        data.put("indicatorSystemName", response.getIndicatorSystemName());
        data.put("executorCode", response.getExecutorCode());
        data.put("generateTime", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));

        data.put("score", response.getScore());
        data.put("grade", formatGrade(response.getGrade()));
        data.put("gradeEn", response.getGrade());
        data.put("conclusion", response.getConclusion());
        data.put("suggestion", response.getSuggestion());

        List<Map<String, Object>> dimensions = new ArrayList<>();
        if (response.getDimensions() != null) {
            JSONArray dims = (JSONArray) response.getDimensions();
            for (int i = 0; i < dims.size(); i++) {
                JSONObject dim = dims.getJSONObject(i);
                Map<String, Object> dimMap = new HashMap<>();
                dimMap.put("name", dim.getString("name"));
                dimMap.put("label", dim.getString("label"));

                BigDecimal weight = dim.getBigDecimal("weight");
                if (weight != null) {
                    BigDecimal weightPercent = weight.multiply(new BigDecimal("100"))
                            .setScale(2, RoundingMode.HALF_UP);
                    dimMap.put("weight", weightPercent);
                    dimMap.put("weightPercent", weightPercent + "%");
                }

                BigDecimal score = dim.getBigDecimal("score");
                if (score != null) {
                    dimMap.put("score", score);
                }

                dimMap.put("tone", dim.getString("tone"));
                dimensions.add(dimMap);
            }
        }
        data.put("dimensions", dimensions);
        data.put("dimensionCount", dimensions.size());

        Object scoredIndicatorTree = response.getScoredIndicatorTree();
        JSONArray parsedTree = null;
        List<IndicatorReportSectionBuilder.Section> indicatorSections = new ArrayList<>();
        if (scoredIndicatorTree != null) {
            try {
                parsedTree = parseTree(scoredIndicatorTree);
                data.put("indicatorTree", parseIndicatorTree(parsedTree));
                data.put("treeNodeCount", countTreeNodes(parsedTree));
                indicatorSections = IndicatorReportSectionBuilder.buildSections(scoredIndicatorTree);
                enrichSectionCharts(indicatorSections);
            } catch (Exception e) {
                log.warn("解析指标树失败: {}", e.getMessage());
                data.put("indicatorTree", new ArrayList<>());
                data.put("treeNodeCount", 0);
            }
        }
        data.put("IndicatorSections", indicatorSections);
        data.put("indicatorSections", indicatorSections);

        String weightedTreeJson = request.getString("weightedTreeJson");
        if (weightedTreeJson != null && !weightedTreeJson.isEmpty()) {
            try {
                data.put("weightedTree", JSON.parse(weightedTreeJson));
            } catch (Exception e) {
                log.warn("解析加权树失败: {}", e.getMessage());
            }
        }

        // ── 处理流程模板中的 placeholderMappings 配置 ──
        // 根据 reportConfig.placeholderMappings 将数据映射到模板占位符名称
        JSONObject reportConfig = request.getJSONObject("reportConfig");
        if (reportConfig != null) {
            JSONArray mappings = reportConfig.getJSONArray("placeholderMappings");
            if (mappings != null && !mappings.isEmpty()) {
                log.info("开始处理占位符映射，共 {} 条", mappings.size());

                // 预构建 AUTO_INDICATOR 解析表
                Map<String, Object> autoIndicatorMap = buildAutoIndicatorMap(response, parsedTree, indicatorSections, dimensions, request);
                log.info("自动报告字段已构建: keys={}, indicatorSections={}", autoIndicatorMap.keySet(),
                        indicatorSections == null ? 0 : indicatorSections.size());

                for (int i = 0; i < mappings.size(); i++) {
                    JSONObject mapping = mappings.getJSONObject(i);
                    String key = mapping.getString("key");
                    String mappingType = mapping.getString("mappingType");
                    String mappingValue = mapping.getString("mappingValue");
                    if (key == null || mappingType == null) {
                        continue;
                    }

                    Object resolvedValue = null;
                    switch (mappingType) {
                        case "STATIC_TEXT":
                            resolvedValue = mappingValue;
                            break;
                        case "TASK_PROPERTY":
                            resolvedValue = resolveTaskProperty(mappingValue, request);
                            break;
                        case "AUTO_INDICATOR":
                            resolvedValue = autoIndicatorMap.get(mappingValue);
                            break;
                        default:
                            log.warn("未知的映射类型: {}, key={}", mappingType, key);
                    }

                    if (resolvedValue != null) {
                        data.put(key, resolvedValue);
                        log.debug("占位符映射: {} -> {} (type={})", key, resolvedValue, mappingType);
                    } else {
                        log.warn("占位符映射值为空: key={}, type={}, value={}", key, mappingType, mappingValue);
                    }
                }
            }
        }

        return data;
    }

    /**
     * 构建 AUTO_INDICATOR 自动指标值映射表
     */
    private Map<String, Object> buildAutoIndicatorMap(CalcExecuteResponse response,
                                                       JSONArray parsedTree,
                                                       List<IndicatorReportSectionBuilder.Section> indicatorSections,
                                                       List<Map<String, Object>> dimensions,
                                                       JSONObject request) {
        Map<String, Object> map = new HashMap<>();

        // overall_score → 综合得分
        if (response.getScore() != null) {
            map.put("overall_score", response.getScore());
        }

        // Deprecated: raw JSON snapshots should not be rendered into reports.
        map.put("eval_result_snapshot", "");

        String capabilityScoreTable = buildCapabilityScoreTableHtml(parsedTree, dimensions);
        map.put("indicator_summary_table", capabilityScoreTable);
        map.put("capability_score_table", capabilityScoreTable);

        map.put("indicator_tree", buildIndicatorTreeOutlineHtml(parsedTree));
        map.put("indicator_sections", indicatorSections == null ? new ArrayList<>() : indicatorSections);
        map.put("experiment_overview", buildExperimentOverviewHtml(response, request, dimensions));
        map.put("capability_radar_chart", renderRadarChart(selectRadarDimensions(parsedTree, dimensions)));
        map.put("overall_conclusion_paragraph", buildOverallConclusionParagraph(response));
        map.put("key_findings", buildKeyFindingsHtml(response, selectRadarDimensions(parsedTree, dimensions)));
        map.put("improvement_suggestions", buildImprovementSuggestions(response, selectRadarDimensions(parsedTree, dimensions)));
        map.put("final_conclusion", buildFinalConclusion(response));

        return map;
    }

    private String buildExperimentOverviewHtml(CalcExecuteResponse response,
                                               JSONObject request,
                                               List<Map<String, Object>> dimensions) {
        String taskName = IndicatorReportSectionBuilder.cleanTaskName(request != null ? request.getString("taskName") : null);
        String assessTaskId = request != null ? request.getString("assessTaskId") : null;
        String systemName = IndicatorReportSectionBuilder.cleanIndicatorSystemName(response.getIndicatorSystemName());
        StringBuilder html = new StringBuilder();
        html.append("<table border=\"1\" cellspacing=\"0\" cellpadding=\"6\" style=\"width:100%;border-collapse:collapse;\">");
        appendTableRow(html, "任务名称", defaultText(taskName, "未命名评估任务"));
        appendTableRow(html, "任务编号", defaultText(assessTaskId, "-"));
        appendTableRow(html, "指标体系", defaultText(systemName, "-"));
        appendTableRow(html, "执行器", defaultText(response.getExecutorCode(), "-"));
        appendTableRow(html, "综合得分", formatObject(response.getScore()));
        appendTableRow(html, "评定等级", formatGrade(response.getGrade()));
        appendTableRow(html, "能力维度数", String.valueOf(dimensions != null ? dimensions.size() : 0));
        html.append("</table>");
        return html.toString();
    }

    private void enrichSectionCharts(List<IndicatorReportSectionBuilder.Section> sections) {
        if (sections == null || sections.isEmpty()) {
            return;
        }
        for (IndicatorReportSectionBuilder.Section section : sections) {
            if (section != null && section.isLeaf()) {
                try {
                    section.setChartImg(chartRenderer.renderIndicatorBar(
                            section.getTitle(),
                            section.getEvalValue() != null ? section.getEvalValue() : section.getScore(),
                            section.getReferenceValue()
                    ));
                } catch (Exception e) {
                    log.warn("渲染指标图表失败: title={}, error={}", section.getTitle(), e.getMessage());
                }
            }
        }
    }

    private String renderRadarChart(List<Map<String, Object>> dimensions) {
        if (dimensions == null || dimensions.isEmpty()) {
            return "";
        }
        try {
            return chartRenderer.renderRadar(dimensions);
        } catch (Exception e) {
            log.warn("渲染能力雷达图失败: {}", e.getMessage());
            return "";
        }
    }

    private List<Map<String, Object>> selectRadarDimensions(JSONArray parsedTree, List<Map<String, Object>> fallbackDimensions) {
        List<Map<String, Object>> selected = new ArrayList<>();
        if (parsedTree != null && !parsedTree.isEmpty()) {
            for (int i = 0; i < parsedTree.size(); i++) {
                JSONObject node = parsedTree.getJSONObject(i);
                BigDecimal score = firstNumber(node, "score", "calculatedValue", "evalScore", "evalValue");
                String label = defaultText(node.getString("label"), node.getString("name"));
                if (score != null && label != null && !label.trim().isEmpty()) {
                    Map<String, Object> row = new HashMap<>();
                    row.put("label", label);
                    row.put("score", score);
                    row.put("tone", node.getString("tone"));
                    selected.add(row);
                }
            }
        }
        if (!selected.isEmpty()) {
            return selected;
        }
        return fallbackDimensions == null ? new ArrayList<>() : fallbackDimensions;
    }

    private String buildCapabilityScoreTableHtml(JSONArray parsedTree, List<Map<String, Object>> dimensions) {
        if (parsedTree != null && !parsedTree.isEmpty()) {
            return IndicatorReportSectionBuilder.buildHierarchicalSummaryTable(parsedTree);
        }
        if (dimensions == null || dimensions.isEmpty()) {
            return "<p>暂无能力维度得分数据。</p>";
        }
        StringBuilder html = new StringBuilder();
        html.append("<table border=\"1\" cellspacing=\"0\" cellpadding=\"6\" style=\"width:100%;border-collapse:collapse;\">")
                .append("<thead><tr>")
                .append("<th>能力维度</th><th>权重</th><th>得分</th><th>评定</th>")
                .append("</tr></thead><tbody>");
        for (Map<String, Object> dimension : dimensions) {
            html.append("<tr>")
                    .append("<td>").append(escapeHtml(defaultText(asString(dimension.get("label")), asString(dimension.get("name"))))).append("</td>")
                    .append("<td>").append(escapeHtml(defaultText(asString(dimension.get("weightPercent")), formatObject(dimension.get("weight"))))).append("</td>")
                    .append("<td>").append(escapeHtml(formatObject(dimension.get("score")))).append("</td>")
                    .append("<td>").append(escapeHtml(formatGrade(asString(dimension.get("tone"))))).append("</td>")
                    .append("</tr>");
        }
        html.append("</tbody></table>");
        return html.toString();
    }

    private String buildIndicatorTreeOutlineHtml(JSONArray parsedTree) {
        if (parsedTree == null || parsedTree.isEmpty()) {
            return "<p>暂无指标树数据。</p>";
        }
        StringBuilder html = new StringBuilder("<ul>");
        appendIndicatorTreeNodes(html, parsedTree);
        html.append("</ul>");
        return html.toString();
    }

    private void appendIndicatorTreeNodes(StringBuilder html, JSONArray nodes) {
        for (int i = 0; i < nodes.size(); i++) {
            JSONObject node = nodes.getJSONObject(i);
            html.append("<li>").append(escapeHtml(defaultText(node.getString("name"), "未命名指标")));
            BigDecimal score = node.getBigDecimal("score");
            if (score != null) {
                html.append("：").append(escapeHtml(formatObject(score))).append("分");
            }
            JSONArray children = node.getJSONArray("children");
            if (children != null && !children.isEmpty()) {
                html.append("<ul>");
                appendIndicatorTreeNodes(html, children);
                html.append("</ul>");
            }
            html.append("</li>");
        }
    }

    private String buildOverallConclusionParagraph(CalcExecuteResponse response) {
        String conclusion = sanitizeNarrative(response.getConclusion());
        if (conclusion != null && !conclusion.trim().isEmpty()) {
            return conclusion;
        }
        return "本次评估综合得分为 " + formatObject(response.getScore())
                + "，评定结果为 " + formatGrade(response.getGrade()) + "。";
    }

    private String buildKeyFindingsHtml(CalcExecuteResponse response, List<Map<String, Object>> dimensions) {
        StringBuilder html = new StringBuilder("<ul>");
        html.append("<li>综合得分 ").append(escapeHtml(formatObject(response.getScore())))
                .append("，评定结果为 ").append(escapeHtml(formatGrade(response.getGrade()))).append("。</li>");
        if (dimensions != null && !dimensions.isEmpty()) {
            for (Map<String, Object> dimension : dimensions) {
                html.append("<li>")
                        .append(escapeHtml(defaultText(asString(dimension.get("label")), asString(dimension.get("name")))))
                        .append("得分 ")
                        .append(escapeHtml(formatObject(dimension.get("score"))))
                        .append("，评定为 ")
                        .append(escapeHtml(formatGrade(asString(dimension.get("tone")))))
                        .append("。</li>");
            }
        }
        html.append("</ul>");
        return html.toString();
    }

    private String buildImprovementSuggestions(CalcExecuteResponse response, List<Map<String, Object>> dimensions) {
        String suggestion = sanitizeNarrative(response.getSuggestion());
        if (suggestion != null && !suggestion.trim().isEmpty()) {
            return "<p>" + escapeHtml(suggestion) + "</p>";
        }
        StringBuilder html = new StringBuilder("<ol>");
        html.append("<li>优先复核低分能力域的数据来源、计算规则和权重配置，确认评估输入与实际试验记录一致。</li>");
        if (dimensions != null && !dimensions.isEmpty()) {
            int added = 0;
            for (Map<String, Object> dimension : dimensions) {
                BigDecimal score = toBigDecimal(dimension.get("score"));
                if (score == null) {
                    score = toBigDecimal(dimension.get("value"));
                }
                if (score != null && score.compareTo(new BigDecimal("75")) < 0 && added < 3) {
                    html.append("<li>针对")
                            .append(escapeHtml(defaultText(asString(dimension.get("label")), asString(dimension.get("name")))))
                            .append("开展专项改进，补充试验样本并跟踪复测结果。</li>");
                    added++;
                }
            }
        }
        html.append("<li>形成整改闭环后重新生成评估报告，保留前后版本对比，支撑后续决策复盘。</li>");
        html.append("</ol>");
        return html.toString();
    }

    private String buildFinalConclusion(CalcExecuteResponse response) {
        String conclusion = sanitizeNarrative(response.getConclusion());
        if (conclusion != null && !conclusion.trim().isEmpty()) {
            return conclusion;
        }
        return "综上，本次通信对抗试验评估结果为" + formatGrade(response.getGrade())
                + "，综合得分为 " + formatObject(response.getScore()) + "。";
    }

    private void appendTableRow(StringBuilder html, String label, String value) {
        html.append("<tr><th style=\"width:24%;text-align:left;\">")
                .append(escapeHtml(label))
                .append("</th><td>")
                .append(escapeHtml(value))
                .append("</td></tr>");
    }

    private String defaultText(String value, String fallback) {
        return value != null && !value.trim().isEmpty() ? value : fallback;
    }

    private String asString(Object value) {
        return value != null ? String.valueOf(value) : null;
    }

    private BigDecimal firstNumber(JSONObject node, String... keys) {
        if (node == null) {
            return null;
        }
        for (String key : keys) {
            BigDecimal value = node.getBigDecimal(key);
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private BigDecimal toBigDecimal(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof BigDecimal) {
            return (BigDecimal) value;
        }
        if (value instanceof Number) {
            return BigDecimal.valueOf(((Number) value).doubleValue());
        }
        try {
            return new BigDecimal(String.valueOf(value));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String sanitizeNarrative(String raw) {
        if (raw == null) {
            return null;
        }
        String cleaned = raw.trim()
                .replaceAll("(?i)\\broute\\s*=\\s*[^,，。;；]+[,，。;；]?\\s*", "")
                .replaceAll("(?i)\\bblock\\s*=\\s*[^,，。;；]+[,，。;；]?\\s*", "")
                .replaceAll("(?i)\\bmisfire\\s*=\\s*[^,，。;；]+[,，。;；]?\\s*", "")
                .replaceAll("(?i)^.*\\s+completed on\\s+.*score\\s*=\\s*[^,，。;；]+[,，。;；]?\\s*grade\\s*=\\s*[^,，。;；]+\\.?$", "")
                .replaceAll("(?i)^Result is stable\\.?$", "")
                .replaceAll("(?i)^Review low-score indicators and mapping\\.?$", "")
                .replaceAll("[,，;；]\\s*$", "")
                .trim();
        return cleaned.isEmpty() ? null : cleaned;
    }

    private String formatObject(Object value) {
        if (value == null) {
            return "-";
        }
        if (value instanceof BigDecimal) {
            return ((BigDecimal) value).stripTrailingZeros().toPlainString();
        }
        return String.valueOf(value);
    }

    private String escapeHtml(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    /**
     * 从请求参数中解析任务属性值
     */
    private Object resolveTaskProperty(String propertyName, JSONObject request) {
        if (propertyName == null || request == null) {
            return null;
        }
        // executorParam 中已包含这些任务属性
        Object value = request.get(propertyName);
        if (value != null) {
            return value;
        }
        // 兼容：某些属性可能来源于其他字段
        switch (propertyName) {
            case "evaluateTarget":
                // evaluateTarget 暂用 taskName 兜底
                Object assessTaskId = request.get("assessTaskId");
                return assessTaskId != null ? "评估任务#" + assessTaskId : request.getString("taskName");
            default:
                return null;
        }
    }

    private JSONArray parseTree(Object scoredIndicatorTree) {
        if (scoredIndicatorTree instanceof JSONArray) {
            return (JSONArray) scoredIndicatorTree;
        }
        if (scoredIndicatorTree instanceof JSONObject) {
            JSONObject object = (JSONObject) scoredIndicatorTree;
            JSONObject treeData = object.getJSONObject("treeData");
            if (treeData != null) {
                return parseTree(treeData);
            }
            JSONArray children = object.getJSONArray("children");
            if (children != null) {
                return children;
            }
            JSONArray array = new JSONArray();
            array.add(object);
            return array;
        }
        if (scoredIndicatorTree instanceof String) {
            String treeJson = unescapeJsonIfNeeded((String) scoredIndicatorTree);
            Object parsed = JSON.parse(treeJson);
            return parseTree(parsed);
        }
        throw new IllegalArgumentException("不支持的指标树格式: " + scoredIndicatorTree.getClass());
    }

    private String unescapeJsonIfNeeded(String json) {
        if (json == null || json.isEmpty()) {
            return json;
        }
        String trimmed = json.trim();
        if (trimmed.startsWith("\"") && trimmed.endsWith("\"")) {
            try {
                return JSON.parseObject("{\"value\":" + trimmed + "}").getString("value");
            } catch (Exception e) {
                log.debug("JSON 反转义失败，使用原始字符串: {}", e.getMessage());
            }
        }
        return json;
    }

    private String formatGrade(String grade) {
        if (grade == null) {
            return "-";
        }
        switch (grade.toLowerCase()) {
            case "excellent":
                return "优秀";
            case "good":
                return "良好";
            case "qualified":
                return "合格";
            case "unqualified":
                return "不合格";
            default:
                return grade;
        }
    }

    private List<Map<String, Object>> parseIndicatorTree(JSONArray tree) {
        List<Map<String, Object>> result = new ArrayList<>();
        if (tree == null) {
            return result;
        }
        for (int i = 0; i < tree.size(); i++) {
            result.add(parseIndicatorNode(tree.getJSONObject(i), 0));
        }
        return result;
    }

    private Map<String, Object> parseIndicatorNode(JSONObject node, int level) {
        Map<String, Object> map = new HashMap<>();
        map.put("name", node.getString("name"));
        map.put("level", level);
        map.put("indent", level > 0 ? new String(new char[level]).replace("\0", "  ") : "");

        BigDecimal weight = node.getBigDecimal("weight");
        if (weight != null) {
            map.put("weight", weight.multiply(new BigDecimal("100")).setScale(1, RoundingMode.HALF_UP));
        }

        BigDecimal score = node.getBigDecimal("score");
        if (score != null) {
            map.put("score", score);
        }

        BigDecimal calculatedValue = node.getBigDecimal("calculatedValue");
        if (calculatedValue != null) {
            map.put("calculatedValue", calculatedValue);
        }

        JSONArray children = node.getJSONArray("children");
        if (children != null && !children.isEmpty()) {
            List<Map<String, Object>> childList = new ArrayList<>();
            for (int i = 0; i < children.size(); i++) {
                childList.add(parseIndicatorNode(children.getJSONObject(i), level + 1));
            }
            map.put("children", childList);
            map.put("hasChildren", true);
        } else {
            map.put("hasChildren", false);
        }
        return map;
    }

    private int countTreeNodes(JSONArray tree) {
        if (tree == null) {
            return 0;
        }
        int count = tree.size();
        for (int i = 0; i < tree.size(); i++) {
            JSONArray children = tree.getJSONObject(i).getJSONArray("children");
            if (children != null) {
                count += countTreeNodes(children);
            }
        }
        return count;
    }

    private String uploadToMinIO(byte[] data, String objectName, String contentType) {
        int lastSlash = objectName.lastIndexOf('/');
        String dirPath = lastSlash >= 0 ? objectName.substring(0, lastSlash + 1) : STORAGE_ROOT;
        String fileName = lastSlash >= 0 ? objectName.substring(lastSlash + 1) : objectName;
        MultipartFile multipart = new ByteArrayMultipartFile("file", fileName, contentType, data);

        R<SysFile> result = remoteFileService.upload(bucketName, dirPath, multipart);
        if (result == null || !Boolean.TRUE.equals(result.isSuccess()) || result.getData() == null) {
            throw new RuntimeException(result != null ? result.getMsg() : "ruoyi-file 无响应，无法上传报告");
        }
        String fileUrl = result.getData().getUrl();
        if (fileUrl == null || fileUrl.trim().isEmpty()) {
            throw new RuntimeException("ruoyi-file 上传成功但未返回文件路径");
        }
        return fileUrl;
    }

    private static final class ByteArrayMultipartFile implements MultipartFile {

        private final String name;
        private final String originalFilename;
        private final String contentType;
        private final byte[] content;

        ByteArrayMultipartFile(String name, String originalFilename, String contentType, byte[] content) {
            this.name = name;
            this.originalFilename = originalFilename;
            this.contentType = contentType;
            this.content = content != null ? content : new byte[0];
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public String getOriginalFilename() {
            return originalFilename;
        }

        @Override
        public String getContentType() {
            return contentType;
        }

        @Override
        public boolean isEmpty() {
            return content.length == 0;
        }

        @Override
        public long getSize() {
            return content.length;
        }

        @Override
        public byte[] getBytes() {
            return content;
        }

        @Override
        public InputStream getInputStream() {
            return new ByteArrayInputStream(content);
        }

        @Override
        public void transferTo(File dest) throws IOException {
            Files.write(dest.toPath(), content);
        }
    }

    public static class ReportUrls {
        private final String pdfUrl;
        private final String wordUrl;
        private final String wpsUrl;

        public ReportUrls(String pdfUrl, String wordUrl, String wpsUrl) {
            this.pdfUrl = pdfUrl;
            this.wordUrl = wordUrl;
            this.wpsUrl = wpsUrl;
        }

        public String getPdfUrl() {
            return pdfUrl;
        }

        public String getWordUrl() {
            return wordUrl;
        }

        public String getWpsUrl() {
            return wpsUrl;
        }
    }

    private static class LocalReportPaths {
        private final String wordPath;
        private final String pdfPath;

        private LocalReportPaths(String wordPath, String pdfPath) {
            this.wordPath = wordPath;
            this.pdfPath = pdfPath;
        }

        public String getWordPath() {
            return wordPath;
        }

        public String getPdfPath() {
            return pdfPath;
        }
    }
}
