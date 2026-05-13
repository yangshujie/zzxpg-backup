package com.ruoyi.zhpgcalc.algs;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;
import java.util.Map;

public class ZgpgAlgsClientTest {

    @Test
    public void shouldBuildBodyForLegacyZgpgAlgsAndNewHubFormats() {
        ZgpgAlgsClient client = new ZgpgAlgsClient();

        Map<String, Object> legacyBody = client.buildLegacyZgpgAlgsRequestBody(
                "cal_weight",
                "[[1,2],[3,4]]",
                "{\"k\":1,\"enabled\":true}",
                "weight");

        Assert.assertEquals("[[1,2],[3,4]]", legacyBody.get("data"));
        Assert.assertEquals("0", legacyBody.get("dataType"));
        Assert.assertEquals("cal_weight", legacyBody.get("algs_name"));
        Assert.assertEquals("weight", legacyBody.get("algs_type"));
        Assert.assertTrue("legacy zgpg_algs expects config to be a JSON string", legacyBody.get("config") instanceof String);
        Assert.assertEquals("{\"k\":1,\"enabled\":true}", legacyBody.get("config"));

        Map<String, Object> hubBody = client.buildHubRequestBody(
                "cal_weight",
                "[[1,2],[3,4]]",
                "{\"k\":1,\"enabled\":true}",
                "weight");

        Assert.assertEquals("weight/cal_weight", hubBody.get("alg_name"));
        Assert.assertTrue(hubBody.get("args") instanceof java.util.List);
        Object firstArg = ((java.util.List<?>) hubBody.get("args")).get(0);
        Assert.assertTrue(firstArg instanceof JSONArray);
        Assert.assertTrue("new Hub expects structured config object", hubBody.get("config") instanceof JSONObject);
        Assert.assertEquals(1, ((JSONObject) hubBody.get("config")).getIntValue("k"));
        Assert.assertTrue(((JSONObject) hubBody.get("config")).getBooleanValue("enabled"));
    }

    @Test
    public void shouldPreferLegacyZgpgAlgsRequestBeforeHubRequest() {
        ZgpgAlgsClient client = new ZgpgAlgsClient();

        List<Map<String, Object>> bodies = client.buildCompatibleRequestBodies(
                "sigmoid",
                "[1,2,3]",
                "{\"min\":0,\"max\":50}",
                "norm");

        Assert.assertEquals("legacy zgpg_algs should be tried first to avoid config object eval failures", "sigmoid", bodies.get(0).get("algs_name"));
        Assert.assertEquals("0", bodies.get(0).get("dataType"));
        Assert.assertTrue(bodies.get(0).get("config") instanceof String);
        Assert.assertEquals("norm/sigmoid", bodies.get(1).get("alg_name"));
        Assert.assertTrue(bodies.get(1).get("config") instanceof JSONObject);
    }
}
