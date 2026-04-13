package com.ruoyi.common.core.utils.file;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;

public class MFile{
    private String name;

    private String originalFilename;

    private String contentType;

    private boolean isEmpty;

    private long size;

    private byte[] bytes;

    private InputStream inputStream;

    private String bucketName;

    private String dirPath;

    public MFile(File file) {
        try {
            name = file.getName();
            originalFilename = file.getName();
            inputStream = new FileInputStream(file);
            size = file.length();
            isEmpty = size == 0;
            contentType = "multipart/form-data";
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getOriginalFilename() {
        return originalFilename;
    }

    public void setOriginalFilename(String originalFilename) {
        this.originalFilename = originalFilename;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public boolean isEmpty() {
        return isEmpty;
    }

    public void setEmpty(boolean empty) {
        isEmpty = empty;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public byte[] getBytes() {
        return bytes;
    }

    public void setBytes(byte[] bytes) {
        this.bytes = bytes;
    }

    public InputStream getInputStream() {
        return inputStream;
    }

    public void setInputStream(InputStream inputStream) {
        this.inputStream = inputStream;
    }

    public String getBucketName() {
        return bucketName;
    }

    public void setBucketName(String bucketName) {
        this.bucketName = bucketName;
    }

    public String getDirPath() {
        return dirPath;
    }

    public void setDirPath(String dirPath) {
        this.dirPath = dirPath;
    }
}
