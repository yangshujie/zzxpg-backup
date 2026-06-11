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

    @Value("${zhpg.calc.external-data.url:http://127.0.0.1:9501/table-data/field-mapping-data/preprocess-level1}")
    private String url;

    @Value("${zhpg.calc.external-data.connect-timeout-ms:10000}")
    private int connectTimeoutMs;

    @Value("${zhpg.calc.external-data.read-timeout-ms:20000}")
    private int readTimeoutMs;

    /**
     * 获取外部评估数据
     *
     * @param batchId         预处理批次ID
     * @param requirementCode 需求标识（String 形式，原 assessTaskId）
     * @param indicatorCodes  指标标识列表
     */
    public List<ExternalDataItem> fetchEvaluationData(Long batchId, String requirementCode, List<String> indicatorCodes) {
        if (batchId == null || !StringUtils.hasText(requirementCode) || CollectionUtils.isEmpty(indicatorCodes)) {
            return Collections.emptyList();
        }

        List<ExternalDataItem> items = new ArrayList<>();
        RestTemplate restTemplate = buildRestTemplate();

        for (String indicatorCode : indicatorCodes) {
            if (!StringUtils.hasText(indicatorCode)) {
                continue;
            }
            try {
                String requestUrl = buildUrl(batchId, requirementCode, indicatorCode);
                log.info("[外部接口] 请求URL: {}", requestUrl);
                ResponseEntity<String> response = restTemplate.getForEntity(requestUrl, String.class);
                log.info("[外部接口] 响应状态: {}, 响应体: {}", response.getStatusCode(), response.getBody());

                if (!response.getStatusCode().is2xxSuccessful() || !StringUtils.hasText(response.getBody())) {
                    log.warn("[外部接口] 请求失败: url={}, response={}", requestUrl, response);
                    continue;
                }

                JSONObject root = JSON.parseObject(response.getBody());
                Integer code = root.getInteger("code");
                if (code == null || code != 200) {
                    log.warn("[外部接口] 业务响应码错误: code={}, body={}", code, response.getBody());
                    continue;
                }

                JSONObject data = root.getJSONObject("data");
                if (data == null || data.isEmpty()) {
                    continue;
                }

                for (String key : data.keySet()) {
                    JSONArray array = data.getJSONArray(key);
                    if (array == null) {
                        continue;
                    }
                    ExternalDataItem item = new ExternalDataItem();
                    item.setIndicatorCode(indicatorCode);
                    item.setTableCode(""); // 新接口没有 tableCode，置空
                    item.setDataItemCode(key);
                    item.setDataJson(array.toJSONString());
                    items.add(item);
                }
            } catch (Exception ex) {
                log.error("[外部接口] 请求异常: indicatorCode={}, error={}", indicatorCode, ex.getMessage(), ex);
            }
        }
        return items;
    }

    private String buildUrl(Long batchId, String requirementCode, String indicatorCode) {
        return UriComponentsBuilder.fromHttpUrl(url)
                .queryParam("batchId", batchId)
                .queryParam("indicatorCode", indicatorCode.trim())
                .queryParam("requirementCode", requirementCode.trim())
                .build(false).toUriString();
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
