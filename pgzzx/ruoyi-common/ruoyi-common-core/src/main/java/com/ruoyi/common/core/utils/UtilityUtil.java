package com.ruoyi.common.core.utils;

import com.alibaba.fastjson2.JSONArray;
import com.sun.imageio.plugins.common.BogusColorSpace;
import org.junit.Test;
import org.omg.Messaging.SYNC_WITH_TRANSPORT;
import org.python.modules.math;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * 效用函数
 */
public class UtilityUtil {


    /**
     * 线性效用函数
     * @param x
     * @param min
     * @param max
     * @param indicatorType   0表示成本型，1效益型，2区间效益型
     * @return
     */
    public static double linearFunc(double x,double min,double max,Integer indicatorType) {


        double result = 0.0;
        if (max==min) {
            result =1.0;    //最大值等于最小值时认为其为1
        } else {
            if (indicatorType == 0) {
                result = 1 - (x-min) / (max-min);
            } else if (indicatorType == 1) {
                result = (x-min) / (max-min);
            } else if (indicatorType == 2) {
                result = (x-min) / (max-min);
            }
        }
        return result;

    }


    /**
     * sigmoid效用函数
     * @param x
     * @param min
     * @param max
     * @param indicatorType  0表示成本型，1效益型，2区间效益型
     * @return
     */
    public static double sigmoidFunc(double x,double min,double max,Integer indicatorType) {

        double result = 0d;
        min=floor_min(min);
        max=floor_max(max);


        if (max==min) {
            result =1.0;    //最大值等于最小值时认为其为1
        } else {

//            double a=2*Math.log(9) /(max-min);
//            double b=(max + min) /2;

            double b = (Math.log(99)*max + 2*Math.log(10)*min) / (2*Math.log(10)+Math.log(99));
            double a = 2 * Math.log(10) / (max - b);


            if (indicatorType == 0) {
                result = 1 - (1 / (1 + Math.exp(-a * (x - b))));
            } else if (indicatorType == 1) {
                result = (1 / (1 + Math.exp(-a * (x - b))));
            } else if (indicatorType == 2) {
                result = (1 / (1 + Math.exp(-a * (x - b))));
            }
        }
        return result;
    }


    @Test
    public void test_sigmoid() {

        double [] aaa = new double[]{
                0.00842537909018356,
                0.008243633762517882,
                0.00846164817749604,
                0.008622844486631864,
                0.008538814955640052,
                0.008482589427033872,
                0.008350245758680831,
                0.008449124467581637,
                0.008369077656293805,
                0.008389180278735201,
                0.00839138547659773,
                0.008450719822812848,
                0.008427081687470375
        };
        List<Double> bbb = new ArrayList<>();
        for (int i=0;i<aaa.length;i++) {
            double tmpdata = UtilityUtil.sigmoidFunc(aaa[i],0.008243633762517882,0.008622844486631864, 0);
            bbb.add(tmpdata);
        }
        System.out.println(bbb);

    }


    /**
     * 高斯效用函数(传入K值)
     * @param x
     * @param min_x
     * @param ideal_x
     * @param indicatorType  0表示成本型，1效益型，2区间效益型
     * @return
     */
    public static double gaussianFunc(double x,double ideal_x,Integer indicatorType, double k,int roundTFlag,double min_x,double max_x) {

        double result = 0d;


        if (roundTFlag==1) {
            ////////////二级门限范围放大20%
            min_x = min_x >= 0?min_x*0.8:min_x*1.2;
            max_x = max_x >= 0?max_x*1.2:max_x*0.8;
            ///////////////
            if (x >= min_x && x <= max_x) {
                result = 1d;
                return result;
            } else if (x < min_x) {
                ideal_x = min_x;
            } else if (x > max_x) {
                ideal_x = max_x;
            }
        }
        if (Math.abs(ideal_x) > 0.0001) {
            min_x = floor_min(min_x);

            //无用户输入则默认为0，按照公式算
            if (k <= 0d){
                k = Math.pow((min_x - ideal_x) / ideal_x,2) / Math.log(10);
            }
            if (k<0.001d) {
                k=1d;       //min_x==ideal_x时，k应该趋于0，这边给他默认到0.01
            }
//            double fx = Math.exp(-Math.pow((x-ideal_x)/(min_x-ideal_x),2)* Math.log(10)) ;
//            double fx = Math.exp(-Math.pow((x-ideal_x)/(ideal_x),2)* Math.log(Math.E)) ;
            double fx = 0;
            if (Math.abs(x-ideal_x) > 0.0005){
                fx = Math.exp(-Math.pow((x - ideal_x) / (ideal_x), 2) / k);
            }else {
                fx = 1;
            }
            if (indicatorType == 0) {
                result = 1 - fx;
            } else if (indicatorType == 1) {
                result = fx;
            } else if (indicatorType == 2) {
                result = fx;
            }
        } else {
            result = 1d;
        }
        return result;
    }


    @Test
    public void aaa () {
        double [] aaa = new double[]{
                20.696612401714127,
                20.362635358174273,
                20.781473421125607,
                20.73281668750485,
                20.84047088349576,
                20.904220625420056,
                20.985194114458757,
                20.94766808205376,
                21.20719618926238,
                21.30523528132922,
                21.11321116914446
        };
        List<Double> bbb = new ArrayList<>();
        for (int i=0;i<aaa.length;i++) {
            double tmpdata = UtilityUtil.gaussianFunc(aaa[i],20.898,1, 0,0,20.363,21.305);
            bbb.add(tmpdata);
        }
        System.out.println(bbb);
    }


    public static double floor_min(double input) {
        if (input <= 0.0001){
            return 0.0001;
        }
        double output=0d;
        if (Math.abs(input) >= 1) {
            output=Math.floor(input);
        } else {
            output = input;
            int count = 0;
            while (Math.abs(output) < 1) {
                output*=10;
                count++;
            }
            output=Math.floor(output);
            while (count >0) {
                output=output/10.0;
                count--;
            }
//            System.out.println(output);
        }
        return output;
    }

    public static double floor_max(double input) {
        if (input <= 0.0001){
            return 0;
        }
        double output=0d;
        if (Math.abs(input) >= 1) {
            output=Math.floor(input+1);
        } else {
            output = input;
            int count = 0;
            while (Math.abs(output) < 1) {
                output*=10;
                count++;
            }
            output=Math.floor(output+1);
            while (count >0) {
                output=output/10.0;
                count--;
            }
//            System.out.println(output);
        }
        return output;
    }

}
