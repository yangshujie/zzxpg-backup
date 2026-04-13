package com.ruoyi.zhpgcalc.algs;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
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

import java.util.HashMap;
import java.util.Map;

/**
 * 调用 zgpg_algs（Flask + Celery）算法管理接口
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

    /**
     * 调用算法服务
     *
     * @param algsName 算法包名，如 calMean、sigmoid
     * @param dataLiteral 数据字面量，如 [[1.0,2.0],[3.0,4.0]]
     * @param configJson  算法配置JSON
     * @param algsType    算法类型：character(特征提取)、norm(指标量化)等
     * @return 算法结果
     */
    public Object runAlgorithm(String algsName, String dataLiteral, String configJson, String algsType) {
        if (algsName == null || algsName.isEmpty()) {
            throw new IllegalArgumentException("算法名不能为空");
        }
        String cfg = configJson == null || configJson.isEmpty() ? "{}" : configJson;
        String type = algsType == null || algsType.isEmpty() ? "character" : algsType;

        Map<String, Object> body = new HashMap<>();
        body.put("data", dataLiteral);
        body.put("dataType", "0");  // 0=内存数据
        body.put("algs_name", algsName);
        body.put("algs_type", type);
        body.put("config", cfg);

        String loadPath = postAlgsLoad(body);
        return pollUntilDone(resolveStatusUrl(loadPath), body);
    }

    private String postAlgsLoad(Map<String, Object> body) {
        String url = joinUrl(baseUrl, "/algsmanagement/algs_load");
        try {
            RestTemplate rt = buildRestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
            String raw = rt.postForObject(url, entity, String.class);
            if (raw == null || raw.isEmpty()) {
                throw new RuntimeException("算法服务 algs_load 返回空响应");
            }
            JSONObject o = JSON.parseObject(raw);
            String loc = o.getString("location");
            if (loc == null || loc.isEmpty()) {
                throw new RuntimeException("算法服务未返回任务 location: " + raw);
            }
            return loc;
        } catch (RestClientException ex) {
            throw new RuntimeException("无法连接算法服务: " + ex.getMessage(), ex);
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
                if (code == null) {
                    sleepPoll();
                    continue;
                }
                if (code == 200) {
                    return o.get("result");
                }
                if (code == 500) {
                    throw new RuntimeException("算法执行失败: " + o.getString("result"));
                }
            } catch (RuntimeException ex) {
                if (ex.getMessage() != null && ex.getMessage().contains("算法执行失败")) {
                    throw ex;
                }
                log.warn("轮询算法状态异常: {}", ex.getMessage());
            }
            sleepPoll();
        }
        throw new RuntimeException("等待算法结果超时（" + (pollMaxWaitMs / 1000) + "s）");
    }

    private void sleepPoll() {
        try {
            Thread.sleep(pollIntervalMs);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("等待算法结果被中断");
        }
    }

    private String resolveStatusUrl(String location) {
        if (location == null || location.isEmpty()) {
            throw new RuntimeException("任务 location 为空");
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
