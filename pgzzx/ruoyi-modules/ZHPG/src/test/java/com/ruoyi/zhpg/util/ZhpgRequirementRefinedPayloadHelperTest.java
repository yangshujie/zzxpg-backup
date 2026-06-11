package com.ruoyi.zhpg.util;

import com.alibaba.fastjson2.JSONObject;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ZhpgRequirementRefinedPayloadHelperTest {

    @Test
    public void resolveSystemNameReplacesPreviousAutoRequirementSuffix() {
        JSONObject payload = new JSONObject();
        payload.put("requirementId", 59L);

        String name = ZhpgRequirementRefinedPayloadHelper.resolveSystemName(
                payload,
                "通信对抗试验评估指标体系（source_filters·split_starts）（需求43）");

        assertEquals("通信对抗试验评估指标体系（source_filters·split_starts）（需求59）", name);
    }
}
