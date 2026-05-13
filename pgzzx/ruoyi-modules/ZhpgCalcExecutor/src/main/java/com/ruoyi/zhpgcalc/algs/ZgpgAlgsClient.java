package com.ruoyi.zhpgcalc.algs;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Client for algorithm services.
 */
@Slf4j
@Component
public class ZgpgAlgsClient {

    @Value("${zhpg.algs.base-url:http://127.0.0.1:6090}")
    private String baseUrl;

    @Value("${zhpg.algs.connect-timeout-ms:10000}")
    private int connectTimeoutMs;

    @Value("${zhpg.algs.read-timeout-ms:120000}")
    private int readTimeoutMs;

    @Value("${zhpg.algs.poll-interval-ms:800}")
    private long pollIntervalMs;

    @Value("${zhpg.algs.poll-max-wait-ms:300000}")
    private long pollMaxWaitMs;

    public Object runAlgorithm(String algsName, String dataLiteral, String configJson, String algsType) {
        if (algsName == null || algsName.isEmpty()) {
            throw new IllegalArgumentException("Algorithm name must not be empty");
        }
        String cfg = configJson == null || configJson.isEmpty() ? "{}" : configJson;
        String type = algsType == null || algsType.isEmpty() ? "character" : algsType;

        List<Map<String, Object>> bodies = buildCompatibleRequestBodies(algsName, dataLiteral, cfg, type);
        return executeWithCompatibility(bodies.get(0), bodies.get(1));
    }

    List<Map<String, Object>> buildCompatibleRequestBodies(String algsName, String dataLiteral, String configJson, String algsType) {
        Map<String, Object> legacyBody = buildLegacyZgpgAlgsRequestBody(algsName, dataLiteral, configJson, algsType);
        Map<String, Object> hubBody = buildHubRequestBody(algsName, dataLiteral, configJson, algsType);
        return java.util.Arrays.asList(legacyBody, hubBody);
    }

    Map<String, Object> buildHubRequestBody(String algsName, String dataLiteral, String configJson, String algsType) {
        Map<String, Object> body = new HashMap<>();
        String cfg = configJson == null || configJson.isEmpty() ? "{}" : configJson;
        String type = algsType == null || algsType.isEmpty() ? "character" : algsType;

        // New Hub format: alg_name + args + structured config object.
        String fullAlgName = algsName;
        if (algsType != null && !algsType.isEmpty() && !algsName.contains("/")) {
            fullAlgName = type + "/" + algsName;
        }
        body.put("alg_name", fullAlgName);
        try {
            body.put("args", Collections.singletonList(JSON.parse(dataLiteral)));
        } catch (Exception e) {
            body.put("args", Collections.singletonList(dataLiteral));
        }
        try {
            body.put("config", JSON.parseObject(cfg));
        } catch (Exception e) {
            Map<String, Object> fallbackCfg = new HashMap<>();
            fallbackCfg.put("rawConfig", cfg);
            body.put("config", fallbackCfg);
        }
        return body;
    }

    Map<String, Object> buildLegacyZgpgAlgsRequestBody(String algsName, String dataLiteral, String configJson, String algsType) {
        Map<String, Object> body = new HashMap<>();
        String cfg = configJson == null || configJson.isEmpty() ? "{}" : configJson;
        String type = algsType == null || algsType.isEmpty() ? "character" : algsType;

        // Legacy zgpg_algs format: s_algs_load reads these exact fields.
        // Important: config must stay a JSON string because Python does eval(task["config"]).
        body.put("data", dataLiteral);
        body.put("dataType", "0");
        body.put("algs_name", algsName);
        body.put("algs_type", type);
        body.put("config", cfg);
        return body;
    }

    private Object executeWithCompatibility(Map<String, Object> preferredBody, Map<String, Object> fallbackBody) {
        try {
            return executeOnce(preferredBody);
        } catch (RuntimeException ex) {
            log.warn("Preferred algorithm request format failed, retrying compatible request format: {}", ex.getMessage());
            return executeOnce(fallbackBody);
        }
    }

    private Object executeOnce(Map<String, Object> body) {
        String url = joinUrl(baseUrl, "/algsmanagement/algs_load");
        try {
            RestTemplate rt = buildRestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
            String raw = rt.postForObject(url, entity, String.class);
            if (raw == null || raw.isEmpty()) {
                throw new RuntimeException("Algorithm service returned an empty response");
            }
            JSONObject o = JSON.parseObject(raw);

            String loc = o.getString("location");
            String taskId = o.getString("task_id");
            if (loc != null && !loc.isEmpty()) {
                return pollUntilDone(resolveStatusUrl(loc), body);
            } else if (taskId != null && !taskId.isEmpty()) {
                String statusUrl = "/algsmanagement/status/" + taskId;
                return pollUntilDone(resolveStatusUrl(statusUrl), body);
            }

            if (o.containsKey("error") || (o.containsKey("success") && !o.getBooleanValue("success"))) {
                throw new RuntimeException("Algorithm start failed: " + o.getString("error"));
            }

            throw new RuntimeException("Unsupported algorithm service response, no task id found: " + raw);
        } catch (RestClientException ex) {
            throw new RuntimeException("Unable to connect algorithm service: " + ex.getMessage(), ex);
        }
    }

    private Object pollUntilDone(String statusUrl, Map<String, Object> pollBody) {
        long deadline = System.currentTimeMillis() + pollMaxWaitMs;
        RestTemplate rt = buildRestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(pollBody, headers);

        while (System.currentTimeMillis() < deadline) {
            try {
                String raw = rt.postForObject(statusUrl, entity, String.class);
                if (raw == null || raw.isEmpty()) {
                    sleepPoll();
                    continue;
                }
                JSONObject o = JSON.parseObject(raw);
                Integer code = o.getInteger("code");
                String status = o.getString("status");

                if (Integer.valueOf(200).equals(code) || "completed".equalsIgnoreCase(status) || "success".equalsIgnoreCase(status)) {
                    return o.get("result");
                }
                if (Integer.valueOf(500).equals(code) || "failed".equalsIgnoreCase(status) || "error".equalsIgnoreCase(status)) {
                    String errorMsg = o.containsKey("error") ? o.getString("error") : String.valueOf(o.get("result"));
                    throw new RuntimeException("Algorithm execution failed: " + errorMsg);
                }
            } catch (RuntimeException ex) {
                if (ex.getMessage() != null && ex.getMessage().contains("Algorithm execution failed")) {
                    throw ex;
                }
                log.warn("Algorithm status polling error: {}", ex.getMessage());
            }
            sleepPoll();
        }
        throw new RuntimeException("Timed out waiting for algorithm result: " + (pollMaxWaitMs / 1000) + "s");
    }

    private void sleepPoll() {
        try {
            Thread.sleep(pollIntervalMs);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while waiting for algorithm result");
        }
    }

    private String resolveStatusUrl(String location) {
        if (location == null || location.isEmpty()) {
            throw new RuntimeException("Task location is empty");
        }
        String t = location.trim();
        if (t.startsWith("http://") || t.startsWith("https://")) {
            return t;
        }
        if (!t.startsWith("/")) {
            t = "/" + t;
        }
        return joinUrl(baseUrl, t);
    }

    private static String joinUrl(String base, String path) {
        String b = base == null ? "" : base.trim();
        while (b.endsWith("/")) {
            b = b.substring(0, b.length() - 1);
        }
        String p = path == null ? "" : path.trim();
        if (!p.startsWith("/")) {
            p = "/" + p;
        }
        return b + p;
    }

    private RestTemplate buildRestTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Math.max(connectTimeoutMs, 1000));
        factory.setReadTimeout(Math.max(readTimeoutMs, 5000));
        return new RestTemplate(factory);
    }
}
