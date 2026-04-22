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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * 评估报告生成后上传文件服务（MinIO），路径与数据源上传一致走 {@link RemoteFileService#upload}。
 */
@Service
public class EvalReportFileStorageService {

    private static final String STORAGE_ROOT = "zhpg/evalReport/";

    @Autowired
    private RemoteFileService remoteFileService;

    @Value("${custom-config.minio.bucketName}")
    private String bucketName;

    /**
     * @param calcTaskId 计算任务 id
     * @param resultCode 结果编号（用于文件名）
     * @param extension  html / docx
     * @param bytes        文件内容
     * @return MinIO 相对路径（与算法包、数据源相同，存入业务表）
     */
    public String uploadEvalReport(long calcTaskId, String resultCode, String extension, byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            throw new ServiceException("报告内容为空，无法上传");
        }
        String ext = normalizeExt(extension);
        String dirPath = buildStorageDir();
        String safeCode = sanitizeFileToken(resultCode);
        String targetName = "calc" + calcTaskId + "_" + safeCode + "." + ext;
        String contentType = "html".equals(ext) ? "text/html;charset=UTF-8"
                : "pdf".equals(ext) ? "application/pdf"
                : "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
        MultipartFile multipart = new ByteArrayMultipartFile(
                "file", targetName, contentType, bytes);
        R<SysFile> result = remoteFileService.upload(bucketName, dirPath, multipart);
        if (result == null || !Boolean.TRUE.equals(result.isSuccess()) || result.getData() == null) {
            throw new ServiceException(result != null ? result.getMsg() : "文件服务无响应");
        }
        String url = normalizeManagedFilePath(result.getData().getUrl());
        if (StringUtils.isBlank(url)) {
            throw new ServiceException("上传成功但未返回文件路径");
        }
        validateManagedPath(url);
        return url;
    }

    private static String normalizeExt(String extension) {
        if (StringUtils.isBlank(extension)) {
            return "html";
        }
        String e = extension.trim().toLowerCase(Locale.ROOT);
        if (e.startsWith(".")) {
            e = e.substring(1);
        }
        if ("docx".equals(e) || "pdf".equals(e) || "html".equals(e) || "htm".equals(e)) {
            return "htm".equals(e) ? "html" : e;
        }
        return "html";
    }

    private String buildStorageDir() {
        return STORAGE_ROOT + new SimpleDateFormat("yyyyMM").format(new Date()) + "/";
    }

    private static String sanitizeFileToken(String raw) {
        if (StringUtils.isBlank(raw)) {
            return "noref";
        }
        return raw.replaceAll("[^a-zA-Z0-9._-]", "_");
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

    private void validateManagedPath(String path) {
        if (path.contains("..")) {
            throw new ServiceException("文件服务路径不合法");
        }
        if (!path.startsWith(STORAGE_ROOT)) {
            throw new ServiceException("报告路径必须位于 " + STORAGE_ROOT);
        }
    }

    private static final class ByteArrayMultipartFile implements MultipartFile {

        private final String name;
        private final String originalFilename;
        private final String contentType;
        private final byte[] content;

        ByteArrayMultipartFile(String name, String originalFilename, String contentType, byte[] content) {
            this.name = name;
            this.originalFilename = originalFilename;
            this.contentType = contentType;
            this.content = content != null ? content : new byte[0];
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public String getOriginalFilename() {
            return originalFilename;
        }

        @Override
        public String getContentType() {
            return contentType;
        }

        @Override
        public boolean isEmpty() {
            return content.length == 0;
        }

        @Override
        public long getSize() {
            return content.length;
        }

        @Override
        public byte[] getBytes() {
            return content;
        }

        @Override
        public InputStream getInputStream() {
            return new ByteArrayInputStream(content);
        }

        @Override
        public void transferTo(File dest) throws IOException {
            java.nio.file.Files.write(dest.toPath(), content);
        }
    }
}
