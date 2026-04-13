package com.ruoyi.zhpgcalc.service;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Slf4j
@Component
public class ExternalEvaluationDataClient {

    @Value("${zhpg.calc.external-data.url:http://127.0.0.1:9501/evaluationData/list}")
    private String url;

    @Value("${zhpg.calc.external-data.connect-timeout-ms:10000}")
    private int connectTimeoutMs;

    @Value("${zhpg.calc.external-data.read-timeout-ms:20000}")
    private int readTimeoutMs;

    public List<ExternalDataItem> fetchEvaluationData(Long evaluationTaskId, List<String> indicatorCodes) {
        if (evaluationTaskId == null || evaluationTaskId <= 0 || CollectionUtils.isEmpty(indicatorCodes)) {
            return Collections.emptyList();
        }
        String requestUrl = buildUrl(evaluationTaskId, indicatorCodes);
        log.info("[外部接口] 请求URL: {}", requestUrl);
        ResponseEntity<String> response = buildRestTemplate().getForEntity(requestUrl, String.class);
        log.info("[外部接口] 响应状态: {}, 响应体: {}", response.getStatusCode(), response.getBody());
        if (!response.getStatusCode().is2xxSuccessful() || !StringUtils.hasText(response.getBody())) {
            throw new IllegalStateException("external data api response is empty");
        }
        JSONObject root = JSON.parseObject(response.getBody());
        Integer code = root.getInteger("code");
        if (code == null || code != 200) {
            throw new IllegalStateException("external data api returned non-200 business code: " + code);
        }
        JSONArray data = root.getJSONArray("data");
        if (data == null || data.isEmpty()) {
            return Collections.emptyList();
        }
        List<ExternalDataItem> items = new ArrayList<>();
        for (int i = 0; i < data.size(); i++) {
            JSONObject row = data.getJSONObject(i);
            if (row == null) {
                continue;
            }
            ExternalDataItem item = new ExternalDataItem();
            item.setIndicatorCode(row.getString("indicatorCode"));
            item.setTableCode(row.getString("tableCode"));
            item.setDataItemCode(row.getString("dataItemCode"));
            item.setDataJson(row.getString("dataJson"));
            items.add(item);
        }
        return items;
    }

    private String buildUrl(Long evaluationTaskId, List<String> indicatorCodes) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(url)
                .queryParam("evaluationTaskId", evaluationTaskId);
        for (String indicatorCode : indicatorCodes) {
            if (StringUtils.hasText(indicatorCode)) {
                builder.queryParam("indicatorCodes", indicatorCode.trim());
            }
        }
        return builder.build(false).toUriString();
    }

    private RestTemplate buildRestTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Math.max(connectTimeoutMs, 1000));
        factory.setReadTimeout(Math.max(readTimeoutMs, 5000));
        return new RestTemplate(factory);
    }

    @Data
    public static class ExternalDataItem {
        private String indicatorCode;
        private String tableCode;
        private String dataItemCode;
        private String dataJson;
    }
}
