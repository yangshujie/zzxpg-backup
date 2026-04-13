package com.ruoyi.common.core.utils;


import org.apache.commons.collections4.CollectionUtils;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Collections;
import java.lang.Math;
import java.util.stream.Collectors;

/**
 * 指标聚合单机逻辑关系计算模型
 */
public class AggregateUtil {


    private String[] operator = {"s","p","h","c","v"};


    /**
     * 故障因子
     * @param bv_unit
     * @param m
     * @return
     */
    public static double failurefactor(double bv_unit, double m) {
        return bv_unit*(1-m);
    }

    @Test
    public void test() {
        List<Double> w= new ArrayList<>();
        w.add(0.348);
        w.add(0.274);

        List<Double> v= new ArrayList<>();
        v.add(0.1105);
        v.add(0.8894);



        double series=series(w,v,0.03);
        System.out.println(series);

        double parallel=parallel(w,v);
        System.out.println(parallel);


        List<Integer> worklist=new ArrayList<>();
        worklist.add(1);
        worklist.add(1);

        double vote = vote(1, worklist,0.03,w,v);
        System.out.println(vote);

    }


    /**
     * 串联
     * @param weightlist
     * @param valuelist
     * @param k
     * @return
     */
    public static double series(List<Double> weightlist, List<Double> valuelist, double k) {
        int n = weightlist.size();
        double tv_system = 0;
        if (k == 0){
            k = 0.01;
        }
        if (valuelist.contains(0d)) {
            tv_system = 0;
        } else {
            double e_value = 1;
            double w_value = 1;
            for (int j=0; j<n; j++) {
                e_value *= weightlist.get(j) * valuelist.get(j);
                w_value *= weightlist.get(j);
            }
            tv_system = Math.exp(-Math.pow(e_value - w_value,2)/k);
        }
        return tv_system;
    }


    /**
     * 并联、加权和
     * @param weightlist
     * @param valuelist
     * @return
     */
    public static double parallel(List<Double> weightlist, List<Double> valuelist) {
        int n = weightlist.size();
        double tv_system = 0;
        for (int j=0; j<n; j++) {
            tv_system += weightlist.get(j) * valuelist.get(j);
        }
        return tv_system;
    }


    /**
     * 热备
     * @param weightlist
     * @param valuelist
     * @return
     */
    public static double hotspare(List<Double> weightlist, List<Double> valuelist) {
        int n = weightlist.size();
        double tv_system = 0;
        for (int j=0; j<n; j++) {
            tv_system += weightlist.get(j) * valuelist.get(j);
        }
        return tv_system;
    }


    /**
     * 冷备
     * @param t_main_unit
     * @param silverValues
     * @return
     */
//    public static double coldstandby(double t_main_unit, int n) {
//        double tv_system = (t_main_unit + n - 1) / n;
//        return tv_system;
//    }
    public static double coldstandby(double t_main_unit, List<Double> silverValues) {
        silverValues.add(t_main_unit);
        return silverValues.parallelStream().collect(Collectors.averagingDouble(Double::doubleValue));
    }

    /**
     * 表决
     * @param r 表决门限
     * @param worklist
     * @param coef_k
     * @param weightlist
     * @param valuelist
     * @return
     */
    public static double vote(int r, List<Integer> worklist,double coef_k,List<Double> weightlist, List<Double> valuelist) {
        int n = weightlist.size();
        double tv_system = 0d;

        int[] all_index_list=new int[worklist.size()];   //所有索引
        int k=0;
        List<Integer> ini_valid_index_list=new ArrayList<>();
        for (int i=0;i<worklist.size();i++) {
            all_index_list[i]=i;
            if (worklist.get(i)==1){
                k+=1;
                ini_valid_index_list.add(i);
            }
        }


        if (k<r) {
            tv_system = 0d;
        } else {
            int[] valid_index_list = new int[ini_valid_index_list.size()];   //所有有效索引
            for (int i = 0; i < ini_valid_index_list.size(); i++) {
                valid_index_list[i] = ini_valid_index_list.get(i);
            }

            List all_combineList = new ArrayList();    // 所有组合
            List valid_combineList = new ArrayList();  // 所有有效组合
            List invalid_combineList = new ArrayList(); // 所有无效组合
            for (int tmp_k = r; tmp_k <= n; tmp_k++) {
                List tmp_all_combineList = combinationSelect(all_index_list, tmp_k);
                List tmp_valid_combineList = combinationSelect(valid_index_list, tmp_k);
                all_combineList.addAll(tmp_all_combineList);
                valid_combineList.addAll(tmp_valid_combineList);
            }


            for (int i = 0; i < all_combineList.size(); i++) {
                boolean exist = false;
                for (int j = 0; j < valid_combineList.size(); j++) {
                    if (compare((int[]) valid_combineList.get(j), (int[]) all_combineList.get(i))) {
                        exist = true;
                        break;
                    }
                }
                if (exist == false) {
                    invalid_combineList.add(all_combineList.get(i));
                }
            }


            /////////////////////////////////////////////
            double up_value = 0d;
            double down_value = 0d;
            for (int i = 0; i < valid_combineList.size(); i++) {
                double tmp_up_value = 0d;
                double tmp_down_value = 0d;

                double tmp_up_e_value = 1d;
                double tmp_down_e_value = 1d;
                double tmp_w_value = 1d;
                int[] current_combine = (int[]) valid_combineList.get(i);
                for (int j = 0; j < current_combine.length; j++) {
                    tmp_up_e_value *= weightlist.get(current_combine[j]) * valuelist.get(current_combine[j]);
                    tmp_down_e_value *= weightlist.get(current_combine[j]) * 1;
                    tmp_w_value *= weightlist.get(current_combine[j]);
                }
//                System.out.println("tmp_up_e_value：" + tmp_up_e_value + "  tmp_down_e_value：" + tmp_down_e_value + "  tmp_w_value：" + tmp_w_value);

                double up_right_value = 0d;
                double down_right_value = 0d;
                if (current_combine.length + 1 <= n) {
                    List all_list = Arrays.stream(all_index_list).boxed().collect(Collectors.toList());
                    List cur_list = Arrays.stream(current_combine).boxed().collect(Collectors.toList());
                    List<Integer> substract = (List) CollectionUtils.subtract(all_list, cur_list);
                    for (int m = 0; m < substract.size(); m++) {
                        up_right_value += weightlist.get(substract.get(m)) * valuelist.get(substract.get(m));
                        down_right_value += weightlist.get(substract.get(m)) * 1;
                    }
                }

//                System.out.println("up_right_value:" + up_right_value + "  down_right_value:" + down_right_value);

                tmp_up_value = Math.exp(-Math.pow(tmp_up_e_value - tmp_w_value, 2) / coef_k) + up_right_value;
                tmp_down_value = Math.exp(-Math.pow(tmp_down_e_value - tmp_w_value, 2) / coef_k) + down_right_value;
//                System.out.println("tmp_up_value:" + tmp_up_value + "  tmp_down_value:" + tmp_down_value);

                up_value += tmp_up_value;
                down_value += tmp_down_value;
            }
            //无效
            for (int i = 0; i < invalid_combineList.size(); i++) {
                double tmp_up_value = 0d;
                double tmp_down_value = 0d;
                int[] current_combine = (int[]) invalid_combineList.get(i);


                double tmp_down_e_value = 1d;
                double tmp_w_value = 1d;
                for (int j = 0; j < current_combine.length; j++) {
                    tmp_down_e_value *= weightlist.get(current_combine[j]) * 1;
                    tmp_w_value *= weightlist.get(current_combine[j]);
                }
//                System.out.println("tmp_down_e_value：" + tmp_down_e_value + "  tmp_w_value：" + tmp_w_value);


                double up_right_value = 0d;
                double down_right_value = 0d;
                if (current_combine.length + 1 <= n) {
                    List all_list = Arrays.stream(all_index_list).boxed().collect(Collectors.toList());
                    List cur_list = Arrays.stream(current_combine).boxed().collect(Collectors.toList());
                    List<Integer> substract = (List) CollectionUtils.subtract(all_list, cur_list);
                    for (int m = 0; m < substract.size(); m++) {
                        up_right_value += weightlist.get(substract.get(m)) * valuelist.get(substract.get(m));
                        down_right_value += weightlist.get(substract.get(m)) * 1;
                    }
                }

//                System.out.println("up_right_value:" + up_right_value + "  down_right_value:" + down_right_value);

                tmp_up_value =  up_right_value;
                tmp_down_value =  Math.exp(-Math.pow(tmp_down_e_value - tmp_w_value, 2) / coef_k) + down_right_value;;
//                System.out.println("tmp_up_value:" + tmp_up_value + "  tmp_down_value:" + tmp_down_value);

                up_value += tmp_up_value;
                down_value += tmp_down_value;
            }


//            System.out.println("up_value:"+up_value+"  down_value:"+down_value);
            tv_system = up_value / down_value;
        }

        return tv_system;
    }

    // n 的阶乘
    private static long factorial(int n) {
        long sum = 1;
        while (n>0) {
            sum = sum*n--;
        }
        return sum;
    }

    // 计算组合个数
    private static long count_combination(int m,int n) {
        return m < n ? factorial(n)/(factorial(n-m)*factorial(m)) : 0;
    }

    // 展示组合情况
    private static List combinationSelect(int[] datalist,int n) {
        List combine_list = new ArrayList();
        combinationSelect(datalist,0,new int[n],0,combine_list);
        return combine_list;
    }

    private static void combinationSelect(int[] datalist,int dataIndex,int[] resultList,int resultIndex,List combine_list) {
        int resultlen = resultList.length;
        int resultCount = resultIndex + 1;
        if (resultCount > resultlen) {
            int[] new_resulList=resultList.clone();  //深拷贝
            combine_list.add(new_resulList);
            return;
        }
        //递归选择下一个
        for (int i = dataIndex; i<datalist.length+resultCount-resultlen;i++) {
            resultList[resultIndex] = datalist[i];
            combinationSelect(datalist,i+1,resultList,resultIndex+1,combine_list);
        }
    }

    private static boolean compare(int[] a,int b[]) {
        if(a.length==b.length) {
            for (int i=0;i<a.length;i++) {
                if (a[i]!=b[i]) {
                    return false;
                }
            }
            return true;
        } else{
            return false;
        }
    }

//    //展示排列情况
//    private static void arrangementSelect(String[] datalist,int n) {
//        arrangementSelectRes(datalist,new String[n],0);
//    }
//
//    private static void arrangementSelectRes(String[] datalist, String[] resultlist, int resultindex) {
//        int resultlen = resultlist.length;
//        if (resultindex >= resultlen) {
//            return;
//        }
//
//        //递归选择下一个
//        for(int i=0; i<datalist.length;i++) {
//            //判断待选项是否存在于排列结果中
//            boolean exists =false;
//            for (int j=0;j<resultindex;j++) {
//                if (datalist[i].equals(resultlist[j])) {
//                    exists = true;
//                    break;
//                }
//            }
//            if (!exists) { // 排列结果不存在该项，才可选择
//                resultlist[resultindex] = datalist[i];
//                arrangementSelectRes(datalist,resultlist,resultindex+1);
//            }
//        }
//    }
}

