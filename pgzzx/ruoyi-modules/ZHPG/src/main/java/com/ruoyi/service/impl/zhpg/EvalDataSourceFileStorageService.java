package com.ruoyi.service.impl.zhpg;

import com.ruoyi.common.core.domain.R;
import com.ruoyi.common.core.exception.ServiceException;
import com.ruoyi.system.api.RemoteFileService;
import com.ruoyi.system.api.domain.SysFile;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * 评估数据源文件接入服务，统一走文件服务并收口路径规则。
 */
@Service
public class EvalDataSourceFileStorageService {

    private static final String STORAGE_ROOT = "zhpg/evalDataSource/";

    private static final Map<String, Set<String>> ALLOWED_EXTENSIONS;

    static {
        Map<String, Set<String>> extMap = new HashMap<>();
        extMap.put("CSV", asSet("csv"));
        extMap.put("EXCEL", asSet("xls", "xlsx"));
        extMap.put("TXT", asSet("txt"));
        extMap.put("WORD", asSet("doc", "docx"));
        extMap.put("WPS", asSet("wps", "et", "dps"));
        extMap.put("PDF", asSet("pdf"));
        extMap.put("DAT", asSet("dat"));
        extMap.put("JSON", asSet("json"));
        ALLOWED_EXTENSIONS = Collections.unmodifiableMap(extMap);
    }

    @Autowired
    private RemoteFileService remoteFileService;

    @Value("${custom-config.minio.bucketName}")
    private String bucketName;

    public String uploadDataSourceFile(String sourceType, String sourceName, MultipartFile file) throws IOException {
        String normalizedType = normalizeSourceType(sourceType);
        if (!isFileType(normalizedType)) {
            throw new ServiceException("仅文件类数据源支持上传文件");
        }
        if (file == null || file.isEmpty()) {
            throw new ServiceException("请选择需要上传的数据文件");
        }
        String extension = resolveExtension(file.getOriginalFilename());
        validateExtension(normalizedType, extension);

        String dirPath = buildStorageDir(normalizedType);
        String baseName = sanitizeBaseName(sourceName);
        String targetName = baseName + "_" + System.currentTimeMillis() + "." + extension;
        MultipartFile toUpload = new RenamedMultipartFile(file, targetName);

        R<SysFile> result = remoteFileService.upload(bucketName, dirPath, toUpload);
        if (result == null || !Boolean.TRUE.equals(result.isSuccess()) || result.getData() == null) {
            throw new ServiceException(result != null ? result.getMsg() : "文件服务无响应");
        }
        String filePath = normalizeManagedFilePath(result.getData().getUrl());
        validateManagedFilePath(normalizedType, filePath);
        return filePath;
    }

    public void validateManagedFilePath(String sourceType, String filePath) {
        String normalizedType = normalizeSourceType(sourceType);
        String normalizedPath = normalizeManagedFilePath(filePath);
        if (StringUtils.isBlank(normalizedPath)) {
            throw new ServiceException("文件类数据源必须上传文件并生成文件服务路径");
        }
        if (!isFileType(normalizedType)) {
            throw new ServiceException("当前数据源类型不支持文件路径配置");
        }
        if (normalizedPath.contains("..")) {
            throw new ServiceException("文件服务路径不合法，不允许包含上级目录跳转");
        }
        String requiredPrefix = STORAGE_ROOT + normalizedType.toLowerCase(Locale.ROOT) + "/";
        if (!normalizedPath.startsWith(requiredPrefix)) {
            throw new ServiceException("文件服务路径必须位于受控目录: " + requiredPrefix);
        }
        validateExtension(normalizedType, resolveExtension(normalizedPath));
    }

    public String normalizeManagedFilePath(String filePath) {
        if (StringUtils.isBlank(filePath)) {
            return null;
        }
        String normalized = filePath.trim().replace("\\", "/");
        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        return normalized;
    }

    public boolean isFileType(String sourceType) {
        return ALLOWED_EXTENSIONS.containsKey(normalizeSourceType(sourceType));
    }

    private String buildStorageDir(String sourceType) {
        return STORAGE_ROOT
                + sourceType.toLowerCase(Locale.ROOT)
                + "/"
                + new SimpleDateFormat("yyyyMM").format(new Date())
                + "/";
    }

    private void validateExtension(String sourceType, String extension) {
        if (StringUtils.isBlank(extension)) {
            throw new ServiceException("无法识别上传文件后缀，请检查文件名");
        }
        Set<String> allowed = ALLOWED_EXTENSIONS.get(normalizeSourceType(sourceType));
        if (allowed == null || !allowed.contains(extension)) {
            throw new ServiceException("当前数据源类型仅支持以下文件格式: " + String.join("/", allowed));
        }
    }

    private String normalizeSourceType(String sourceType) {
        return StringUtils.trimToEmpty(sourceType).toUpperCase(Locale.ROOT);
    }

    private String resolveExtension(String fileName) {
        String normalized = normalizeManagedFilePath(fileName);
        if (StringUtils.isBlank(normalized) || !normalized.contains(".")) {
            return null;
        }
        return StringUtils.substringAfterLast(normalized, ".").toLowerCase(Locale.ROOT);
    }

    private String sanitizeBaseName(String sourceName) {
        String baseName = StringUtils.defaultIfBlank(StringUtils.trimToNull(sourceName), "data_source");
        baseName = baseName.replaceAll("[\\\\/:*?\"<>|\\s]+", "_");
        baseName = baseName.replaceAll("_+", "_");
        baseName = StringUtils.strip(baseName, "_");
        return StringUtils.defaultIfBlank(baseName, "data_source");
    }

    private static Set<String> asSet(String... values) {
        return Collections.unmodifiableSet(new HashSet<>(Arrays.asList(values)));
    }

    private static final class RenamedMultipartFile implements MultipartFile {

        private final MultipartFile delegate;
        private final String originalFilename;

        private RenamedMultipartFile(MultipartFile delegate, String originalFilename) {
            this.delegate = delegate;
            this.originalFilename = originalFilename;
        }

        @Override
        public String getName() {
            return delegate.getName();
        }

        @Override
        public String getOriginalFilename() {
            return originalFilename;
        }

        @Override
        public String getContentType() {
            return delegate.getContentType();
        }

        @Override
        public boolean isEmpty() {
            return delegate.isEmpty();
        }

        @Override
        public long getSize() {
            return delegate.getSize();
        }

        @Override
        public byte[] getBytes() throws IOException {
            return delegate.getBytes();
        }

        @Override
        public InputStream getInputStream() throws IOException {
            return delegate.getInputStream();
        }

        @Override
        public void transferTo(File dest) throws IOException, IllegalStateException {
            delegate.transferTo(dest);
        }
    }
}
