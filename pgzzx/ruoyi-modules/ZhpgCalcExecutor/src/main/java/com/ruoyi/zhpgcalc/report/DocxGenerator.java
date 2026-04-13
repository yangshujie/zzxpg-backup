package com.ruoyi.zhpgcalc.report;

import lombok.extern.slf4j.Slf4j;
import org.docx4j.convert.in.xhtml.XHTMLImporterImpl;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.util.CollectionUtils;

import java.io.ByteArrayOutputStream;
import java.util.List;

/**
 * DOCX 生成服务
 * 使用docx4j将XHTML转换为Word文档
 */
@Slf4j
public class DocxGenerator {

    /**
     * 将XHTML转换为DOCX
     *
     * @param xhtml XHTML内容
     * @return DOCX字节数组
     */
    public byte[] htmlToDocx(String xhtml) {
        if (xhtml == null || xhtml.trim().isEmpty()) {
            throw new IllegalArgumentException("xhtml 不能为空");
        }

        try {
            WordprocessingMLPackage wordPackage = WordprocessingMLPackage.createPackage();
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
                throw new IllegalArgumentException("HTML内容无法转换为DOCX正文");
            }
            wordPackage.getMainDocumentPart().getContent().addAll(content);

            try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
                wordPackage.save(outputStream);
                return outputStream.toByteArray();
            }
        } catch (Exception e) {
            String detail = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
            throw new RuntimeException("HTML 转 DOCX 失败: " + detail, e);
        }
    }

    /**
     * 移除所有图片标签
     */
    private String stripAllImages(String xhtml) {
        Document doc = Jsoup.parse(xhtml);
        doc.select("img").remove();
        doc.outputSettings().syntax(Document.OutputSettings.Syntax.xml).prettyPrint(false);
        return doc.outerHtml();
    }
}
