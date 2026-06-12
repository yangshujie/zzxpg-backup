package com.ruoyi.common.report;

import com.aspose.words.Document;
import com.aspose.words.License;
import com.aspose.words.SaveFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;

/**
 * Word(DOCX) → PDF 转换器
 * <p>
 * 基于 Aspose.Words，封装许可证加载和转换逻辑。
 * 原先在 {@code EvalReportInstanceServiceImpl} 和 {@code ReportGenerationService} 中
 * 各有一份完全重复的实现（含许可证 XML 硬编码），统一为此类。
 *
 * <h3>使用方式</h3>
 * <pre>{@code
 *   // 方式1: 使用默认许可证（兼容现有硬编码）
 *   byte[] pdf = PdfConverter.getInstance().wordToPdf(docxBytes);
 *
 *   // 方式2: 使用自定义许可证（未来改为配置加载）
 *   PdfConverter converter = new PdfConverter(licenseXmlString);
 *   byte[] pdf = converter.wordToPdf(docxBytes);
 * }</pre>
 */
public class PdfConverter {

    private static final Logger log = LoggerFactory.getLogger(PdfConverter.class);

    /**
     * 当前硬编码的 Aspose 许可证 XML。
     * 后续改进方向：改为从配置文件/环境变量/数据库加载，不再硬编码在 Java 源码中。
     */
    static final String BUILT_IN_LICENSE_XML = "<License>\n"
            + "    <Data>\n"
            + "        <Products>\n"
            + "            <Product>Aspose.Total for Java</Product>\n"
            + "            <Product>Aspose.Words for Java</Product>\n"
            + "        </Products>\n"
            + "        <EditionType>Enterprise</EditionType>\n"
            + "        <SubscriptionExpiry>20991231</SubscriptionExpiry>\n"
            + "        <LicenseExpiry>20991231</LicenseExpiry>\n"
            + "        <SerialNumber>8bfe198c-7f0c-4ef8-8ff0-acc3237bf0d7</SerialNumber>\n"
            + "    </Data>\n"
            + "    <Signature>\n"
            + "        sNLLKGMUdF0r8O1kKilWAGdgfs2BvJb/2Xp8p5iuDVfZXmhppo+d0Ran1P9TKdjV4ABwAgKXxJ3jcQTqE/2IRfqwnPf8itN8aFZlV3TJPYeD3yWE7IT55Gz6EijUpC7aKeoohTb4w2fpox58wWoF3SNp6sK6jDfiAUGEHYJ9pjU=\n"
            + "    </Signature>\n"
            + "</License>";

    /** 单例实例的许可证 XML */
    private final String licenseXml;
    /** 许可证是否已加载 */
    private volatile boolean licenseLoaded;

    /** 单例（懒加载 + DCL），使用内置硬编码许可证 */
    private static volatile PdfConverter INSTANCE;

    /**
     * 获取使用内置许可证的单例实例。
     */
    public static PdfConverter getInstance() {
        if (INSTANCE == null) {
            synchronized (PdfConverter.class) {
                if (INSTANCE == null) {
                    INSTANCE = new PdfConverter(BUILT_IN_LICENSE_XML);
                }
            }
        }
        return INSTANCE;
    }

    /**
     * @param licenseXml Aspose 许可证 XML 字符串（非 null）
     */
    public PdfConverter(String licenseXml) {
        if (licenseXml == null || licenseXml.trim().isEmpty()) {
            throw new IllegalArgumentException("licenseXml must not be null or empty");
        }
        this.licenseXml = licenseXml;
    }

    /**
     * 将 Word (DOCX) 字节数组转换为 PDF 字节数组。
     *
     * @param wordBytes DOCX 文件字节
     * @return PDF 文件字节
     * @throws RuntimeException 转换失败时抛出
     */
    public byte[] wordToPdf(byte[] wordBytes) {
        ensureLicenseLoaded();
        try (ByteArrayInputStream input = new ByteArrayInputStream(wordBytes);
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Document document = new Document(input);
            document.save(output, SaveFormat.PDF);
            return output.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Word转PDF失败: " + e.getMessage(), e);
        }
    }

    private void ensureLicenseLoaded() {
        if (licenseLoaded) {
            return;
        }
        synchronized (this) {
            if (licenseLoaded) {
                return;
            }
            try (InputStream inputStream = new ByteArrayInputStream(licenseXml.getBytes())) {
                License license = new License();
                license.setLicense(inputStream);
                licenseLoaded = true;
            } catch (Exception e) {
                log.warn("Aspose 许可加载失败，继续尝试转换: {}", e.getMessage());
                // 仍然标记为已加载，避免每次转换都重试加载
                licenseLoaded = true;
            }
        }
    }
}
