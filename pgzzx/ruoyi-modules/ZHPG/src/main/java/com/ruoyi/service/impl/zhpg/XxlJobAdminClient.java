package com.ruoyi.service.impl.zhpg;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.ruoyi.common.core.exception.ServiceException;
import com.ruoyi.domain.zhpg.dto.XxlJobInfoRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

/**
 * 封装对 xxl-job-admin HTTP 接口的调用：add / update / remove / stop / start / trigger
 */
@Slf4j
@Component
public class XxlJobAdminClient {

    @Value("${zhpg.calc.xxl-job.admin-base-url:}")
    private String adminBaseUrl;

    public boolean isAvailable() {
        return StringUtils.isNotBlank(StringUtils.trimToNull(adminBaseUrl));
    }

    /**
     * 注册任务，返回新建的 jobId
     */
    public int addJob(XxlJobInfoRequest request) {
        String url = buildUrl("/jobinfo/add");
        String body = JSON.toJSONString(request);
        log.info("xxl-job-admin addJob: {}", body);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<>(body, headers);

        ResponseEntity<String> resp = restTemplate().postForEntity(url, entity, String.class);
        JSONObject ret = JSON.parseObject(resp.getBody());
        if (ret == null || ret.getIntValue("code") != 200) {
            String msg = ret == null ? "empty response" : ret.getString("msg");
            throw new ServiceException("xxl-job addJob failed: " + msg);
        }
        return Integer.parseInt(ret.getString("content"));
    }

    /**
     * 更新已有任务
     */
    public void updateJob(XxlJobInfoRequest request) {
        String url = buildUrl("/jobinfo/update");
        String body = JSON.toJSONString(request);
        log.info("xxl-job-admin updateJob: {}", body);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<>(body, headers);

        ResponseEntity<String> resp = restTemplate().postForEntity(url, entity, String.class);
        JSONObject ret = JSON.parseObject(resp.getBody());
        if (ret == null || ret.getIntValue("code") != 200) {
            String msg = ret == null ? "empty response" : ret.getString("msg");
            log.warn("xxl-job updateJob failed: {}", msg);
        }
    }

    /**
     * 删除任务
     */
    public void removeJob(int jobId) {
        postForm("/jobinfo/remove", jobId);
    }

    /**
     * 停止任务调度
     */
    public void stopJob(int jobId) {
        postForm("/jobinfo/stop", jobId);
    }

    /**
     * 启动任务调度
     */
    public void startJob(int jobId) {
        postForm("/jobinfo/start", jobId);
    }

    /**
     * 手动触发一次执行
     */
    public void triggerJob(int jobId, String executorParam, String addressList) {
        String url = buildUrl("/jobinfo/trigger");
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("id", String.valueOf(jobId));
        form.add("executorParam", executorParam == null ? "" : executorParam);
        form.add("addressList", addressList == null ? "" : addressList);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(form, headers);

        ResponseEntity<String> resp = restTemplate().postForEntity(url, entity, String.class);
        JSONObject ret = JSON.parseObject(resp.getBody());
        if (ret == null || ret.getIntValue("code") != 200) {
            String msg = ret == null ? "empty response" : ret.getString("msg");
            throw new ServiceException("xxl-job triggerJob failed: " + msg);
        }
        log.info("xxl-job triggerJob success, jobId={}", jobId);
    }

    private void postForm(String path, int jobId) {
        String url = buildUrl(path);
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("id", String.valueOf(jobId));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(form, headers);

        ResponseEntity<String> resp = restTemplate().postForEntity(url, entity, String.class);
        JSONObject ret = JSON.parseObject(resp.getBody());
        if (ret == null || ret.getIntValue("code") != 200) {
            String msg = ret == null ? "empty response" : ret.getString("msg");
            log.warn("xxl-job {} failed for jobId={}: {}", path, jobId, msg);
        }
    }

    private String buildUrl(String path) {
        if (StringUtils.isBlank(adminBaseUrl)) {
            throw new ServiceException("XXL-JOB Admin URL not configured (zhpg.calc.xxl-job.admin-base-url)");
        }
        return StringUtils.removeEnd(adminBaseUrl.trim(), "/") + path;
    }

    private RestTemplate restTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(10000);
        factory.setReadTimeout(15000);
        return new RestTemplate(factory);
    }
}
