# coding: utf-8

import pandas as pd
import numpy as np
import requests
import json, time, math, re


# [[[time],[data1],[data2]],[]]

def algsMainOld(*args, **kwargs):
    '''
    充电终压(18的是29，3G的是89.2)
    :param args:
    :param kwargs:
    :return:
    '''
    try:

        # base_value = 89.2  #设计值
        ########################

        config = kwargs["config"]
        base_value = eval(config["base_value"])
        index = config["charge"]
        rst = []
        result = []
        datas = args[0]
        all_days_datas = [np.asarray(item[1:]) for item in datas]
        for idx, every_day_data in enumerate(all_days_datas):
            todaytimes = datas[idx][0]
            if len(todaytimes) == 0:
                rst.append("null")
                continue
            y_I = every_day_data[index]
            y_xu = []
            for i in range(len(every_day_data)):
                if i != index:
                    y_xu.extend(every_day_data[i])

            y_i_idx = np.where(abs(y_I) > 0.01)
            y_xu_idx = np.where(abs(np.array(y_xu) - base_value) <= 0.01 * base_value, 1, 0)

            # 把 1 置为 1，0置为0
            norm = y_xu_idx
            # 找连续5次触发1的置为c，其他不替换
            norm_continous = re.sub(r'1{5,}', lambda x: 'c' * len(x.group()), ''.join(map(str, norm)))
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

                final_valid_idx = [np.intersect1d(j, y_i_idx) for j in valid_idx]
                final_valid_idx = [j for j in final_valid_idx if len(j) > 0]

                if len(final_valid_idx) == 0:
                    rst.append(np.nanmax(y_xu))
                else:
                    rst.append(y_xu[final_valid_idx[0][0]])
            else:
                if len(y_xu) > 0:
                    tel_diff = np.diff(y_xu)
                    standard_tiff = np.where(tel_diff == 0, 0, 1)
                    tel_sub = np.array(
                        list(re.sub(r'0{5,}', lambda x: 'c' * len(x.group()), ''.join(map(str, standard_tiff)))))
                    satisfied_index = np.where(tel_sub == "c")[0]  # TODO:有零的情况
                    stability_voltage = [y_xu[voltage_i] for voltage_i in satisfied_index]

                    rst.append(np.nanmax(stability_voltage))
            result.append(None)
        result.extend(rst)
        return True, result
    except Exception as e:
        return False, e.args


def algsMain(*args, **kwargs):
    try:
        '''
           均值高斯
           :param args:
           :param kwargs:
           :return:
           '''
        print("充电时间终压已开始计算")
        datas = args[0]
        config = kwargs["config"]

        all_days_datas = [np.asarray(item[1:]) for item in datas]
        rst = calFinalVoltage(config, all_days_datas)

        print("充电时间终压已计算完成")
        return True, rst
    except Exception as e:
        return False, e.args


def calFinalVoltage(config, all_days_datas):
    try:
        base_value = eval(config["base_value"])
        index = config["charge"]
        coefficient = eval(config["coefficient"])
        count = eval(config['count'])
        rst = []
        for idx, every_day_data in enumerate(all_days_datas):
            y_I = every_day_data[index]
            if len(y_I) == 0:
                rst.append("null")
                continue
            y_xu = []
            for i in range(len(every_day_data)):
                if i != index:
                    y_xu.extend(every_day_data[i])

            if config["illumination_min"] != '' and config["illumination_max"] != '':
                illumination_min = eval(config["illumination_min"])
                illumination_max = eval(config["illumination_max"])
                y_i_idx = np.where(((illumination_min <= abs(y_I)) & (abs(y_I) <= illumination_max)), 1, 0)
            elif config["illumination_min"] != '':
                illumination_min = eval(config["illumination_min"])
                y_i_idx = np.where(illumination_min <= y_I, 1, 0)
            elif config["illumination_max"] != '':
                illumination_max = eval(config["illumination_max"])
                y_i_idx = np.where(illumination_max >= y_I, 1, 0)
            else:
                y_i_idx = []

            y_xu_idx = np.where(abs(np.array(y_xu) - base_value) <= coefficient * base_value, 1, 0)

            # 把 1 置为 1，0置为0
            norm = y_xu_idx
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

                final_valid_idx = [np.intersect1d(j, y_i_idx) for j in valid_idx]
                final_valid_idx = [j for j in final_valid_idx if len(j) > 0]

                if len(final_valid_idx) == 0:
                    rst.append(np.nanmax(y_xu))
                else:
                    rst.append(y_xu[final_valid_idx[0][0]])
            else:
                if len(y_xu) > 0:
                    tel_diff = np.diff(y_xu)
                    standard_tiff = np.where(tel_diff == 0, 0, 1)
                    tel_sub = np.array(
                        list(re.sub(r'0{' + str(count) + ',}', lambda x: 'c' * len(x.group()),
                                    ''.join(map(str, standard_tiff)))))
                    satisfied_index = np.where(tel_sub == "c")[0]  # TODO:有零的情况
                    stability_voltage = [y_xu[voltage_i] for voltage_i in satisfied_index]

                    rst.append(np.nanmax(stability_voltage))

        return rst
    except Exception as e:
        print(e.args)


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


def cal_exception(data, exception_time, factor):
    time_list = np.array(data[0])
    norm_data = pd.DataFrame(data[1], columns=["data"])
    time_stamp = timeToStamp(exception_time)
    if time_stamp in time_list:
        index = np.where(time_list == time_stamp)
        norm_data[int(index[0]):] = norm_data[int(index[0]):] * factor
        return norm_data["data"].tolist()
    else:
        return data[1]


def timeToStamp(time_in):
    timearray = time.strptime(time_in, "%Y-%m-%d %H:%M:%S")
    return time.mktime(timearray)


def stampToTime(stamp):
    strtime = time.strftime("%Y-%m-%d %H:%M:%S", time.localtime(stamp))
    return strtime


# 1678204799
if __name__ == '__main__':
    eval('12')
    args = [[[1678118400, 1678118401, 1678118402, 1678118403, 1678118404, 1678118405, 1678118406, 1678118407,
              1678118410, 1678118412], [20, 84.7, 84.7, 84.7, 84.7, 84.7, 0, 84.7, 84.7, 84.7],
             [20, 84.7, 84.7, 84.7, 84.7, 84.7, 0, 84.7, 84.7, 84.7]],
            [[1678118400, 1678118401, 1678118402, 1678118403, 1678118404, 1678118405, 1678118406, 1678118407,
              1678118408, 1678204799], [20, 84.7, 84.7, 84.7, 84.7, 84.7, 0, 84.7, 84.7, 84.7],
             [20, 84.7, 84.7, 84.7, 84.7, 84.7, 0, 84.7, 84.7, 84.7]]]
    kwargs = {}
    status, rst = algsMain(args, **kwargs)
    print(rst)

    aaa = np.array([1, 2, 3, 4, 5])
    bbb = np.array([2, 5])
    ccc = np.intersect1d(aaa, bbb)
    print(ccc)
