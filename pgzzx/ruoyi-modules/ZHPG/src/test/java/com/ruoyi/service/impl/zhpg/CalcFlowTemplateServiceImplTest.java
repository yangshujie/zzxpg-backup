package com.ruoyi.service.impl.zhpg;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import org.junit.Test;

import java.lang.reflect.Method;

import static org.junit.Assert.assertFalse;

public class CalcFlowTemplateServiceImplTest {

    @Test
    public void normalizeConfigJsonDoesNotRestoreWeightCalcStage() throws Exception {
        CalcFlowTemplateServiceImpl service = new CalcFlowTemplateServiceImpl(null, null, null);
        Method method = CalcFlowTemplateServiceImpl.class.getDeclaredMethod("normalizeConfigJson", String.class);
        method.setAccessible(true);

        String configJson = "{"
                + "\"stages\":{"
                + "\"scheduleConfig\":{\"config\":{\"executorName\":\"zhpgCalcHandler\"}},"
                + "\"comprehensiveCalc\":{\"config\":{}},"
                + "\"reportOutput\":{\"config\":{\"outputTargets\":[\"RESULT_DB\"]}}"
                + "},"
                + "\"runtimePolicy\":{\"blockStrategy\":\"SERIAL_EXECUTION\"}"
                + "}";

        String normalized = (String) method.invoke(service, configJson);
        JSONObject stages = JSON.parseObject(normalized).getJSONObject("stages");

        assertFalse(stages.containsKey("weightCalc"));
    }

    @Test
    public void normalizeConfigJsonDropsLegacyWeightCalcStage() throws Exception {
        CalcFlowTemplateServiceImpl service = new CalcFlowTemplateServiceImpl(null, null, null);
        Method method = CalcFlowTemplateServiceImpl.class.getDeclaredMethod("normalizeConfigJson", String.class);
        method.setAccessible(true);

        String configJson = "{"
                + "\"stages\":{"
                + "\"scheduleConfig\":{\"config\":{\"executorName\":\"zhpgCalcHandler\"}},"
                + "\"weightCalc\":{\"config\":{\"weightSource\":\"TEMPLATE_OVERRIDE\"}},"
                + "\"comprehensiveCalc\":{\"config\":{}},"
                + "\"reportOutput\":{\"config\":{\"outputTargets\":[\"RESULT_DB\"]}}"
                + "},"
                + "\"runtimePolicy\":{\"blockStrategy\":\"SERIAL_EXECUTION\"}"
                + "}";

        String normalized = (String) method.invoke(service, configJson);
        JSONObject stages = JSON.parseObject(normalized).getJSONObject("stages");

        assertFalse(stages.containsKey("weightCalc"));
    }
}
