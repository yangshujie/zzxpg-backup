# coding: utf-8

import pandas as pd
import numpy as np
import requests
import json, time, math
import datetime


# [[[time],[data1],[data2]],[]]

def algsMain(*args, **kwargs):
    '''
    独立高斯均值
    :param args:
    :param kwargs:
    :return:
    '''
    try:
        print("高斯算法已开始计算")
        rst = args[0]
        config = kwargs["config"]
        rst_base = config["base_data"]
        replaceList(rst, 'null', None)
        replaceList(rst_base, 'null', None)
        round_t_flag = config["roundTFlag"]
        if "k" in config.keys():
            k = float(config["k"])
        else:
            k = 1
        indicator_type = config["indicatorType"]
        rst_base_data = []
        for rst_base_item in rst_base:
            if rst_base_item is not None:
                rst_base_data.append(rst_base_item)
        mapRange = '高斯量化算法使用的理想点或区间为'
        fx_list = []
        if len(rst_base_data) == 0:
            data_ideal = None
            min_value = None
        else:
            data_ideal = np.mean(rst_base_data)
            min_value = np.min(rst_base_data)
            mapRange += str(data_ideal)
        max_value = 1
        if "min" in config.keys():
            min_value = config["min"]
        if "max" in config.keys():
            max_value = config["max"]
        if round_t_flag == 1:
            mapRange += ', [' + str(min_value) + ', ' + str(max_value) + ']'


        if "reConductDate" in config.keys() and config["reConductDate"] != '':
            start_date_str = config["zgpg_start_time"]
            end_date_str = config["zgpg_end_time"]
            re_conduct_date = config["reConductDate"]
            days = time_delta(start_date_str, re_conduct_date)
            # 4.14后的理想点和最小值
            if time_to_stamp(start_date_str) <= time_to_stamp(re_conduct_date) < time_to_stamp(end_date_str):
                data_ideal_re_conduct = rst[days + 1]
                for i, item in enumerate(rst):
                    if i > days + 1:
                        data_ideal = data_ideal_re_conduct
                        min_value = data_ideal_re_conduct
                    fx_list.append(calGuassian(item, data_ideal, indicator_type, k, round_t_flag, min_value, max_value))
            else:
                for item in rst:
                    fx_list.append(calGuassian(item, data_ideal, indicator_type, k, round_t_flag, min_value, max_value))
        else:
            for item in rst:
                fx_list.append(calGuassian(item, data_ideal, indicator_type, k, round_t_flag, min_value, max_value))

        rst.extend(fx_list)


        return_data = {
            "mapRange": mapRange,
            "value": rst
        }
        print("高斯算法已计算完成")
        return True, return_data
    except Exception as e:
        return False, e.args


def replaceList(inputList, pre, current):
    for index, item in enumerate(inputList):
        if str(item) == str(pre):
            inputList[index] = current


def time_to_stamp(date_time):
    time_array = time.strptime(date_time, "%Y-%m-%d %H:%M:%S")
    return int(time.mktime(time_array))


def time_delta(start_date, end_date):
    return (datetime.datetime.strptime(end_date, "%Y-%m-%d %H:%M:%S") - datetime.datetime.strptime(start_date,
                                                                                                   "%Y-%m-%d %H:%M:%S")).days


def calGuassian(x, ideal_x, indicatorType, k, roundTFlag, min_x, max_x):
    if x is None or ideal_x is None:
        return 1
    result = 0
    if roundTFlag == 1:
        if min_x <= x <= max_x:
            result = 1
            return result
        elif x < min_x:
            ideal_x = min_x
        elif x > max_x:
            ideal_x = max_x

    if ideal_x > 0.0001:
        min_x = floor_min(min_x)
        if k == 0:
            # k = 1.0
            k = pow((min_x - ideal_x), 2) / (pow(ideal_x, 2) * math.log(10, math.e))
        if k <= 0.0001:
            k = 1
        fx = 0
        if abs(x - ideal_x) > 0.0005:
            fx = math.exp(-pow((x - ideal_x) / (ideal_x), 2) / k)
        else:
            fx = 1

        if indicatorType == 0:
            result = 1 - fx
        elif indicatorType == 1:
            result = fx
        elif indicatorType == 2:
            result = fx
    else:
        result = 1

    return result


def floor_min(min_x):
    if min_x <= 0.0001:
        return 0
    if abs(min_x) >= 1:
        output = math.floor(min_x)
    else:
        output = min_x
        count = 0
        while abs(output) < 1:
            output = output * 10
            count += 1
        output = math.floor(output)
        while count > 0:
            output = output / 10.0
            count -= 1
    return output
