package com.ruoyi.common.core.utils;

import com.alibaba.fastjson2.JSONArray;
import org.apache.commons.math3.stat.descriptive.summary.Sum;
import org.junit.Test;
import org.springframework.cloud.client.discovery.ManagementServerPortUtils;

import javax.validation.constraints.Min;
import java.io.File;
import java.io.FileInputStream;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;


/**
 * 计算指标属性值
 */
public class CalIndicatorAttributeUtil {

    /**
     * 计算均值
     * @param data
     * @return
     */
    public static double calMean(List<Double> data) {

        double result=0d;
        if (data.size() == 0) {
            result = 0d;
        } else {
//            double sum  = 0d;
//            for (int i=0,length = data.size();i < length;i++) {
//                sum += data.get(i) ;
//            }
//            result = sum/data.size();

            result = data.parallelStream().collect(Collectors.averagingDouble(Double::doubleValue));
        }


        return result;

    }


    public static double calMin(List<Double> data) {

        double result=0d;
        if (data.size() == 0) {
            result = 0d;
        } else {
            result  = data.get(0);
            for (int i=1,length = data.size();i < length;i++) {
                result = Math.min(result,data.get(i));
            }
        }

        return result;

    }

    public static double calMax(List<Double> data) {

        double result=0d;
        if (data.size() == 0) {
            result = 0d;
        } else {
            result  = data.get(0);
            for (int i=1,length = data.size();i < length;i++) {
                result = Math.max(result,data.get(i));
            }
        }

        return result;

    }

    /**
     * 计算标准差
     * @param data
     * @return
     */
    public static double calStndardDeviation(List<Double> data) {
        double result=0d;
        int length = data.size();

        if (length == 0) {
            result = 0d;
        } else {
            double mean = calMean(data);
            double sum = 0;

            for(int i = 0; i < length; i++) {
                sum += Math.pow((data.get(i)-mean),2);
            }
            result = Math.sqrt(sum / length);
        }
        return result;
    }

    @Test
    public void test(){
        Double[] value = new Double[]{
                3.901007470552976E-4,
                4.766028781086549E-4,
                4.931755171505318E-4,
                4.677427826957026E-4,
                3.6394175693811804E-4,
                0.0,
                3.9919206707609797E-4,
                0.0,
                4.760357215888263E-4,
                3.489831505966224E-4,
                0.0,
                4.260863144278115E-4,
                0.0,
                4.0777473967971696E-4,
                3.702721519731007E-4,
                4.414708162781982E-4,
                4.901580739356656E-4,
                4.913074954165747E-4,
                3.1281027485502736E-4,
                5.109625264721892E-4,
                3.5071677077461774E-4,
                4.148702473581347E-4,
                3.678031943077205E-4,
                5.007719669063072E-4,
                3.155791289005962E-4,
                5.001620239510963E-4,
                3.582375175837783E-4,
                3.8488568906428134E-4,
                4.447780456682449E-4,
                3.700350456584677E-4,
                4.988428094580109E-4,
                4.961956336462303E-4,
                4.948095392249391E-4};
        List<Double> valueList = new ArrayList(Arrays.asList(value));
        System.out.println(calMean(valueList));
    }


    /**
     * 累积法
     * @param data
     * @return
     */
    public static double calAccumulation(List<Double> data) {
        double result = 0;
        int length = data.size();
        if (length != 0 ) {
            result = data.get(length-1) -data.get(0);
        }
        return result;
    }

}
