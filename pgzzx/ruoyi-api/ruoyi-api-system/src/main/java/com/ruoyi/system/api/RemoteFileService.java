package com.ruoyi.system.api;

import com.ruoyi.common.core.constant.Constants;
import com.ruoyi.common.core.utils.file.MFile;
import com.ruoyi.common.core.utils.ip.IpUtils;
import com.ruoyi.common.core.web.domain.AjaxResult;
import io.swagger.annotations.ApiOperation;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.junit.Test;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.ruoyi.common.core.constant.ServiceNameConstants;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.system.api.domain.SysFile;
import com.ruoyi.system.api.factory.RemoteFileFallbackFactory;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.UnsupportedEncodingException;

/**
 * 文件服务
 * 
 * @author ruoyi
 */

@FeignClient(contextId = "remoteFileService", value = ServiceNameConstants.FILE_SERVICE, fallbackFactory = RemoteFileFallbackFactory.class, url = "${custom-config.file-ip}")
public interface RemoteFileService {

    /**
     * 上传文件
     *
     * @param file 文件信息
     * @return 结果
     */
    @PostMapping(value = "/minio-upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public R<SysFile> upload(@RequestParam("bucketname") String bucketname,
                             @RequestParam("path") String path,
                             @RequestPart("file") MultipartFile file);

    @PostMapping(value = "minio-uploadFileStream")
    public R<SysFile> upload(@RequestParam("bucketname") String bucketname,
                             @RequestParam("path") String path,
                             @RequestParam("file") String file);

    /**
     * 下载文件
     *
     * @param fileName 文件信息
     * @return 结果
     */
    @GetMapping(value = "/download")
    public void downloadFile(@RequestParam("fileName") String fileName);

    @GetMapping(value = "/download")
    public void download(@RequestParam("fileName") String fileName);


    /**
     * 下载文件
     *
     * @param filePath 文件信息
     * @return 结果
     */
    @GetMapping(value = "minio-downloadToDir")
    public boolean downloadToDir(@RequestParam("bucketname") String bucketname,
                                 @RequestParam("filePath") String filePath,
                                 @RequestParam("downloadPath") String downloadPath);
    /**
     * @description: 下载报告，并将其存储至订阅此报告用户的文件夹中
     * @param reportUrl:
     * @param userid:
     * @author: pbw
     * @date: 2023/8/7
     */
    @GetMapping(value = "downloadToMinIO")
    public String downloadToMinIO(@RequestParam("reportUrl") String reportUrl, @RequestParam("userid") int userid) throws UnsupportedEncodingException;

    /**
     * @description: 删除报告
     * @param bucketname:
     * @param path:
     * @author: pbw
     * @date: 2023/8/7
     */
    @DeleteMapping("minio-deleteFile")
    public R deleteFile(@RequestParam("bucketname") String bucketname,
                        @RequestParam("path") String path);

    @PostMapping("getPreviewUrl")
    public AjaxResult getPreviewUrl(@RequestParam(value = "fileName")String fileName);


    @DeleteMapping("minio-deleteOuttimeFile")
    public R deleteOuttimeFile(@RequestParam("bucketname") String bucketname,
                               @RequestParam("prefix") String prefix,@RequestParam("days") int days);

    @PostMapping("/isFileExist")
    public Boolean isFileExist(@RequestParam(value = "bucketname")String bucketname,@RequestParam(value = "url")String url);

    @PostMapping("/getFileSize")
    public Long getFileSize(@RequestParam(value = "bucketname")String bucketname,@RequestParam(value = "url")String url);
}
