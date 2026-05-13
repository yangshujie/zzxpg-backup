package com.ruoyi.common.report;

import org.junit.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class PlaceholderServiceTest {

    @Test
    public void extractPlaceholdersIncludesEachCollections() {
        PlaceholderService service = new PlaceholderService();

        List<String> placeholders = service.extractPlaceholders(
                "{{ExperimentOverview}}{{#each IndicatorSections}}<p>{{item.title}}</p>{{/each}}");

        assertTrue(placeholders.contains("ExperimentOverview"));
        assertTrue(placeholders.contains("IndicatorSections"));
    }

    @Test
    public void renderSkipsIfBlockWhenLoopItemFieldIsMissing() {
        PlaceholderService service = new PlaceholderService();
        String template = service.toFreemarkerTemplate(
                "{{#each IndicatorSections}}<section>{{#if item.summary}}<p>{{item.summary}}</p>{{/if}}<h2>{{item.title}}</h2></section>{{/each}}");

        Map<String, Object> item = new HashMap<>();
        item.put("title", "总体效能");
        Map<String, Object> data = new HashMap<>();
        data.put("IndicatorSections", Collections.singletonList(item));

        String rendered = new HtmlTemplateRenderer().render("missing-summary", template, data);

        assertEquals("<section><h2>总体效能</h2></section>", rendered);
    }
}
