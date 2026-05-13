package com.ruoyi.common.report;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.NumberTickUnit;
import org.jfree.chart.labels.StandardCategoryItemLabelGenerator;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.SpiderWebPlot;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.chart.renderer.category.CategoryItemRenderer;
import org.jfree.chart.renderer.category.LineAndShapeRenderer;
import org.jfree.data.category.DefaultCategoryDataset;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.math.BigDecimal;
import java.util.Base64;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ChartRenderer {

    private static final int RADAR_WIDTH = 800;
    private static final int RADAR_HEIGHT = 520;
    private static final int BAR_WIDTH = 760;
    private static final int BAR_HEIGHT = 260;

    private final Font titleFont;
    private final Font labelFont;

    public ChartRenderer() {
        Font baseFont = loadReportFont();
        this.titleFont = baseFont.deriveFont(Font.BOLD, 20f);
        this.labelFont = baseFont.deriveFont(Font.PLAIN, 13f);
    }

    public String renderRadar(List<Map<String, Object>> capabilities) {
        List<Map<String, Object>> drawable = new ArrayList<>();
        if (capabilities != null) {
            for (Map<String, Object> capability : capabilities) {
                String label = firstText(capability, "label", "name");
                BigDecimal score = readableScore(capability);
                if (label != null && score != null) {
                    drawable.add(capability);
                }
            }
        }
        if (drawable.size() < 3) {
            return "";
        }
        drawable.sort(Comparator.comparing((Map<String, Object> item) -> readableScore(item), Comparator.nullsLast(Comparator.naturalOrder())).reversed());
        if (drawable.size() > 8) {
            drawable = new ArrayList<>(drawable.subList(0, 8));
        }

        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        for (Map<String, Object> capability : drawable) {
            String label = firstText(capability, "label", "name");
            BigDecimal score = readableScore(capability);
            dataset.addValue(score, "能力得分", label);
        }

        SpiderWebPlot plot = new SpiderWebPlot(dataset);
        plot.setBackgroundPaint(Color.WHITE);
        plot.setOutlineVisible(false);
        plot.setLabelFont(labelFont);
        plot.setLabelPaint(new Color(51, 65, 85));
        plot.setSeriesPaint(0, new Color(37, 99, 235, 190));
        plot.setSeriesOutlinePaint(0, new Color(30, 64, 175));
        plot.setWebFilled(true);
        plot.setMaxValue(100);

        JFreeChart chart = new JFreeChart("能力雷达图", titleFont, plot, false);
        chart.setBackgroundPaint(Color.WHITE);

        return toBase64Png(chart, RADAR_WIDTH, RADAR_HEIGHT);
    }

    private BigDecimal readableScore(Map<String, Object> capability) {
        BigDecimal score = number(capability.get("score"));
        if (score == null) {
            score = number(capability.get("value"));
        }
        return score;
    }

    public String renderIndicatorBar(String name, BigDecimal evalValue, BigDecimal referenceValue) {
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        String label = name == null || name.trim().isEmpty() ? "指标" : name.trim();
        BigDecimal measured = evalValue != null ? evalValue : BigDecimal.ZERO;
        dataset.addValue(measured, "实测值", label);
        if (referenceValue != null) {
            dataset.addValue(referenceValue, "参考阈值", label);
        }

        JFreeChart chart = ChartFactory.createBarChart(
                label + " 指标对比",
                "",
                "数值",
                dataset,
                PlotOrientation.HORIZONTAL,
                true,
                false,
                false
        );
        chart.setBackgroundPaint(Color.WHITE);
        chart.getTitle().setFont(titleFont);
        if (chart.getLegend() != null) {
            chart.getLegend().setItemFont(labelFont);
        }

        CategoryPlot plot = chart.getCategoryPlot();
        plot.setBackgroundPaint(Color.WHITE);
        plot.setOutlineVisible(false);
        plot.setRangeGridlinePaint(new Color(203, 213, 225));
        plot.getDomainAxis().setTickLabelFont(labelFont);
        plot.getDomainAxis().setLabelFont(labelFont);

        NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
        BigDecimal max = measured.max(referenceValue != null ? referenceValue : BigDecimal.ZERO);
        rangeAxis.setRange(0, Math.max(100, max.doubleValue() * 1.15));
        rangeAxis.setTickUnit(new NumberTickUnit(20));
        rangeAxis.setTickLabelFont(labelFont);
        rangeAxis.setLabelFont(labelFont);

        CategoryItemRenderer renderer = plot.getRenderer();
        renderer.setSeriesPaint(0, new Color(14, 165, 233));
        renderer.setSeriesPaint(1, new Color(239, 68, 68));
        renderer.setSeriesItemLabelFont(0, labelFont);
        renderer.setSeriesItemLabelFont(1, labelFont);
        renderer.setDefaultItemLabelGenerator(new StandardCategoryItemLabelGenerator());
        renderer.setDefaultItemLabelsVisible(true);
        if (renderer instanceof BarRenderer) {
            ((BarRenderer) renderer).setBarPainter(new org.jfree.chart.renderer.category.StandardBarPainter());
            ((BarRenderer) renderer).setShadowVisible(false);
        }
        if (renderer instanceof LineAndShapeRenderer) {
            ((LineAndShapeRenderer) renderer).setSeriesStroke(1, new BasicStroke(2f));
        }

        return toBase64Png(chart, BAR_WIDTH, BAR_HEIGHT);
    }

    private String toBase64Png(JFreeChart chart, int width, int height) {
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            ChartUtils.writeChartAsPNG(output, chart, width, height);
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(output.toByteArray());
        } catch (Exception e) {
            throw new RuntimeException("chart render failed: " + e.getMessage(), e);
        }
    }

    private Font loadReportFont() {
        String fontPath = System.getProperty("zhpg.report.chart.font");
        if (fontPath != null && !fontPath.trim().isEmpty()) {
            try {
                Font font = Font.createFont(Font.TRUETYPE_FONT, new File(fontPath.trim()));
                GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(font);
                return font.deriveFont(Font.PLAIN, 13f);
            } catch (Exception ignored) {
                // Fall back to installed system fonts below.
            }
        }

        String[] candidates = {"Microsoft YaHei", "SimHei", "Noto Sans CJK SC", "Source Han Sans SC", "PingFang SC", "WenQuanYi Micro Hei", "SansSerif"};
        List<String> installedFonts = java.util.Arrays.asList(GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames(Locale.CHINA));
        for (String candidate : candidates) {
            if (installedFonts.contains(candidate)) {
                return new Font(candidate, Font.PLAIN, 13);
            }
        }
        return new Font(Font.SANS_SERIF, Font.PLAIN, 13);
    }

    private String firstText(Map<String, Object> source, String... keys) {
        if (source == null) {
            return null;
        }
        for (String key : keys) {
            Object value = source.get(key);
            if (value != null && !String.valueOf(value).trim().isEmpty()) {
                return String.valueOf(value).trim();
            }
        }
        return null;
    }

    private BigDecimal number(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof BigDecimal) {
            return (BigDecimal) value;
        }
        if (value instanceof Number) {
            return BigDecimal.valueOf(((Number) value).doubleValue());
        }
        try {
            return new BigDecimal(String.valueOf(value));
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
