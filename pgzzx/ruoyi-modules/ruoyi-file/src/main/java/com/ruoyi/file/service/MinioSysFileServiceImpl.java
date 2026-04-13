package com.ruoyi.file.service;

import cn.hutool.core.date.DateTime;
import com.google.common.collect.Multimap;
import com.ruoyi.common.core.exception.base.BaseException;
import com.ruoyi.common.core.utils.CommonUtil;
import com.ruoyi.common.core.utils.StringUtils;
import com.ruoyi.common.core.utils.file.MFile;
import com.ruoyi.common.security.utils.SecurityUtils;
import com.ruoyi.file.domain.vo.FileInfo;
import com.ruoyi.file.utils.MyFileException;
import com.ruoyi.system.api.domain.SysDept;
import io.minio.*;
import io.minio.errors.*;
import io.minio.http.Method;
import io.minio.messages.DeleteObject;
import io.minio.messages.Item;
import io.minio.messages.Tags;
import org.apache.commons.collections.MultiMap;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.junit.Test;
import org.python.modules._io._jyio;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import com.ruoyi.file.config.MinioConfig;
import com.ruoyi.file.utils.FileUploadUtils;

import javax.servlet.http.HttpServletResponse;
import java.io.*;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;


/**
 * Minio 文件存储
 * 
 * @author ruoyi
 */
@Primary
@Service
public class MinioSysFileServiceImpl
{

    private final String reportEmail = "reportEmail";

    @Autowired
    private MinioConfig minioConfig;

    @Autowired
    private MinioClient client;

    /**
     * @description: 文件上传
     * @param bucketName: 桶名
     * @param path: 目录（不包括桶名） 如 "dir/"、"dir/dir/"
     * @param file: 文件
     * @return java.lang.String
     * @author: jy
     * @date: 2023/4/1 13:26
     */
    public String uploadFile(String bucketName, String path, MultipartFile file) throws Exception
    {
        String fileName = FileUploadUtils.extractFilename(file);
        if (!client.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build())) {
            client.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
        }

        PutObjectArgs args = PutObjectArgs.builder()
                .bucket(bucketName)
                .object(path+fileName)
                .stream(file.getInputStream(), file.getSize(), -1)
                .contentType(file.getContentType())
                .build();

        client.putObject(args);
        //jy改，暂时保留
//        return minioConfig.getUrl() + "/" + bucketName + "/" + path + fileName;
        return path + fileName;


    }


    public Boolean isFileExist(String bucketname, String prefix) throws Exception {
        InputStream stream = null;
        try {
            stream=client.getObject(
                GetObjectArgs.builder().bucket(bucketname).object(prefix).build());
            if (stream == null) {
                return false;
            }
            stream.close();
            return true;
        } catch (Exception e) {
            if (stream!=null) {
                stream.close();
            }
            System.out.println("-------------------------"+e.getMessage());
            return false;
        }
    }

    /**
     * 上传File格式文件
     * @param bucketName
     * @param path
     * @param file
     * @throws Exception
     */
    public String uploadFileStream(String bucketName, String path, String file) throws Exception{
        File fileTemp = new File(file);
//        OutputStream outputStream = new FileOutputStream(fileTemp);
//        file.write(outputStream);
//        outputStream.close();

//        String fileName = fileTemp.getName();
        String fileName = fileTemp.getName();
        FileInputStream fileInputStream = new FileInputStream(fileTemp);
        client.putObject(PutObjectArgs.builder().bucket(bucketName).object(path + fileName).stream(fileInputStream, fileTemp.length(), -1).contentType("application/pdf").build());
        fileInputStream.close();
        CommonUtil.delFile(fileTemp);
        return path + fileName;
    }


    public InputStream download(String bucketName, String path) throws IOException, InvalidKeyException, InvalidResponseException, InsufficientDataException, NoSuchAlgorithmException, ServerException, InternalException, XmlParserException, ErrorResponseException {
        InputStream inputStream = client.getObject(GetObjectArgs.builder().bucket(bucketName).object(path).build());
        return inputStream;
    }


    /**
     * 文件下载
     */
    public void downloadFile(String filePath, HttpServletResponse res){
        try {
            OutputStream outputStream = res.getOutputStream();

            InputStream stream =
                    client.getObject(
                            GetObjectArgs.builder().bucket(minioConfig.getBucketName()).object(filePath).build());

            // Read the input stream and print to the console till EOF.
            byte[] buf = new byte[16384];
            int bytesRead;
            while ((bytesRead = stream.read(buf, 0, buf.length)) >= 0) {
                outputStream.write(buf, 0, bytesRead);
                outputStream.flush();
            }
            // Close the input stream.
            outputStream.close();
            stream.close();
        } catch (MinioException | InvalidKeyException | IOException | NoSuchAlgorithmException e) {
            throw new MyFileException("Error occurred: "+e);
        }
    }

    /**
     * 下载报告，并将其存储至订阅此报告用户的文件夹中
     * @date 2023.8/7
     */
    public String downloadToMinIO(String reportUrl, int userid){
        try {

            InputStream inputStream =
                    client.getObject(
                            GetObjectArgs.builder().bucket(minioConfig.getBucketName()).object(reportUrl).build());
            String fileName = reportUrl.trim().substring(reportUrl.lastIndexOf("/") + 1, reportUrl.lastIndexOf("."));
            String fileExtension = reportUrl.trim().substring(reportUrl.lastIndexOf("."));
            Date date = new Date();
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMddhhmmss");
//            String reportName = fileName + "-" + dateFormat.format(date) + fileExtension;
            String reportName = fileName + fileExtension;


            client.putObject(
                    PutObjectArgs.builder()
                            .bucket(minioConfig.getBucketName())
                            .object(reportEmail + "/" + userid + "/" + reportName)
                            .stream(inputStream, -1,10485760)
                            .contentType("application/pdf")
                            .build()
            );

            String url = reportEmail + "/" + userid + "/" + reportName;
            return url;
        } catch (MinioException | InvalidKeyException | IOException | NoSuchAlgorithmException e) {
            throw new MyFileException("Error occurred: "+e);
        }
    }


    /**
     * 下载文件到指定文件夹（不经过前端）
     * @param filePath minio中文件所在路径
     * @param dirPath 下载到本地的文件夹路径
     * @return
     */
    public Boolean download(String bucketName,String filePath, String dirPath) throws IOException, InvalidKeyException, InvalidResponseException, InsufficientDataException, NoSuchAlgorithmException, ServerException, InternalException, XmlParserException, ErrorResponseException {
        FileOutputStream file = new FileOutputStream(dirPath + "/"+filePath.substring(filePath.lastIndexOf("/")+1));
        InputStream stream = client.getObject(
                GetObjectArgs.builder().bucket(bucketName).object(filePath).build());
        try{


            // Read the input stream and print to the console till EOF.
            byte[] buf = new byte[16384];
            int bytesRead;
            while ((bytesRead = stream.read(buf, 0, buf.length)) >= 0) {
//                System.out.println(new String(buf, 0, bytesRead, StandardCharsets.UTF_8));

                file.write(buf, 0, bytesRead);
                file.flush();
            }
            // Close the input stream.
            file.close();
            stream.close();
            return true;
        }catch (Exception e){
            file.close();
            stream.close();
            return false;
        }
    }

    /**
     * @description: 递归获取某路径下的所有文件，并输出,FileInfo形式
     * @param bucketname:
     * @param prefix:
     * @return java.util.List<java.lang.String>
     * @author: jy
     * @date: 2023/4/1 16:24
     */
    public List<FileInfo> getFiles(String bucketname, String prefix) throws Exception {
        List<FileInfo> fileInfos = new ArrayList<>();
        Iterable<Result<Item>> results = client.listObjects(ListObjectsArgs.builder().bucket(bucketname).recursive(true).prefix(prefix).build());
        for (Result<Item> result : results) {
            Item item = result.get();

            FileInfo fileInfo = new FileInfo();
            fileInfo.setFilename(item.objectName());
            fileInfo.setSize(item.size());
            fileInfo.setTime(item.lastModified().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            fileInfo.setLabel(item.objectName().substring(item.objectName().lastIndexOf("/")+1));
            fileInfos.add(fileInfo);
        }
        return fileInfos;
    }

    /**
     * @description: 递归获取某路径下的所有文件，文件夹，并输出,str形式
     * @param bucketname:
     * @param prefix:
     * @return java.util.List<java.lang.String>
     * @author: jy
     * @date: 2023/4/1 16:24
     */
    public List<String> getFiles_str(String bucketname, String prefix) throws Exception {
        List<String> list = new ArrayList<>();
        Iterable<Result<Item>> results = client.listObjects(ListObjectsArgs.builder().bucket(bucketname).recursive(true).prefix(prefix).build());
        for (Result<Item> result : results) {
            Item item = result.get();
            if (item.isDir()) {
                List<String> files = getFiles_str(bucketname, item.objectName());
                list.addAll(files);
            } else {
                list.add(item.objectName());
            }
        }
        return list;
    }


    //------------------------------删除-------------------------------------
    /**
     * @description: 删除桶里单个文件
     * @param bucketName:
     * @param objectName:
     * @return void
     * @author: jy
     * @date: 2023/4/1 14:39
     */
    public void deleteFile(String bucketName,String objectName) throws IOException, InvalidKeyException, InvalidResponseException, InsufficientDataException, NoSuchAlgorithmException, ServerException, InternalException, XmlParserException, ErrorResponseException {
        client.removeObject(RemoveObjectArgs.builder().bucket(bucketName).object(objectName).build());
    }

    /**
     * @description: 删除某个目录下的过期文件
     * @param bucketName:
     * @param prefix:
     * @param daysToNow: 过期天数
     * @return void
     * @author: jy
     * @date: 2023/4/1 14:39
     */
    public void deleteOuttimeFile(String bucketName,String prefix,int daysToNow) throws Exception {
        List<FileInfo> fileinfos = getFiles(bucketName,prefix);
        List<String> outtimeUrls =new ArrayList<>();
        for(FileInfo tmpfileinfo:fileinfos) {
            SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            long stamp = formatter.parse(tmpfileinfo.getTime()).getTime() / 1000L;
            long nowtimestamp = formatter.parse(DateTime.now().toString()).getTime() /1000L;
            if (nowtimestamp-stamp>86000L * daysToNow) {
                outtimeUrls.add(tmpfileinfo.getFilename());
            }
        }
        for (String url:outtimeUrls) {
            deleteFile(bucketName,url);
        }
    }

    /**
     * @description: 删除指定目录及目录下的所有文件
     * @param bucketName:
     * @param prefix: "/"表示桶内根目录,"aaa/bb"表示桶内aaa目录下的bb
     * @return void
     * @author: jy
     * @date: 2023/4/1 16:22
     */
    public void deleteDir(String bucketName, String prefix) throws Exception {
        List<String> files = getFiles_str(bucketName,prefix);
        for (String file : files) {
            deleteFile(bucketName, file);
        }
    }

    /**
     * @description: 删除桶
     * @param bucketName:
     * @return void
     * @author: jy
     * @date: 2023/4/1 11:25
     */
    public void deleteBucket(String bucketName) throws IOException, InvalidKeyException, InvalidResponseException, InsufficientDataException, NoSuchAlgorithmException, ServerException, InternalException, XmlParserException, ErrorResponseException {
//        //删除加密桶
//        minioClient.deleteBucketEncryption(DeleteBucketEncryptionArgs.builder().bucket("my-bucketname").build());
//        //删除桶生命周期
//        minioClient.deleteBucketLifecycle(DeleteBucketLifecycleArgs.builder().bucket("my-bucketname").build());
//        //删除桶标签
//        minioClient.deleteBucketTags(DeleteBucketTagsArgs.builder().bucket("my-bucketname").build());
//        //删除桶策略
//        minioClient.deleteBucketTags(DeleteBucketPolicyArgs.builder().bucket("my-bucketname").build());
//        //删除桶复制配置
//        minioClient.deleteBucketReplication(DeleteBucketReplicationArgs.builder().bucket("my-bucketname").build());
//        //删除桶通知
//        minioClient.deleteBucketNotification(DeleteBucketNotificationArgs.builder().bucket("my-bucketname").build());
//        //删除桶对象锁object-lock配置在
//        minioClient.deleteObjectLockConfiguration(DeleteObjectLockConfigurationArgs.builder().bucket("my-bucketname").build());
        boolean found =  client.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
        if (found) {
            client.removeBucket(RemoveBucketArgs.builder().bucket(bucketName).build());
        }
    }

    /**
     * 获取文件预览地址
     * @param fileName
     * @return
     * @throws Exception
     */
    public String getPreviewUrl(String fileName) throws Exception{
        HashMap<String, String> headers =new HashMap<>();
//        headers.put("response-content-disposition","inline");
//        headers.put("response-content-type","image/png");
//        headers.put("x-amz-meta-preview","'true'");

        return client.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder().bucket(minioConfig.getBucketName()).object(fileName).extraHeaders(headers).method(Method.GET).build());
    }

    public void test(String prefix){
        Iterable<Result<Item>> results = client.listObjects(ListObjectsArgs.builder().bucket("test").recursive(true).prefix(prefix).build());


    }

    public Long getFileSize(String bucketName,String allfilename) throws Exception{
        List<FileInfo> fileInfos = new ArrayList<>();
        Iterable<Result<Item>> results = client.listObjects(ListObjectsArgs.builder().bucket(bucketName).recursive(true).prefix(allfilename).build());
        Long size = 0L;
        for (Result<Item> result : results) {
            Item item = result.get();
            size=item.size();

        }
        return size;
    }

}
