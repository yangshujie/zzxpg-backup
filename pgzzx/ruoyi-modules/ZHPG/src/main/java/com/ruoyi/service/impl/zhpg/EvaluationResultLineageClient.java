package com.ruoyi.service.impl.zhpg;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.ruoyi.domain.zhpg.EvalResult;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@Service
public class EvaluationResultLineageClient {

    private static final String RESULT_NODE_TYPE = "EVALUATION_RESULT";
    private static final String EDGE_TYPE = "INDEX_TO_RESULT";
    private static final String INDEX_NODE_TYPE = "INDEX";
    private static final String TEMPLATE_STEP_NODE_TYPE = "TEMPLATE_STEP";
    private static final String SOURCE_COLUMN_NODE_TYPE = "SOURCE_COLUMN";
    private static final String REQUIREMENT_TO_INDEX = "REQUIREMENT_TO_INDEX";
    private static final String INDEX_TO_INDEX = "INDEX_TO_INDEX";
    private static final String INDEX_TO_ALGORITHM = "INDEX_TO_ALGORITHM";
    private static final String OPERATOR_TO_SOURCE = "OPERATOR_TO_SOURCE";

    @Value("${zhpg.huage.base-url}")
    private String huageBaseUrl;

    @Value("${zhpg.huage.lineage-path}")
    private String lineagePath;

    @Value("${zhpg.huage.lineage-interval-ms}")
    private long lineageIntervalMs;

    @Value("${zhpg.huage.connect-timeout-ms}")
    private int connectTimeoutMs;

    @Value("${zhpg.huage.read-timeout-ms}")
    private int readTimeoutMs;

    private final ExecutorService lineageExecutor;
    private RestTemplate restTemplate;

    public EvaluationResultLineageClient() {
        this.lineageExecutor = Executors.newSingleThreadExecutor(r -> {
            Thread thread = new Thread(r, "zhpg-eval-result-lineage");
            thread.setDaemon(true);
            return thread;
        });
    }

    @PostConstruct
    public void init() {
        this.restTemplate = buildRestTemplate(connectTimeoutMs, readTimeoutMs);
        log.info("评估结果血缘客户端初始化: url={}, intervalMs={}, connectTimeoutMs={}, readTimeoutMs={}",
                buildLineageUrl(), lineageIntervalMs, connectTimeoutMs, readTimeoutMs);
    }

    public void submitEvalResultLineage(EvalResult evalResult, Object scoredIndicatorTree, Long requirementId) {
        List<JSONObject> requests = buildLineageRequests(evalResult, scoredIndicatorTree, requirementId);
        Long evalResultId = evalResult != null ? evalResult.getId() : null;
        if (requests.isEmpty()) {
            log.info("评估结果血缘写入跳过: evalResultId={}, reason=无可写入指标节点", evalResultId);
            return;
        }
        log.info("评估结果血缘写入入队: evalResultId={}, requestCount={}, url={}",
                evalResultId, requests.size(), buildLineageUrl());
        lineageExecutor.submit(() -> postLineageRequests(evalResultId, requests));
    }

    public void submitIndicatorSystemLineage(Object indicatorTree, Long requirementId) {
        List<JSONObject> requests = buildIndicatorSystemLineageRequests(indicatorTree, requirementId);
        if (requests.isEmpty()) {
            log.info("指标体系血缘写入跳过: requirementId={}, reason=无可写入指标节点", requirementId);
            return;
        }
        log.info("指标体系血缘写入入队: requirementId={}, requestCount={}, url={}",
                requirementId, requests.size(), buildLineageUrl());
        lineageExecutor.submit(() -> postLineageRequests(requirementId, requests));
    }

    List<JSONObject> buildLineageRequests(EvalResult evalResult, Object scoredIndicatorTree, Long requirementId) {
        List<JSONObject> requests = new ArrayList<>();
        if (evalResult == null || evalResult.getId() == null || scoredIndicatorTree == null) {
            return requests;
        }
        JSONArray roots = parseRoots(scoredIndicatorTree);
        for (int i = 0; i < roots.size(); i++) {
            collectNodeRequests(roots.getJSONObject(i), true, evalResult.getId(), requirementId, requests);
        }
        return requests;
    }

    List<JSONObject> buildIndicatorSystemLineageRequests(Object indicatorTree, Long requirementId) {
        List<JSONObject> requests = new ArrayList<>();
        JSONArray roots = parseRoots(indicatorTree);
        for (int i = 0; i < roots.size(); i++) {
            collectIndicatorSystemRequests(roots.getJSONObject(i), null, true, requirementId, requests);
        }
        return requests;
    }

    private void collectIndicatorSystemRequests(JSONObject node, String parentIndicatorId, boolean root,
                                                Long requirementId, List<JSONObject> requests) {
        if (node == null) {
            return;
        }
        String indicatorId = firstNotBlank(node.getString("id"), node.getString("uid"),
                node.getString("idCode"), node.getString("indicatorId"));
        if (StringUtils.isBlank(indicatorId)) {
            return;
        }

        String sourceNode = null;
        String edgeType = null;
        if (root && requirementId != null) {
            sourceNode = "REQUIREMENT_" + requirementId;
            edgeType = REQUIREMENT_TO_INDEX;
        } else if (StringUtils.isNotBlank(parentIndicatorId)) {
            sourceNode = "INDEX_" + parentIndicatorId;
            edgeType = INDEX_TO_INDEX;
        }
        if (StringUtils.isNotBlank(sourceNode)) {
            requests.add(buildRequest(
                    "INDEX_" + indicatorId,
                    INDEX_NODE_TYPE,
                    resolveNodeName(node, indicatorId),
                    indicatorId,
                    sourceNode,
                    "INDEX_" + indicatorId,
                    edgeType
            ));
        }

        JSONArray children = node.getJSONArray("children");
        boolean leaf = children == null || children.isEmpty();
        if (leaf) {
            collectLeafAlgorithmRequests(node, indicatorId, requests);
            return;
        }
        for (int i = 0; i < children.size(); i++) {
            collectIndicatorSystemRequests(children.getJSONObject(i), indicatorId, false, requirementId, requests);
        }
    }

    private void collectLeafAlgorithmRequests(JSONObject indicatorNode, String indicatorId, List<JSONObject> requests) {
        JSONObject method = extractComputeMethod(indicatorNode);
        if (method == null) {
            return;
        }
        JSONArray nodes = method.getJSONArray("node");
        if (nodes == null || nodes.isEmpty()) {
            return;
        }

        Map<String, JSONObject> flowNodeById = new LinkedHashMap<>();
        for (int i = 0; i < nodes.size(); i++) {
            JSONObject flowNode = nodes.getJSONObject(i);
            String flowNodeId = flowNode.getString("id");
            if (StringUtils.isNotBlank(flowNodeId)) {
                flowNodeById.put(flowNodeId, flowNode);
            }
            if ("algo".equalsIgnoreCase(flowNode.getString("type"))) {
                addAlgorithmRequestIfAbsent(requests, indicatorId, flowNode);
            }
        }

        JSONArray lineList = method.getJSONArray("lineList");
        if (lineList == null || lineList.isEmpty()) {
            return;
        }
        for (int i = 0; i < lineList.size(); i++) {
            JSONObject line = lineList.getJSONObject(i);
            JSONObject source = flowNodeById.get(line.getString("sourceId"));
            JSONObject target = flowNodeById.get(line.getString("targetId"));
            if (source == null || target == null) {
                continue;
            }
            if ("start".equalsIgnoreCase(source.getString("type"))
                    && "algo".equalsIgnoreCase(target.getString("type"))) {
                addDataSourceToAlgorithmRequestIfAbsent(requests, source, target);
            }
        }
    }

    private JSONObject extractComputeMethod(JSONObject indicatorNode) {
        JSONObject computeRule = indicatorNode.getJSONObject("computeRule");
        if (computeRule == null) {
            return null;
        }
        return computeRule.getJSONObject("method");
    }

    private void addAlgorithmRequestIfAbsent(List<JSONObject> requests, String indicatorId, JSONObject algoNode) {
        String algorithmId = resolveAlgorithmId(algoNode);
        if (StringUtils.isBlank(algorithmId)) {
            return;
        }
        String targetNode = "TEMPLATE_STEP_" + algorithmId;
        JSONObject request = buildRequest(
                targetNode,
                TEMPLATE_STEP_NODE_TYPE,
                resolveAlgorithmName(algoNode, algorithmId),
                algorithmId,
                "INDEX_" + indicatorId,
                targetNode,
                INDEX_TO_ALGORITHM
        );
        addIfEdgeAbsent(requests, request);
    }

    private void addDataSourceToAlgorithmRequestIfAbsent(List<JSONObject> requests, JSONObject sourceNode, JSONObject algoNode) {
        String sourceId = firstNotBlank(sourceNode.getString("value"), sourceNode.getString("id"));
        String algorithmId = resolveAlgorithmId(algoNode);
        if (StringUtils.isBlank(sourceId) || StringUtils.isBlank(algorithmId)) {
            return;
        }
        addDataSourceRequest(requests, sourceNode, algorithmId, sourceId);
    }

    private void addDataSourceRequest(List<JSONObject> requests, JSONObject sourceNode, String algorithmId,
                                      String sourceId) {
        String sourceColumnNodeId = "SOURCE_COLUMN_" + sourceId;
        JSONObject request = buildRequest(
                sourceColumnNodeId,
                SOURCE_COLUMN_NODE_TYPE,
                resolveNodeName(sourceNode, sourceId),
                sourceId,
                "TEMPLATE_STEP_" + algorithmId,
                sourceColumnNodeId,
                OPERATOR_TO_SOURCE
        );
        request.getJSONObject("node").put("physicalName", resolvePhysicalName(sourceNode));
        addIfEdgeAbsent(requests, request);
    }

    private void addIfEdgeAbsent(List<JSONObject> requests, JSONObject request) {
        JSONObject edge = request.getJSONObject("edge");
        for (JSONObject existing : requests) {
            JSONObject existingEdge = existing.getJSONObject("edge");
            if (existingEdge != null
                    && StringUtils.equals(existingEdge.getString("sourceNode"), edge.getString("sourceNode"))
                    && StringUtils.equals(existingEdge.getString("targetNode"), edge.getString("targetNode"))
                    && StringUtils.equals(existingEdge.getString("edgeType"), edge.getString("edgeType"))) {
                return;
            }
        }
        requests.add(request);
    }

    private void collectNodeRequests(JSONObject node, boolean root, Long evalResultId,
                                     Long requirementId, List<JSONObject> requests) {
        if (node == null) {
            return;
        }
        String indicatorId = root && requirementId != null
                ? String.valueOf(requirementId)
                : firstNotBlank(node.getString("id"), node.getString("uid"),
                node.getString("idCode"), node.getString("indicatorId"));
        if (StringUtils.isNotBlank(indicatorId)) {
            requests.add(buildEvalResultRequest(evalResultId, indicatorId, resolveNodeName(node, indicatorId)));
        }
        JSONArray children = node.getJSONArray("children");
        if (children == null) {
            return;
        }
        for (int i = 0; i < children.size(); i++) {
            collectNodeRequests(children.getJSONObject(i), false, evalResultId, requirementId, requests);
        }
    }

    private JSONObject buildEvalResultRequest(Long evalResultId, String indicatorId, String nodeName) {
        String resultPrimaryId = evalResultId + "_" + indicatorId;
        String resultNodeId = RESULT_NODE_TYPE + "_" + resultPrimaryId;
        return buildRequest(resultNodeId, RESULT_NODE_TYPE, nodeName, resultPrimaryId,
                "INDEX_" + indicatorId, resultNodeId, EDGE_TYPE);
    }

    private JSONObject buildRequest(String nodeId, String nodeType, String nodeName, String bizPrimaryId,
                                    String sourceNode, String targetNode, String edgeType) {
        JSONObject node = new JSONObject();
        node.put("nodeId", nodeId);
        node.put("nodeType", nodeType);
        node.put("nodeName", nodeName);
        node.put("bizPrimaryId", bizPrimaryId);

        JSONObject edge = new JSONObject();
        edge.put("sourceNode", sourceNode);
        edge.put("targetNode", targetNode);
        edge.put("edgeType", edgeType);

        JSONObject body = new JSONObject();
        body.put("node", node);
        body.put("edge", edge);
        return body;
    }

    private JSONArray parseRoots(Object scoredIndicatorTree) {
        if (scoredIndicatorTree instanceof JSONArray) {
            return (JSONArray) scoredIndicatorTree;
        }
        if (scoredIndicatorTree instanceof JSONObject) {
            JSONObject object = (JSONObject) scoredIndicatorTree;
            Object treeData = object.get("treeData");
            if (treeData != null) {
                return parseRoots(treeData);
            }
            JSONArray roots = new JSONArray();
            roots.add(object);
            return roots;
        }
        if (scoredIndicatorTree instanceof String) {
            String text = ((String) scoredIndicatorTree).trim();
            if (StringUtils.isBlank(text)) {
                return new JSONArray();
            }
            Object parsed = JSON.parse(text);
            return parseRoots(parsed);
        }
        return new JSONArray();
    }

    private void postLineageRequests(Long evalResultId, List<JSONObject> requests) {
        String url = buildLineageUrl();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        int successCount = 0;
        int failCount = 0;
        for (int i = 0; i < requests.size(); i++) {
            JSONObject body = requests.get(i);
            JSONObject node = body.getJSONObject("node");
            JSONObject edge = body.getJSONObject("edge");
            try {
                ResponseEntity<String> response = restTemplate.postForEntity(url, new HttpEntity<>(body, headers), String.class);
                successCount++;
                log.info("评估结果血缘写入成功: evalResultId={}, progress={}/{}, status={}, url={}, nodeId={}, sourceNode={}, targetNode={}, response={}",
                        evalResultId, i + 1, requests.size(), response.getStatusCodeValue(), url,
                        node != null ? node.getString("nodeId") : null,
                        edge != null ? edge.getString("sourceNode") : null,
                        edge != null ? edge.getString("targetNode") : null,
                        abbreviate(response.getBody(), 300));
            } catch (RestClientException ex) {
                failCount++;
                log.warn("评估结果血缘写入失败: evalResultId={}, progress={}/{}, url={}, nodeId={}, sourceNode={}, targetNode={}, error={}",
                        evalResultId, i + 1, requests.size(), url,
                        node != null ? node.getString("nodeId") : null,
                        edge != null ? edge.getString("sourceNode") : null,
                        edge != null ? edge.getString("targetNode") : null,
                        ex.getMessage());
            } catch (Exception ex) {
                failCount++;
                log.warn("评估结果血缘写入异常: evalResultId={}, progress={}/{}, url={}, nodeId={}, sourceNode={}, targetNode={}, error={}",
                        evalResultId, i + 1, requests.size(), url,
                        node != null ? node.getString("nodeId") : null,
                        edge != null ? edge.getString("sourceNode") : null,
                        edge != null ? edge.getString("targetNode") : null,
                        ex.getMessage(), ex);
            }
            sleepBeforeNext(i, requests.size());
        }
        log.info("评估结果血缘写入批次完成: evalResultId={}, total={}, success={}, failed={}, url={}",
                evalResultId, requests.size(), successCount, failCount, url);
    }

    private void sleepBeforeNext(int index, int total) {
        if (index >= total - 1 || lineageIntervalMs <= 0) {
            return;
        }
        try {
            Thread.sleep(lineageIntervalMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private String buildLineageUrl() {
        String base = StringUtils.defaultIfBlank(huageBaseUrl, "http://172.16.2.89:9501");
        String path = StringUtils.defaultIfBlank(lineagePath, "/lineage/node/with-edge");
        if (base.endsWith("/") && path.startsWith("/")) {
            return base.substring(0, base.length() - 1) + path;
        }
        if (!base.endsWith("/") && !path.startsWith("/")) {
            return base + "/" + path;
        }
        return base + path;
    }

    private String resolveNodeName(JSONObject node, String fallback) {
        return firstNotBlank(node.getString("label"), node.getString("name"),
                node.getString("indicatorName"), node.getString("title"), fallback);
    }

    private String resolveAlgorithmId(JSONObject algoNode) {
        Object value = algoNode.get("value");
        if (value != null && StringUtils.isNotBlank(String.valueOf(value))) {
            return String.valueOf(value).trim();
        }
        return firstNotBlank(algoNode.getString("algorithmId"), algoNode.getString("id"));
    }

    private String resolveAlgorithmName(JSONObject algoNode, String fallback) {
        return firstNotBlank(algoNode.getString("name"), algoNode.getString("algorithmName"),
                algoNode.getString("algoType"), algoNode.getString("url"), fallback);
    }

    private String resolvePhysicalName(JSONObject sourceNode) {
        return firstNotBlank(sourceNode.getString("value"), sourceNode.getString("physicalName"), sourceNode.getString("id"));
    }

    private String firstNotBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (StringUtils.isNotBlank(value)) {
                return value.trim();
            }
        }
        return null;
    }

    private RestTemplate buildRestTemplate(int connectTimeout, int readTimeout) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(connectTimeout);
        factory.setReadTimeout(readTimeout);
        return new RestTemplate(factory);
    }

    private String abbreviate(String text, int maxLength) {
        if (StringUtils.isBlank(text) || text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength) + "...";
    }

    @PreDestroy
    public void shutdown() {
        lineageExecutor.shutdownNow();
    }
}
