package com.ruoyi.service.impl.zhpg;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.ruoyi.domain.zhpg.EvalResult;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;

public class EvaluationResultLineageClientTest {

    @Test
    public void buildLineageRequestsUsesRequirementIdForRootAndIndicatorIdForChildren() {
        EvalResult evalResult = new EvalResult();
        evalResult.setId(88L);

        JSONObject root = new JSONObject();
        root.put("id", "root_indicator_id");
        root.put("label", "Root Index");
        JSONArray children = new JSONArray();
        JSONObject child = new JSONObject();
        child.put("id", "1_1775118978642_z2e0vh");
        child.put("label", "Child Index");
        children.add(child);
        root.put("children", children);

        EvaluationResultLineageClient client = new EvaluationResultLineageClient();

        List<JSONObject> requests = client.buildLineageRequests(evalResult, root, 40L);

        assertEquals(2, requests.size());
        assertEquals("INDEX_40", requests.get(0).getJSONObject("edge").getString("sourceNode"));
        assertEquals("Root Index", requests.get(0).getJSONObject("node").getString("nodeName"));
        assertEquals("INDEX_1_1775118978642_z2e0vh", requests.get(1).getJSONObject("edge").getString("sourceNode"));
        assertEquals("Child Index", requests.get(1).getJSONObject("node").getString("nodeName"));
        assertEquals("INDEX_TO_RESULT", requests.get(1).getJSONObject("edge").getString("edgeType"));
    }

    @Test
    public void buildIndicatorSystemLineageRequestsLinksRequirementParentsLeafAlgorithmsAndDataSources() {
        JSONObject root = new JSONObject();
        root.put("id", "root_id");
        root.put("label", "设备健康总分");

        JSONObject leaf = new JSONObject();
        leaf.put("id", "leaf_id");
        leaf.put("label", "温升风险分");
        leaf.put("children", new JSONArray());

        JSONObject start = new JSONObject();
        start.put("type", "start");
        start.put("id", "source_node_id");
        start.put("value", "temperature_table");
        start.put("name", "温度数据");

        JSONObject algo = new JSONObject();
        algo.put("type", "algo");
        algo.put("id", "algo_node_id");
        algo.put("value", 306);
        algo.put("url", "algs/character/calMean.zip");

        JSONArray methodNodes = new JSONArray();
        methodNodes.add(start);
        methodNodes.add(algo);

        JSONObject line = new JSONObject();
        line.put("sourceId", "source_node_id");
        line.put("targetId", "algo_node_id");
        JSONArray lines = new JSONArray();
        lines.add(line);

        JSONObject method = new JSONObject();
        method.put("node", methodNodes);
        method.put("lineList", lines);
        JSONObject computeRule = new JSONObject();
        computeRule.put("method", method);
        leaf.put("computeRule", computeRule);

        JSONArray children = new JSONArray();
        children.add(leaf);
        root.put("children", children);

        EvaluationResultLineageClient client = new EvaluationResultLineageClient();

        List<JSONObject> requests = client.buildIndicatorSystemLineageRequests(root, 43L);

        assertEquals(4, requests.size());
        assertEquals("REQUIREMENT_43", requests.get(0).getJSONObject("edge").getString("sourceNode"));
        assertEquals("INDEX_root_id", requests.get(0).getJSONObject("edge").getString("targetNode"));
        assertEquals("REQUIREMENT_TO_INDEX", requests.get(0).getJSONObject("edge").getString("edgeType"));
        assertEquals("INDEX_root_id", requests.get(1).getJSONObject("edge").getString("sourceNode"));
        assertEquals("INDEX_leaf_id", requests.get(1).getJSONObject("edge").getString("targetNode"));
        assertEquals("INDEX_TO_INDEX", requests.get(1).getJSONObject("edge").getString("edgeType"));
        assertEquals("INDEX_leaf_id", requests.get(2).getJSONObject("edge").getString("sourceNode"));
        assertEquals("TEMPLATE_STEP_306", requests.get(2).getJSONObject("edge").getString("targetNode"));
        assertEquals("INDEX_TO_ALGORITHM", requests.get(2).getJSONObject("edge").getString("edgeType"));
        assertEquals("SOURCE_COLUMN_temperature_table", requests.get(3).getJSONObject("node").getString("nodeId"));
        assertEquals("SOURCE_COLUMN", requests.get(3).getJSONObject("node").getString("nodeType"));
        assertEquals("温度数据", requests.get(3).getJSONObject("node").getString("nodeName"));
        assertEquals("temperature_table", requests.get(3).getJSONObject("node").getString("physicalName"));
        assertEquals("temperature_table", requests.get(3).getJSONObject("node").getString("bizPrimaryId"));
        assertEquals("TEMPLATE_STEP_306", requests.get(3).getJSONObject("edge").getString("sourceNode"));
        assertEquals("SOURCE_COLUMN_temperature_table", requests.get(3).getJSONObject("edge").getString("targetNode"));
        assertEquals("OPERATOR_TO_SOURCE", requests.get(3).getJSONObject("edge").getString("edgeType"));
    }
}
