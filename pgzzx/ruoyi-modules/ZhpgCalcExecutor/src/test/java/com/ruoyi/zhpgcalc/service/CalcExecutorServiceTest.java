package com.ruoyi.zhpgcalc.service;

import com.alibaba.fastjson2.JSONObject;
import com.ruoyi.zhpgcalc.algs.ZgpgAlgsClient;
import com.ruoyi.zhpgcalc.dto.CalcExecuteRequest;
import com.ruoyi.zhpgcalc.dto.CalcExecuteResponse;
import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

public class CalcExecutorServiceTest {

    @Test
    public void shouldUseExternalDataWhenModeIsExternalApi() throws Exception {
        CalcExecutorService service = new CalcExecutorService();
        FakeExternalClient externalClient = new FakeExternalClient();
        ExternalEvaluationDataClient.ExternalDataItem item = new ExternalEvaluationDataClient.ExternalDataItem();
        item.setIndicatorCode("时间占空比");
        item.setDataItemCode("1_1775999000001_a1");
        item.setTableCode("jammer_actions");
        item.setDataJson("[{\"value\":10},{\"value\":30}]");
        externalClient.items = Collections.singletonList(item);

        setField(service, "zgpgAlgsClient", new FakeAlgsClient());
        setField(service, "externalEvaluationDataClient", externalClient);
        setField(service, "defaultDataInputMode", "MOCK");

        CalcExecuteRequest request = baseRequest();
        request.setAssessTaskId(39L);
        request.setComprehensiveConfig(JSONObject.parseObject("{\"dataInputMode\":\"EXTERNAL_API\"}"));

        CalcExecuteResponse response = service.execute(request);

        Assert.assertEquals(new BigDecimal("20.00"), response.getScore());
        Assert.assertTrue(externalClient.called);
        Assert.assertEquals(Long.valueOf(39L), externalClient.lastTaskId);
    }

    @Test
    public void shouldKeepMockModeByDefault() throws Exception {
        CalcExecutorService service = new CalcExecutorService();
        FakeExternalClient externalClient = new FakeExternalClient();

        setField(service, "zgpgAlgsClient", new FakeAlgsClient());
        setField(service, "externalEvaluationDataClient", externalClient);
        setField(service, "defaultDataInputMode", "MOCK");

        CalcExecuteRequest request = baseRequest();
        request.setComprehensiveConfig(new JSONObject());

        CalcExecuteResponse response = service.execute(request);

        Assert.assertNotNull(response.getScore());
        Assert.assertFalse(externalClient.called);
    }

    private CalcExecuteRequest baseRequest() {
        CalcExecuteRequest req = new CalcExecuteRequest();
        req.setTaskId(1L);
        req.setTaskName("task");
        req.setTemplateId(1L);
        req.setTemplateName("template");
        req.setIndicatorSystemId(1L);
        req.setIndicatorSystemName("system");
        req.setWeightedTreeJson("{\"treeData\":{\"label\":\"root\",\"children\":[{\"id\":\"leaf1\",\"label\":\"时间占空比\",\"weight\":1,\"children\":[],\"computeRule\":{\"method\":{\"node\":[{\"type\":\"start\",\"id\":\"1_1775999000001_a1\",\"value\":\"jammer_actions\",\"fields\":[\"value\"]},{\"type\":\"result\",\"id\":\"r1\"}],\"lineList\":[{\"sourceId\":\"1_1775999000001_a1\",\"targetId\":\"r1\"}]}}}]}}");
        req.setScheduleConfig(new JSONObject());
        req.setRuntimePolicy(new JSONObject());
        return req;
    }

    private void setField(Object target, String name, Object value) throws Exception {
        Field field = CalcExecutorService.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static class FakeExternalClient extends ExternalEvaluationDataClient {
        private boolean called;
        private Long lastTaskId;
        private List<ExternalDataItem> items = Collections.emptyList();

        @Override
        public List<ExternalDataItem> fetchEvaluationData(Long evaluationTaskId, List<String> indicatorCodes) {
            this.called = true;
            this.lastTaskId = evaluationTaskId;
            return items;
        }
    }

    private static class FakeAlgsClient extends ZgpgAlgsClient {
        @Override
        public Object runAlgorithm(String algsName, String dataLiteral, String configJson, String algsType) {
            return 0D;
        }
    }
}
