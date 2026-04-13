# coding: utf-8

import pandas as pd
import numpy as np
import requests
import json, time, math, re


# [[[time],[data1],[data2]],[]]

def algsMain(*args, **kwargs):
    '''
    当圈能量裕度
    :param args:
    :param kwargs:
    :return:
    '''
    try:
        print("当圈能量裕度算法已开始计算")
        datas = args[0]
        config = kwargs["config"]
        rst = calMain(datas, config)

        return True, rst
    except Exception as e:
        return False, e.args


def calMain(datas, config):
    rst = []
    all_days_datas = [np.asarray(item[1:]) for item in datas]
    for idx, every_day_data in enumerate(all_days_datas):
        todaytimes = datas[idx][0]
        if len(todaytimes) == 0:
            rst.append("null")
            continue
        u_index = config["U_Load"]
        i_index = config["I_Load"]
        u_index_c = config["U_Charge"]
        i_index_c = config["I_Charge"]
        u_index_b = config["U_Branch"]
        i_index_b = config["I_Branch"]
        u_index_l = config["U_Loss"]
        i_index_l = config["I_Loss"]
        index_list = [u_index, i_index, u_index_c, i_index_c, u_index_b, i_index_b, u_index_l, i_index_l]

        split = 0
        y_SA_list = []
        y_SA_tmp = list(np.ones(len(todaytimes)))
        for i in range(len(every_day_data)):
            if i not in index_list:
                if split % 2 == 0 and split != 0:
                    y_SA_list.append(y_SA_tmp)
                    y_SA_tmp = list(np.ones(len(todaytimes)))
                y_SA_tmp = list(np.multiply(y_SA_tmp, abs(every_day_data[i])))
                split = split + 1

        y_SA = list(np.zeros(len(todaytimes)))
        for i in range(len(y_SA_list)):
            y_SA += abs(np.array(y_SA_list[i]))
        # 积分
        Q_SA = np.trapz(y_SA, np.array(todaytimes), axis=-1)

        if u_index_c == -1 and i_index_c == -1 and u_index_b == -1 and i_index_b == -1 and u_index_l == -1 and i_index_l == -1:
            y_SAT_U = abs(every_day_data[u_index])
            y_SAT_I = abs(every_day_data[i_index])
            y_SAT = list(np.multiply(y_SAT_U, y_SAT_I))
            Q_SAT = np.trapz(y_SAT, np.array(todaytimes), axis=-1)
            rst.append(Q_SA / 3600.0 - Q_SAT / 3600.0)

        else:
            if u_index != -1 and i_index != -1:
                y_SAT_Load_U = abs(every_day_data[u_index])
                y_SAT_Load_I = abs(every_day_data[i_index])
                y_SAT_Load = list(np.multiply(y_SAT_Load_U, y_SAT_Load_I))
            else:
                y_SAT_Load = list(np.zeros(len(todaytimes)))
            Q_SAT_Load = np.trapz(y_SAT_Load, np.array(todaytimes), axis=-1)

            if u_index_l != -1 and i_index_l != -1:
                y_SAT_Loss_U = abs(every_day_data[u_index_l])
                y_SAT_Loss_I = abs(every_day_data[i_index_l])
                y_SAT_Loss = list(np.multiply(y_SAT_Loss_U, y_SAT_Loss_I))
            else:
                y_SAT_Loss = list(np.zeros(len(todaytimes)))
            Q_SAT_Loss = np.trapz(y_SAT_Loss, np.array(todaytimes), axis=-1)

            if u_index_c != -1 and i_index_c != -1:
                y_SAT_Charge_U = abs(every_day_data[u_index_c])
                y_SAT_Charge_I = abs(every_day_data[i_index_c])
                y_SAT_Charge = list(np.multiply(y_SAT_Charge_U, y_SAT_Charge_I))
            else:
                y_SAT_Charge = list(np.zeros(len(todaytimes)))

            if u_index_b != -1 and i_index_b != -1:
                y_SAT_Branch_U = abs(every_day_data[u_index_b])
                y_SAT_Branch_I = abs(every_day_data[i_index_b])
                y_SAT_Branch = list(np.multiply(y_SAT_Branch_U, y_SAT_Branch_I))
            else:
                y_SAT_Branch = list(np.zeros(len(todaytimes)))

            count = eval(config['count'])
            I_SA = abs(every_day_data[config['I_SA']])
            # 光照时间长度
            if config["illumination_min"] != '' and config["illumination_max"] != '':
                illumination_min = eval(config["illumination_min"])
                illumination_max = eval(config["illumination_max"])
                y_i_idx = np.where(
                    ((illumination_min <= np.array(I_SA)) & (np.array(I_SA) <= illumination_max)), 1, 0)
            elif config["illumination_min"] != '':
                illumination_min = eval(config["illumination_min"])
                y_i_idx = np.where(illumination_min <= np.array(I_SA), 1, 0)
            elif config["illumination_max"] != '':
                illumination_max = eval(config["illumination_max"])
                y_i_idx = np.where(illumination_max >= np.array(I_SA), 1, 0)
            else:
                y_i_idx = []

            # 把 1 置为 1，0置为0
            norm = y_i_idx
            # 找连续5次触发1的置为c，其他不替换
            norm_continous = re.sub(r'1{' + str(count) + ',}', lambda x: 'c' * len(x.group()), ''.join(map(str, norm)))
            # 找出连续为c的索引
            norm_continous_idx = np.array(list(norm_continous)) == 'c'
            norm_continous_idx = np.where(norm_continous_idx == True)
            if len(norm_continous_idx[0]) != 0:
                # 找出索引差分>1的索引，即每一段连续的开始的索引
                diffidxofidx = np.where((np.diff(norm_continous_idx) > 1)[0])
                diffidxofidx = diffidxofidx[0]

                list_norm_continous_idx = list(norm_continous_idx)[0]
                valid_idx = []
                for i in range(-1, len(diffidxofidx)):
                    if len(diffidxofidx) == 0:
                        valid_idx.append(list_norm_continous_idx[:])
                    else:
                        if i == -1:
                            valid_idx.append(list_norm_continous_idx[:diffidxofidx[i + 1] + 1])
                        elif i == len(diffidxofidx) - 1:
                            valid_idx.append(list_norm_continous_idx[diffidxofidx[i] + 1:])
                        else:
                            valid_idx.append(list_norm_continous_idx[diffidxofidx[i] + 1:diffidxofidx[i + 1] + 1])

                if u_index_c != -1 and i_index_c != -1:
                    Q_SAT_Charge = [np.trapz(y_SAT_Charge[tmpitem.tolist()[0]: tmpitem.tolist()[-1] + 1],
                                             np.array(todaytimes[tmpitem.tolist()[0]: tmpitem.tolist()[-1] + 1]),
                                             axis=-1)
                                    for tmpitem in valid_idx]
                else:
                    Q_SAT_Charge = 0

                if u_index_b != -1 and i_index_b != -1:
                    Q_SAT_Branch = [np.trapz(y_SAT_Branch[tmpitem.tolist()[0]: tmpitem.tolist()[-1] + 1],
                                             np.array(todaytimes[tmpitem.tolist()[0]: tmpitem.tolist()[-1] + 1]),
                                             axis=-1)
                                    for tmpitem in valid_idx]
                else:
                    Q_SAT_Branch = 0
            else:
                Q_SAT_Charge = 0
                Q_SAT_Branch = 0
            Q_SAT = np.nansum(Q_SAT_Charge) + np.nansum(Q_SAT_Branch) + Q_SAT_Load + Q_SAT_Loss
            rst.append(Q_SA / 3600.0 - Q_SAT / 3600.0)

    return rst


def calSigmoid(x, indicatorType, min_x, max_x):
    result = 0
    # min_x = floor_min(min_x)
    # max_x = floor_max(max_x)

    if min_x == max_x:
        result = 1
    else:
        b = (math.log(99) * max_x + 2 * math.log(10) * min_x) / (2 * math.log(10) + math.log(99))
        a = 2 * math.log(10) / (max_x - b)

        if indicatorType == 0:
            result = 1 - (1 / (1 + math.exp(-a * (x - b))))
        elif indicatorType == 1:
            result = (1 / (1 + math.exp(-a * (x - b))))
        elif indicatorType == 2:
            result = (1 / (1 + math.exp(-a * (x - b))))

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


def floor_max(max_x):
    if max_x <= 0.0001:
        return 0
    if abs(max_x) >= 1:
        output = math.floor(max_x + 1)
    else:
        output = max_x
        count = 0
        while abs(output) < 1:
            output = output * 10
            count += 1
        output = math.floor(output + 1)
        while count > 0:
            output = output / 10.0
            count -= 1
    return output


def timeToStamp(time_in):
    timearray = time.strptime(time_in, "%Y-%m-%d %H:%M:%S")
    return time.mktime(timearray)


def stampToTime(stamp):
    strtime = time.strftime("%Y-%m-%d %H:%M:%S", time.localtime(stamp))
    return strtime


def calGuassian(x, ideal_x, indicatorType, k, roundTFlag, min_x, max_x):
    if x == "null":
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


# 1678204799
if __name__ == '__main__':
    args = [[[1678118400, 1678118401, 1678118402, 1678118403, 1678118404, 1678118405, 1678118406, 1678118407,
              1678118410, 1678118412], [2, 2, 2, 2, 2, 2, 2, 2, 2, 2], [2, 2, 2, 2, 2, 2, 2, 2, 2, 2],
             [2, 2, 2, 2, 2, 2, 2, 2, 2, 2]],
            [[1678118400, 1678118401, 1678118402, 1678118403, 1678118404, 1678118405, 1678118406, 1678118407,
              1678118408, 1678204799], [2, 2, 2, 2, 2, 2, 2, 2, 2, 2], [2, 2, 2, 2, 2, 2, 2, 2, 2, 2],
             [2, 2, 2, 2, 2, 2, 2, 2, 2, 2]]]
    kwargs = {}
    status, rst = algsMain(args, *kwargs)
    print(rst)
