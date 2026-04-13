package com.ruoyi.common.core.utils;


import com.ruoyi.common.core.utils.file.FileUtils;
import org.junit.Test;

import java.io.*;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;


public class ZipUtil {
    /**
     * 解压缩包zip文件
     * @param filePath zip文件绝对路径
     * @param zipDir 解压到文件夹
     * @return
     */
    public static String unzip(String filePath,String zipDir) throws IOException {
        String name = "";
        BufferedOutputStream dest = null;
        BufferedInputStream is = null;
        ZipFile zipfile = new ZipFile(filePath);

        try {

            ZipEntry entry;
//            File file = new File(filePath);
//            ZipFile zipfile = null;


//            Enumeration dir = zipfile.entries();
//            while (dir.hasMoreElements()){
//                entry = (ZipEntry) dir.nextElement();
//
//                if(entry.isDirectory()){
//                    name = entry.getName();
//                    name = name.substring(0, name.length() - 1);
//                    File fileObject = new File(zipDir + name);
//                    fileObject.mkdir();
//                }
//            }

            Enumeration e = zipfile.entries();
            while (e.hasMoreElements()) {
                entry = (ZipEntry) e.nextElement();
                if( entry.isDirectory()) {
                    name = entry.getName();
                    name = name.substring(0, name.length() - 1);
                    File fileObject = new File(zipDir + name);
                    fileObject.mkdir();
                    continue;

                }else {
                    is = new BufferedInputStream(zipfile.getInputStream(entry));
                    int count;
                    byte[] dataByte = new byte[1024];
                    FileOutputStream fos = new FileOutputStream(zipDir+entry.getName());
                    dest = new BufferedOutputStream(fos, 1024);
                    while ((count = is.read(dataByte, 0, 1024)) != -1) {
                        dest.write(dataByte, 0, count);
                    }
                    dest.flush();
                    is.close();
                    dest.close();

                }
            }
            zipfile.close();
        } catch (Exception e) {

            if (is!=null) {
                is.close();
            }
            if (dest!=null) {
                dest.close();
            }
            zipfile.close();

            e.printStackTrace();
        }
        return name;
    }

    /**
     * 压缩某个目录下的所有文件，不包含本目录
     * @param folderFile
     * @return
     * @throws IOException
     */
    public static File compressedFolder(String folderFile) throws IOException {
        File zipFilename = createNewFile(folderFile+".zip");
        try (
            FileOutputStream fos = new FileOutputStream(zipFilename.getPath());
            ZipOutputStream zipOutputStream = new ZipOutputStream(fos);
            ) {
            File folder = new File(folderFile);
            if (!folder.exists()) {
                throw new RuntimeException("需要压缩的文件不存在");
            }
            File[] folders = folder.listFiles();
            for (int i=0;i<folders.length;i++) {
                addZipFolder(zipOutputStream,folders[i],"");
            }

            return zipFilename;
        }
    }

    public static void addZipFolder(ZipOutputStream zipOutputStream,File sourcePath,String zipPath) throws IOException {
        if(sourcePath.isDirectory()) {
            zipPath=zipPath+sourcePath.getName()+"/";
            String[] files=sourcePath.list();
            ZipEntry entry = new ZipEntry(zipPath);
            zipOutputStream.putNextEntry(entry);
            if (files==null || files.length==0) {
//                ZipEntry entry = new ZipEntry(zipPath);
//                zipOutputStream.putNextEntry(entry);
                zipOutputStream.closeEntry();
            } else {
                for(String fileName:files) {
                    String filePath = sourcePath.getPath() + "/" + fileName;
                    File fileChile = new File(filePath);
//                    if (fileChile.isDirectory()) {
                        addZipFolder(zipOutputStream,fileChile,zipPath);
//                    } else {
//                        writeZip(zipOutputStream,fileChile,zipPath);
//                    }
                }
            }
        }else {
            writeZip(zipOutputStream,sourcePath,zipPath);
        }
    }

    public static void writeZip(ZipOutputStream zipOutputStream,File inputFile,String zipPath) throws IOException {
        byte[] buffer = new byte[1024];
        ZipEntry entry = new ZipEntry(zipPath+inputFile.getName());
        zipOutputStream.putNextEntry(entry);
        FileInputStream fileInputStream=new FileInputStream(inputFile);
        int len=0;
        while((len=fileInputStream.read(buffer)) > 0) {
            zipOutputStream.write(buffer,0,len);
            zipOutputStream.flush();
        }
        zipOutputStream.closeEntry();
        fileInputStream.close();
    }

    public static File createNewFile(String filename) throws IOException {
        File file=new File(filename);
        if (!file.exists()) {
            file.createNewFile();
            FileWriter writter=new FileWriter(file);
            writter.close();
        } else {
            String base = filename.substring(0,filename.lastIndexOf("."));
            String ext = filename.substring(filename.lastIndexOf("."));
            int i = 0;
            while (true) {
                i++;
                filename = base + "(" + i + ")" + ext;
                file=new File(filename);
                if (!file.exists()) {
                    file.createNewFile();
                    FileWriter writer = new FileWriter(file);
                    writer.close();
                    break;
                }
            }
        }
        return file;
    }

    @Test
    public void test2() throws IOException {
//        String dir = "D:/zgpg/test";
//        File dirF = new File(dir);
//        dirF.mkdirs();
//        compressedFolder("D:\\zgpg\\ruoyi\\ruoyi-job-executor\\teldata\\aggerate\\c2ed675a-2680-4767-99b3-52c5d10cae55");
        unzip("D:\\zgpg\\ruoyi\\ruoyi-system\\telData\\weight\\1919572d-b602-4484-a701-39b069bf81ce.zip", "D:\\zgpg\\ruoyi\\ruoyi-system\\telData\\weight\\1919572d-b602-4484-a701-39b069bf81ce" + "/");
    }
}
