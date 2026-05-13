
package com.ruoyi.zhpgcalc.service;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.ruoyi.common.report.IndicatorReportSectionBuilder;
import com.ruoyi.zhpgcalc.algs.ZgpgAlgsClient;
import com.ruoyi.zhpgcalc.dto.CalcExecuteRequest;
import com.ruoyi.zhpgcalc.dto.CalcExecuteResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import com.ruoyi.zhpgcalc.ZhpgCallbackClient;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Slf4j
@Service
public class CalcExecutorService {
    private static final String EXECUTOR_CODE = "ZHPG_CALC_EXECUTOR";

    @Resource
    private ZgpgAlgsClient zgpgAlgsClient;

    @Resource
    private ExternalEvaluationDataClient externalEvaluationDataClient;

    @Resource
    private ZhpgCallbackClient zhpgCallbackClient;

    @Value("${zhpg.calc.data-input-mode:MOCK}")
    private String defaultDataInputMode;

    public CalcExecuteResponse execute(CalcExecuteRequest request) {
        JSONObject cc = toObj(request.getComprehensiveConfig());
        JSONObject sc = toObj(request.getScheduleConfig());
        JSONObject rp = toObj(request.getRuntimePolicy());

        int rows = Math.max(1, cc.getIntValue("mockSampleRows", 8));
        ParsedTree t = parseTree(request.getWeightedTreeJson());
        
        zhpgCallbackClient.notifyProgress(request.getTaskId(), 10, "解析指标体系完成");
        
        ExternalDataContext externalDataContext = loadExternalDataContext(request, cc, t.roots);
        zhpgCallbackClient.notifyProgress(request.getTaskId(), 20, "外部数据对接完成");
        
        processLeafNodes(t.roots, rows, externalDataContext);
        zhpgCallbackClient.notifyProgress(request.getTaskId(), 50, "底层指标计算完成");

        JSONArray dimensions = new JSONArray();
        BigDecimal score = calcOverall(t.roots, dimensions, cc, request.getTaskId());
        if (score == null) score = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

        CalcExecuteResponse r = new CalcExecuteResponse();
        r.setScore(score);
        r.setGrade(resolveGrade(score, cc));
        r.setConclusion(buildConclusion(request, score, r.getGrade()));
        r.setSuggestion(buildSuggestion(score, readRoute(sc), rp));
        r.setDimensions(dimensions);
        r.setExecutorCode(EXECUTOR_CODE);
        r.setWeightedTreeNodeCount(t.nodeCount);
        r.setIndicatorSystemId(request.getIndicatorSystemId());
        r.setIndicatorSystemName(request.getIndicatorSystemName());
        r.setScoredIndicatorTree(t.rawTop == null ? t.roots : t.rawTop);

        if (cc.getBooleanValue("intermediateResultOutput", false)) {
            JSONObject mid = new JSONObject();
            mid.put("dimensionScores", dimensions);
            mid.put("algorithmChainMode", readMode(cc));
            mid.put("aggregationSource", readAggSource(cc));
            mid.put("parallelBatchSize", batchSize(cc));
            r.setIntermediateResults(mid);
        }
        return r;
    }

    private void processLeafNodes(JSONArray nodes, int rows, ExternalDataContext externalDataContext) {
        if (nodes == null || nodes.isEmpty()) return;
        for (int i = 0; i < nodes.size(); i++) {
            JSONObject n = nodes.getJSONObject(i);
            JSONArray ch = n.getJSONArray("children");
            if (ch == null || ch.isEmpty()) processLeafNode(n, rows, externalDataContext);
            else processLeafNodes(ch, rows, externalDataContext);
        }
    }

    private void processLeafNode(JSONObject n, int rows, ExternalDataContext externalDataContext) {
        BigDecimal existing = readLeafScore(n);
        if (existing != null) {
            BigDecimal s = existing.setScale(2, RoundingMode.HALF_UP);
            n.put("score", s);
            n.put("calculatedValue", s);
            applyLeafReportFields(n, s);
            return;
        }
        JSONObject cr = n.getJSONObject("computeRule");
        if (cr == null) {
            n.put("score", BigDecimal.ZERO);
            n.put("calculatedValue", BigDecimal.ZERO);
            applyLeafReportFields(n, BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
            return;
        }
        try {
            Double v = computeRule(cr, null, rows, meta(n), externalDataContext, n.getString("label"));
            BigDecimal s = v == null ? BigDecimal.ZERO : BigDecimal.valueOf(v).setScale(2, RoundingMode.HALF_UP);
            n.put("score", s);
            n.put("calculatedValue", s);
            applyLeafReportFields(n, s);
        } catch (Exception ex) {
            log.warn("leaf compute failed: {}", ex.getMessage());
            n.put("score", BigDecimal.ZERO);
            n.put("calculatedValue", BigDecimal.ZERO);
            applyLeafReportFields(n, BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
        }
    }

    private void applyLeafReportFields(JSONObject n, BigDecimal score) {
        if (n == null || score == null) return;
        n.put("evalValue", score);
        n.put("tone", tone(score));
        if (!n.containsKey("referenceValue")) {
            BigDecimal reference = firstNumber(n, "threshold", "valueMax", "valueMin");
            if (reference != null) {
                n.put("referenceValue", reference);
            }
        }
        if (!n.containsKey("unit")) {
            n.put("unit", "");
        }
    }

    private JSONObject meta(JSONObject n) {
        JSONObject m = new JSONObject();
        for (String k : Arrays.asList("indicatorType", "valueCategory", "unit", "valueMin", "valueMax", "workMode", "type")) {
            Object v = n.get(k);
            if (v != null) m.put(k, v);
        }
        return m;
    }

    private Double computeRule(JSONObject cr, String targetId, int rows, JSONObject meta, ExternalDataContext externalDataContext, String indicatorLabel) {
        JSONObject method = cr.getJSONObject("method");
        if (method == null) return null;
        JSONArray nodes = method.getJSONArray("node");
        JSONArray lines = method.getJSONArray("lineList");
        if (nodes == null || nodes.isEmpty()) return null;
        if (!StringUtils.hasText(targetId)) {
            for (int i = 0; i < nodes.size(); i++) {
                JSONObject n = nodes.getJSONObject(i);
                if ("result".equalsIgnoreCase(n.getString("type"))) {
                    targetId = n.getString("id");
                    break;
                }
            }
        }
        if (!StringUtils.hasText(targetId)) targetId = nodes.getJSONObject(nodes.size() - 1).getString("id");
        return nodeValue(nodes, lines, targetId, rows, new HashMap<>(), meta, externalDataContext, indicatorLabel);
    }
    private Double nodeValue(JSONArray nodes, JSONArray lines, String id, int rows, Map<String, Double> cache, JSONObject meta, ExternalDataContext externalDataContext, String indicatorLabel) {
        if (cache.containsKey(id)) return cache.get(id);
        JSONObject n = findNode(nodes, id);
        if (n == null) return null;
        String type = n.getString("type");
        if ("result".equalsIgnoreCase(type)) {
            List<String> src = sources(lines, id);
            if (src.isEmpty()) return null;
            Double v = nodeValue(nodes, lines, src.get(0), rows, cache, meta, externalDataContext, indicatorLabel);
            cache.put(id, v);
            return v;
        }
        if ("algo".equalsIgnoreCase(type)) {
            Double v = runAlgoNode(n, nodes, lines, rows, cache, meta, externalDataContext, indicatorLabel);
            cache.put(id, v);
            return v;
        }
        if ("start".equalsIgnoreCase(type)) {
            Double v = externalDataContext == null ? null : externalDataContext.resolveStartValue(indicatorLabel, n);
            if (v == null) {
                v = mockAvg(rows);
                log.info("[start节点] indicator={}, startId={} → 未命中外部数据，使用模拟值={}", indicatorLabel, n.getString("id"), String.format("%.4f", v));
            } else {
                log.info("[start节点] indicator={}, startId={} → 命中外部数据，值={}", indicatorLabel, n.getString("id"), String.format("%.4f", v));
            }
            cache.put(id, v);
            return v;
        }
        return null;
    }

    private Double runAlgoNode(JSONObject algoNode, JSONArray nodes, JSONArray lines, int rows, Map<String, Double> cache, JSONObject meta, ExternalDataContext externalDataContext, String indicatorLabel) {
        List<Double> input = new ArrayList<>();
        for (String srcId : sources(lines, algoNode.getString("id"))) {
            Double v = nodeValue(nodes, lines, srcId, rows, cache, meta, externalDataContext, indicatorLabel);
            if (v != null) input.add(v);
        }

        String path = algoNode.getString("url");
        if (!StringUtils.hasText(path)) return input.isEmpty() ? 0D : input.stream().mapToDouble(Double::doubleValue).average().orElse(0D);
        String[] arr = path.replace('\\', '/').split("/");
        String name = arr[arr.length - 1].replace(".zip", "");

        JSONObject cfg = new JSONObject();
        JSONArray cfgArr = algoNode.getJSONArray("config");
        if (cfgArr != null) {
            for (int i = 0; i < cfgArr.size(); i++) {
                JSONObject c = cfgArr.getJSONObject(i);
                String field = c.getString("field");
                String key = StringUtils.hasText(field) ? field : c.getString("name");
                if (StringUtils.hasText(key)) cfg.put(key, c.getString("defaultValue"));
            }
        }
        if (meta != null) for (String k : meta.keySet()) cfg.putIfAbsent(k, meta.get(k));

        try {
            Object result = zgpgAlgsClient.runAlgorithm(name, buildData(input, rows, externalDataContext != null), cfg.toJSONString(), mapAlgoType(algoNode.getString("algoType")));
            Double parsed = numeric(result);
            return parsed != null ? parsed : (input.isEmpty() ? 0D : input.stream().mapToDouble(Double::doubleValue).average().orElse(0D));
        } catch (Exception ex) {
            log.warn("algo failed: {}", ex.getMessage());
            return input.isEmpty() ? 0D : input.stream().mapToDouble(Double::doubleValue).average().orElse(0D);
        }
    }

    private String buildData(List<Double> input, int rows, boolean deterministic) {
        Random r = new Random();
        if (input == null || input.isEmpty()) return sample(rows, 1).toJSONString();
        if (input.size() == 1) {
            JSONArray d = new JSONArray();
            double base = input.get(0);
            for (int i = 0; i < rows; i++) {
                d.add(deterministic ? base : base * (0.8 + r.nextDouble() * 0.4));
            }
            return d.toJSONString();
        }
        JSONArray d = new JSONArray();
        for (int i = 0; i < rows; i++) {
            JSONArray row = new JSONArray();
            for (Double v : input) row.add(deterministic ? v : v * (0.8 + r.nextDouble() * 0.4));
            d.add(row);
        }
        return d.toJSONString();
    }

    private JSONArray sample(int rows, int cols) {
        Random r = new Random();
        JSONArray data = new JSONArray();
        for (int i = 0; i < rows; i++) {
            if (cols == 1) data.add(r.nextDouble() * 100);
            else {
                JSONArray row = new JSONArray();
                for (int j = 0; j < cols; j++) row.add(r.nextDouble() * 100);
                data.add(row);
            }
        }
        return data;
    }

    private Double mockAvg(int rows) {
        Random r = new Random();
        double sum = 0D;
        for (int i = 0; i < rows; i++) sum += r.nextDouble() * 100;
        return sum / Math.max(1, rows);
    }

    /**
     * 兼容性提取字段名：支持字符串 "field" 和对象 {"field": "中文名"} 两种格式。
     */
    private static String extractFieldName(Object item) {
        if (item == null) return null;
        if (item instanceof String) return (String) item;
        if (item instanceof Map) {
            Map<?, ?> map = (Map<?, ?>) item;
            if (map.isEmpty()) return null;
            return String.valueOf(map.keySet().iterator().next());
        }
        return String.valueOf(item);
    }


    private ExternalDataContext loadExternalDataContext(CalcExecuteRequest request, JSONObject cc, JSONArray roots) {
        String resolvedMode = cc == null ? null : cc.getString("dataInputMode");
        if (!StringUtils.hasText(resolvedMode)) {
            resolvedMode = defaultDataInputMode;
        }
        log.info("[数据输入] 模式判断: comprehensiveConfig.dataInputMode={}, defaultDataInputMode={}, 最终使用={}",
                cc == null ? null : cc.getString("dataInputMode"), defaultDataInputMode, resolvedMode);

        if (!useExternalData(cc)) {
            log.info("[数据输入] 模式=MOCK，使用随机模拟数据");
            return null;
        }
        if (request.getAssessTaskId() == null || request.getAssessTaskId() <= 0) {
            log.warn("[数据输入] 模式=EXTERNAL_API，但 assessTaskId 为空，降级为模拟数据");
            return null;
        }
        List<String> indicatorLabels = collectLeafLabels(roots);
        log.info("[数据输入] 模式=EXTERNAL_API, assessTaskId={}, 叶节点指标列表={}", request.getAssessTaskId(), indicatorLabels);
        if (indicatorLabels.isEmpty()) {
            log.warn("[数据输入] 叶节点指标列表为空，降级为模拟数据");
            return null;
        }
        try {
            List<ExternalEvaluationDataClient.ExternalDataItem> items =
                    externalEvaluationDataClient.fetchEvaluationData(request.getAssessTaskId(), indicatorLabels);
            log.info("[数据输入] 外部接口返回 {} 条数据项", items == null ? 0 : items.size());
            if (items != null) {
                for (ExternalEvaluationDataClient.ExternalDataItem item : items) {
                    log.info("[数据输入]   dataItemCode={}, indicatorCode={}, tableCode={}, dataJson={}",
                            item.getDataItemCode(), item.getIndicatorCode(), item.getTableCode(), item.getDataJson());
                }
            }
            return ExternalDataContext.of(items);
        } catch (Exception ex) {
            log.warn("[数据输入] 外部接口调用失败，降级为模拟数据: {}", ex.getMessage());
            return null;
        }
    }

    private boolean useExternalData(JSONObject cc) {
        String mode = cc == null ? null : cc.getString("dataInputMode");
        if (!StringUtils.hasText(mode)) {
            mode = defaultDataInputMode;
        }
        return "EXTERNAL_API".equalsIgnoreCase(mode);
    }

    private List<String> collectLeafLabels(JSONArray nodes) {
        List<String> labels = new ArrayList<>();
        collectLeafLabelsRec(nodes, labels);
        return labels;
    }

    private void collectLeafLabelsRec(JSONArray nodes, List<String> labels) {
        if (nodes == null || nodes.isEmpty()) {
            return;
        }
        for (int i = 0; i < nodes.size(); i++) {
            JSONObject node = nodes.getJSONObject(i);
            JSONArray children = node.getJSONArray("children");
            if (children == null || children.isEmpty()) {
                String label = node.getString("label");
                if (StringUtils.hasText(label) && !labels.contains(label)) {
                    labels.add(label);
                }
                continue;
            }
            collectLeafLabelsRec(children, labels);
        }
    }

    static class ExternalDataContext {
        // 按 indicatorCode 分组
        private final Map<String, List<ExternalEvaluationDataClient.ExternalDataItem>> byIndicator;
        // 按 dataItemCode 索引，用于没有 indicatorCode 的数据项直接命中
        private final Map<String, ExternalEvaluationDataClient.ExternalDataItem> byDataItemCode;

        private ExternalDataContext(Map<String, List<ExternalEvaluationDataClient.ExternalDataItem>> byIndicator,
                                    Map<String, ExternalEvaluationDataClient.ExternalDataItem> byDataItemCode) {
            this.byIndicator = byIndicator;
            this.byDataItemCode = byDataItemCode;
        }

        static ExternalDataContext of(List<ExternalEvaluationDataClient.ExternalDataItem> items) {
            Map<String, List<ExternalEvaluationDataClient.ExternalDataItem>> indicatorMap = new HashMap<>();
            Map<String, ExternalEvaluationDataClient.ExternalDataItem> dataItemMap = new HashMap<>();
            if (items != null) {
                for (ExternalEvaluationDataClient.ExternalDataItem item : items) {
                    if (item == null) {
                        continue;
                    }
                    if (StringUtils.hasText(item.getDataItemCode())) {
                        dataItemMap.put(item.getDataItemCode(), item);
                    }
                    if (StringUtils.hasText(item.getIndicatorCode())) {
                        indicatorMap.computeIfAbsent(item.getIndicatorCode(), k -> new ArrayList<>()).add(item);
                    }
                }
            }
            return new ExternalDataContext(indicatorMap, dataItemMap);
        }

        Double resolveStartValue(String indicatorLabel, JSONObject startNode) {
            if (startNode == null) {
                return null;
            }
            String startId = startNode.getString("id");
            // 优先通过 dataItemCode 精确匹配（start节点的id即为dataItemCode）
            if (StringUtils.hasText(startId) && byDataItemCode.containsKey(startId)) {
                return toStartValue(byDataItemCode.get(startId), startNode.getJSONArray("fields"));
            }
            // 降级：通过 indicatorCode（叶节点label）查找
            if (!StringUtils.hasText(indicatorLabel)) {
                return null;
            }
            List<ExternalEvaluationDataClient.ExternalDataItem> entries = byIndicator.get(indicatorLabel);
            if (entries == null || entries.isEmpty()) {
                return null;
            }
            String tableCode = startNode.getString("value");
            ExternalEvaluationDataClient.ExternalDataItem matched = null;
            if (StringUtils.hasText(tableCode)) {
                for (ExternalEvaluationDataClient.ExternalDataItem one : entries) {
                    if (tableCode.equals(one.getTableCode())) {
                        matched = one;
                        break;
                    }
                }
            }
            if (matched == null) {
                matched = entries.get(0);
            }
            return toStartValue(matched, startNode.getJSONArray("fields"));
        }

        private Double toStartValue(ExternalEvaluationDataClient.ExternalDataItem item, JSONArray fields) {
            if (item == null || !StringUtils.hasText(item.getDataJson())) {
                return null;
            }
            JSONArray data;
            try {
                data = JSON.parseArray(item.getDataJson());
            } catch (Exception ex) {
                return null;
            }
            if (data == null || data.isEmpty()) {
                return null;
            }
            List<Double> values = new ArrayList<>();
            for (int i = 0; i < data.size(); i++) {
                Object rowRaw = data.get(i);
                if (!(rowRaw instanceof JSONObject)) {
                    Double single = asDouble(rowRaw);
                    if (single != null) {
                        values.add(single);
                    }
                    continue;
                }
                JSONObject row = (JSONObject) rowRaw;
                if (fields != null && !fields.isEmpty()) {
                    for (int j = 0; j < fields.size(); j++) {
                        String fieldName = extractFieldName(fields.get(j));
                        if (StringUtils.hasText(fieldName)) {
                            Double one = asDouble(row.get(fieldName));
                            if (one != null) {
                                values.add(one);
                            }
                        }
                    }
                } else {
                    for (String key : row.keySet()) {
                        Double one = asDouble(row.get(key));
                        if (one != null) {
                            values.add(one);
                        }
                    }
                }
            }
            if (values.isEmpty()) {
                return (double) data.size();
            }
            double sum = 0D;
            boolean binary = true;
            for (Double value : values) {
                sum += value;
                if (value < 0D || value > 1D) {
                    binary = false;
                }
            }
            double avg = sum / values.size();
            return binary ? avg * 100D : avg;
        }

        private Double asDouble(Object value) {
            if (value == null) {
                return null;
            }
            if (value instanceof Number) {
                return ((Number) value).doubleValue();
            }
            String text = String.valueOf(value);
            if (!StringUtils.hasText(text)) {
                return null;
            }
            try {
                return Double.parseDouble(text.trim());
            } catch (Exception ignore) {
                return null;
            }
        }
    }
    private String mapAlgoType(String t) {
        if (!StringUtils.hasText(t)) return "character";
        String s = t.trim();
        if ("norm".equals(s) || "指标量化算法".equals(s)) return "norm";
        if ("character".equals(s) || "属性值计算方法".equals(s)) return "character";
        if ("weight".equals(s) || "权重分配".equals(s)) return "weight";
        if ("conduction".equals(s) || "权重传导".equals(s) || "聚合传导".equals(s) || "综合传导".equals(s)) return "conduction";
        if ("assess".equals(s) || "方案评价".equals(s)) return "assess";
        if ("dataProcess".equals(s) || "预处理算法".equals(s)) return "dataProcess";
        return "character";
    }

    private JSONObject findNode(JSONArray nodes, String id) {
        if (nodes == null || !StringUtils.hasText(id)) return null;
        for (int i = 0; i < nodes.size(); i++) {
            JSONObject n = nodes.getJSONObject(i);
            if (id.equals(n.getString("id"))) return n;
        }
        return null;
    }

    private List<String> sources(JSONArray lines, String targetId) {
        List<String> ids = new ArrayList<>();
        if (lines == null || !StringUtils.hasText(targetId)) return ids;
        for (int i = 0; i < lines.size(); i++) {
            JSONObject l = lines.getJSONObject(i);
            if (targetId.equals(l.getString("targetId"))) ids.add(l.getString("sourceId"));
        }
        return ids;
    }
    private JSONObject toObj(Object raw) {
        if (raw == null) return new JSONObject();
        if (raw instanceof JSONObject) return (JSONObject) raw;
        if (raw instanceof String) {
            String s = ((String) raw).trim();
            return s.isEmpty() ? new JSONObject() : JSON.parseObject(s);
        }
        return JSON.parseObject(JSON.toJSONString(raw));
    }

    private ParsedTree parseTree(String json) {
        ParsedTree r = new ParsedTree();
        r.roots = new JSONArray();
        if (!StringUtils.hasText(json)) return r;
        try {
            String s = json.trim();
            if (s.startsWith("[")) {
                JSONArray arr = JSON.parseArray(s);
                r.roots = arr;
                r.nodeCount = countNodes(arr);
                JSONObject wrap = new JSONObject();
                wrap.put("children", arr);
                r.rawTop = wrap;
                return r;
            }
            JSONObject obj = JSON.parseObject(s);
            r.rawTop = obj;
            JSONObject inner = obj.getJSONObject("treeData");
            if (inner == null) inner = obj;
            JSONArray ch = inner.getJSONArray("children");
            if (ch != null && !ch.isEmpty()) r.roots = ch;
            else {
                r.roots = new JSONArray();
                r.roots.add(inner);
            }
            r.nodeCount = countNodes(r.roots);
        } catch (Exception ex) {
            log.warn("parse tree failed: {}", ex.getMessage());
        }
        return r;
    }

    private int countNodes(JSONArray nodes) {
        if (nodes == null || nodes.isEmpty()) return 0;
        int c = 0;
        for (int i = 0; i < nodes.size(); i++) {
            JSONObject n = nodes.getJSONObject(i);
            c++;
            c += countNodes(n.getJSONArray("children"));
        }
        return c;
    }

    private String readRoute(JSONObject sc) {
        String route = sc == null ? null : sc.getString("routeStrategy");
        return StringUtils.hasText(route) ? route.trim() : "FIRST";
    }

    private String readMode(JSONObject cc) {
        String mode = cc == null ? null : cc.getString("algorithmChainMode");
        return StringUtils.hasText(mode) ? mode.trim().toUpperCase(Locale.ROOT) : "SERIAL";
    }

    private String readAggSource(JSONObject cc) {
        String src = cc == null ? null : cc.getString("aggregationSource");
        return StringUtils.hasText(src) ? src.trim().toUpperCase(Locale.ROOT) : "INDICATOR_SYSTEM";
    }

    private int batchSize(JSONObject cc) {
        int v = cc == null ? 5 : cc.getIntValue("parallelBatchSize", 5);
        return Math.max(1, Math.min(v, 64));
    }

    private boolean useParallel(JSONObject cc, int size) {
        return "PARALLEL".equals(readMode(cc)) && size > 1;
    }

    private List<List<Integer>> chunks(int size, int batch) {
        List<Integer> ids = new ArrayList<>(size);
        for (int i = 0; i < size; i++) ids.add(i);
        List<List<Integer>> out = new ArrayList<>();
        int b = Math.max(1, batch);
        for (int i = 0; i < ids.size(); i += b) out.add(ids.subList(i, Math.min(i + b, ids.size())));
        return out;
    }

    private boolean useOverrideAgg(JSONObject cc) {
        return "TEMPLATE_OVERRIDE".equals(readAggSource(cc)) && StringUtils.hasText(overrideAggName(cc));
    }

    private String overrideAggName(JSONObject cc) {
        if (cc == null) return null;
        String n = cc.getString("overrideAggregationAlgName");
        if (StringUtils.hasText(n)) return n.trim();
        Object raw = cc.get("overrideAggregationAlg");
        if (raw instanceof String) {
            String s = ((String) raw).trim();
            if (!s.matches("\\d+")) return s;
        }
        return null;
    }

    private Double numeric(Object v) {
        if (v == null) return null;
        if (v instanceof Number) return ((Number) v).doubleValue();
        if (v instanceof JSONArray) {
            JSONArray a = (JSONArray) v;
            return a.isEmpty() ? null : numeric(a.get(a.size() - 1));
        }
        if (v instanceof JSONObject) {
            JSONObject o = (JSONObject) v;
            Double d = numeric(o.get("value"));
            if (d != null) return d;
            for (String k : Arrays.asList("score", "result", "data", "output")) {
                d = numeric(o.get(k));
                if (d != null) return d;
            }
        }
        return null;
    }
    private BigDecimal calcOverall(JSONArray roots, JSONArray dims, JSONObject cc, Long taskId) {
        if (roots == null || roots.isEmpty()) return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        BigDecimal sum = BigDecimal.ZERO;
        int cnt = 0;
        
        int total = roots.size();
        if (useParallel(cc, roots.size())) {
            List<BigDecimal> scores = Collections.synchronizedList(new ArrayList<>());
            chunks(roots.size(), batchSize(cc)).parallelStream().forEach(batch -> {
                for (Integer i : batch) {
                    JSONObject root = roots.getJSONObject(i);
                    String label = StringUtils.hasText(root.getString("label")) ? root.getString("label") : ("Dimension" + (i + 1));
                    zhpgCallbackClient.notifyProgress(taskId, 50 + (int)(40.0 * (scores.size() + 1) / total), "正在计算维度: " + label);
                    
                    BigDecimal s = calcNode(root, cc);
                    if (s != null) {
                        scores.add(s);
                    }
                }
            });
            for (BigDecimal s : scores) sum = sum.add(s);
            cnt = scores.size();
            // 收集所有得分
            for (int i = 0; i < roots.size(); i++) {
                collectAllScores(roots.getJSONObject(i), dims, cc);
            }
        } else {
            for (int i = 0; i < roots.size(); i++) {
                JSONObject root = roots.getJSONObject(i);
                String label = StringUtils.hasText(root.getString("label")) ? root.getString("label") : ("Dimension" + (i + 1));
                zhpgCallbackClient.notifyProgress(taskId, 50 + (int)(40.0 * (i + 1) / total), "正在计算维度: " + label);
                
                BigDecimal s = calcNode(root, cc);
                if (s != null) {
                    sum = sum.add(s);
                    cnt++;
                }
                // 递归收集所有节点到 dimensions，以便前端展示所有层级的得分
                collectAllScores(root, dims, cc);
            }
        }
        if (cnt == 0) return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        BigDecimal overall = sum.divide(BigDecimal.valueOf(cnt), 2, RoundingMode.HALF_UP);
        
        // 最终修正：确保总体得分也是百分制
        overall = normalizeToHundred(overall);
        
        return overall;
    }

    /**
     * 归一化分值到百分制
     */
    private BigDecimal normalizeToHundred(BigDecimal s) {
        if (s == null) return null;
        // 如果结果在 [0, 1] 之间，且不是因为本来就是极低分，则转成百分制
        // 这里增加一个判断：如果 scale 较大或者明确是比例，则乘以 100
        if (s.compareTo(BigDecimal.ONE) <= 0 && s.compareTo(BigDecimal.ZERO) >= 0) {
            return s.multiply(BigDecimal.valueOf(100)).setScale(2, RoundingMode.HALF_UP);
        }
        return s.setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * 递归收集所有层级指标的得分
     */
    private void collectAllScores(JSONObject node, JSONArray dims, JSONObject cc) {
        if (node == null) return;
        BigDecimal s = node.getBigDecimal("score");
        if (s != null) {
            JSONObject row = new JSONObject();
            row.put("label", node.getString("label"));
            row.put("value", s);
            row.put("tone", tone(s));
            dims.add(row);
        }
        JSONArray children = node.getJSONArray("children");
        if (children != null) {
            for (int i = 0; i < children.size(); i++) {
                collectAllScores(children.getJSONObject(i), dims, cc);
            }
        }
    }

    private BigDecimal calcNode(JSONObject n, JSONObject cc) {
        if (n == null) return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        JSONArray ch = n.getJSONArray("children");
        if (ch == null || ch.isEmpty()) return scoreLeaf(n, cc);

        JSONArray childScores = new JSONArray();
        JSONArray childWeights = new JSONArray();
        BigDecimal weighted = BigDecimal.ZERO;
        BigDecimal totalW = BigDecimal.ZERO;
        for (int i = 0; i < ch.size(); i++) {
            JSONObject c = ch.getJSONObject(i);
            BigDecimal s = calcNode(c, cc);
            if (s == null) continue;
            BigDecimal w = weight(c);
            childScores.add(s);
            childWeights.add(w);
            weighted = weighted.add(s.multiply(w));
            totalW = totalW.add(w);
        }

        BigDecimal agg;
        if (totalW.compareTo(BigDecimal.ZERO) <= 0) {
            String p = nullPolicy(cc);
            if ("SKIP".equalsIgnoreCase(p)) agg = null;
            else if ("TERMINATE".equalsIgnoreCase(p)) throw new IllegalStateException("No valid child score and policy TERMINATE");
            else agg = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        } else if (useOverrideAgg(cc)) {
            BigDecimal o = overrideAgg(childScores, childWeights, cc);
            agg = o != null ? o : weighted.divide(totalW, 2, RoundingMode.HALF_UP);
        } else {
            agg = weighted.divide(totalW, 2, RoundingMode.HALF_UP);
        }

        if (agg != null) {
            // 确保聚合后的父节点得分也是百分制
            agg = normalizeToHundred(agg);
            n.put("score", agg);
            n.put("calculatedValue", agg);
        }
        return agg;
    }

    private BigDecimal overrideAgg(JSONArray scores, JSONArray weights, JSONObject cc) {
        String name = overrideAggName(cc);
        if (!StringUtils.hasText(name)) return null;
        try {
            JSONObject cfg = new JSONObject();
            cfg.put("weights", weights);
            Object v = zgpgAlgsClient.runAlgorithm(name, scores.toJSONString(), cfg.toJSONString(), mapAlgoType(cc.getString("overrideAggregationAlgType")));
            Double d = numeric(v);
            return d == null ? null : BigDecimal.valueOf(d).setScale(2, RoundingMode.HALF_UP);
        } catch (Exception ex) {
            log.warn("override aggregation failed: {}", ex.getMessage());
            return null;
        }
    }

    private BigDecimal scoreLeaf(JSONObject n, JSONObject cc) {
        BigDecimal s = readLeafScore(n);
        if (s != null) {
            // 如果读取到的得分是 [0, 1] 之间的比例，自动转成 [0, 100] 的分值
            if (s.compareTo(BigDecimal.ONE) <= 0 && s.compareTo(BigDecimal.ZERO) >= 0) {
                // 排除刚好是 0 或 1 的情况（也统一处理没太大副作用，因为 1.0 变 100 也是对的）
                s = s.multiply(BigDecimal.valueOf(100));
            }
            return s.setScale(2, RoundingMode.HALF_UP);
        }
        String p = nullPolicy(cc);
        if ("TERMINATE".equalsIgnoreCase(p)) throw new IllegalStateException("Leaf score missing");
        if ("SKIP".equalsIgnoreCase(p)) return null;
        return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    }

    private String nullPolicy(JSONObject cc) {
        String p = cc == null ? null : cc.getString("nullDataPolicy");
        return StringUtils.hasText(p) ? p.trim() : "ZERO_FILL";
    }

    private BigDecimal readLeafScore(JSONObject n) {
        BigDecimal s = firstNumber(n, "score", "calculatedValue", "evalScore", "evalValue");
        if (s != null) return s;
        BigDecimal raw = firstNumber(n, "metricValue", "value", "measurement", "rawValue");
        if (raw == null) return null;
        BigDecimal min = n.getBigDecimal("valueMin");
        BigDecimal max = n.getBigDecimal("valueMax");
        if (min == null || max == null || max.compareTo(min) <= 0) return raw;
        BigDecimal ratio = raw.subtract(min).divide(max.subtract(min), 8, RoundingMode.HALF_UP);
        String t = StringUtils.hasText(n.getString("type")) ? n.getString("type") : n.getString("valueCategory");
        if ("COST".equalsIgnoreCase(t)) ratio = BigDecimal.ONE.subtract(ratio);
        if (ratio.compareTo(BigDecimal.ZERO) < 0) ratio = BigDecimal.ZERO;
        if (ratio.compareTo(BigDecimal.ONE) > 0) ratio = BigDecimal.ONE;
        return ratio.multiply(BigDecimal.valueOf(100));
    }

    private BigDecimal firstNumber(JSONObject n, String... keys) {
        if (n == null) return null;
        for (String k : keys) {
            BigDecimal v = n.getBigDecimal(k);
            if (v != null) return v;
        }
        return null;
    }

    private BigDecimal weight(JSONObject n) {
        BigDecimal w = n == null ? null : n.getBigDecimal("weight");
        return (w == null || w.compareTo(BigDecimal.ZERO) <= 0) ? BigDecimal.ONE : w;
    }

    private String resolveGrade(BigDecimal score, JSONObject cc) {
        JSONArray levels = cc == null ? null : cc.getJSONArray("scoreLevels");
        if (levels != null && !levels.isEmpty()) {
            List<JSONObject> ordered = new ArrayList<>();
            for (int i = 0; i < levels.size(); i++) ordered.add(levels.getJSONObject(i));
            ordered.sort(Comparator.comparing((JSONObject o) -> o.getBigDecimal("threshold") == null ? BigDecimal.ZERO : o.getBigDecimal("threshold")).reversed());
            for (JSONObject lv : ordered) {
                BigDecimal th = lv.getBigDecimal("threshold");
                String name = lv.getString("name");
                if (th != null && score.compareTo(th) >= 0 && StringUtils.hasText(name)) return name;
            }
            String lowest = ordered.get(ordered.size() - 1).getString("name");
            return StringUtils.hasText(lowest) ? lowest : "D";
        }
        JSONObject custom = cc == null ? null : cc.getJSONObject("scoreLevelConfig");
        BigDecimal e = custom == null || custom.getBigDecimal("EXCELLENT") == null ? new BigDecimal("90") : custom.getBigDecimal("EXCELLENT");
        BigDecimal g = custom == null || custom.getBigDecimal("GOOD") == null ? new BigDecimal("75") : custom.getBigDecimal("GOOD");
        BigDecimal p = custom == null || custom.getBigDecimal("PASS") == null ? new BigDecimal("60") : custom.getBigDecimal("PASS");
        if (score.compareTo(e) >= 0) return "A";
        if (score.compareTo(g) >= 0) return "B";
        if (score.compareTo(p) >= 0) return "C";
        return "D";
    }

    private String tone(BigDecimal score) {
        if (score.compareTo(new BigDecimal("90")) >= 0) return "excellent";
        if (score.compareTo(new BigDecimal("75")) >= 0) return "good";
        if (score.compareTo(new BigDecimal("60")) >= 0) return "pass";
        return "risk";
    }

    private String buildConclusion(CalcExecuteRequest req, BigDecimal score, String grade) {
        String tn = StringUtils.hasText(req.getTemplateName())
                ? IndicatorReportSectionBuilder.cleanTaskName(req.getTemplateName())
                : "当前评估任务";
        String sn = StringUtils.hasText(req.getIndicatorSystemName())
                ? IndicatorReportSectionBuilder.cleanIndicatorSystemName(req.getIndicatorSystemName())
                : "当前指标体系";
        return tn + "已基于" + sn + "完成综合评估，综合得分为" + score + "，评定等级为" + grade + "。";
    }

    private String buildSuggestion(BigDecimal score, String route, JSONObject rp) {
        if (score.compareTo(new BigDecimal("75")) >= 0) {
            return "当前评估结果总体稳定，建议继续保持现有能力建设节奏，并定期复核关键指标的数据质量和权重配置。";
        }
        return "建议优先复核低分指标的数据来源、计算规则和权重配置，针对薄弱能力域制定专项改进措施并组织复测。";
    }

    private static class ParsedTree {
        JSONArray roots;
        int nodeCount;
        JSONObject rawTop;
    }
}
