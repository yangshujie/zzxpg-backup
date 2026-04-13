package com.ruoyi.zhpgcalc.service;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.aspose.words.Document;
import com.aspose.words.License;
import com.aspose.words.SaveFormat;
import com.ruoyi.common.core.domain.R;
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
        if (scoredIndicatorTree != null) {
            try {
                parsedTree = parseTree(scoredIndicatorTree);
                data.put("indicatorTree", parseIndicatorTree(parsedTree));
                data.put("treeNodeCount", countTreeNodes(parsedTree));
            } catch (Exception e) {
                log.warn("解析指标树失败: {}", e.getMessage());
                data.put("indicatorTree", new ArrayList<>());
                data.put("treeNodeCount", 0);
            }
        }

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
                Map<String, Object> autoIndicatorMap = buildAutoIndicatorMap(response, parsedTree, dimensions);

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
                                                       List<Map<String, Object>> dimensions) {
        Map<String, Object> map = new HashMap<>();

        // overall_score → 综合得分
        if (response.getScore() != null) {
            map.put("overall_score", response.getScore());
        }

        // eval_result_snapshot → 评估结果快照(JSON)
        JSONObject snapshot = new JSONObject();
        snapshot.put("score", response.getScore());
        snapshot.put("grade", response.getGrade());
        snapshot.put("gradeCn", formatGrade(response.getGrade()));
        snapshot.put("conclusion", response.getConclusion());
        snapshot.put("suggestion", response.getSuggestion());
        if (response.getDimensions() != null) {
            snapshot.put("dimensions", response.getDimensions());
        }
        map.put("eval_result_snapshot", snapshot.toJSONString());

        // indicator_summary_table → 指标汇总表(JSON数组)
        if (dimensions != null && !dimensions.isEmpty()) {
            map.put("indicator_summary_table", JSON.toJSONString(dimensions));
        }

        // indicator_tree → 指标树(JSON)
        if (parsedTree != null) {
            map.put("indicator_tree", parsedTree.toJSONString());
        } else if (response.getScoredIndicatorTree() != null) {
            map.put("indicator_tree", JSON.toJSONString(response.getScoredIndicatorTree()));
        }

        return map;
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
