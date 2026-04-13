package com.ruoyi.common.core.utils;

import com.alibaba.fastjson2.JSONArray;
import org.junit.Test;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class CommonUtil {

    public static void delFile(File index) {
        if (index.isDirectory()) {
            File[] files = index.listFiles();
            for (File file : files) {
                delFile(file);
            }
        }
        index.delete();
    }


    /**
     * jsonArray转List
     * @param jsonArray
     * @return
     */
    public static List<Double> jsonArrayToList(JSONArray jsonArray){
        List<Double> valueList = new ArrayList<>();
        for (int i=0;i < jsonArray.size();i++){
            valueList.add(jsonArray.getDouble(i));
        }
        return valueList;
    }

    public static List<Integer> jsonArrayToIntList(JSONArray jsonArray) {
        List<Integer> valueList = new ArrayList<>();
        for (int i = 0, length = jsonArray.size(); i < length; i++) {
            valueList.add(jsonArray.getInteger(i));
        }
        return valueList;
    }

    /**
     * 列表转置
     *
     * @param valueList
     */
    public static List<List<Double>> listTrans(List<JSONArray> valueList) {
        int column = valueList.size();
        int row = valueList.get(0).size();
        List<List<Double>> columnList = new ArrayList<>();
        for (int i = 0; i < row; i++) {
            List<Double> rowList = new ArrayList<>();
            for (int j = 0; j < column; j++) {
                rowList.add(valueList.get(j).getDouble(i));
            }
            columnList.add(rowList);
        }
        return columnList;
    }

    /**
     * 列表转置
     *
     * @param valueList
     * @return
     */
    public static List<List<Double>> listTrans2(List<List<Double>> valueList) {
        int column = valueList.size();
        int row = valueList.get(0).size();
        List<List<Double>> columnList = new ArrayList<>();
        for (int i = 0; i < row; i++) {
            List<Double> rowList = new ArrayList<>();
            for (int j = 0; j < column; j++) {
                rowList.add(valueList.get(j).get(i));
            }
            columnList.add(rowList);
        }
        return columnList;
    }

    /**
     * 计算两个时间点之间的天数列表
     * @param startTime 2023-01-01 00:00:00
     * @param endTime 2023-01-02 23:59:59
     * @return [2023-01-01, 2023-01-02]
     */
    public static List<String> calDays(String startTime, String endTime){
        try {
            SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

            Date start_d = formatter.parse(startTime);
            Date end_d = formatter.parse(endTime);
            List<String> dayList = new ArrayList<>();

            long stamp_start = start_d.getTime() / 1000L;
            long stamp_end = end_d.getTime() / 1000L;
            long days = (stamp_end + 1 - stamp_start) / 86400;

            for (int i = 0; i < days; i++) {
                dayList.add(startTime.split(" ")[0]);
                Date tmp_start_d = new Date();
                long tmp_start = stamp_start + (i + 1) * 86400L;
                tmp_start_d.setTime(tmp_start * 1000L);
                startTime = formatter.format(tmp_start_d);
            }
            return dayList;
        }catch (Exception e){
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 计算两个时间点之间的天数列表
     * @param startTime 2023-01-01 00:00:00
     * @param endTime 2023-01-02 23:59:59
     * @return [2023-01-01 00:00:00, 2023-01-02 00:00:00]
     */
    public static List<String> calDays2(String startTime, String endTime){
        try {
            SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

            Date start_d = formatter.parse(startTime);
            Date end_d = formatter.parse(endTime);
            List<String> dayList = new ArrayList<>();

            long stamp_start = start_d.getTime() / 1000L;
            long stamp_end = end_d.getTime() / 1000L;
            long days = (stamp_end + 1 - stamp_start) / 86400;

            for (int i = 0; i < days; i++) {
                dayList.add(startTime);
                Date tmp_start_d = new Date();
                long tmp_start = stamp_start + (i + 1) * 86400L;
                tmp_start_d.setTime(tmp_start * 1000L);
                startTime = formatter.format(tmp_start_d);
            }
            return dayList;
        }catch (Exception e){
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 计算某个索引下的时间
     *
     * @param index
     * @param startTime
     * @return
     */
    public static String calDay(int index, String startTime) {
        try {
            SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

            Date start_d = formatter.parse(startTime);

            long stamp_start = start_d.getTime() / 1000L;
            return formatter.format((stamp_start + index * 86400L) * 1000L).split(" ")[0];
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static int calIndexByDay(String currentTime, String startTime) {
        try {
            SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

            Date start_d = formatter.parse(startTime);
            Date current_d = formatter.parse(currentTime);

            long stamp_start = start_d.getTime() / 1000L;
            long stamp_current = current_d.getTime() / 1000L;
            return (int) ((stamp_current-stamp_start + 1)/86400L);
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }


    /**
     * 3σ检测
     *
     * @param valueList
     * @return
     */
    public static List<List<String>> threeSigma(List<Double> valueList) {
        double average = valueList.parallelStream().collect(Collectors.averagingDouble(Double::doubleValue));
        ;//判断异常值方法，若异常，则输出
        double standardValue;
        List<String> indexList = new ArrayList<>();
        List<String> percentList = new ArrayList<>();
        List<List<String>> resultList = new ArrayList<>();
        if (average == 0) {
            standardValue = 0;
        } else {
            double sum = 0;
            int length = valueList.size();
            for (int i = 0; i < length; i++) {
                sum += Math.pow(valueList.get(i) - average, 2);
            }
            standardValue = Math.sqrt(sum / (length - 1));
        }
        for (int i = 0, length = valueList.size(); i < length; i++) {
            if (Math.abs(valueList.get(i) - average) > (5 * standardValue) && Math.abs(valueList.get(i) - average) > 0.05) {
                indexList.add(String.valueOf(i));
                percentList.add(String.valueOf(Math.round((valueList.get(i) - average) * 10000 / average) / 100));
            }
        }
        resultList.add(indexList);
        resultList.add(percentList);
        return resultList;
    }


    /**
     * 切分出有哪些遥测量
     * @param codes
     * @return
     */
    public static List<String> splitTelCode(String codes) {

        String[] tmpitems = codes.split("\\!|@|\\\\|\\||\\、|\\*|\\||&|\\（|\\）|\\(|\\)|\\+|\\/|\\,");
        List<String> items=new ArrayList<>();
        for (int i=0;i<tmpitems.length;i++) {
            if(!tmpitems[i].trim().equals("")) {
                if (Pattern.compile("(?i)[a-z]").matcher(tmpitems[i].trim()).find()) {  //如果包含字母，则认定为是遥测量
                    items.add(tmpitems[i].trim());
                }

            }
        }
        return items;
    }

    /**
     * 判断时间段是否有交集
     * @return
     */
    public static boolean adjustPeriodAnd(String startTime, String endTime, String anotherStartTime, String anotherEndTime){
        try {
            SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

            Date startDate = formatter.parse(startTime);
            Date endDate = formatter.parse(endTime);
            Date anoStartDate = formatter.parse(anotherStartTime);
            Date anoEndDate = formatter.parse(anotherEndTime);
            if (endDate.getTime() < anoStartDate.getTime() || anoEndDate.getTime() < startDate.getTime()){
                return false;
            }else return true;
        }catch (Exception e){
            e.printStackTrace();
            return false;
        }
    }

    public static List<String> extendPeriod(String startTime, String endTime, String anotherStartTime, String anotherEndTime){
        try {
            SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

            Date startDate = formatter.parse(startTime);
            Date endDate = formatter.parse(endTime);
            Date anoStartDate = formatter.parse(anotherStartTime);
            Date anoEndDate = formatter.parse(anotherEndTime);
            String finStartTime = startDate.getTime()>anoStartDate.getTime()?anotherStartTime:startTime;
            String finEndTime = endDate.getTime()>anoEndDate.getTime()?endTime:anotherEndTime;
            List<String> result = new ArrayList<>();
            result.add(finStartTime);
            result.add(finEndTime);
            return result;
        }catch (Exception e){
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 输入流转byte数组
     * @param inputStream
     * @return
     * @throws IOException
     */
    public static byte[] streamTransToBytes(InputStream inputStream) throws IOException {
//        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
//        byte[] bytes = new byte[1024];
//        int num;
//        while((num = inputStream.read(bytes)) != -1){
//            byteArrayOutputStream.write(bytes, 0, num);
//        }
//        byteArrayOutputStream.flush();
        byte[] bytes1 = new byte[inputStream.available()];
        inputStream.read(bytes1);;
        return bytes1;
    }

    /**
     * 字节数组转输入流
     * @param bytes
     * @return
     * @throws IOException
     */
    public static InputStream bytesTranToStream(byte[] bytes) throws IOException {
        return new ByteArrayInputStream(bytes);
    }


    /**
     * 二维数组生成CSV文件
     * @param dataList  [[],[]]
     * @param titleList  []
     * @param path  D:/..../test.csv
     * @return
     */
    public static void ToCsv(List<List<Double>> dataList, List<String> titleList, String path){
        try{
            File file = new File(path);
            FileWriter fileWriter = new FileWriter(file);
            BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
            for(int i=0,size=titleList.size();i<size;i++){
                bufferedWriter.write("\""+titleList.get(i)+"\"");
                if (i<size-1) {
                    bufferedWriter.write(",");
                }
            }
            bufferedWriter.newLine();
            for(int i=0, size=dataList.size();i<size;i++){

                for(int j=0,length=dataList.get(i).size();j<length;j++){
                    bufferedWriter.write(String.valueOf(dataList.get(i).get(j)));
                    if (j<length-1) {
                        bufferedWriter.write(",");
                    }
                }
                bufferedWriter.newLine();
            }
            bufferedWriter.close();
            fileWriter.close();
        }catch (Exception e){
            e.printStackTrace();
        }
    }


    /**
     * 二维数组生成CSV文件
     * @param dataList  [[],[]]
     * @param titleList  []
     * @param path  D:/..../test.csv
     * @return
     */
    public static void ToCsv2(List<List<Double>> dataList, List<String> titleList, String path){
        try{
            File file = new File(path);
            FileWriter fileWriter = new FileWriter(file);
            BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
            for(int i=0,size=titleList.size();i<size;i++){
                bufferedWriter.write("\""+titleList.get(i)+"\"");
                if (i<size-1) {
                    bufferedWriter.write(",");
                }
            }
            bufferedWriter.newLine();
            for(int i=0, size=dataList.size();i<size;i++){

                for(int j=0,length=dataList.get(i).size();j<length;j++){
                    if (j==length-1) {
                        Integer tmpvalue = dataList.get(i).get(j).intValue();
                        bufferedWriter.write(String.valueOf(tmpvalue));
                    } else {
                        bufferedWriter.write(String.valueOf(dataList.get(i).get(j)));
                    }
                    if (j<length-1) {
                        bufferedWriter.write(",");
                    }
                }
                bufferedWriter.newLine();
            }
            bufferedWriter.close();
            fileWriter.close();
        }catch (Exception e){
            e.printStackTrace();
        }
    }


    public static Integer timeToStamp(String time) {
        try {
            SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

            Date start_d = formatter.parse(time);
            long stamp_start = start_d.getTime() / 1000L;
            return (int)stamp_start;
        } catch (Exception e) {
            return null;
        }

    }

    @Test
    public void test(){
        System.out.println(timeToStamp("2018-04-01 00:00:00"));
    }


}
