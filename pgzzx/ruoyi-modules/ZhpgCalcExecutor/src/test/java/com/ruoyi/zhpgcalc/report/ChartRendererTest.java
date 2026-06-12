package com.ruoyi.zhpgcalc.report;

import com.ruoyi.common.report.ChartRenderer;
import com.ruoyi.common.report.ReportEngine;
import com.ruoyi.common.report.ReportEngineRequest;
import com.ruoyi.common.report.ReportEngineResult;
import org.junit.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class ChartRendererTest {

    @Test
    public void rendersRadarChartAsBase64Png() throws Exception {
        List<Map<String, Object>> capabilities = Arrays.asList(
                capability("探测识别", "0.25", "88.5"),
                capability("干扰压制", "0.35", "91.0"),
                capability("抗毁生存", "0.40", "76.25")
        );

        String image = new ChartRenderer().renderRadar(capabilities);

        BufferedImage decoded = decodePng(image);
        assertEquals(800, decoded.getWidth());
        assertEquals(520, decoded.getHeight());
    }

    @Test
    public void skipsRadarChartWhenCapabilityCountIsTooSmall() {
        List<Map<String, Object>> capabilities = Arrays.asList(
                capability("探测识别", "0.25", "88.5"),
                capability("干扰压制", "0.35", "91.0")
        );

        String image = new ChartRenderer().renderRadar(capabilities);

        assertEquals("", image);
    }

    @Test
    public void rendersIndicatorBarAsBase64Png() throws Exception {
        String image = new ChartRenderer().renderIndicatorBar("时间占空比", new BigDecimal("92"), new BigDecimal("80"));

        BufferedImage decoded = decodePng(image);
        assertEquals(760, decoded.getWidth());
        assertEquals(260, decoded.getHeight());
    }

    @Test
    public void embedsRenderedChartIntoDocx() throws Exception {
        Map<String, Object> data = new HashMap<>();
        data.put("CapabilityRadarChart", new ChartRenderer().renderRadar(Arrays.asList(
                capability("探测识别", "0.25", "88.5"),
                capability("干扰压制", "0.35", "91.0"),
                capability("抗毁生存", "0.40", "76.25")
        )));

        ReportEngineRequest chartReq = ReportEngineRequest.builder()
                .templateName("chart-report")
                .htmlContent("<h1>图表报告</h1><p><img src=\"{{CapabilityRadarChart}}\" alt=\"能力雷达图\" /></p>")
                .dataModel(data)
                .build();
        byte[] docx = new ReportEngine().generate(chartReq).getDocxBytes();

        assertTrue(containsWordMedia(docx));
    }

    @Test
    public void generatedDocxUsesWordHeadingStyles() throws Exception {
        ReportEngineRequest headingReq = ReportEngineRequest.builder()
                .templateName("heading-report")
                .htmlContent("<h1>试验评估报告</h1><h2>概况</h2><p>正文</p>")
                .dataModel(new HashMap<>())
                .build();
        byte[] docx = new ReportEngine().generate(headingReq).getDocxBytes();

        String documentXml = readZipEntry(docx, "word/document.xml");
        String stylesXml = readZipEntry(docx, "word/styles.xml");

        assertTrue(documentXml.contains("w:val=\"Heading1\""));
        assertTrue(documentXml.contains("w:val=\"Heading2\""));
        assertTrue(stylesXml.contains("w:styleId=\"Heading1\""));
        assertTrue(stylesXml.contains("w:styleId=\"Heading2\""));
    }

    private Map<String, Object> capability(String label, String weight, String score) {
        Map<String, Object> capability = new LinkedHashMap<>();
        capability.put("label", label);
        capability.put("weight", new BigDecimal(weight));
        capability.put("score", new BigDecimal(score));
        return capability;
    }

    private BufferedImage decodePng(String image) throws Exception {
        assertNotNull(image);
        assertTrue(image.startsWith("data:image/png;base64,"));
        String base64 = image.substring("data:image/png;base64,".length());
        BufferedImage decoded = ImageIO.read(new ByteArrayInputStream(Base64.getDecoder().decode(base64)));
        assertNotNull(decoded);
        return decoded;
    }

    private boolean containsWordMedia(byte[] docx) throws Exception {
        try (ZipInputStream input = new ZipInputStream(new ByteArrayInputStream(docx))) {
            ZipEntry entry;
            while ((entry = input.getNextEntry()) != null) {
                if (entry.getName().startsWith("word/media/")) {
                    return true;
                }
            }
        }
        return false;
    }

    private String readZipEntry(byte[] docx, String entryName) throws Exception {
        try (ZipInputStream input = new ZipInputStream(new ByteArrayInputStream(docx))) {
            ZipEntry entry;
            while ((entry = input.getNextEntry()) != null) {
                if (entryName.equals(entry.getName())) {
                    ByteArrayOutputStream output = new ByteArrayOutputStream();
                    byte[] buffer = new byte[4096];
                    int len;
                    while ((len = input.read(buffer)) != -1) {
                        output.write(buffer, 0, len);
                    }
                    return output.toString("UTF-8");
                }
            }
        }
        return "";
    }
}
