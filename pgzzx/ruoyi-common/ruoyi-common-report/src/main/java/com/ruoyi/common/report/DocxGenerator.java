package com.ruoyi.common.report;

import org.docx4j.convert.in.xhtml.XHTMLImporterImpl;
import org.docx4j.jaxb.Context;
import org.docx4j.openpackaging.parts.WordprocessingML.StyleDefinitionsPart;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.docx4j.wml.BooleanDefaultTrue;
import org.docx4j.wml.Color;
import org.docx4j.wml.HpsMeasure;
import org.docx4j.wml.ObjectFactory;
import org.docx4j.wml.PPr;
import org.docx4j.wml.PPrBase;
import org.docx4j.wml.P;
import org.docx4j.wml.RFonts;
import org.docx4j.wml.RPr;
import org.docx4j.wml.Style;
import org.docx4j.wml.Styles;
import org.docx4j.wml.Text;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;

import javax.xml.bind.JAXBElement;
import java.io.ByteArrayOutputStream;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

public class DocxGenerator {

    private static final Logger log = LoggerFactory.getLogger(DocxGenerator.class);

    public byte[] htmlToDocx(String xhtml) {
        if (xhtml == null || xhtml.trim().isEmpty()) {
            throw new IllegalArgumentException("xhtml cannot be empty");
        }

        try {
            WordprocessingMLPackage wordPackage = WordprocessingMLPackage.createPackage();
            ensureHeadingStyles(wordPackage);
            XHTMLImporterImpl importer = new XHTMLImporterImpl(wordPackage);
            List<?> content;
            try {
                content = importer.convert(xhtml, "");
            } catch (NullPointerException npe) {
                String xhtmlWithoutImages = stripAllImages(xhtml);
                log.warn("XHTML to DOCX failed with image-related NPE, retrying without images", npe);
                content = importer.convert(xhtmlWithoutImages, "");
            }
            if (CollectionUtils.isEmpty(content)) {
                throw new IllegalArgumentException("HTML content cannot be converted to DOCX body");
            }
            applyHeadingStyles(content, xhtml);
            wordPackage.getMainDocumentPart().getContent().addAll(content);

            try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
                wordPackage.save(outputStream);
                return outputStream.toByteArray();
            }
        } catch (Exception e) {
            String detail = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
            throw new RuntimeException("HTML to DOCX failed: " + detail, e);
        }
    }

    private String stripAllImages(String xhtml) {
        Document doc = Jsoup.parse(xhtml);
        doc.select("img").remove();
        doc.outputSettings().syntax(Document.OutputSettings.Syntax.xml).prettyPrint(false);
        return doc.outerHtml();
    }

    private void applyHeadingStyles(List<?> content, String xhtml) {
        List<HeadingMarker> headings = extractHeadingMarkers(xhtml);
        if (headings.isEmpty()) {
            return;
        }
        int cursor = 0;
        for (Object item : content) {
            if (cursor >= headings.size()) {
                return;
            }
            Object value = unwrap(item);
            if (!(value instanceof P)) {
                continue;
            }
            P paragraph = (P) value;
            String text = extractText(paragraph).trim();
            HeadingMarker marker = headings.get(cursor);
            if (marker.text.equals(text)) {
                setParagraphStyle(paragraph, "Heading" + marker.level);
                cursor++;
            }
        }
    }

    private List<HeadingMarker> extractHeadingMarkers(String xhtml) {
        Document doc = Jsoup.parse(xhtml == null ? "" : xhtml);
        List<HeadingMarker> markers = new ArrayList<>();
        for (Element heading : doc.select("h1,h2,h3,h4,h5")) {
            String text = heading.text().trim();
            if (!text.isEmpty()) {
                markers.add(new HeadingMarker(Integer.parseInt(heading.tagName().substring(1)), text));
            }
        }
        return markers;
    }

    private void setParagraphStyle(P paragraph, String styleId) {
        ObjectFactory factory = Context.getWmlObjectFactory();
        PPr pPr = paragraph.getPPr();
        if (pPr == null) {
            pPr = factory.createPPr();
            paragraph.setPPr(pPr);
        }
        PPrBase.PStyle pStyle = factory.createPPrBasePStyle();
        pStyle.setVal(styleId);
        pPr.setPStyle(pStyle);
    }

    private String extractText(Object value) {
        Object unwrapped = unwrap(value);
        StringBuilder text = new StringBuilder();
        if (unwrapped instanceof Text) {
            text.append(((Text) unwrapped).getValue());
        } else if (unwrapped instanceof org.docx4j.wml.ContentAccessor) {
            for (Object child : ((org.docx4j.wml.ContentAccessor) unwrapped).getContent()) {
                text.append(extractText(child));
            }
        }
        return text.toString();
    }

    private Object unwrap(Object value) {
        return value instanceof JAXBElement ? ((JAXBElement<?>) value).getValue() : value;
    }

    private void ensureHeadingStyles(WordprocessingMLPackage wordPackage) throws Exception {
        StyleDefinitionsPart stylesPart = wordPackage.getMainDocumentPart().getStyleDefinitionsPart();
        if (stylesPart == null) {
            stylesPart = new StyleDefinitionsPart();
            wordPackage.getMainDocumentPart().addTargetPart(stylesPart);
        }
        Styles styles = stylesPart.getJaxbElement();
        if (styles == null) {
            styles = Context.getWmlObjectFactory().createStyles();
            stylesPart.setJaxbElement(styles);
        }
        ensureNormalStyle(styles);
        for (int i = 1; i <= 5; i++) {
            String styleId = "Heading" + i;
            if (findStyle(styles, styleId) == null) {
                styles.getStyle().add(createHeadingStyle(styleId, "heading " + i, i - 1, 34 - (i * 2)));
            }
        }
    }

    private void ensureNormalStyle(Styles styles) {
        if (findStyle(styles, "Normal") != null) {
            return;
        }
        ObjectFactory factory = Context.getWmlObjectFactory();
        Style style = factory.createStyle();
        style.setType("paragraph");
        style.setStyleId("Normal");
        Style.Name name = factory.createStyleName();
        name.setVal("Normal");
        style.setName(name);
        RPr rPr = factory.createRPr();
        rPr.setRFonts(reportFonts(factory));
        HpsMeasure size = factory.createHpsMeasure();
        size.setVal(BigInteger.valueOf(21));
        rPr.setSz(size);
        style.setRPr(rPr);
        styles.getStyle().add(style);
    }

    private Style createHeadingStyle(String styleId, String styleName, int outlineLevel, int halfPoints) {
        ObjectFactory factory = Context.getWmlObjectFactory();
        Style style = factory.createStyle();
        style.setType("paragraph");
        style.setStyleId(styleId);

        Style.Name name = factory.createStyleName();
        name.setVal(styleName);
        style.setName(name);

        Style.BasedOn basedOn = factory.createStyleBasedOn();
        basedOn.setVal("Normal");
        style.setBasedOn(basedOn);

        Style.Next next = factory.createStyleNext();
        next.setVal("Normal");
        style.setNext(next);

        BooleanDefaultTrue quickFormat = new BooleanDefaultTrue();
        quickFormat.setVal(Boolean.TRUE);
        style.setQFormat(quickFormat);

        PPr pPr = factory.createPPr();
        PPrBase.OutlineLvl outline = factory.createPPrBaseOutlineLvl();
        outline.setVal(BigInteger.valueOf(outlineLevel));
        pPr.setOutlineLvl(outline);
        style.setPPr(pPr);

        RPr rPr = factory.createRPr();
        rPr.setB(new BooleanDefaultTrue());
        rPr.setRFonts(reportFonts(factory));
        HpsMeasure size = factory.createHpsMeasure();
        size.setVal(BigInteger.valueOf(Math.max(22, halfPoints)));
        rPr.setSz(size);
        Color color = factory.createColor();
        color.setVal("1F2937");
        rPr.setColor(color);
        style.setRPr(rPr);
        return style;
    }

    private RFonts reportFonts(ObjectFactory factory) {
        RFonts fonts = factory.createRFonts();
        fonts.setAscii("Arial");
        fonts.setHAnsi("Arial");
        fonts.setEastAsia("Microsoft YaHei");
        fonts.setCs("Arial");
        return fonts;
    }

    private Style findStyle(Styles styles, String styleId) {
        if (styles == null || styles.getStyle() == null) {
            return null;
        }
        for (Style style : styles.getStyle()) {
            if (styleId.equals(style.getStyleId())) {
                return style;
            }
        }
        return null;
    }

    private static class HeadingMarker {
        private final int level;
        private final String text;

        private HeadingMarker(int level, String text) {
            this.level = level;
            this.text = text;
        }
    }
}
