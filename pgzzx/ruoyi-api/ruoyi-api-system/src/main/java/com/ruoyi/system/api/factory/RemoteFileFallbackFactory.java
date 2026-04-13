package com.ruoyi.system.api.factory;

import com.ruoyi.common.core.utils.file.MFile;
import com.ruoyi.common.core.web.domain.AjaxResult;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.system.api.RemoteFileService;
import com.ruoyi.system.api.domain.SysFile;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.UnsupportedEncodingException;

/**
 * 文件服务降级处理
 * 
 * @author ruoyi
 */
@Component
public class RemoteFileFallbackFactory implements FallbackFactory<RemoteFileService>
{
    private static final Logger log = LoggerFactory.getLogger(RemoteFileFallbackFactory.class);

    @Override
    public RemoteFileService create(Throwable throwable)
    {
        log.error("文件服务调用失败:{}", throwable.getMessage());
        return new RemoteFileService()
        {
            @Override
            public R<SysFile> upload(String bucketname,String path,MultipartFile file)
            {
                return R.fail("上传文件失败:" + throwable.getMessage());
            }

            @Override
            public R<SysFile> upload(String bucketname,String path,String file) {
                return null;
            }

            @Override
            public void downloadFile(String fileName) {

            }

            @Override
            public void download(String fileName) {

            }

//            @Override
//            public void downloadFile(String fileName, HttpServletResponse response) {
//
//            }

            @Override
            public boolean downloadToDir(String bucketname, String filePath,String downloadPath) {
                return false;
            }

            /**
             * @author: pbw
             * @date: 2023/8/7
             */
            @Override
            public String downloadToMinIO(String reportUrl, int userid) throws UnsupportedEncodingException {

                return reportUrl;
            }

            /**
             * @author: pbw
             * @date: 2023/8/7
             */
            @Override
            public R deleteFile(String bucketname, String path) {
                return null;
            }

            @Override
            public AjaxResult getPreviewUrl(String fileName) {
                return null;
            }

            @Override
            public R deleteOuttimeFile(String bucketname,String prefix,int days) {
                return null;
        }

            @Override
            public Boolean isFileExist(String bucketname,String url) {
                return false;
            }

            @Override
            public Long getFileSize(@RequestParam(value = "bucketname")String bucketname,@RequestParam(value = "url")String url) {return 0L;}
        };
    }
}
