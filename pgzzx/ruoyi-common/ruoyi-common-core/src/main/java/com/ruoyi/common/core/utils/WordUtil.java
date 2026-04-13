package com.ruoyi.common.core.utils;

import cn.hutool.core.date.DateRange;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.lang.Tuple;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;

import jdk.nashorn.internal.runtime.ListAdapter;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.sl.usermodel.PictureData;
import org.apache.poi.ss.formula.functions.T;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.util.Units;
import org.apache.poi.xddf.usermodel.XDDFColor;
import org.apache.poi.xddf.usermodel.XDDFLineProperties;
import org.apache.poi.xddf.usermodel.XDDFShapeProperties;
import org.apache.poi.xddf.usermodel.XDDFSolidFillProperties;
import org.apache.poi.xddf.usermodel.chart.*;
import org.apache.poi.xddf.usermodel.text.XDDFRunProperties;
import org.apache.poi.xddf.usermodel.text.XDDFTextBody;
import org.apache.poi.xddf.usermodel.text.XDDFTextParagraph;
import org.apache.poi.xddf.usermodel.text.XDDFTextRun;
import org.apache.poi.xwpf.usermodel.*;
import org.apache.xmlbeans.XmlCursor;
import org.junit.Test;
import org.openxmlformats.schemas.drawingml.x2006.chart.*;
import org.openxmlformats.schemas.drawingml.x2006.main.CTLineProperties;
import org.openxmlformats.schemas.drawingml.x2006.main.CTShapeProperties;
import org.openxmlformats.schemas.drawingml.x2006.main.CTSolidColorFillProperties;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.*;


import java.io.*;
import java.math.BigInteger;
import java.util.*;

public class WordUtil {
    private XWPFDocument document;

//    /**
//     * 操作模板中的图表， 需要根据图表的标题来找到指定的图表
//     */
//    public static void charGeneration(XWPFDocument doc, String tit, List<JSONObject> dataArray) {
//        if (dataArray != null && dataArray.size() > 0) {
//            List<POIXMLDocumentPart> relations = doc.getRelations(); // 获取模版中所有的表格模版
//            int index = 0;
//            for (POIXMLDocumentPart poixmlDocumentPart : relations)
//                if (poixmlDocumentPart instanceof XWPFChart) { //判断是不是图表类型
//                    XWPFChart chart = (XWPFChart) poixmlDocumentPart;
//                    String charType = ""; // charType 1 普通图表柱状图  2 折线图单条和2条 3 雷达图 4 多条折线图
//                    XDDFTitle xddfTitletitle = chart.getTitle();
//                    XDDFTextBody body = xddfTitletitle.getBody();
//                    CTTextBody xmlObject = body.getXmlObject();
//                    String tt = xmlObject.toString(); //图表的标题
//                    List<String> keyList = new ArrayList<>();
//                    List<String> keyListTemp = new ArrayList<>();
//                    List<String> titleArr = new ArrayList<>();
//                    //根据属性第一列名称切换数据类型
//                    CTChart ctChart = null;
//                    CTPlotArea plotArea = null;
//
//                    if (tt.contains(tit) && tit.equals("图表")) {//折线图
//                        //刷新内置excel数据
//                        charType = "2"; // charType 1 普通图表柱状图  2 折线图 3 雷达图
//                        keyList.add("时间");
//                        keyList.add("系列1");
//                        keyList.add("系列2");
//                        titleArr.add("时间");
//                        titleArr.add("系列1");
//                        titleArr.add("系列2");
//                        ctChart = chart.getCTChart();
//                        plotArea = ctChart.getPlotArea();
//                    } else if (tt.contains(tit) && tit.contains("图表2")) {
//                        charType = "1";
//                        keyList.add("ssnd");
//                        keyList.add("value1");
//                        keyList.add("value2");
//                        titleArr.add("");
//                        titleArr.add("收入");
//                        titleArr.add("支出");
//                        ctChart = chart.getCTChart();
//                        plotArea = ctChart.getPlotArea();
//                    }
//                    if (StringUtils.isNotEmpty(charType)) {
//                        /**
//                         * 每个word 中的图表都会对应一个内置的excel .用来存放表格的数据.
//                         * 所以需要像图表的数据先写入内置的excel 中
//                         */
//                        refreshExcel(chart, dataArray, keyList, titleArr);
//
//                        //刷新页面显示数
//                        List<String> newKey = new ArrayList<>(); //之所以要new 一个新的对象,直接赋值,只是赋值了引用地址
//
////                        if (charType.equals("1")) {
////                            CTBarChart barChart = plotArea.getBarChartArray(0);
////                            List<CTBarSer> serList = barChart.getSerList();
////                            int position = 1;
//////                            refreshNumGraphContent(barChart, serList, dataArray, keyList, titleArr);
////                            refreshNumGraphContent(barChart, serList, dataArray, keyList, titleArr);
////                        }
//                        if (charType.equals("2")) {
//                            CTLineChart lineChart = plotArea.getLineChartArray(0);
//                            List<CTLineSer> serList = lineChart.getSerList();
//                            int position = 1;
//                            refreshLineStrGraphContent(lineChart, serList, dataArray, keyList, titleArr);
//                        }
//
//
//                        break;
//                    }
//
//                }
//
//
//        }
//    }
//
//    /**
//     * 刷新折线图数据方法
//     *
//     * @param
//     * @param serList
//     * @param dataList
//     * @param
//     * @return
//     */
//    public static boolean refreshLineStrGraphContent(CTLineChart lineChart, List<?> serList, List<JSONObject> dataList,List<String> keyList, List<String> titleList) {
//
//        boolean result = true;
//        int position = 1;
//
//        if (dataList.size() < 1) {
//            return false;
//        }
//        List<String> tList = new ArrayList();
//
//        int keyIndex=1;
//        //更新数据区域
//        for (int i = 0; i < serList.size(); i++) {
//            CTAxDataSource cat = null;
//            CTNumDataSource val = null;
//            CTLineSer ser =lineChart.getSerArray(i);
//            cat = ser.getCat();
//            // 获取图表的值
//            val = ser.getVal();
//            CTSerTx tx = ser.getTx();
//            CTStrRef strRefH = cat.getStrRef();
//            CTStrData strCacheH = strRefH.getStrCache();
//            CTStrData strCache = tx.getStrRef().getStrCache();
//            CTNumData numData = val.getNumRef().getNumCache();
//            strCache.setPtArray((CTStrVal[]) null); // unset old axis text
//            strCacheH.setPtArray((CTStrVal[]) null); // unset old axis text
//            numData.setPtArray((CTNumVal[]) null); // unset old values
//            // set model
//            int idx = 0;
//
//            CTStrVal strVal1 = strCache.addNewPt();//序列名称
//            if (titleList.size() == 2) {
//                strVal1.setIdx(i);
//                strVal1.setV(titleList.get(1));
//            }else{
//                strVal1.setIdx(i);
//                strVal1.setV(titleList.get(i+1));
//            }
//            for (int j = 0; j < dataList.size(); j++) {
//
//                CTStrVal strVal = strCacheH.addNewPt();//序列名称
//                strVal.setIdx(idx);
//                strVal.setV(dataList.get(j).getString(keyList.get(0)));
//
//                for (int i1 = 0; i1 < keyList.size(); i1++) {
//                    String value = "0";
//
//                    if (i1 == 0) {
//                        if (idx>0){
//                            continue;
//                        }else{
//
//                        }
//                    }else{
//                        // TODO:是否含有中文？
////                        if (StringUtil.checkChinese(dataList.get(j).getString(keyList.get(keyIndex)))){
////                            continue;
////                        }
//                        String zb = "0";
//                        if (dataList.get(j).get(keyList.get(keyIndex))!=null && !dataList.get(j).getString(keyList.get(keyIndex)).trim().equals("--")){
//                            zb = dataList.get(j).getString(keyList.get(keyIndex));
////                            if(StringUtil.isNullString(zb)){
////                                zb = "0";
////                            }
//                            if("".equals(zb)){
//                                zb = "0";
//                            }
//                            if (zb.indexOf("%")>0){
//                                BigDecimal b = new BigDecimal(100);
//                                zb = zb.replace("%","");
//                                zb = new BigDecimal(zb).divide(b).setScale(4, BigDecimal.ROUND_HALF_UP).toString();
//                            }
//                        }
//                        if(new BigDecimal(zb)!=null){
//                            value=new BigDecimal(zb).toString();
//                        }
//                        if(!"0".equals(value)){
//                            CTNumVal numVal = numData.addNewPt();//序列值
//                            numVal.setIdx(idx);
//                            numVal.setV(value);
//                        }
//                        idx++;
//                        break;
//                    }
//                }
//            }
//            numData.getPtCount().setVal(idx);
//            if (i==0){
//                strCache.getPtCount().setVal(idx);
//                String legendDataRange = new CellRangeAddress(0, 0, 1, idx + 1)
//                        .formatAsString("Sheet1", true);
//                tx.getStrRef().setF(legendDataRange);
//                //赋值横坐标数据区域
//                String axDataRange = new CellRangeAddress(1, dataList.size(), 0, 0)
//                        .formatAsString("Sheet1", true);
//                cat.getStrRef().setF(axDataRange);
//            }
//            //数据区域
//            String numDataRange = new CellRangeAddress(1, dataList.size(), i + position, i + position)
//                    .formatAsString("Sheet1", false);
//            val.getNumRef().setF(numDataRange);
//            // 设置系列生成方向
//            if (keyList.size() != 2) {
//                keyIndex++;
//            }
//        }
//        return result;
//    }
//
//    /**
//     * 刷新数据
//     * @param chart
//     * @param dataList
//     * @param titleArr
//     * @param keyList
//     * @return
//     */
//    public static boolean refreshExcel(XWPFChart chart,
//            List<JSONObject> dataList,List<String> keyList,List<String> titleArr) {
//        boolean result = true;
//        Workbook wb = new XSSFWorkbook();
//        Sheet sheet = wb.createSheet("Sheet1");
//        //根据数据创建excel第一行标题行
//        sheet.createRow(0).createCell(0).setCellValue("");
//        for (int i = 1; i < titleArr.size(); i++) {
//            sheet.getRow(0).createCell(i).setCellValue(titleArr.get(i)==null?"":titleArr.get(i));
//        }
//
//        //遍历数据行
//        for (int i = 0; i < dataList.size(); i++) {
//            JSONObject baseFormMap = dataList.get(i);//数据行
//            //fldNameArr字段属性
//            for (int j = 0; j < keyList.size(); j++) {
//                if(sheet.getRow(i+1)==null){
//                    if(j==0){
//                        try {
//                            sheet.createRow(i+1).createCell(j).setCellValue(baseFormMap.getString(keyList.get(j))==null?"":baseFormMap.getString(keyList.get(j)));
//                        } catch (Exception e) {
//                            if(baseFormMap.get(keyList.get(i))==null){
//                                sheet.createRow(i+1).createCell(j).setCellValue("");
//                            }else{
//                                sheet.createRow(i+1).createCell(j).setCellValue(baseFormMap.getString(keyList.get(j)));
//                            }
//                        }
//                    }
//                }else{
//                    String dvl = baseFormMap.getString(keyList.get(j));
////                        if (StringUtil.checkChinese(dvl)) {
////                            continue;
////                        }
//                    double value=0d;
//                    String zb = "0";
//                    if (baseFormMap.getString(keyList.get(j))!=null && !baseFormMap.getString(keyList.get(j)).trim().equals("--")){
//                        zb = baseFormMap.getString(keyList.get(j));
////                            if(StringUtil.isNullString(zb)){
////                                zb = "0";
////                            }
//                        if("".equals(zb)){
//                            zb = "0";
//                        }
//                        if (zb.indexOf("%")>0){
//                            BigDecimal b = new BigDecimal(100);
//                            zb = zb.replace("%","");
//                            zb = new BigDecimal(zb).divide(b).setScale(4, BigDecimal.ROUND_UP).toString();
//                        }
//                    }
//                    if(new BigDecimal(zb)!=null){
//                        value=new BigDecimal(zb).doubleValue();
//                    }
//                    if(StringUtils.isEmpty(dvl)){
//                        sheet.getRow(i+1).createCell(j);
//                    }else{
//                        sheet.getRow(i+1).createCell(j).setCellValue(value);
//                    }
//                }
//            }
//        }
//        // 更新嵌入的workbook
//        POIXMLDocumentPart xlsPart = chart.getRelations().get(0);
//        OutputStream xlsOut = xlsPart.getPackagePart().getOutputStream();
//
//        try {
//            wb.write(xlsOut);
//            xlsOut.close();
//        } catch (IOException e) {
//            e.printStackTrace();
//            result = false;
//        } finally {
//            if (wb != null) {
//                try {
//                    wb.close();
//                } catch (IOException e) {
//                    e.printStackTrace();
//                    result = false;
//                }
//            }
//        }
//        return result;
//    }
//    @Test
//    public void test() throws IOException {
//        String path = "";
//        JSONObject dataJson1 = new JSONObject();
//        dataJson1.put("时间", "2022-0819");
//        dataJson1.put("系列1", "11");
//        dataJson1.put("系列2", "22");
//        JSONObject dataJson2 = new JSONObject();
//        dataJson1.put("时间", "2022-0819");
//        dataJson2.put("系列1", "11");
//        dataJson2.put("系列2", "22");
//        JSONObject dataJson3 = new JSONObject();
//        dataJson1.put("时间", "2022-0819");
//        dataJson3.put("系列1", "11");
//        dataJson3.put("系列2", "22");
//        List<JSONObject> dataList = new ArrayList<>();
//        dataList.add(dataJson1);
//        dataList.add(dataJson2);
//        dataList.add(dataJson3);
//        InputStream is = new FileInputStream("C:\\Users\\pierre\\Desktop\\test.docx");
//        XWPFDocument doc = new XWPFDocument(is);
//        WordTest.charGeneration(doc, "图表", dataList);
//
//    }



    public static void setHorizontalPage(XWPFDocument document) {
//        int numParagraphs = document.getParagraphs().size();
//        XWPFParagraph lastParagraph = document.getParagraphs().get(numParagraphs - 1);
//        XmlCursor cursor = lastParagraph.getCTP().newCursor();
//        while (cursor.toNextSibling()) {
//            if (cursor.getName().getLocalPart().equals("sectPr")) {
//                cursor.selectPath("./*");
//                cursor.insertAttributeWithValue("w:orient", "landscape");
//                break;
//            }
//        }

    }

    /**
     * 设置一二级标签
     *
     * @param docxDocument
     */
    private static void addCustomHeadingStyle(XWPFDocument docxDocument, XWPFStyles styles, String strStyleId, int headingLevel, int pointSize) {
        CTStyle ctStyle = CTStyle.Factory.newInstance();
        ctStyle.setStyleId(strStyleId);

        CTString styleName = CTString.Factory.newInstance();
        styleName.setVal(strStyleId);
        ctStyle.setName(styleName);

        CTDecimalNumber indentNumber = CTDecimalNumber.Factory.newInstance();
        indentNumber.setVal(BigInteger.valueOf(headingLevel));

        ctStyle.setUiPriority(indentNumber);

        CTOnOff onoffnull = CTOnOff.Factory.newInstance();
        ctStyle.setUnhideWhenUsed(onoffnull);

        CTPPr ppr = CTPPr.Factory.newInstance();
        ppr.setOutlineLvl(indentNumber);
        ctStyle.setPPr(ppr);

        XWPFStyle style = new XWPFStyle(ctStyle);

//        CTHpsMeasure size = CTHpsMeasure.Factory.newInstance();
//        size.setVal(new BigInteger(String.valueOf(pointSize)));
//        CTHpsMeasure size2 = CTHpsMeasure.Factory.newInstance();
//        size2.setVal(new BigInteger("16"));

        CTFonts fonts = CTFonts.Factory.newInstance();
//        fonts.setAscii("Times New Roman");

        CTRPr rpr = CTRPr.Factory.newInstance();
        rpr.setRFonts(fonts);
//        rpr.setSz(size);
//        rpr.setSzCs(size2);

//        CTColor color = CTColor.Factory.newInstance();
//        color.setVal(hexToBytes(hexColor));
//        rpr.setColor(color);
        style.getCTStyle().setRPr(rpr);
        style.setType(STStyleType.PARAGRAPH);
        styles.addStyle(style);
    }

    /**
     * 添加段落带标题设置
     *
     * @param document
     * @param content   内容
     * @param alignment 对齐方式
     */
    public static void writeToDocTest(XWPFDocument document, String style, String color, Integer font_size, String content, String alignment, Integer textPosition, boolean newRow) {
        try {
            XWPFParagraph p = document.createParagraph();
            p.setStyle("Heading1");
            switch (alignment) {
                // 设置居中
                case "center":
                    p.setAlignment(ParagraphAlignment.CENTER);
                    break;
                case "left":
                    p.setAlignment(ParagraphAlignment.LEFT);
                    break;
                case "right":
                    p.setAlignment(ParagraphAlignment.RIGHT);
                    break;
                default:
                    break;
            }
            XWPFStyles styles = document.createStyles();
            XWPFRun r = p.createRun();
            if (!StringUtils.isEmpty(style)) {
                Integer level = Integer.parseInt(style.split("heading")[1]);
                addCustomHeadingStyle(document, styles, style, level, 16);
                p.setStyle(style);
                switch (level){
                    //居中标题
                    case 0:{
                        r.setBold(false);
                        // 字体大小
                        r.setFontSize(22);
                        r.setFontFamily("方正小标宋简体");
                        r.setFontFamily("Times New Roman");
                        p.setSpacingBetween(1.5, LineSpacingRule.AUTO);
                        p.setSpacingBefore(300);
                        p.setSpacingAfter(300);
                        break;
                    }
                    //一级标题
                    case 1:{
                        r.setBold(true);
                        // 字体大小
                        r.setFontSize(22);
                        r.setFontFamily("宋体");
                        r.setFontFamily("Times New Roman");
                        p.setSpacingBetween(1.5, LineSpacingRule.AUTO);
                        p.setSpacingBefore(300);
                        p.setSpacingAfter(300);
                        break;
                    }
                    //二级标题
                    case 2:{
                        // 字体大小
                        r.setFontSize(16);
                        r.setFontFamily("宋体");
                        r.setFontFamily("Times New Roman");
                        r.setBold(true);
                        p.setSpacingBetween(1.5, LineSpacingRule.AUTO);
                        p.setSpacingBefore(260);
                        p.setSpacingAfter(260);
                        break;
                    }
                    //三级标题
                    case 3:{
                        // 字体大小
                        r.setFontSize(16);
                        r.setFontFamily("宋体");
                        r.setFontFamily("Times New Roman");
                        r.setBold(true);
                        p.setSpacingBetween(1.5, LineSpacingRule.AUTO);
                        p.setSpacingBefore(260);
                        p.setSpacingAfter(260);
                        break;
                    }
                    //四级标题
                    case 4:{
                        // 字体大小
                        r.setFontSize(14);
                        r.setFontFamily("宋体");
                        r.setFontFamily("Times New Roman");
                        r.setBold(true);
                        p.setSpacingBetween(1.5, LineSpacingRule.AUTO);
                        p.setSpacingBefore(0);
                        p.setSpacingAfter(0);
                        break;
                    }
                    default:{
                        break;
                    }
                }
            }

            // 段落内容
            r.setColor(color);
            r.setText(content);

            // 与下一行的距离
            if (textPosition == null) {
                textPosition = 10;
            }
            r.setTextPosition(textPosition);
            // 增加换行
            if (newRow)
                r.addCarriageReturn();
        } catch (Exception e) {
            e.printStackTrace();
//            return null;
        }
    }


    /**
     * 添加段落
     * @param document
     * @param content 内容
     * @param type 字体类型
     * @param alignment 对齐方式
     */
    public static void writeToDoc(XWPFDocument document, String content, int type, String alignment, Integer textPosition){
        try{
            XWPFParagraph p = document.createParagraph();


            switch (alignment){
                // 设置居中
                case "center":p.setAlignment(ParagraphAlignment.CENTER);break;
                case "left":p.setAlignment(ParagraphAlignment.LEFT);break;
                case "right":p.setAlignment(ParagraphAlignment.RIGHT);break;
                default:break;
            }

            XWPFRun r = p.createRun();
            switch (type){
                //首页的一号标题
                case 1:{
                    // 是否加粗
                    r.setBold(false);
                    // 字体大小
                    r.setFontSize(22);
                    r.setFontFamily("黑体");
                    break;
                }
                // 首页的二级标题
                case 2:{
                    // 是否加粗
                    r.setBold(false);
                    // 字体大小
                    r.setFontSize(18);
                    r.setFontFamily("楷体");
                    break;
                }
                //一级标题
                case 3:{
                    // 是否加粗
                    r.setBold(false);
                    // 字体大小
                    r.setFontSize(16);
                    r.setFontFamily("黑体");
                    break;
                }
                //二级标题
                case 4:{
                    // 是否加粗
                    r.setBold(false);
                    // 字体大小
                    r.setFontSize(16);
                    r.setFontFamily("黑体");
                    break;
                }
                //正文
                case 5:{
                    // 是否加粗
                    r.setBold(false);
                    // 字体大小
                    r.setFontSize(16);
                    r.setFontFamily("仿宋");
                    r.setFontFamily("Times New Roman");
                    p.setIndentationFirstLine(480);
                    p.setSpacingBetween(1.5, LineSpacingRule.AUTO);
                    p.setAlignment(ParagraphAlignment.BOTH);
                    break;
                }
                //三级标题
                case 6:{
                    // 是否加粗
                    r.setBold(false);
                    // 字体大小
                    r.setFontSize(14);
                    r.setFontFamily("黑体");
                    p.setSpacingBetween(1.5, LineSpacingRule.AUTO);
                    p.setSpacingBefore(300);
                    p.setSpacingAfter(300);
                    break;
                }
                //四级标题
                case 7:{
                    // 是否加粗
                    r.setBold(false);
                    // 字体大小
                    r.setFontSize(12);
                    r.setFontFamily("宋体");
                    break;
                }
                //图表标题
                case 8: {
                    // 是否加粗
                    r.setBold(false);
                    // 字体大小
                    r.setFontSize(11);
                    r.setFontFamily("黑体");
                    p.setSpacingBetween(1.5, LineSpacingRule.AUTO);
                    break;
                }

                default:{

                    break;
                }
            }
            // 段落内容
            r.setText(content);
            // 与下一行的距离
            if (textPosition == null) {
                textPosition = 10;
            }
            r.setTextPosition(textPosition);
            // 增加换行
//            r.addCarriageReturn();
        }catch (Exception e){
            e.printStackTrace();
//            return null;
        }
    }

    /**
     * 获取图表对象
     *
     * @param document word对象
     * @param width    默认15
     * @param height   默认10
     * @return
     */
    public static XWPFChart getChart(XWPFDocument document, Integer width, Integer height) throws IOException, InvalidFormatException {
        if (width == null) {
            width = 15;
        }
        if (height == null) {
            height = 10;
        }
        return document.createChart(width * Units.EMU_PER_CENTIMETER, height * Units.EMU_PER_CENTIMETER);
    }

    public static XWPFChart newChart(XWPFRun run, XWPFDocument document, Integer width, Integer height) throws IOException, InvalidFormatException {
        return document.createChart(run, width * Units.EMU_PER_CENTIMETER, height * Units.EMU_PER_CENTIMETER);
    }


//    public void createLineChart(XWPFChart chart, BarChartForm lineChartForm) {
//        // 标题
//        chart.setTitleText(lineChartForm.getTitle());
//        //标题覆盖
//        chart.setTitleOverlay(false);
//        //图例位置
//        XDDFChartLegend legend = chart.getOrAddLegend();
//        legend.setPosition(LegendPosition.TOP);
//        //分类轴标(X轴),标题位置
//        XDDFCategoryAxis bottomAxis = chart.createCategoryAxis(AxisPosition.BOTTOM);
//        bottomAxis.setTitle(lineChartForm.getBottomTitle());
//        //值(Y轴)轴,标题位置
//        XDDFValueAxis leftAxis = chart.createValueAxis(AxisPosition.LEFT);
//        leftAxis.setTitle(lineChartForm.getLeftTitle());
//        // 处理数据
//        XDDFCategoryDataSource bottomDataSource = XDDFDataSourcesFactory.fromArray(lineChartForm.getBottomData());
//        XDDFNumericalDataSource<Integer> leftDataSource = XDDFDataSourcesFactory.fromArray(lineChartForm.getLeftData());
//
//        // 生成数据
//        XDDFLineChartData data = (XDDFLineChartData) chart.createData(ChartTypes.LINE, bottomAxis, leftAxis);
//
//        // 不自动生成颜色
//        data.setVaryColors(lineChartForm.getVaryColors());
//
//        //图表加载数据，折线1
//        XDDFLineChartData.Series series = (XDDFLineChartData.Series) data.addSeries(bottomDataSource, leftDataSource);
//
//        //是否弯曲
//        series.setSmooth(lineChartForm.getSmooth());
//
//        //设置标记样式
//        series.setMarkerStyle(lineChartForm.getStyle());
//
//        //绘制
//        chart.plot(data);
//    }


    /**
     * 插入折线图
     * @param document
     * @param title
     * @param content {"xAxis":["x1","x2","x3"],"yAxis":[[0.1,0.2,0.3],[0.4,0.5,0.6]], "nameList":["name1","name2"]}
     * @throws Exception
     */
    public static void insertLineChart(XWPFDocument document, String title, JSONObject content) throws Exception{
//        XWPFParagraph paragraph = document.createParagraph();
//        XWPFRun run = paragraph.createRun();
//        run.setText("折线图");
//        run.addCarriageReturn();

//        List<JSONObject> dataObjList = JSONObject.parseArray(dataArray.toJSONString(), JSONObject.class);
//        XWPFChart chartLine = document.createChart(run, (int)(14.5 * Units.EMU_PER_CENTIMETER),
//                9 * Units.EMU_PER_CENTIMETER);
        XWPFChart chartLine = getChart(document, 15, 10);
//        XWPFChart chartLine = newChart(r, document, 5, 3);
        chartLine.setChartTopMargin(10L);
        // 3、图表相关设置
//        XDDFTitle xddfTitle = chartLine.getTitle();
//        xddfTitle.getOrAddTextProperties().setFontSize(3d);
//        xddfTitle.setText(title);
//        xddfTitle.setOverlay(false);
//        chartLine.getTitle().getOrAddTextProperties().setFontSize(3d);
//        CTTitle ctTitle = chartLine.getCTChart().getTitle();
//        ctTitle.
//        chartLine.setTitleText(title);
        chartLine.setTitleText(title); // 图表标题
        chartLine.setTitleOverlay(false); // 图例是否覆盖标题
//        XDDFTitle xddfTitle = chartLine.getTitle();
//        XDDFRunProperties properties = chartLine.getTitle().getOrAddTextProperties();
//        properties.setFontSize(10d);
//        properties.setBold(true);
//        xddfTitle.setText(title);
//        xddfTitle.setOverlay(false);


        // 4、图例设置
        XDDFChartLegend legendLine = chartLine.getOrAddLegend();
        legendLine.setPosition(LegendPosition.BOTTOM); // 图例位置:上下左右

        // 5、X轴(分类轴)相关设置
        XDDFCategoryAxis xAxisLine = chartLine.createCategoryAxis(AxisPosition.BOTTOM); // 创建X轴,并且指定位置
        xAxisLine.setTickLabelPosition(AxisTickLabelPosition.LOW); // 设置X周的文字一直在最下方
        String[] xAxisDataLine = JSONObject.parseObject(content.getJSONArray("xAxis").toString(), String[].class);
        XDDFCategoryDataSource xAxisSourceLine = XDDFDataSourcesFactory.fromArray(xAxisDataLine); // 设置X轴数据

        // 6、Y轴(值轴)相关设置
        XDDFValueAxis yAxisLine = chartLine.createValueAxis(AxisPosition.LEFT); // 创建Y轴,指定位置
        yAxisLine.setCrossBetween(AxisCrossBetween.BETWEEN); // 设置图柱的位置:BETWEEN居中
//        yAxisLine.setMinimum(0d);
//        yAxisLine.setMaximum(1.2d);
//        yAxisLine.setMinorUnit(0d);
//        yAxisLine.setMajorUnit(1.2d);

        // 7、创建折线图对象
        XDDFLineChartData lineChart = (XDDFLineChartData) chartLine.createData(ChartTypes.LINE, xAxisLine, yAxisLine);
        //获取y轴数据
        JSONArray dataArray = content.getJSONArray("yAxis");
        JSONArray nameArray = content.getJSONArray("nameList");
        // 设置y轴的取值范围
//        yAxisLine.setMinimum(calMin(dataArray));
//        yAxisLine.setMaximum(calMax(dataArray));
        CTLineChart ctLineChart = chartLine.getCTChart().getPlotArea().getLineChartArray(0);
        for(int i=0,size=dataArray.size();i<size;i++){
            Double[] yAxisDataLine = JSONObject.parseObject(dataArray.getJSONArray(i).toString(), Double[].class);
            String name = nameArray.getString(i);
            XDDFNumericalDataSource<Double> yAxisSourceLine = XDDFDataSourcesFactory.fromArray(yAxisDataLine); // 设置Y轴数据
            XDDFLineChartData.Series lineSeries = (XDDFLineChartData.Series) lineChart.addSeries(xAxisSourceLine, yAxisSourceLine);

            XDDFLineProperties lineProperties = new XDDFLineProperties();
            byte[] color = new byte[] {(byte) (Math.random()*255),(byte)(Math.random()*255),(byte)(Math.random()*255)};
            XDDFSolidFillProperties seriesProperties = new XDDFSolidFillProperties(XDDFColor.from(color));
            lineProperties.setWidth(2.0);
            lineProperties.setFillProperties(seriesProperties);
            lineSeries.setLineProperties(lineProperties);
            //设置每条线的名字
            lineSeries.setTitle(name,null);

            //是否弯曲
//            lineSeries.setSmooth(true);

            CTLineSer ser = ctLineChart.getSerArray(i);
            CTDLbls ctdLbls = ser.addNewDLbls();
            ctdLbls.addNewShowCatName().setVal(false);// 是否展示对应x轴上的值（类型名称）
            ctdLbls.addNewShowVal().setVal(false);// 是否展示数值
            ctdLbls.addNewShowSerName().setVal(false);// 是否展示归属折线名称（系列名称）
            ctdLbls.addNewShowLegendKey().setVal(false);// 是否展示图例（图例项标示）
//            ctdLbls.addNewDLblPos().setVal(STDLblPos.IN_END);//数据标签

            CTMarker marker = ser.addNewMarker();
            setMarkerColor(marker, color, 2);
        }
        // 9、绘制折线图
        chartLine.plot(lineChart);


//        XDDFChartLegend legend = chartLine.getOrAddLegend();
//        legend.setPosition(LegendPosition.LEFT);
//        legend.setOverlay(false);
    }

    //格式修改：折线图显示在同一行
    public static void insertLineChart2(XWPFRun r, XWPFDocument document, String title, JSONObject content) throws Exception{
//        XWPFParagraph paragraph = document.createParagraph();
//        XWPFRun run = paragraph.createRun();
//        run.setText("折线图");
//        run.addCarriageReturn();

//        List<JSONObject> dataObjList = JSONObject.parseArray(dataArray.toJSONString(), JSONObject.class);
//        XWPFChart chartLine = document.createChart(run, (int)(14.5 * Units.EMU_PER_CENTIMETER),
//                9 * Units.EMU_PER_CENTIMETER);
        XWPFChart chartLine = newChart(r, document, 7, 6);
//        XWPFChart chartLine = newChart(r, document, 5, 3);
        chartLine.setChartTopMargin(10L);
        // 3、图表相关设置
//        XDDFTitle xddfTitle = chartLine.getTitle();
//        xddfTitle.getOrAddTextProperties().setFontSize(3d);
//        xddfTitle.setText(title);
//        xddfTitle.setOverlay(false);
//        chartLine.getTitle().getOrAddTextProperties().setFontSize(3d);
//        CTTitle ctTitle = chartLine.getCTChart().getTitle();
//        ctTitle.
//        chartLine.setTitleText(title);
        chartLine.setTitleText(title); // 图表标题
        chartLine.setTitleOverlay(false); // 图例是否覆盖标题

        XDDFTextBody textBody = chartLine.getTitle().getBody();
        XDDFTextParagraph paragraph = textBody.getParagraph(0);
        XDDFRunProperties run = paragraph.getDefaultRunProperties();
        double titleFontSize = 8.0;
        run.setFontSize(titleFontSize);
//        XDDFTitle xddfTitle = chartLine.getTitle();
//        XDDFRunProperties properties = chartLine.getTitle().getOrAddTextProperties();
//        properties.setFontSize(10d);
//        properties.setBold(true);
//        xddfTitle.setText(title);
//        xddfTitle.setOverlay(false);


        // 4、图例设置
        XDDFChartLegend legendLine = chartLine.getOrAddLegend();
        legendLine.setPosition(LegendPosition.BOTTOM); // 图例位置:上下左右

        XDDFTextBody legendTextBody = new XDDFTextBody(legendLine);
        legendTextBody.getXmlObject().addNewBodyPr();
        legendTextBody.addNewParagraph().getDefaultRunProperties().setFontSize(6.0);
        legendLine.setTextBody(legendTextBody);

        // 5、X轴(分类轴)相关设置
        XDDFCategoryAxis xAxisLine = chartLine.createCategoryAxis(AxisPosition.BOTTOM); // 创建X轴,并且指定位置
        xAxisLine.setTickLabelPosition(AxisTickLabelPosition.LOW); // 设置X周的文字一直在最下方
        String[] xAxisDataLine = JSONObject.parseObject(content.getJSONArray("xAxis").toString(), String[].class);
        XDDFCategoryDataSource xAxisSourceLine = XDDFDataSourcesFactory.fromArray(xAxisDataLine); // 设置X轴数据
        xAxisLine.getOrAddTextProperties().setFontSize(6.0);

        // 6、Y轴(值轴)相关设置
        XDDFValueAxis yAxisLine = chartLine.createValueAxis(AxisPosition.LEFT); // 创建Y轴,指定位置
        yAxisLine.setCrossBetween(AxisCrossBetween.BETWEEN); // 设置图柱的位置:BETWEEN居中
        yAxisLine.getOrAddTextProperties().setFontSize(6.0);
//        yAxisLine.setMinimum(0d);
//        yAxisLine.setMaximum(1.2d);
//        yAxisLine.setMinorUnit(0d);
//        yAxisLine.setMajorUnit(1.2d);

        // 7、创建折线图对象
        XDDFLineChartData lineChart = (XDDFLineChartData) chartLine.createData(ChartTypes.LINE, xAxisLine, yAxisLine);
        //获取y轴数据
        JSONArray dataArray = content.getJSONArray("yAxis");
        JSONArray nameArray = content.getJSONArray("nameList");
        // 设置y轴的取值范围
//        yAxisLine.setMinimum(calMin(dataArray));
//        yAxisLine.setMaximum(calMax(dataArray));
        CTLineChart ctLineChart = chartLine.getCTChart().getPlotArea().getLineChartArray(0);
        for(int i=0,size=dataArray.size();i<size;i++){
            Double[] yAxisDataLine = JSONObject.parseObject(dataArray.getJSONArray(i).toString(), Double[].class);
            String name = nameArray.getString(i);
            XDDFNumericalDataSource<Double> yAxisSourceLine = XDDFDataSourcesFactory.fromArray(yAxisDataLine); // 设置Y轴数据
            XDDFLineChartData.Series lineSeries = (XDDFLineChartData.Series) lineChart.addSeries(xAxisSourceLine, yAxisSourceLine);

            XDDFLineProperties lineProperties = new XDDFLineProperties();
            byte[] color = new byte[] {(byte) (Math.random()*255),(byte)(Math.random()*255),(byte)(Math.random()*255)};
            XDDFSolidFillProperties seriesProperties = new XDDFSolidFillProperties(XDDFColor.from(color));
            lineProperties.setWidth(2.0);
            lineProperties.setFillProperties(seriesProperties);
            lineSeries.setLineProperties(lineProperties);
            //设置每条线的名字
            lineSeries.setTitle(name,null);

            //是否弯曲
//            lineSeries.setSmooth(true);

            CTLineSer ser = ctLineChart.getSerArray(i);
            CTDLbls ctdLbls = ser.addNewDLbls();
            ctdLbls.addNewShowCatName().setVal(false);// 是否展示对应x轴上的值（类型名称）
            ctdLbls.addNewShowVal().setVal(false);// 是否展示数值
            ctdLbls.addNewShowSerName().setVal(false);// 是否展示归属折线名称（系列名称）
            ctdLbls.addNewShowLegendKey().setVal(false);// 是否展示图例（图例项标示）
//            ctdLbls.addNewDLblPos().setVal(STDLblPos.IN_END);//数据标签

            CTMarker marker = ser.addNewMarker();
            setMarkerColor(marker, color, 2);
        }
        // 9、绘制折线图
        chartLine.plot(lineChart);


//        XDDFChartLegend legend = chartLine.getOrAddLegend();
//        legend.setPosition(LegendPosition.LEFT);
//        legend.setOverlay(false);
    }

    /**
     * 插入雷达图
     * @param document
     * @param title
     * @param content{"xAxis":["x1","x2","x3"],"yAxis":[[0.1,0.2,0.3],[0.4,0.5,0.6]], "nameList":["name1","name2"]}
     * @throws Exception
     */
    public static void insertRadarChart(XWPFDocument document, String title, JSONObject content) throws Exception{
        XWPFChart chart = getChart(document, 15, 9);
        chart.setChartTopMargin(100);
        // 3、图表相关设置
        chart.setTitleText(title); // 图表标题
        chart.setTitleOverlay(false); // 图例是否覆盖标题
        chart.setChartLeftMargin(1);
        chart.setChartRightMargin(1);

        XDDFTextBody textBody = chart.getTitle().getBody();
        XDDFTextParagraph paragraph = textBody.getParagraph(0);
        XDDFRunProperties run = paragraph.getDefaultRunProperties();
        double fontSize = 10;
        run.setFontSize(fontSize);

        // 4、图例设置
        XDDFChartLegend legendLine = chart.getOrAddLegend();
        legendLine.setPosition(LegendPosition.BOTTOM); // 图例位置:上下左右
        XDDFTextBody legendTextBody = new XDDFTextBody(legendLine);
        legendTextBody.getXmlObject().addNewBodyPr();
        legendTextBody.addNewParagraph().getDefaultRunProperties().setFontSize(10.0);
        legendLine.setTextBody(legendTextBody);

        // 5、X轴(分类轴)相关设置
        XDDFCategoryAxis xAxis = chart.createCategoryAxis(AxisPosition.BOTTOM); // 创建X轴,并且指定位置
        xAxis.setTickLabelPosition(AxisTickLabelPosition.LOW); // 设置X周的文字一直在最下方
        xAxis.getOrAddTextProperties().setFontSize(fontSize);

        // 字体颜色
        XDDFSolidFillProperties fontColor = new XDDFSolidFillProperties(XDDFColor.from(new byte[] {(byte)96,(byte)98,(byte)102}));
        // 标签样式
        XDDFRunProperties xTextProperties = xAxis.getOrAddTextProperties();
        xTextProperties.setFillProperties(fontColor);
        //x轴数据
        String[] xAxisDataLine = JSONObject.parseObject(content.getJSONArray("xAxis").toString(), String[].class);
        // Y轴
        XDDFValueAxis yAxis = chart.createValueAxis(AxisPosition.LEFT);

        // 标签样式
        XDDFRunProperties yTextProperties = yAxis.getOrAddTextProperties();
        yTextProperties.setFillProperties(fontColor);
        yTextProperties.setFontSize(fontSize);
        // 网格线
        XDDFShapeProperties yGridProperties = yAxis.getOrAddMajorGridProperties();
        XDDFLineProperties yGridLine = new XDDFLineProperties();
        yGridLine.setFillProperties(new XDDFSolidFillProperties(XDDFColor.from(new byte[] {(byte)228,(byte)231,(byte)237})));
        yGridLine.setWidth(0.5);
        yGridProperties.setLineProperties(yGridLine);

        XDDFRadarChartData radar = (XDDFRadarChartData) chart.createData(ChartTypes.RADAR, xAxis, yAxis);
        radar.setStyle(RadarStyle.MARKER);

        XDDFCategoryDataSource xAxisSource = XDDFDataSourcesFactory.fromArray(xAxisDataLine);

        //获取y轴数据
        JSONArray dataArray = content.getJSONArray("yAxis");
        JSONArray nameArray = content.getJSONArray("nameList");
        // 设置y轴的取值范围
        yAxis.setMinimum(calMin(dataArray));
        yAxis.setMaximum(calMax(dataArray));
//        yAxis.setMinimum(0d);
//        yAxis.setMaximum(1d);
        CTRadarChart ctRadarChart = chart.getCTChart().getPlotArea().getRadarChartArray(0);
        for(int i=0,size=dataArray.size();i<size;i++){
            Double[] yAxisData = JSONObject.parseObject(dataArray.getJSONArray(i).toString(), Double[].class);
            String name = nameArray.getString(i);
            XDDFNumericalDataSource<Double> yAxisSource = XDDFDataSourcesFactory.fromArray(yAxisData); // 设置Y轴数据
            XDDFRadarChartData.Series series = (XDDFRadarChartData.Series) radar.addSeries(xAxisSource, yAxisSource);

            XDDFLineProperties lineProperties = new XDDFLineProperties();
            byte[] color = new byte[] {(byte) (Math.random()*255),(byte)(Math.random()*255),(byte)(Math.random()*255)};
            XDDFSolidFillProperties seriesProperties = new XDDFSolidFillProperties(XDDFColor.from(color));
            lineProperties.setWidth(1.5);
            lineProperties.setFillProperties(seriesProperties);
            series.setLineProperties(lineProperties);
            //设置每条线的名字
            series.setTitle(name,null);


            CTRadarSer ser = ctRadarChart.getSerArray(i);
//            CTDLbls ctdLbls = ser.addNewDLbls();
//            ctdLbls.addNewShowCatName().setVal(false);// 是否展示对应x轴上的值（类型名称）
//            ctdLbls.addNewShowVal().setVal(true);// 是否展示数值
//            ctdLbls.addNewShowSerName().setVal(false);// 是否展示归属折线名称（系列名称）
//            ctdLbls.addNewShowLegendKey().setVal(false);// 是否展示图例（图例项标示）
//            ctdLbls.addNewDLblPos().setVal(STDLblPos.IN_END);//数据标签

            CTMarker marker = ser.addNewMarker();
            setMarkerColor(marker, color, null);
        }
        chart.plot(radar);
    }


    //格式修改：图片設置為一行 2023/8/12
    public static void insertRadarChart2(XWPFRun r, XWPFDocument document, String title, JSONObject content) throws Exception{
        XWPFChart chart = newChart(r, document, 4, 6);
        chart.setChartTopMargin(100);
        // 3、图表相关设置
        chart.setTitleText(title); // 图表标题
        chart.setTitleOverlay(false); // 图例是否覆盖标题

        XDDFTextBody textBody = chart.getTitle().getBody();
        XDDFTextParagraph paragraph = textBody.getParagraph(0);
        XDDFRunProperties run = paragraph.getDefaultRunProperties();
        double fontSize = 6;
        run.setFontSize(fontSize);

        // 4、图例设置
        XDDFChartLegend legendLine = chart.getOrAddLegend();
        legendLine.setPosition(LegendPosition.BOTTOM); // 图例位置:上下左右
        XDDFTextBody legendTextBody = new XDDFTextBody(legendLine);
        legendTextBody.getXmlObject().addNewBodyPr();
        legendTextBody.addNewParagraph().getDefaultRunProperties().setFontSize(6.0);
        legendLine.setTextBody(legendTextBody);

        // 5、X轴(分类轴)相关设置
        XDDFCategoryAxis xAxis = chart.createCategoryAxis(AxisPosition.BOTTOM); // 创建X轴,并且指定位置
        xAxis.setTickLabelPosition(AxisTickLabelPosition.LOW); // 设置X周的文字一直在最下方
        xAxis.getOrAddTextProperties().setFontSize(fontSize);

        // 字体颜色
        XDDFSolidFillProperties fontColor = new XDDFSolidFillProperties(XDDFColor.from(new byte[] {(byte)96,(byte)98,(byte)102}));
        // 标签样式
        XDDFRunProperties xTextProperties = xAxis.getOrAddTextProperties();
        xTextProperties.setFillProperties(fontColor);
        //x轴数据
        String[] xAxisDataLine = JSONObject.parseObject(content.getJSONArray("xAxis").toString(), String[].class);
        // Y轴
        XDDFValueAxis yAxis = chart.createValueAxis(AxisPosition.LEFT);

        // 标签样式
        XDDFRunProperties yTextProperties = yAxis.getOrAddTextProperties();
        yTextProperties.setFillProperties(fontColor);
        yTextProperties.setFontSize(fontSize);
        // 网格线
        XDDFShapeProperties yGridProperties = yAxis.getOrAddMajorGridProperties();
        XDDFLineProperties yGridLine = new XDDFLineProperties();
        yGridLine.setFillProperties(new XDDFSolidFillProperties(XDDFColor.from(new byte[] {(byte)228,(byte)231,(byte)237})));
        yGridLine.setWidth(0.5);
        yGridProperties.setLineProperties(yGridLine);

        XDDFRadarChartData radar = (XDDFRadarChartData) chart.createData(ChartTypes.RADAR, xAxis, yAxis);
        radar.setStyle(RadarStyle.MARKER);

        XDDFCategoryDataSource xAxisSource = XDDFDataSourcesFactory.fromArray(xAxisDataLine);

        //获取y轴数据
        JSONArray dataArray = content.getJSONArray("yAxis");
        JSONArray nameArray = content.getJSONArray("nameList");
        // 设置y轴的取值范围
        yAxis.setMinimum(calMin(dataArray));
        yAxis.setMaximum(calMax(dataArray));
//        yAxis.setMinimum(0d);
//        yAxis.setMaximum(1d);
        CTRadarChart ctRadarChart = chart.getCTChart().getPlotArea().getRadarChartArray(0);
        for(int i=0,size=dataArray.size();i<size;i++){
            Double[] yAxisData = JSONObject.parseObject(dataArray.getJSONArray(i).toString(), Double[].class);
            String name = nameArray.getString(i);
            XDDFNumericalDataSource<Double> yAxisSource = XDDFDataSourcesFactory.fromArray(yAxisData); // 设置Y轴数据
            XDDFRadarChartData.Series series = (XDDFRadarChartData.Series) radar.addSeries(xAxisSource, yAxisSource);

            XDDFLineProperties lineProperties = new XDDFLineProperties();
            byte[] color = new byte[] {(byte) (Math.random()*255),(byte)(Math.random()*255),(byte)(Math.random()*255)};
            XDDFSolidFillProperties seriesProperties = new XDDFSolidFillProperties(XDDFColor.from(color));
            lineProperties.setWidth(1.5);
            lineProperties.setFillProperties(seriesProperties);
            series.setLineProperties(lineProperties);
            //设置每条线的名字
            series.setTitle(name,null);


            CTRadarSer ser = ctRadarChart.getSerArray(i);
//            CTDLbls ctdLbls = ser.addNewDLbls();
//            ctdLbls.addNewShowCatName().setVal(false);// 是否展示对应x轴上的值（类型名称）
//            ctdLbls.addNewShowVal().setVal(true);// 是否展示数值
//            ctdLbls.addNewShowSerName().setVal(false);// 是否展示归属折线名称（系列名称）
//            ctdLbls.addNewShowLegendKey().setVal(false);// 是否展示图例（图例项标示）
//            ctdLbls.addNewDLblPos().setVal(STDLblPos.IN_END);//数据标签

            CTMarker marker = ser.addNewMarker();
            setMarkerColor(marker, color, null);
        }
        chart.plot(radar);
    }

    /**
     * 	设置标记点颜色
     */
    private static void setMarkerColor(CTMarker marker, byte[] color, Integer type) {
        if (type == null || type == 1){
            //圆形点状
            marker.addNewSymbol().setVal(STMarkerStyle.STAR);
        }else {
            //无形状
            marker.addNewSymbol().setVal(STMarkerStyle.NONE);
        }
        CTShapeProperties shapeProperties = marker.addNewSpPr();
        // 边框颜色
        CTLineProperties borderProperties = shapeProperties.addNewLn();
        CTSolidColorFillProperties borderColor = borderProperties.addNewSolidFill();
        borderColor.addNewSrgbClr().setVal(color);
        // 填充颜色
        CTSolidColorFillProperties fillProperties = shapeProperties.addNewSolidFill();
        fillProperties.addNewSrgbClr().setVal(color);
    }

    public static void addRow(XWPFDocument document, Integer count){
        try{
            XWPFParagraph p = document.createParagraph();
            XWPFRun r = p.createRun();
            // 增加换行
            if (count == null){
                count = 1;
            }
            for(int i=0;i<count;i++){
                r.addCarriageReturn();
            }
        }catch (Exception e){
            e.printStackTrace();
//            return null;
        }
    }

    /**
     * 计算列表中的最小值
     * @param data [[]]
     * @return
     */
    public static double calMin(JSONArray data) {

        double result=1.2d;
        if (data.size() != 0) {
            for (int i=0,length = data.size();i < length;i++) {
                JSONArray dataArray = data.getJSONArray(i);
                if (dataArray.size() != 0){
                    for (int j = 0, size = dataArray.size(); j < size; j++) {
                        if (dataArray.getDouble(j) != null) {
                            result = Math.min(result, dataArray.getDouble(j));
                        }
                    }
                }
            }
        }

        return (result == 1.2 || result < 0.1)?0:Math.round(result*100 - 5)/100d;

    }

    /**
     * 计算列表中的最大值
     * @param data [[],[]]
     * @return
     */
    public static double calMax(JSONArray data) {

        double result=-1d;
        if (data.size() != 0) {
            for (int i=0,length = data.size();i < length;i++) {
                JSONArray dataArray = data.getJSONArray(i);
                if (dataArray.size() != 0){
                    for (int j = 0, size = dataArray.size(); j < size; j++) {
                        if (dataArray.getDouble(j) != null) {
                            result = Math.max(result, dataArray.getDouble(j));
                        }
                    }
                }
            }
        }

        return (result == -1d || Math.round(result*100+5)/100d > 1)?1:Math.round(result*100+5)/100d;

    }

    /**
     * 合并单元格
     */
    public static void mergeCellsVertically(XWPFTable table, int col, int fromRow, int toRow) {
        for (int rowIndex = fromRow; rowIndex <= toRow; rowIndex++) {
            XWPFTableCell cell = table.getRow(rowIndex).getCell(col);
            if (rowIndex == fromRow)
                cell.getCTTc().addNewTcPr().addNewVMerge().setVal(STMerge.RESTART);
            else
                cell.getCTTc().addNewTcPr().addNewVMerge().setVal(STMerge.CONTINUE);
        }
    }

    public static void mergeCellsHorizontal(XWPFTable table, int row, int colFrom, int colTo) {
        for (int col = colFrom; col <= colTo; col++) {
            XWPFTableCell cell = table.getRow(row).getCell(col);
            if (col == colFrom) {
                cell.getCTTc().addNewTcPr().addNewHMerge().setVal(STMerge.RESTART);
            } else {
                cell.getCTTc().addNewTcPr().addNewHMerge().setVal(STMerge.CONTINUE);
            }
        }
    }

    /**
     * 设置表格单元格水平位置和垂直位置
     */
    public static void setCellLocation(XWPFTable xwpfTable, String vecticalLocation, String horizontalLocation){
        List<XWPFTableRow> rows = xwpfTable.getRows();
        for (XWPFTableRow row : rows){
            List<XWPFTableCell> cells = row.getTableCells();
                for(XWPFTableCell cell : cells){
                    CTTc cttc = cell.getCTTc();
                    CTP ctp = cttc.getPList().get(0);
                    CTPPr ctppr = ctp.getPPr();
                    if(ctppr == null){
                        ctppr = ctp.addNewPPr();
                    }
                    CTJc ctjc = ctppr.getJc();
                    if(ctjc == null){
                        ctjc = ctppr.addNewJc();
                    }
                    ctjc.setVal(STJc.Enum.forString(horizontalLocation));
                    cell.setVerticalAlignment(XWPFTableCell.XWPFVertAlign.valueOf(vecticalLocation));
                }
        }
    }

    /**
     * 插入整星及分系统评估情况表
     */
    public static void insertTotalStatusTable(XWPFDocument document, List<List<String>> subSystemInput, List<String> satInput, List<String> note) {
        XWPFTable table = document.createTable();
        int columnNum = 10;
        int rowNum = 5;
        List<Integer> listwidth = Arrays.asList(950, 950, 950, 950, 950, 950, 950, 950, 950, 1150);
        List<String> Project = Arrays.asList("", "常驻故障部件", "权重", "分值", "健康");
        //首个单元格设置
        XWPFTableRow row0 = table.getRow(0);
        XWPFTableCell cell00 = row0.getCell(0);
        CTTcPr tcPr00 = cell00.getCTTc().addNewTcPr();
        CTTblWidth cellw00 = tcPr00.addNewTcW();
        cellw00.setW(BigInteger.valueOf(listwidth.get(0)));
        XWPFParagraph xwpfParagraph00 = cell00.getParagraphArray(0);
        XWPFRun run00 = xwpfParagraph00.createRun();
        run00.setFontFamily("宋体");
        run00.setFontSize(11);
        run00.setBold(true);
        xwpfParagraph00.setAlignment(ParagraphAlignment.CENTER);
        cell00.setVerticalAlignment(XWPFTableCell.XWPFVertAlign.CENTER);
        run00.setText("项目");

        for (int j = 1; j < columnNum; j++) {
            XWPFTableCell cell0 = row0.addNewTableCell();
            CTTcPr tcPr0 = cell0.getCTTc().addNewTcPr();
            CTTblWidth cellw0 = tcPr0.addNewTcW();
            cellw0.setW(BigInteger.valueOf(listwidth.get(j)));
            XWPFParagraph xwpfParagraph0 = cell0.getParagraphArray(0);
            XWPFRun run0 = xwpfParagraph0.createRun();
            run0.setFontFamily("宋体");
            run0.setFontSize(11);
            run0.setBold(true);
            xwpfParagraph0.setAlignment(ParagraphAlignment.CENTER);
            cell0.setVerticalAlignment(XWPFTableCell.XWPFVertAlign.CENTER);
            if (j == 1)
                run0.setText("分系统健康评估");
            else if (j < 8)
                run0.setText("");
            else if (j == 8)
                run0.setText("整星健康评估");
            else
                run0.setText("备注");
        }
        for (int i = 0; i < rowNum; i++) {
            XWPFTableRow row = table.createRow();
            XWPFTableCell tableCell1 = row.getCell(0);
//            XWPFRun run1 = tableCell1.getParagraphArray(0).createRun();
//            run1.setFontSize(11);
            for (int j = 0; j < columnNum; j++) {
                XWPFTableCell tableCell = row.getCell(j);
                CTTcPr ctTcPr = tableCell.getCTTc().addNewTcPr();
                CTTblWidth ctTblWidth = ctTcPr.addNewTcW();
                ctTblWidth.setW(BigInteger.valueOf(listwidth.get(j)));
                ctTblWidth.setType(STTblWidth.DXA);
                XWPFParagraph p = tableCell.getParagraphArray(0);
                XWPFRun run = p.createRun();
                if (j == 0){
                    run.setText(Project.get(i));
                }
                else if (j < columnNum - 2){
                    run.setText(subSystemInput.get(i).get(j - 1));
                }
                else if (j == columnNum - 2)
                    run.setText(satInput.get(i));
                else
                    run.setText(note.get(i));
                run.setFontSize(11);
                if(i==0)
                    run.setBold(true);
                else
                    run.setBold(false);
                p.setAlignment(ParagraphAlignment.CENTER);
                tableCell1.setVerticalAlignment(XWPFTableCell.XWPFVertAlign.CENTER);
            }
        }
        //合并单元格
        mergeCellsHorizontal(table, 0, 1, 7);
        mergeCellsVertically(table, 0, 0, 1);
        mergeCellsVertically(table, 8, 0, 1);
        mergeCellsVertically(table, 9, 0, 1);
        mergeCellsVertically(table, 9, 3, 5);
        setCellLocation(table,"CENTER","center");
    }


    /**
     * 插入分系统器部件评估情况表
     */
    public static void insertSysStatusTable(XWPFDocument document, List<List<String>> input){
        XWPFTable table = document.createTable();
        List<String> title = new ArrayList<>();
        title.add("特征参数");
        title.add("");
        title.add("");
        title.add("正常值范围");
        title.add("评估指标参考门限");
        title.add("评估时段数据范围");
        title.add("评估值");
        title.add("正常值判读");
        title.add("趋势判读");
        title.add("备注");
        title.add("单位");
        int columnNum = 11;
        int rowNum = input.size()+1;
        List<Integer> listwidth = Arrays.asList(750,770,770,770,770,770,770,770,770,770,400);
        //首个单元格设置
        XWPFTableRow row0 = table.getRow(0);
        XWPFTableCell cell00 = row0.getCell(0);
        CTTcPr tcPr00 = cell00.getCTTc().addNewTcPr();
        CTTblWidth cellw00 = tcPr00.addNewTcW();
        cellw00.setW(BigInteger.valueOf(listwidth.get(0)));
        XWPFParagraph xwpfParagraph00 = cell00.getParagraphArray(0);
        XWPFRun run00 = xwpfParagraph00.createRun();
        run00.setFontFamily("仿宋");
        run00.setFontSize(10);
        run00.setBold(true);
        xwpfParagraph00.setAlignment(ParagraphAlignment.CENTER);
        cell00.setVerticalAlignment(XWPFTableCell.XWPFVertAlign.CENTER);
        run00.setText(title.get(0));
        //首行
        for (int j = 1; j < columnNum; j++) {
            XWPFTableCell cell0 = row0.addNewTableCell();
            CTTcPr tcPr0 = cell0.getCTTc().addNewTcPr();
            CTTblWidth cellw0 = tcPr0.addNewTcW();
            cellw0.setW(BigInteger.valueOf(listwidth.get(j)));
            XWPFParagraph xwpfParagraph0 = cell0.getParagraphArray(0);
            XWPFRun run0 = xwpfParagraph0.createRun();
            run0.setFontFamily("仿宋");
            run0.setFontSize(10);
            run0.setBold(true);
            xwpfParagraph0.setAlignment(ParagraphAlignment.CENTER);
            cell0.setVerticalAlignment(XWPFTableCell.XWPFVertAlign.CENTER);
            run0.setText(title.get(j));
        }
        title.set(0,"器部件");
        title.set(1,"遥测代号");
        title.set(2,"计算方法");
        title.set(3,"");
        title.set(4,"");
        title.set(5,"");
        title.set(6,"");
        title.set(7,"");
        title.set(8,"");
        title.set(9,"");
        title.set(10,"");
        for (int i = 0; i < rowNum; i++) {
            XWPFTableRow row = table.createRow();
            XWPFTableCell tableCell1 = row.getCell(0);
            XWPFRun run1 = tableCell1.getParagraphArray(0).createRun();
            run1.setFontSize(10);
            for (int j = 0; j < columnNum; j++) {
                XWPFTableCell tableCell = row.getCell(j);
                CTTcPr ctTcPr = tableCell.getCTTc().addNewTcPr();
                CTTblWidth ctTblWidth = ctTcPr.addNewTcW();
                ctTblWidth.setW(BigInteger.valueOf(listwidth.get(j)));
                ctTblWidth.setType(STTblWidth.DXA);
                XWPFParagraph p = tableCell.getParagraphArray(0);
                XWPFRun run = p.createRun();
                if(i==0){
                    run.setText(title.get(j));
                    run.setFontFamily("仿宋");
                    run.setFontSize(10);
                    run.setBold(true);
                }
                else{
                    run.setText(input.get(i-1).get(j));
                    if (j == 8 && !input.get(i-1).get(j).equals("平稳"))
                        run.setBold(true);
                }

                run.setFontSize(10);
                p.setAlignment(ParagraphAlignment.CENTER);
                tableCell1.setVerticalAlignment(XWPFTableCell.XWPFVertAlign.CENTER);
            }
        }
        mergeCellsHorizontal(table, 0, 0, 2);
//        mergeCellsHorizontal(table, 1, 0, 1);
        for(int i = 3; i < 11; i++)
            mergeCellsVertically(table,i,0,1);
        mergeColumnSame(table, 0);
        setCellLocation(table,"CENTER","center");
    }

    /**
     * 插入器部件趋势分析表
     */
    public static void insertComStatusTable(XWPFDocument document, List<List<String>> input_sum){
        XWPFTable table = document.createTable();
        List<String> title = new ArrayList<>();
        title.add("");
        title.add("物理值一级门限检测");
        title.add("");
        title.add("评估值趋势");
        title.add("");
        title.add("");
        title.add("");
        int columnNum = 7;
        int rowNum = 1+input_sum.size();
        List<Integer> listwidth = Arrays.asList(1200,1200,1200,1200,1200,1200,1200);
        //首个单元格设置
        XWPFTableRow row0 = table.getRow(0);
        XWPFTableCell cell00 = row0.getCell(0);
        CTTcPr tcPr00 = cell00.getCTTc().addNewTcPr();
        CTTblWidth cellw00 = tcPr00.addNewTcW();
        cellw00.setW(BigInteger.valueOf(listwidth.get(0)));
        XWPFParagraph xwpfParagraph00 = cell00.getParagraphArray(0);
        XWPFRun run00 = xwpfParagraph00.createRun();
        run00.setFontFamily("黑体");
        run00.setFontSize(10);
        run00.setBold(true);
        xwpfParagraph00.setAlignment(ParagraphAlignment.CENTER);
        cell00.setVerticalAlignment(XWPFTableCell.XWPFVertAlign.CENTER);
        run00.setText(title.get(0));
        //首行
        for (int j = 1; j < columnNum; j++) {
            XWPFTableCell cell0 = row0.addNewTableCell();
            CTTcPr tcPr0 = cell0.getCTTc().addNewTcPr();
            CTTblWidth cellw0 = tcPr0.addNewTcW();
            cellw0.setW(BigInteger.valueOf(listwidth.get(j)));
            XWPFParagraph xwpfParagraph0 = cell0.getParagraphArray(0);
            XWPFRun run0 = xwpfParagraph0.createRun();
            run0.setFontFamily("黑体");
            run0.setFontSize(10);
            run0.setBold(true);
            xwpfParagraph0.setAlignment(ParagraphAlignment.CENTER);
            cell0.setVerticalAlignment(XWPFTableCell.XWPFVertAlign.CENTER);
            run0.setText(title.get(j));
        }
        title.set(0,"评估指标名称");
        title.set(1,"门限范围");
        title.set(2,"是否符合门限范围");
        title.set(3,"评估指标计算方法");
        title.set(4,"评估指标值变化时间段");
        if(input_sum.size()==1){
            title.set(5,input_sum.get(0).get(3)+"变化");
        }
        else{
            title.set(5,"变化量");
        }
        title.set(6,"评估指标值变化趋势");
        for (int i = 0; i < rowNum; i++) {
            XWPFTableRow row = table.createRow();
            XWPFTableCell tableCell1 = row.getCell(0);
            XWPFRun run1 = tableCell1.getParagraphArray(0).createRun();
            run1.setFontSize(10);
            for (int j = 0; j < columnNum; j++) {
                XWPFTableCell tableCell = row.getCell(j);
                CTTcPr ctTcPr = tableCell.getCTTc().addNewTcPr();
                CTTblWidth ctTblWidth = ctTcPr.addNewTcW();
                ctTblWidth.setW(BigInteger.valueOf(listwidth.get(j)));
                ctTblWidth.setType(STTblWidth.DXA);
                XWPFParagraph p = tableCell.getParagraphArray(0);
                XWPFRun run = p.createRun();
                if(i==0)
                {
                    run.setText(title.get(j));
                    run.setFontFamily("黑体");
                    run.setBold(true);
                }
                else
                {
                    run.setText(input_sum.get(i-1).get(j));
                    run.setFontFamily("宋体");
                    run.setFontFamily("Times New Roman");
                }
                run.setFontSize(10);
                p.setAlignment(ParagraphAlignment.CENTER);
                tableCell1.setVerticalAlignment(XWPFTableCell.XWPFVertAlign.CENTER);
            }
        }

        mergeCellsHorizontal(table, 0, 1, 2);
        mergeCellsHorizontal(table, 0, 3, 6);
        setCellLocation(table,"CENTER","center");
    }

    /**
     * 插入健康状态评估数据表
     */
    public static void insertAssessStatusTable(XWPFDocument document, List<List<String>> input){
        List<Integer> listwidth = Arrays.asList(700,700,700,700,700,700,700,700,700,700,700,700);
        XWPFTable table = document.createTable();
        List<String> title1 = new ArrayList<>();
        title1.add("");
        title1.add("分系统（二级指标）状态评估值");
        title1.add("");
        title1.add("分系统器部件（三级指标）状态评估值");
        title1.add("");
        title1.add("");
        title1.add("");
        title1.add("");
        title1.add("");
        title1.add("");
        title1.add("");
        title1.add("");
        int columnNum = 12;
        int rowNum = input.size()+1;
        List<String> title2 = new ArrayList<>();
        title2.add("整星状态评估值");
        title2.add("分系统名称（权值）");
        title2.add("状态评估值");
        title2.add("器部件名称");
        title2.add("遥测代号");
        title2.add("正常值范围");
        title2.add("正常值判读");
        title2.add("评估指标计算方法");
        title2.add("评估指标参考门限");
        title2.add("评估指标值变化时间段");
        title2.add("评估指标值变化时段数值");
        title2.add("评估指标值变化趋势");
        //首个单元格设置
        XWPFTableRow row0 = table.getRow(0);
        XWPFTableCell cell00 = row0.getCell(0);
        CTTcPr tcPr00 = cell00.getCTTc().addNewTcPr();
        CTTblWidth cellw00 = tcPr00.addNewTcW();
        cellw00.setW(BigInteger.valueOf(listwidth.get(0)));
        XWPFParagraph xwpfParagraph00 = cell00.getParagraphArray(0);
        XWPFRun run00 = xwpfParagraph00.createRun();
        run00.setFontFamily("黑体");
        run00.setFontSize(10);
        run00.setBold(true);
        xwpfParagraph00.setAlignment(ParagraphAlignment.CENTER);
        cell00.setVerticalAlignment(XWPFTableCell.XWPFVertAlign.CENTER);
        run00.setText(title1.get(0));
        //首行
        for (int j = 1; j < columnNum; j++) {
            XWPFTableCell cell0 = row0.addNewTableCell();
            CTTcPr tcPr0 = cell0.getCTTc().addNewTcPr();
            CTTblWidth cellw0 = tcPr0.addNewTcW();
            cellw0.setW(BigInteger.valueOf(listwidth.get(j)));
            XWPFParagraph xwpfParagraph0 = cell0.getParagraphArray(0);
            XWPFRun run0 = xwpfParagraph0.createRun();
            run0.setFontFamily("黑体");
            run0.setFontSize(10);
            run0.setBold(true);
            xwpfParagraph0.setAlignment(ParagraphAlignment.CENTER);
            cell0.setVerticalAlignment(XWPFTableCell.XWPFVertAlign.CENTER);
            run0.setText(title1.get(j));
        }

        for (int i = 0; i < rowNum; i++) {
            XWPFTableRow row = table.createRow();
            XWPFTableCell tableCell1 = row.getCell(0);
            XWPFRun run1 = tableCell1.getParagraphArray(0).createRun();
            run1.setFontSize(10);
            for (int j = 0; j < columnNum; j++) {
                XWPFTableCell tableCell = row.getCell(j);
                CTTcPr ctTcPr = tableCell.getCTTc().addNewTcPr();
                CTTblWidth ctTblWidth = ctTcPr.addNewTcW();
                ctTblWidth.setW(BigInteger.valueOf(listwidth.get(j)));
                ctTblWidth.setType(STTblWidth.DXA);
                XWPFParagraph p = tableCell.getParagraphArray(0);
                XWPFRun run = p.createRun();
                if(i==0)
                {
                    run.setText(title2.get(j));
                    run.setFontFamily("黑体");
                }
                else
                    run.setText(input.get(i-1).get(j));
                run.setFontSize(10);
                p.setAlignment(ParagraphAlignment.CENTER);
                tableCell1.setVerticalAlignment(XWPFTableCell.XWPFVertAlign.CENTER);
            }
        }

        mergeColumnSame(table, 0);
        mergeColumnSame(table, 1);
        mergeColumnSame(table, 2);
        mergeColumnSame(table, 3);
        mergeCellsHorizontal(table,0,1,2);
        mergeCellsHorizontal(table,0,3,11);
        setCellLocation(table,"CENTER","center");

    }

    /**
     * 插入性能状态评估数据汇总表
     */
    public static void insertPerformanceStatusTable(XWPFDocument document, List<List<String>> input, Integer maxLevel){
        List<Integer> listwidth = new ArrayList<>();
        List<String> title1 = new ArrayList<>();
        List<String> title2 = new ArrayList<>();
        for(int i=0;i<maxLevel + 4;i++){
            listwidth.add(8400/(maxLevel+4));
            if(i<maxLevel){
                if(i>0){
                    title1.add("效能指标名称");
                    title2.add(switchNumber(i+1)+"级指标");
                }
                else{
                    title1.add("能力名称"+"(一级指标)");
                    title2.add("能力名称"+"(一级指标)");
                }
            }
        }
        title1.add("效能指标名称");
        title1.add("效能指标名称");
        title1.add("效能指标名称");
        title1.add("效能指标名称");
        title2.add("基准段属性值范围");
        title2.add("评估段属性值范围");
        title2.add("评估段指标值范围");
        title2.add("评估段指标值均值");
        XWPFTable table = document.createTable();
        //首个单元格设置
        XWPFTableRow row0 = table.getRow(0);
        XWPFTableCell cell00 = row0.getCell(0);
        CTTcPr tcPr00 = cell00.getCTTc().addNewTcPr();
        CTTblWidth cellw00 = tcPr00.addNewTcW();
        cellw00.setW(BigInteger.valueOf(listwidth.get(0)));
        XWPFParagraph xwpfParagraph00 = cell00.getParagraphArray(0);
        XWPFRun run00 = xwpfParagraph00.createRun();
        run00.setFontFamily("黑体");
        run00.setFontSize(10);
        run00.setBold(true);
        xwpfParagraph00.setAlignment(ParagraphAlignment.CENTER);
        cell00.setVerticalAlignment(XWPFTableCell.XWPFVertAlign.CENTER);
        run00.setText(title1.get(0));
        //首行
        for (int j = 1; j < maxLevel + 4; j++) {
            XWPFTableCell cell0 = row0.addNewTableCell();
            CTTcPr tcPr0 = cell0.getCTTc().addNewTcPr();
            CTTblWidth cellw0 = tcPr0.addNewTcW();
            cellw0.setW(BigInteger.valueOf(listwidth.get(j)));
            XWPFParagraph xwpfParagraph0 = cell0.getParagraphArray(0);
            XWPFRun run0 = xwpfParagraph0.createRun();
            run0.setFontFamily("黑体");
            run0.setFontSize(10);
            run0.setBold(true);
            xwpfParagraph0.setAlignment(ParagraphAlignment.CENTER);
            cell0.setVerticalAlignment(XWPFTableCell.XWPFVertAlign.CENTER);
            run0.setText(title1.get(j));
        }
        for (int i = 0; i < input.size()+1; i++) {
            XWPFTableRow row = table.createRow();
            XWPFTableCell tableCell1 = row.getCell(0);
            XWPFRun run1 = tableCell1.getParagraphArray(0).createRun();
            run1.setFontSize(10);
            for (int j = 0; j < maxLevel + 4; j++) {
                XWPFTableCell tableCell = row.getCell(j);
                CTTcPr ctTcPr = tableCell.getCTTc().addNewTcPr();
                CTTblWidth ctTblWidth = ctTcPr.addNewTcW();
                ctTblWidth.setW(BigInteger.valueOf(listwidth.get(j)));
                ctTblWidth.setType(STTblWidth.DXA);
                XWPFParagraph p = tableCell.getParagraphArray(0);
                XWPFRun run = p.createRun();
                if(i==0)
                {
                    run.setText(title2.get(j));
                    run.setFontFamily("黑体");
                }
                else{
                    if(j<maxLevel){
                        if(j < input.get(i-1).size()-4)
                            run.setText(input.get(i-1).get(j));
                        else
                            run.setText("/");
                    }
                    else
                        run.setText(input.get(i-1).get(j- maxLevel - 4 + input.get(i-1).size()));
                }
                run.setFontSize(10);
                p.setAlignment(ParagraphAlignment.CENTER);
                tableCell1.setVerticalAlignment(XWPFTableCell.XWPFVertAlign.CENTER);
            }
        }

        mergeColumnSame(table, 0);
        mergeColumnSame(table, 1);
        mergeColumnSame(table, 2);
        mergeCellsHorizontal(table,0,1,maxLevel + 3);
        setCellLocation(table,"CENTER","center");
    }

    /**
     * 插入单个性能状态评估数据汇总表
     */
    public static void insertSinglePerformanceStatusTable(XWPFDocument document, List<String> input){
        List<Integer> listwidth = Arrays.asList(1200,1200,1200,1200,1200,1200,1200);
        List<String> title = new ArrayList<>();
        title.add("效能指标名称");
        title.add("量化函数");
        title.add("评估指标类型");
        title.add("基准段属性值范围");
        title.add("评估时段属性值范围");
        title.add("评估时段指标值范围");
        title.add("评估时段指标值均值");
        XWPFTable table = document.createTable();
        //首个单元格设置
        XWPFTableRow row0 = table.getRow(0);
        XWPFTableCell cell00 = row0.getCell(0);
        CTTcPr tcPr00 = cell00.getCTTc().addNewTcPr();
        CTTblWidth cellw00 = tcPr00.addNewTcW();
        cellw00.setW(BigInteger.valueOf(listwidth.get(0)));
        XWPFParagraph xwpfParagraph00 = cell00.getParagraphArray(0);
        XWPFRun run00 = xwpfParagraph00.createRun();
        run00.setFontFamily("黑体");
        run00.setFontSize(10);
        run00.setBold(true);
        xwpfParagraph00.setAlignment(ParagraphAlignment.CENTER);
        cell00.setVerticalAlignment(XWPFTableCell.XWPFVertAlign.CENTER);
        run00.setText(title.get(0));
        //首行
        for (int j = 1; j < 7; j++) {
            XWPFTableCell cell0 = row0.addNewTableCell();
            CTTcPr tcPr0 = cell0.getCTTc().addNewTcPr();
            CTTblWidth cellw0 = tcPr0.addNewTcW();
            cellw0.setW(BigInteger.valueOf(listwidth.get(j)));
            XWPFParagraph xwpfParagraph0 = cell0.getParagraphArray(0);
            XWPFRun run0 = xwpfParagraph0.createRun();
            run0.setFontFamily("黑体");
            run0.setFontSize(10);
            run0.setBold(true);
            xwpfParagraph0.setAlignment(ParagraphAlignment.CENTER);
            cell0.setVerticalAlignment(XWPFTableCell.XWPFVertAlign.CENTER);
            run0.setText(title.get(j));
        }
        XWPFTableRow row = table.createRow();
        XWPFTableCell tableCell1 = row.getCell(0);
        XWPFRun run1 = tableCell1.getParagraphArray(0).createRun();
        run1.setFontSize(10);
        for (int j = 0; j < 7; j++) {
            XWPFTableCell tableCell = row.getCell(j);
            CTTcPr ctTcPr = tableCell.getCTTc().addNewTcPr();
            CTTblWidth ctTblWidth = ctTcPr.addNewTcW();
            ctTblWidth.setW(BigInteger.valueOf(listwidth.get(j)));
            ctTblWidth.setType(STTblWidth.DXA);
            XWPFParagraph p = tableCell.getParagraphArray(0);
            XWPFRun run = p.createRun();
            run.setText(input.get(j));
            run.setFontSize(10);
            p.setAlignment(ParagraphAlignment.CENTER);
            tableCell1.setVerticalAlignment(XWPFTableCell.XWPFVertAlign.CENTER);
        }
        setCellLocation(table,"CENTER","center");
    }

    public static String switchNumber(int input){
        switch(input){
            case 1:{
                return "一";
            }
            case 2:{
                return "二";
            }
            case 3:{
                return "三";
            }
            case 4:{
                return "四";
            }
            case 5:{
                return "五";
            }
            case 6:{
                return "六";
            }
            case 7:{
                return "七";
            }
            case 8:{
                return "八";
            }
            default:{
                return String.valueOf(input);
            }
        }
    }

    public static void insertTableCommon(XWPFDocument document,List<String> title,List<List<String>> input, List<Integer> listwidth, List<Integer> mergeC,  List<Tuple> mergeR){
        XWPFTable table = document.createTable();
        int rowNum = input.size();
        int columnNum = input.get(0).size();
//        List<Integer> listwidth = Arrays.asList(1500,950,1150,950,1150,950,950,950);
        XWPFTableRow row0 =table.getRow(0);
        XWPFTableCell cell00 =row0.getCell(0);

        CTTcPr tcPr00 = cell00.getCTTc().addNewTcPr();
        CTTblWidth cellw00 = tcPr00.addNewTcW();
        cellw00.setW(BigInteger.valueOf(listwidth.get(0)));
        XWPFParagraph xwpfParagraph00 =cell00.getParagraphArray(0);
        XWPFRun run00 =xwpfParagraph00.createRun();
        run00.setFontFamily("黑体");
        run00.setFontSize(10);
        run00.setBold(true);
        xwpfParagraph00.setAlignment(ParagraphAlignment.CENTER);
        cell00.setVerticalAlignment(XWPFTableCell.XWPFVertAlign.CENTER);
        run00.setText(title.get(0));

        for (int j=1;j<columnNum;j++){

            XWPFTableCell cell0 =row0.addNewTableCell();

            CTTcPr tcPr0 = cell0.getCTTc().addNewTcPr();
            CTTblWidth cellw0 = tcPr0.addNewTcW();
            cellw0.setW(BigInteger.valueOf(listwidth.get(j)));
            XWPFParagraph xwpfParagraph0 =cell0.getParagraphArray(0);
            XWPFRun run0 =xwpfParagraph0.createRun();
            run0.setFontFamily("黑体");
            run0.setFontSize(10);
            run0.setBold(true);
            xwpfParagraph0.setAlignment(ParagraphAlignment.CENTER);
            cell0.setVerticalAlignment(XWPFTableCell.XWPFVertAlign.CENTER);
            run0.setText(title.get(j));




        }
        for (int i=0;i < rowNum;i++){
            XWPFTableRow row = table.createRow();
            XWPFTableCell tableCell1 =row.getCell(0);
            XWPFRun run1 = tableCell1.addParagraph().createRun();
            run1.setFontSize(10);
            for (int j=0;j<columnNum;j++){
                XWPFTableCell tableCell =row.getCell(j);
                CTTcPr ctTcPr = tableCell.getCTTc().addNewTcPr();
                CTTblWidth ctTblWidth =ctTcPr.addNewTcW();
                ctTblWidth.setW(BigInteger.valueOf(listwidth.get(j)));
                ctTblWidth.setType(STTblWidth.DXA);
                XWPFParagraph p = tableCell.addParagraph();
                XWPFRun run = p.createRun();
                run.setText(input.get(i).get(j));
                run.setFontSize(10);
                p.setAlignment(ParagraphAlignment.CENTER);
                tableCell1.setVerticalAlignment(XWPFTableCell.XWPFVertAlign.CENTER);

            }
        }

        for(Integer mergeCIndex : mergeC){
            mergeColumnSame(table, mergeCIndex);
        }
        for(Tuple mergeRIndex : mergeR){
            mergeCellsHorizontal(table,mergeRIndex.get(0),mergeRIndex.get(1),mergeRIndex.get(2));
        }
        setCellLocation(table,"CENTER","center");

    }



    public static void insertTable(XWPFDocument document,List<String> title,List<List<String>> input, List<Integer> listwidth){
        XWPFTable table = document.createTable();
        int columnNum = input.size();
        int rowNum = input.get(0).size();
//        List<Integer> listwidth = Arrays.asList(1500,950,1150,950,1150,950,950,950);
        XWPFTableRow row0 =table.getRow(0);
        XWPFTableCell cell00 =row0.getCell(0);

        CTTcPr tcPr00 = cell00.getCTTc().addNewTcPr();
        CTTblWidth cellw00 = tcPr00.addNewTcW();
        cellw00.setW(BigInteger.valueOf(listwidth.get(0)));
        XWPFParagraph xwpfParagraph00 =cell00.getParagraphArray(0);
        XWPFRun run00 =xwpfParagraph00.createRun();
        run00.setFontFamily("黑体");
        run00.setFontSize(10);
        run00.setBold(true);
        xwpfParagraph00.setAlignment(ParagraphAlignment.CENTER);
        cell00.setVerticalAlignment(XWPFTableCell.XWPFVertAlign.CENTER);
        run00.setText(title.get(0));

//        List<String> titleList = title;
        for (int j=1;j<columnNum;j++){

            XWPFTableCell cell0 =row0.addNewTableCell();

            CTTcPr tcPr0 = cell0.getCTTc().addNewTcPr();
            CTTblWidth cellw0 = tcPr0.addNewTcW();
            cellw0.setW(BigInteger.valueOf(listwidth.get(j)));
            XWPFParagraph xwpfParagraph0 =cell0.getParagraphArray(0);
            XWPFRun run0 =xwpfParagraph0.createRun();
            run0.setFontFamily("黑体");
            run0.setFontSize(10);
            run0.setBold(true);
            xwpfParagraph0.setAlignment(ParagraphAlignment.CENTER);
            cell0.setVerticalAlignment(XWPFTableCell.XWPFVertAlign.CENTER);
            run0.setText(title.get(j));




        }
        for (int i=0;i < rowNum;i++){
            XWPFTableRow row = table.createRow();
            XWPFTableCell tableCell1 =row.getCell(0);
            XWPFRun run1 = tableCell1.addParagraph().createRun();
//            run1.setText(String.valueOf(i+1));
            run1.setFontSize(10);
            for (int j=0;j<columnNum;j++){
//                XWPFTableCell tableCell =row.addNewTableCell();
                XWPFTableCell tableCell =row.getCell(j);
                CTTcPr ctTcPr = tableCell.getCTTc().addNewTcPr();
                CTTblWidth ctTblWidth =ctTcPr.addNewTcW();
//                if (j ==0){
//                    ctTblWidth.setW(BigInteger.valueOf(1500));
//                }else if(j == 2 || j == 4){
//                    ctTblWidth.setW(BigInteger.valueOf(1150));
//                }
//                else {
//                    ctTblWidth.setW(BigInteger.valueOf(950));
//                }
                ctTblWidth.setW(BigInteger.valueOf(listwidth.get(j)));
                ctTblWidth.setType(STTblWidth.DXA);
                XWPFParagraph p = tableCell.addParagraph();
                XWPFRun run = p.createRun();
                run.setText(input.get(j).get(i));
                run.setFontSize(10);
                p.setAlignment(ParagraphAlignment.CENTER);
                tableCell1.setVerticalAlignment(XWPFTableCell.XWPFVertAlign.CENTER);

            }
        }



    }

    public  static  void mergeColumnSame(XWPFTable table, int col) {
        List<XWPFTableCell> cellsToMerge = new ArrayList<>();
        XWPFTableCell prevCell = null;
        for (XWPFTableRow row : table.getRows()) {
            XWPFTableCell cell = row.getCell(col);

            if (prevCell == null) {
                prevCell = cell;
                cellsToMerge.add(cell);
            } else if (cell.getText().equals(prevCell.getText())) {
                cellsToMerge.add(cell);
            } else {
                if (cellsToMerge.size() > 1) {
                    mergeCols(table, col, cellsToMerge);
                }
                cellsToMerge.clear();
                prevCell = cell;
                cellsToMerge.add(cell);
            }
        }
        if (cellsToMerge.size() > 1) {
            mergeCols(table, col, cellsToMerge);
        }
    }

    public static void mergeCols (XWPFTable table, int col, List<XWPFTableCell> cells) {
        for (int i = 1; i < cells.size(); i++) {
            XWPFTableCell cell = cells.get(i);
            cell.getCTTc().addNewTcPr().addNewVMerge().setVal(STMerge.CONTINUE);
        }
        cells.get(0).getCTTc().addNewTcPr().addNewVMerge().setVal(STMerge.RESTART);
    }
    //格式修改：單元格行合并
    public static void mergeCell(XWPFTable table, int rowIdx, int fromColIdx, int toColIdx) {
        XWPFTableRow row = table.getRow(rowIdx);
        System.out.println(table.getRows().size());
        System.out.println(table.getRow(0).getTableCells().size());

        for (int colIdx = fromColIdx + 1; colIdx <= toColIdx; colIdx++) {
            XWPFTableCell cell = row.getCell(fromColIdx);
            cell.getCTTc().newCursor().removeXml();
        }

        XWPFTableCell firstCell = row.getCell(fromColIdx);
        firstCell.getCTTc().addNewTcPr().addNewHMerge().setVal(STMerge.RESTART);

        XWPFTableCell lastCell = row.getCell(toColIdx);
        lastCell.getCTTc().addNewTcPr().addNewHMerge().setVal(STMerge.CONTINUE);
    }

    @Test
    public void test() throws Exception {
        JSONObject contentJson = new JSONObject();
        List<String> xList = new ArrayList<>();
        xList.add("x1");
        xList.add("x2");
        xList.add("x3");
        List<String> yTable = new ArrayList<>();
        yTable.add("y1");
        yTable.add("y2");
        yTable.add("y3");
        List<Double> yList1 = new ArrayList<>();
        yList1.add(1.0);
        yList1.add(0.999999996);
        yList1.add(0.999999999);
        List<Double> yList2 = new ArrayList<>();
        yList2.add(0.999999996);
        yList2.add(0.99999991);
        yList2.add(1.0);
        List<List<Double>> yList = new ArrayList<>();
        yList.add(yList1);
        yList.add(yList2);
        List<String> nameList = new ArrayList<>();
        nameList.add("name1");
        nameList.add("name2");
        contentJson.put("xAxis", xList);
        contentJson.put("yAxis", yList);
        contentJson.put("nameList", nameList);


        XWPFDocument document = new XWPFDocument();
        OutputStream stream = null;
        BufferedOutputStream bufferStream = null;
        stream = new FileOutputStream(new File("D:\\test.docx"));
//        bufferStream = new BufferedOutputStream(stream, 1024);
        String content = "测试写入word文档！";
//        writeToDoc(document, content, 1, "center", null);
//        writeToDoc(document, content, 2, "left", null);
        XWPFRun r = document.createParagraph().createRun();
//        insertLineChart(r, document, "测试图表", contentJson);
//        insertLineChart(r, document, "测试图表", contentJson);
//        insertLineChart(r, document, "测试图表", contentJson);
        insertLineChart(document, "测试图表", contentJson);

//        insertRadarChart(document, "测试雷达图", contentJson);
        Date startTime = DateTime.now();
        Date endTime = DateTime.of("2023-07-01", "yyyy-MM-dd");
//        generateFirstPage(document, startTime, endTime, startTime);
        writeToDoc(document, content, 1, "center", null);
//        XWPFRun r = document.createParagraph().createRun();
//        document.createChart(r, 5, 3);
//        document.createChart(r, 5, 3);
//        document.createChart(r, 5, 3);
        writeToDoc(document, "1111111111", 4, "left", null);
        List<List<String>> input = new ArrayList<>();
        input.add(xList);
        input.add(yTable);
//        insertTable(document, xList, input);
        document.write(stream);
        stream.close();
//        bufferStream.close();
    }
}
