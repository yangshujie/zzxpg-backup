package com.ruoyi.file.controller;

import com.ruoyi.common.core.utils.file.MFile;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.file.domain.vo.FileInfo;
import com.ruoyi.file.service.MinioSysFileServiceImpl;
import io.minio.errors.*;
import io.swagger.annotations.ApiOperation;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.common.core.utils.file.FileUtils;
import com.ruoyi.system.api.domain.SysFile;

import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.URLEncoder;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.List;

/**
 * 文件请求处理
 * 
 * @author ruoyi
 */
@RestController
public class SysFileController
{
    private static final Logger log = LoggerFactory.getLogger(SysFileController.class);

    @Autowired
    private MinioSysFileServiceImpl sysFileService;

    /**
     * @description: 文件上传请求
     * @param bucketname:
     * @param path:
     * @param file:
     * @return com.ruoyi.common.core.domain.R<com.ruoyi.system.api.domain.SysFile>
     * @author: jy
     * @date: 2023/4/1 13:33
     */
    @PostMapping("minio-upload")
    public R<SysFile> upload(@RequestParam("bucketname") String bucketname,
                             @RequestParam("path") String path,
                             @RequestPart("file") MultipartFile file)
    {
        try
        {
            // 上传并返回访问地址
            String url = sysFileService.uploadFile(bucketname,path,file);
            System.out.println(url);
            SysFile sysFile = new SysFile();
            sysFile.setName(FileUtils.getName(url));
            sysFile.setUrl(url);
            return R.ok(sysFile);  //可根据情况进行截取字符串保存到数据库
        }
        catch (Exception e)
        {
            log.error("上传文件失败", e);
            return R.fail(e.getMessage());
        }
    }

    @PostMapping("minio-uploadFileStream")
    public R<SysFile> upload(@RequestParam("bucketname") String bucketname,
                             @RequestParam("path") String path,
                             @RequestParam("file") String file)
    {
        try
        {
            // 上传并返回访问地址
            String url = sysFileService.uploadFileStream(bucketname, path, file);
//            System.out.println(url);
            SysFile sysFile = new SysFile();
            sysFile.setName(FileUtils.getName(url));
            sysFile.setUrl(url);
            return R.ok(sysFile);  //可根据情况进行截取字符串保存到数据库
        }
        catch (Exception e)
        {
            log.error("上传文件失败", e);
            return R.fail(e.getMessage());
        }
    }

    //------------------------------下载-------------------------------------


//    @GetMapping(value = "download")
//    public void download(@RequestParam("bucketname") String bucketname,
//                         @RequestParam("path") String path,
//                         @RequestParam("fileName") String filename) throws UnsupportedEncodingException
//    {
//        ServletRequestAttributes requestAttributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
//        HttpServletResponse response = requestAttributes.getResponse();
//        // 清空response
//        response.reset();
//        // 设置response的Header
//        response.setCharacterEncoding("UTF-8");
//        //Content-Disposition的作用：告知浏览器以何种方式显示响应返回的文件，用浏览器打开还是以附件的形式下载到本地保存
//        //attachment表示以附件方式下载   inline表示在线打开   "Content-Disposition: inline; filename=文件名.mp3"
//        // filename表示文件的默认名称，因为网络传输只支持URL编码的相关支付，因此需要将文件名URL编码后进行传输,前端收到后需要反编码才能获取到真正的名称
//        response.addHeader("Content-Disposition", "attachment;filename=" + URLEncoder.encode(filename, "UTF-8"));
//        // 告知浏览器文件的大小
////        response.addHeader("Content-Length", "" + file.length());
//        response.setContentType("application/octet-stream");
//        sysFileService.download(fileName, response);
//        System.out.println("**********************");
//    }

    @GetMapping(value = "download")
    public void downloadFile(@RequestParam("fileName") String fileName) throws UnsupportedEncodingException {
        ServletRequestAttributes requestAttributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletResponse response = requestAttributes.getResponse();
        // 清空response
        response.reset();
        // 设置response的Header
        response.setCharacterEncoding("UTF-8");
        //Content-Disposition的作用：告知浏览器以何种方式显示响应返回的文件，用浏览器打开还是以附件的形式下载到本地保存
        //attachment表示以附件方式下载   inline表示在线打开   "Content-Disposition: inline; filename=文件名.mp3"
        // filename表示文件的默认名称，因为网络传输只支持URL编码的相关支付，因此需要将文件名URL编码后进行传输,前端收到后需要反编码才能获取到真正的名称
        response.addHeader("Content-Disposition", "attachment;filename=" + URLEncoder.encode(fileName, "UTF-8"));
        // 告知浏览器文件的大小
//        response.addHeader("Content-Length", "" + file.length());
        response.setContentType("application/octet-stream");
        sysFileService.downloadFile(fileName, response);
        System.out.println("**********************");
    }

//    @GetMapping("downloadReport")
//    public AjaxResult download(@RequestParam("fileName") String fileName, @RequestParam("bucketName") String bucketName) throws IOException, InvalidResponseException, InvalidKeyException, NoSuchAlgorithmException, ServerException, ErrorResponseException, XmlParserException, InsufficientDataException, InternalException {
//        InputStream inputStream = sysFileService.download(bucketName, fileName);
//        return AjaxResult.success(inputStream);
//    }

    @GetMapping(value = "minio-downloadToDir")
    public boolean downloadToDir(@RequestParam("bucketname") String bucketname,
                                 @RequestParam("filePath") String filePath,
                                 @RequestParam("downloadPath") String downloadPath){
        try {
            return sysFileService.download(bucketname, filePath, downloadPath);
        }catch (Exception e){
            return false;
        }
    }

    /**
     * @description: 下载报告，并将其存储至订阅此报告用户的文件夹中
     * @param reportUrl:
     * @param userid:
     * @author: pbw
     * @date: 2023/8/7
     */
    @GetMapping(value = "downloadToMinIO")
    public String downloadToMinIO(@RequestParam("reportUrl") String reportUrl, @RequestParam("userid") int userid) throws UnsupportedEncodingException {
        return sysFileService.downloadToMinIO(reportUrl, userid);
    }


    //------------------------------查询-------------------------------------

    /**
     * @description: 获取bucket中的某个目录下的文件列表
     * @param bucketname:
 * @param prefix:
     * @return com.ruoyi.common.core.domain.R<java.util.List < com.ruoyi.file.domain.vo.FileInfo>>
     * @author: jy
     * @date: 2023/4/1 20:02
     */
    @GetMapping("minio-list")
    public R<List<FileInfo>> getList(@RequestParam("bucketname") String bucketname,
                                     @RequestParam("prefix") String prefix){
        R<List<FileInfo>> result = new R<>();
        try{
            result.setData(sysFileService.getFiles(bucketname,prefix));
            return result;
        }catch (Exception e){
            log.error("获取列表成功", e);
            return R.fail(e.getMessage());
        }
    }

    //------------------------------删除-------------------------------------

    @DeleteMapping("minio-deleteFile")
    public R deleteFile(@RequestParam("bucketname") String bucketname,
                          @RequestParam("path") String path) {
        try {
            sysFileService.deleteFile(bucketname,path);
            return R.ok();
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
    }

    @DeleteMapping("minio-deleteDir")
    public R deleteDir(@RequestParam("bucketname") String bucketname,
                        @RequestParam("prefix") String prefix) {
        try {
            sysFileService.deleteDir(bucketname,prefix);
            return R.ok();
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
    }

    @DeleteMapping("minio-deleteBucket")
    public R deleteBucket(@RequestParam("bucketname") String bucketname) {
        try {
            sysFileService.deleteBucket(bucketname);
            return R.ok();
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
    }

    @PostMapping(value = "downloadReport")
    public void downloadReport(@RequestParam("url") String url, HttpServletResponse response) throws UnsupportedEncodingException {
//        ServletRequestAttributes requestAttributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
//        HttpServletResponse response = requestAttributes.getResponse();
        // 清空response
        response.reset();
        // 设置response的Header
        response.setCharacterEncoding("UTF-8");
        //Content-Disposition的作用：告知浏览器以何种方式显示响应返回的文件，用浏览器打开还是以附件的形式下载到本地保存
        //attachment表示以附件方式下载   inline表示在线打开   "Content-Disposition: inline; filename=文件名.mp3"
        // filename表示文件的默认名称，因为网络传输只支持URL编码的相关支付，因此需要将文件名URL编码后进行传输,前端收到后需要反编码才能获取到真正的名称
//        String filename = "接口.txt";
        response.addHeader("Content-Disposition", "attachment;filename=" + URLEncoder.encode(url, "UTF-8"));
        // 告知浏览器文件的大小
//        response.addHeader("Content-Length", "" + file.length());
        response.setContentType("application/octet-stream");
        sysFileService.downloadFile(url, response);
        System.out.println("**********************");
    }

    @ApiOperation("删除过期文件")
    @DeleteMapping("minio-deleteOuttimeFile")
    public R deleteOuttimeFile(@RequestParam("bucketname") String bucketname,
                       @RequestParam("prefix") String prefix,@RequestParam("days") int days) {
        try {
            sysFileService.deleteOuttimeFile(bucketname,prefix,days);
            return R.ok();
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
    }

    @ApiOperation("获取文件预览地址")
    @PostMapping("/getPreviewUrl")
    public AjaxResult getPreviewUrl(@RequestParam(value = "fileName")String fileName){
        try {
            return AjaxResult.success("预览成功", sysFileService.getPreviewUrl(fileName));
        }catch (Exception e){
            e.printStackTrace();
            return AjaxResult.error("获取失败");
        }
    }

    @ApiOperation("判断文件是否存在")
    @PostMapping("/isFileExist")
    public Boolean isFileExist(@RequestParam(value = "bucketname")String bucketname,@RequestParam(value = "url")String url){
        try {
            boolean isexist=sysFileService.isFileExist(bucketname,url);
            return isexist;
        }catch (Exception e){
            e.printStackTrace();
            return false;
        }
    }

    @ApiOperation("获取文件大小")
    @PostMapping("/getFileSize")
    public Long getFileSize(@RequestParam(value = "bucketname")String bucketname,@RequestParam(value = "url")String url){
        try {
            Long size = sysFileService.getFileSize(bucketname,url);
            return size;
        }catch (Exception e){
            e.printStackTrace();
            return 0L;
        }
    }


}