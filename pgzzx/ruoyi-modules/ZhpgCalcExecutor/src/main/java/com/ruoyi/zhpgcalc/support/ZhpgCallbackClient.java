package com.ruoyi.zhpgcalc.support;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/**
 * ZHPG 服务回调客户端
 * 执行完成后通过 HTTP 回调通知 ZHPG 任务执行结果
 */
@Slf4j
@Component
public class ZhpgCallbackClient {

    /**
     * ZHPG 服务地址，例如 http://localhost:9303 或 http://zhpg-service
     * 可通过 Nacos 配置: zhpg.callback.url
     */
    @Value("${zhpg.callback.url:http://localhost:9303}")
    private String zhpgBaseUrl;

    private final RestTemplate restTemplate;

    public ZhpgCallbackClient() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(10000);
        factory.setReadTimeout(30000);
        this.restTemplate = new RestTemplate(factory);
    }

    /**
     * 通知 ZHPG 任务执行成功（不带报告URL，兼容旧版本）
     *
     * @param taskId     任务ID
     * @param resultJson 执行结果JSON
     */
    public void notifySuccess(Long taskId, String resultJson) {
        notifySuccessWithReport(taskId, resultJson, null, null, null);
    }

    /**
     * 通知 ZHPG 任务执行成功（带报告URL）
     *
     * @param taskId     任务ID
     * @param resultJson 执行结果JSON
     * @param reportUrl  PDF报告URL
     * @param wordUrl    Word报告URL
     * @param wpsUrl     WPS报告URL
     */
    public void notifySuccessWithReport(Long taskId, String resultJson, String reportUrl, String wordUrl, String wpsUrl) {
        try {
            String url = buildUrl("/zhpg/calc/callback/success/" + taskId);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            // 构建回调数据
            JSONObject callbackData = new JSONObject();
            callbackData.put("resultJson", resultJson);
            if (reportUrl != null) {
                callbackData.put("reportUrl", reportUrl);
            }
            if (wordUrl != null) {
                callbackData.put("wordUrl", wordUrl);
            }
            if (wpsUrl != null) {
                callbackData.put("wpsUrl", wpsUrl);
            }

            HttpEntity<String> entity = new HttpEntity<>(callbackData.toJSONString(), headers);

            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);
            log.info("回调 ZHPG 成功: taskId={}, response={}", taskId, response.getStatusCode());
        } catch (Exception e) {
            log.error("回调 ZHPG 失败: taskId={}", taskId, e);
            // 不抛异常，避免影响XXL-JOB的日志记录
        }
    }

    /**
     * 通知 ZHPG 任务执行失败
     *
     * @param taskId       任务ID
     * @param errorMessage 错误信息
     */
    public void notifyFailure(Long taskId, String errorMessage) {
        try {
            String url = buildUrl("/zhpg/calc/callback/fail/" + taskId);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> entity = new HttpEntity<>(
                    StringUtils.defaultString(errorMessage, "unknown error"), headers);

            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);
            log.info("回调 ZHPG 失败通知: taskId={}, response={}", taskId, response.getStatusCode());
        } catch (Exception e) {
            log.error("回调 ZHPG 失败通知失败: taskId={}", taskId, e);
            // 不抛异常，避免影响XXL-JOB的日志记录
        }
    }

    /**
     * 检查 ZHPG 回调是否可用（配置了有效的地址）
     */
    public boolean isAvailable() {
        return StringUtils.isNotBlank(zhpgBaseUrl);
    }

    private String buildUrl(String path) {
        return StringUtils.removeEnd(zhpgBaseUrl.trim(), "/") + path;
    }
}
