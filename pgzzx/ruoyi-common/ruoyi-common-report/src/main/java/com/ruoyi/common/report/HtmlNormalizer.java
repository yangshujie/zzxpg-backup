package com.ruoyi.common.report;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.safety.Safelist;

public class HtmlNormalizer {

    public String toXhtml(String html) {
        if (html == null || html.trim().isEmpty()) {
            throw new IllegalArgumentException("html cannot be empty");
        }

        String safeHtml = Jsoup.clean(
                html,
                "",
                Safelist.relaxed()
                        .addTags("table", "thead", "tbody", "tfoot", "tr", "td", "th", "colgroup", "col")
                        .addAttributes(":all", "style", "class")
                        .addAttributes("td", "rowspan", "colspan")
                        .addAttributes("th", "rowspan", "colspan")
                        .addAttributes("table", "border", "cellspacing", "cellpadding", "width")
                        .addAttributes("img", "src", "width", "height", "alt")
                        .addProtocols("img", "src", "data", "http", "https"),
                new Document.OutputSettings().prettyPrint(false)
        );

        Document doc = Jsoup.parse(safeHtml);
        doc.outputSettings()
                .syntax(Document.OutputSettings.Syntax.xml)
                .escapeMode(org.jsoup.nodes.Entities.EscapeMode.xhtml)
                .prettyPrint(false)
                .charset("UTF-8");

        if (doc.head() != null && doc.head().children().isEmpty()) {
            doc.head().appendElement("meta").attr("http-equiv", "Content-Type")
                    .attr("content", "text/html; charset=UTF-8");
        }

        return doc.outerHtml();
    }
}
