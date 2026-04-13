package com.ruoyi.common.core.utils;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.util.ArrayList;
import java.util.List;


public class ReadExcelUtil {

    /**
     * 读EXCEL文件，获取信息集合
     * @return
     */
    public static List getExcelInfo(MultipartFile mFile) {
        String fileName = mFile.getOriginalFilename();//获取文件名
        try {
            if (!validateExcel(fileName)) {// 验证文件名是否合格
                return null;
            }
            boolean isExcel2003 = true;// 根据文件名判断文件是2003版本还是2007版本
            if (isExcel2007(fileName)) {
                isExcel2003 = false;
            }
            List infoList = createExcel(mFile.getInputStream(), isExcel2003);
            return infoList;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
    /**
     * 根据excel里面的内容读取客户信息
     * @param is 输入流
     * @param isExcel2003 excel是2003还是2007版本
     * @return
     * @throws IOException
     */
    public static List createExcel(InputStream is, boolean isExcel2003) {
        try{
            Workbook wb = null;
            if (isExcel2003) {// 当excel是2003时,创建excel2003
                wb = new HSSFWorkbook(is);
            } else {// 当excel是2007时,创建excel2007
                wb = new XSSFWorkbook(is);
            }
            List infoList=new ArrayList<>();
            for (int i=0;i<wb.getNumberOfSheets();i++) {
                List tmplist = readExcelValue(wb,i);// 读取Excel里面客户的信息
                infoList.add(tmplist);
            }

            return infoList;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
    /**
     * 读取Excel里面的信息
     * @param wb
     * @return
     */
    private static List readExcelValue(Workbook wb,int index) {
        //默认会跳过第一行标题
        // 得到第一个shell
        Sheet sheet = wb.getSheetAt(index);
        // 得到Excel的行数
        int totalRows = sheet.getPhysicalNumberOfRows();
        int totalColumns = 0;
        // 得到Excel的列数(前提是有行数)
        if (totalRows > 0 && sheet.getRow(index) != null) {
            totalColumns = sheet.getRow(index).getPhysicalNumberOfCells();
        }
        List infoList = new ArrayList<>();
        // 循环Excel行数
        for (int r = 0; r < totalRows; r++) {
            List<String> rowList = new ArrayList<>();
            Row row = sheet.getRow(r);
            if (row == null){
                continue;
            }
            // 循环Excel的列
            for (int c = 0; c < totalColumns; c++) {
                String cellValue = new DataFormatter().formatCellValue(row.getCell(c));
                rowList.add(cellValue);
            }
            infoList.add(rowList);
        }

        return infoList;
    }


    /**
     * 验证EXCEL文件
     * @param filePath
     * @return
     */
    public static boolean validateExcel(String filePath) {
        if (filePath == null || !(isExcel2003(filePath) || isExcel2007(filePath))) {
            String errorMsg = "文件名不是excel格式";
            return false;
        }
        return true;
    }

    /**
     * 是否是2003的excel，返回true是2003
     * @param filePath
     * @return
     */
    public static boolean isExcel2003(String filePath)  {
        return filePath.matches("^.+\\.(?i)(xls)$");
    }

    /**
     * 是否是2007的excel，返回true是2007
     * @param filePath
     * @return
     */
    public static boolean isExcel2007(String filePath)  {
        return filePath.matches("^.+\\.(?i)(xlsx)$");
    }

}





