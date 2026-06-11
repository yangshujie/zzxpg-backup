package com.ruoyi.zhpg.algs;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.ruoyi.common.core.exception.ServiceException;
import com.ruoyi.common.core.utils.StringUtils;
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
 * 调用 zgpg_algs（Flask + Celery）算法管理接口：先 POST algs_load，再轮询 task status。
 * 与历史项目 {@code HttpUtil + /algsmanagement/algs_load} 流程一致。
 */
@Component
public class ZhpgZgpgAlgsClient {

    @Value("${zhpg.algs.base-url:http://127.0.0.1:6000}")
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
     * 提交权重类算法（dataType=0，内存数据），返回权重列表（与特征列顺序一致）。
     *
     * @param algsName Python 包名，如 cal_weight、ahp
     * @param dataLiteral 可被 Python eval 解析的数据字面量，如 {@code [[1.0,2.0],[3.0,4.0]]}
     * @param configJson  算法 config 的 JSON 字符串，无则 "{}}"
     */
    public JSONArray runWeightAlgorithmSync(String algsName, String dataLiteral, String configJson) {
        if (StringUtils.isEmpty(algsName)) {
            throw new ServiceException("算法名不能为空");
        }
        String cfg = StringUtils.isEmpty(configJson) ? "{}" : configJson;
        Map<String, Object> body = new HashMap<>();
        body.put("data", dataLiteral);
        body.put("dataType", "0");
        body.put("algs_name", algsName);
        body.put("algs_type", "weight");
        body.put("config", cfg);

        String loadPath = postAlgsLoad(body);
        return pollUntilDone(resolveStatusUrl(loadPath), body);
    }

    /**
     * 提交通用算法（特征提取/指标量化等），返回算法计算结果。
     *
     * @param algsName Python 包名或算法标识，如 calMean、sigmoid
     * @param dataLiteral 可被 Python eval 解析的数据字面量
     * @param configJson  算法 config 的 JSON 字符串
     * @param algsType    算法类型，如 "character"(特征提取)、"norm"(量化)
     */
    public Object runAlgorithmSync(String algsName, String dataLiteral, String configJson, String algsType) {
        if (StringUtils.isEmpty(algsName)) {
            throw new ServiceException("算法名不能为空");
        }
        String cfg = StringUtils.isEmpty(configJson) ? "{}" : configJson;
        String type = StringUtils.isEmpty(algsType) ? "character" : algsType;
        Map<String, Object> body = new HashMap<>();
        body.put("data", dataLiteral);
        body.put("dataType", "0");
        body.put("algs_name", algsName);
        body.put("algs_type", type);
        body.put("config", cfg);

        String loadPath = postAlgsLoad(body);
        return pollUntilDoneGeneric(resolveStatusUrl(loadPath), body);
    }

    /**
     * 轮询算法任务状态，返回通用结果对象（可能是Number、JSONArray、JSONObject等）
     */
    private Object pollUntilDoneGeneric(String statusUrl, Map<String, Object> pollBody) {
        long deadline = System.currentTimeMillis() + pollMaxWaitMs;
        RestTemplate rt = buildRestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(pollBody, headers);
        while (System.currentTimeMillis() < deadline) {
            try {
                String raw = rt.postForObject(statusUrl, entity, String.class);
                if (StringUtils.isEmpty(raw)) {
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
                    Object res = o.get("result");
                    if (res == null) {
                        return null;
                    }
                    return res;
                }
                if (code == 500) {
                    throw new ServiceException("算法执行失败: " + o.getString("result"));
                }
            } catch (ServiceException ex) {
                throw ex;
            } catch (Exception ex) {
                throw new ServiceException("轮询算法任务状态异常: " + ex.getMessage());
            }
            sleepPoll();
        }
        throw new ServiceException("等待算法结果超时（" + (pollMaxWaitMs / 1000) + "s），请检查 Celery Worker 是否运行");
    }

    private String postAlgsLoad(Map<String, Object> body) {
        String url = joinUrl(baseUrl, "/algsmanagement/algs_load");
        try {
            RestTemplate rt = buildRestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
            String raw = rt.postForObject(url, entity, String.class);
            if (StringUtils.isEmpty(raw)) {
                throw new ServiceException("算法服务 algs_load 返回空响应");
            }
            JSONObject o = JSON.parseObject(raw);
            String loc = o.getString("location");
            if (StringUtils.isEmpty(loc)) {
                throw new ServiceException("算法服务未返回任务 location: " + raw);
            }
            return loc;
        } catch (RestClientException ex) {
            throw new ServiceException("无法连接算法服务（请确认 zgpg_algs 已启动且地址正确 zhpg.algs.base-url）: " + ex.getMessage());
        }
    }

    private JSONArray pollUntilDone(String statusUrl, Map<String, Object> pollBody) {
        long deadline = System.currentTimeMillis() + pollMaxWaitMs;
        RestTemplate rt = buildRestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(pollBody, headers);
        while (System.currentTimeMillis() < deadline) {
            try {
                String raw = rt.postForObject(statusUrl, entity, String.class);
                if (StringUtils.isEmpty(raw)) {
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
                    Object res = o.get("result");
                    if (res instanceof JSONArray) {
                        return (JSONArray) res;
                    }
                    if (res instanceof String) {
                        return JSON.parseArray((String) res);
                    }
                    return JSON.parseArray(JSON.toJSONString(res));
                }
                if (code == 500) {
                    throw new ServiceException("算法执行失败: " + o.getString("result"));
                }
            } catch (ServiceException ex) {
                throw ex;
            } catch (Exception ex) {
                throw new ServiceException("轮询算法任务状态异常: " + ex.getMessage());
            }
            sleepPoll();
        }
        throw new ServiceException("等待算法结果超时（" + (pollMaxWaitMs / 1000) + "s），请检查 Celery Worker 是否运行");
    }

    private void sleepPoll() {
        try {
            Thread.sleep(pollIntervalMs);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new ServiceException("等待算法结果被中断");
        }
    }

    private String resolveStatusUrl(String location) {
        if (StringUtils.isEmpty(location)) {
            throw new ServiceException("任务 location 为空");
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
        String b = StringUtils.removeEnd(base == null ? "" : base.trim(), "/");
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
