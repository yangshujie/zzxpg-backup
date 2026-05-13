package com.ruoyi.service.impl.zhpg;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class StandardReportTemplateSqlTest {

    @Test
    public void standardTemplateMigrationSeedsBusinessReportPlaceholdersWithoutJsonSnapshot() throws Exception {
        Path path = findMigrationPath();

        assertTrue("standard report template migration should exist", Files.exists(path));

        String sql = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
        assertTrue(sql.contains("通信对抗试验评估报告（标准版）"));
        assertTrue(sql.contains("template_name"));
        assertTrue(sql.contains("template_description"));
        assertTrue(sql.contains("evaluation_type"));
        assertTrue(sql.contains("html_content"));
        assertTrue(sql.contains("{{ExperimentOverview}}"));
        assertTrue(sql.contains("data-component-type=\"experimentOverview\""));
        assertTrue(sql.contains("data-component-type=\"comprehensiveEval\""));
        assertTrue(sql.contains("data-component-type=\"indicatorSections\""));
        assertTrue(sql.contains("data-component-type=\"conclusionAdvice\""));
        assertTrue(sql.contains("{{CapabilityRadarChart}}"));
        assertTrue(sql.contains("{{#each IndicatorSections}}"));
        assertTrue(sql.contains("{{=item.chartImg}}"));
        assertTrue(sql.contains("{{KeyFindings}}"));
        assertTrue(sql.contains("{{ImprovementSuggestions}}"));
        assertTrue(sql.contains("{{FinalConclusion}}"));
        assertFalse("report templates must not expose raw JSON snapshots", sql.contains("eval_result_snapshot"));
    }

    private Path findMigrationPath() {
        Path cwd = Paths.get("").toAbsolutePath();
        for (Path cursor = cwd; cursor != null; cursor = cursor.getParent()) {
            Path candidate = cursor.resolve(Paths.get("sql", "zhpg_standard_report_template_kingbase.sql"));
            if (Files.exists(candidate)) {
                return candidate;
            }
        }
        return cwd.resolve(Paths.get("sql", "zhpg_standard_report_template_kingbase.sql"));
    }
}
