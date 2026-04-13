package com.ruoyi.common.core.utils;

import com.alibaba.fastjson2.JSONArray;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class PreHandleUtil {

    //    private double arr[];
    private List<Double> list; //接受原始数组
    private JSONArray json_data; //接受原始数组
    private JSONArray json_times; //接受原始数组

    public PreHandleUtil(List<Double> temp) { //利用构造方法来得的原始数组
        this.list = temp;
    }

    public PreHandleUtil(JSONArray temp, JSONArray times) { //利用构造方法来得的原始数组
        this.json_data = temp;
        this.json_times = times;
    }

    public double average() {    //原始数组的算数平均值方法
//        double sum = 0;
//        for(int i = 0; i < list.size(); i++) {
//            sum += list.get(i);
//        }
        double result = list.parallelStream().collect(Collectors.averagingDouble(Double::doubleValue));
//        return sum / list.size();

        return result;
    }


    public List<Double> residualError(double average) {//原始数组的剩余误差方法
        List<Double> re = new ArrayList<>();
        for(int i = 0; i < list.size(); i++) {
            re.set(i, list.get(i) - average);
        }
        return re;
    }


    public double standardVariance(double average) {//原始数组的标准方差值计算方法
        double sum = 0;
        int length = list.size();
        if (length == 0) {
            return 0;
        }
        for(int i = 0; i < length; i++) {
            sum += Math.pow(list.get(i) - average, 2);
        }
        return Math.sqrt(sum /(length - 1));

    }

    public List<Double> judge() {
//        double average = average();//判断异常值方法，若异常，则输出
//        double standardValue = standardVariance(average);
//        for(int i = list.size()-1; i > 0; i--) {
//            if(Math.abs(list.get(i) - average) > (3 * standardValue)) {
//                list.remove(i);
//            }
//        }
        int data_size = list.size();
        int count=5; //2次比较好，但是甲方要求剔野力度更大一点，评估看的是整体趋势
        while (count>0) {
            iter_judge();
            if (list.size() == data_size) {
                break;
            } else {
                data_size = list.size();
            }
            count-=1;
        }
        return list;
    }

    public List<Double> iter_judge() {
        double average = average();//判断异常值方法，若异常，则输出
        double standardValue = standardVariance(average);
        for(int i = list.size()-1; i >= 0; i--) {
            if(Math.abs(list.get(i) - average) > (3 * standardValue) || Math.abs(list.get(i)) > 5000) {
                list.remove(i);
            }
        }
        return list;
    }

    /**
     * z-score剔野
     * @return
     */
    public List<Double> judge2() {
//        double average = average();//判断异常值方法，若异常，则输出
//        double standardValue = standardVariance(average);
//        for(int i = list.size()-1; i > 0; i--) {
//            if(Math.abs(list.get(i) - average) > (3 * standardValue)) {
//                list.remove(i);
//            }
//        }
        int data_size = list.size();
        int count=5; //2次比较好，但是甲方要求剔野力度更大一点，评估看的是整体趋势
        while (count>0) {
            iter_judge2();
            if (list.size() == data_size) {
                break;
            } else {
                data_size = list.size();
            }
            count-=1;
        }
        return list;
    }

    public List<Double> iter_judge2() {
        double average = average();//判断异常值方法，若异常，则输出
        double standardValue = standardVariance(average);
        for(int i = list.size()-1; i >= 0; i--) {
            if(Math.abs(list.get(i) - average) > (1 * standardValue) || Math.abs(list.get(i)) > 5000) {
                list.remove(i);
            }
        }
        return list;
    }


    public double json_data_average() {    //原始数组的算数平均值方法
        List<Double> tmplist = new ArrayList<>();
        for(int i = 0,length=json_data.size(); i < length; i++) {
            tmplist.add(json_data.getDouble(i));
        }
        double result = tmplist.parallelStream().collect(Collectors.averagingDouble(Double::doubleValue));
//        return sum / list.size();

        return result;
    }

    public double json_data_standardVariance(double average) {//原始数组的标准方差值计算方法
        List<Double> tmplist = new ArrayList<>();
        for(int i = 0,length=json_data.size(); i < length; i++) {
            tmplist.add(json_data.getDouble(i));
        }
        double sum = 0;
        int length = tmplist.size();
        if (length == 0) {
            return 0;
        }
        for(int i = 0; i < length; i++) {
            sum += Math.pow(tmplist.get(i) - average, 2);
        }
        return Math.sqrt(sum /(length - 1));

    }

    public JSONArray judge_with_time() {
//        double average = average();//判断异常值方法，若异常，则输出
//        double standardValue = standardVariance(average);
//        for(int i = list.size()-1; i > 0; i--) {
//            if(Math.abs(list.get(i) - average) > (3 * standardValue)) {
//                list.remove(i);
//            }
//        }


        int data_size = json_data.size();
        int count=2; //2次比较好，但是甲方要求剔野力度更大一点，评估看的是整体趋势
        while (count>0) {
            iter_judge_with_time();
            if (json_data.size() == data_size) {
                break;
            } else {
                data_size = json_data.size();
            }
            count-=1;
        }
        return json_data;
    }

    public JSONArray iter_judge_with_time() {
        double average = json_data_average();//判断异常值方法，若异常，则输出
        double standardValue = json_data_standardVariance(average);
        for(int i = json_data.size()-1; i >= 0; i--) {
            if(Math.abs(json_data.getDouble(i) - average) > (3 * standardValue) || Math.abs(json_data.getDouble(i)) > 5000) {
                json_data.remove(i);
                json_times.remove(i);
            }
        }
        return json_data;
    }


    /**
     * Z-score剔野方法
     * @return
     */
    public JSONArray judge_with_time2() {
//        double average = average();//判断异常值方法，若异常，则输出
//        double standardValue = standardVariance(average);
//        for(int i = list.size()-1; i > 0; i--) {
//            if(Math.abs(list.get(i) - average) > (3 * standardValue)) {
//                list.remove(i);
//            }
//        }


        int data_size = json_data.size();
        int count=5; //2次比较好，但是甲方要求剔野力度更大一点，评估看的是整体趋势
        while (count>0) {
            iter_judge_with_time2();
            if (json_data.size() == data_size) {
                break;
            } else {
                data_size = json_data.size();
            }
            count-=1;
        }
        return json_data;
    }

    public JSONArray iter_judge_with_time2() {
        double average = json_data_average();//判断异常值方法，若异常，则输出
        double standardValue = json_data_standardVariance(average);
        for(int i = json_data.size()-1; i >= 0; i--) {
            if(Math.abs(json_data.getDouble(i) - average) > (1 * standardValue) || Math.abs(json_data.getDouble(i)) > 5000) {
                json_data.remove(i);
                json_times.remove(i);
            }
        }
        return json_data;
    }


}