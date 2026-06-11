package com.ruoyi.zhpgcalc.algs;

import org.junit.Assert;
import org.junit.Test;

import java.util.List;
import java.util.Map;

public class ZgpgAlgsClientTest {

    @Test
    public void shouldBuildBodyForLegacyZgpgAlgsFormatOnly() {
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
    }

    @Test
    public void shouldBuildOnlyOneCompatibleRequestBody() {
        ZgpgAlgsClient client = new ZgpgAlgsClient();

        List<Map<String, Object>> bodies = client.buildCompatibleRequestBodies(
                "sigmoid",
                "[1,2,3]",
                "{\"min\":0,\"max\":50}",
                "norm");

        Assert.assertEquals("only legacy zgpg_algs request should be generated", 1, bodies.size());
        Assert.assertEquals("sigmoid", bodies.get(0).get("algs_name"));
        Assert.assertEquals("0", bodies.get(0).get("dataType"));
        Assert.assertTrue(bodies.get(0).get("config") instanceof String);
    }
}
