package com.ruoyi.common.report;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import org.junit.Assert;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.List;

public class IndicatorReportSectionBuilderTest {

    @Test
    public void shouldFlattenScoredIndicatorTreeIntoNumberedSections() {
        JSONObject tree = JSON.parseObject("{\"label\":\"root\",\"children\":[{\"label\":\"通信干扰能力\",\"score\":80,\"tone\":\"good\",\"children\":[{\"label\":\"压制成功率\",\"score\":92,\"evalValue\":0.92,\"referenceValue\":0.8,\"unit\":\"%\",\"tone\":\"excellent\"}]}]}");

        List<IndicatorReportSectionBuilder.Section> sections = IndicatorReportSectionBuilder.buildSections(tree);

        Assert.assertEquals(2, sections.size());
        Assert.assertEquals("1", sections.get(0).getNumbering());
        Assert.assertEquals(Integer.valueOf(2), sections.get(0).getLevel());
        Assert.assertEquals("通信干扰能力", sections.get(0).getTitle());
        Assert.assertFalse(sections.get(0).isLeaf());

        IndicatorReportSectionBuilder.Section leaf = sections.get(1);
        Assert.assertEquals("1.1", leaf.getNumbering());
        Assert.assertEquals(Integer.valueOf(3), leaf.getLevel());
        Assert.assertTrue(leaf.isLeaf());
        Assert.assertEquals(new BigDecimal("92"), leaf.getScore());
        Assert.assertEquals(new BigDecimal("0.92"), leaf.getEvalValue());
        Assert.assertEquals(new BigDecimal("0.8"), leaf.getReferenceValue());
        Assert.assertEquals("%", leaf.getUnit());
        Assert.assertEquals("excellent", leaf.getTone());
        Assert.assertEquals("该指标得分 92，评定结果为 excellent。实测值 0.92%，参考阈值 0.8%。", leaf.getSummary());
        Assert.assertTrue(leaf.getEvalTable().contains("压制成功率"));
        Assert.assertTrue(leaf.getEvalTable().contains("0.92%"));
    }

    @Test
    public void shouldCleanGeneratedTaskAndIndicatorSystemNames() {
        Assert.assertEquals("联调-综合计算流程模板",
                IndicatorReportSectionBuilder.cleanTaskName("联调-综合计算流程模板-1777518872058"));
        Assert.assertEquals("通信对抗试验评估指标体系",
                IndicatorReportSectionBuilder.cleanIndicatorSystemName("通信对抗试验评估指标体系（source_filters·split_starts）（需求43）"));
    }

    @Test
    public void shouldBuildHierarchicalSummaryTableWithRowspanCells() {
        JSONObject tree = JSON.parseObject("{\"label\":\"root\",\"children\":[{\"label\":\"通信干扰能力\",\"score\":80,\"children\":[{\"label\":\"压制能力\",\"score\":90,\"children\":[{\"label\":\"压制成功率\",\"score\":92,\"evalValue\":0.92,\"referenceValue\":0.8,\"unit\":\"%\",\"tone\":\"excellent\"},{\"label\":\"压制时长\",\"score\":70,\"evalValue\":70,\"referenceValue\":60,\"unit\":\"s\",\"tone\":\"pass\"}]}]}]}");

        String html = IndicatorReportSectionBuilder.buildHierarchicalSummaryTable(JSON.parseArray(tree.getJSONArray("children").toJSONString()));

        Assert.assertTrue(html.contains("rowspan=\"2\""));
        Assert.assertTrue(html.contains("一级指标"));
        Assert.assertTrue(html.contains("二级指标"));
        Assert.assertTrue(html.contains("压制成功率"));
        Assert.assertFalse(html.contains("📁"));
        Assert.assertFalse(html.contains("L1"));
    }
}
