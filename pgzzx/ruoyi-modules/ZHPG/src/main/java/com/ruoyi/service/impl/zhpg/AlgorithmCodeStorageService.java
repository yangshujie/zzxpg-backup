package com.ruoyi.service.impl.zhpg;

import cn.hutool.core.util.ZipUtil;
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
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * 算法包上传（MinIO）与本地缓存解压预览，路径规则与原先评估项目一致：
 * 存储键 = algs/{分类子目录}/{算法名}.zip
 */
@Service
public class AlgorithmCodeStorageService {

    private static final String HEAD = "algs/";

    @Autowired
    private RemoteFileService remoteFileService;

    @Value("${custom-config.minio.bucketName}")
    private String bucketName;

    @Value("${custom-config.algs-main-path}")
    private String algsPath;

    /**
     * 上传 zip：按算法类型选子目录，对象名为清洗后的算法名称 + .zip
     */
    public String uploadAlgorithmZip(String algorithmType, String algorithmName, MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new ServiceException("请选择算法包文件");
        }
        String original = file.getOriginalFilename();
        if (original == null || !original.toLowerCase().endsWith(".zip")) {
            throw new ServiceException("请上传 .zip 格式的算法包");
        }
        if (StringUtils.isBlank(algorithmType)) {
            throw new ServiceException("请先选择算法类型");
        }
        if (StringUtils.isBlank(algorithmName)) {
            throw new ServiceException("请先填写算法名称");
        }
        String subDir = resolveTypeSubDir(algorithmType);
        String baseName = sanitizeBaseName(algorithmName);
        String dirPath = HEAD + subDir;
        MultipartFile toUpload = new RenamedMultipartFile(file, baseName + ".zip");
        R<SysFile> r = remoteFileService.upload(bucketName, dirPath, toUpload);
        if (r == null || !Boolean.TRUE.equals(r.isSuccess()) || r.getData() == null) {
            throw new ServiceException(r != null ? r.getMsg() : "文件服务无响应");
        }
        String url = r.getData().getUrl();
        if (StringUtils.isBlank(url)) {
            throw new ServiceException("上传成功但未返回文件路径");
        }
        return url;
    }

    /**
     * 下载 zip 到本地工作目录并解压，读取与 zip 主文件名同名的 .py 源码（与原 preview 逻辑一致）
     */
    public String readMainPythonForPreview(String algorithmCodeUrl) {
        if (StringUtils.isBlank(algorithmCodeUrl)) {
            throw new ServiceException("未配置算法代码包路径");
        }
        String weightAssignAlgorithmURL = algorithmCodeUrl.replace("\\", "/");
        String[] nameArray = weightAssignAlgorithmURL.split("/");
        String name = nameArray[nameArray.length - 1].split("\\.")[0];
        String dirPath = weightAssignAlgorithmURL.split("/" + name + ".zip")[0];
        String root = normalizeAlgsRoot();
        File file = new File(root + weightAssignAlgorithmURL.replace("/", File.separator));
        String zipAbs = root + dirPath.replace("/", File.separator) + File.separator + name + ".zip";
        if (!file.exists()) {
            File dir = new File(root + dirPath.replace("/", File.separator) + File.separator + name);
            if (!dir.exists()) {
                //noinspection ResultOfMethodCallIgnored
                dir.mkdirs();
            }
            String downloadPath = root + dirPath.replace("/", File.separator);
            boolean flag = remoteFileService.downloadToDir(bucketName, weightAssignAlgorithmURL, downloadPath);
            Long fileSize = remoteFileService.getFileSize(bucketName, weightAssignAlgorithmURL);
            if (fileSize != null && fileSize > 0) {
                int guard = 0;
                while (file.exists() && file.length() != fileSize && guard < 60) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                    guard++;
                }
            }
            if (flag) {
                ZipUtil.unzip(zipAbs, downloadPath + File.separator + name + File.separator);
            } else {
                CommonUtilDel.safeDelete(file);
            }
        }
        String pyPath = root + dirPath.replace("/", File.separator) + File.separator + name + File.separator + name + ".py";
        return readUtf8File(pyPath);
    }

    static String resolveTypeSubDir(String algorithmType) {
        if (algorithmType == null) {
            return "others/";
        }
        switch (algorithmType) {
            case "指标量化算法":
                return "norm/";
            case "属性值计算方法":
                return "character/";
            case "权重分配":
                return "weight/";
            case "聚合传导":
                return "conduction/";
            case "方案评价":
                return "assess/";
            case "预处理算法":
                return "dataProcess/";
            default:
                return "others/";
        }
    }

    static String sanitizeBaseName(String algorithmName) {
        String s = algorithmName.trim();
        if (s.isEmpty()) {
            return "algorithm";
        }
        return s.replaceAll("[\\\\/:*?\"<>|]", "_");
    }

    private String normalizeAlgsRoot() {
        if (algsPath == null) {
            return "";
        }
        String p = algsPath.trim();
        return p.endsWith(File.separator) ? p : p + File.separator;
    }

    private static String readUtf8File(String filePath) {
        File f = new File(filePath);
        if (!f.isFile()) {
            throw new ServiceException("未找到主程序文件（期望与压缩包同名的 .py）: " + filePath);
        }
        long length = f.length();
        if (length > 10 * 1024 * 1024) {
            throw new ServiceException("源码文件过大，无法预览");
        }
        byte[] fileContent = new byte[(int) length];
        try (FileInputStream in = new FileInputStream(f)) {
            //noinspection ResultOfMethodCallIgnored
            in.read(fileContent);
            return new String(fileContent, java.nio.charset.StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new ServiceException("读取算法源码失败: " + e.getMessage());
        }
    }

    private static final class RenamedMultipartFile implements MultipartFile {

        private final MultipartFile delegate;
        private final String originalFilename;

        RenamedMultipartFile(MultipartFile delegate, String originalFilename) {
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

    /** 避免依赖 ruoyi-system 的 CommonUtil.delFile */
    private static final class CommonUtilDel {
        static void safeDelete(File file) {
            if (file != null && file.exists()) {
                //noinspection ResultOfMethodCallIgnored
                file.delete();
            }
        }
    }
}
