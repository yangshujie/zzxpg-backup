# coding: utf-8

import pandas as pd
import numpy as np
import requests
import json, time, math, re


# [[[time],[data1],[data2]],[]]

def fill_missing_data(todaytimes, data_arrays, min_interval=1):
    """
    补齐缺失的数据点
    :param todaytimes: 原始时间戳列表
    :param data_arrays: 数据数组列表（每个通道的数据）
    :param min_interval: 最小采样间隔（秒），如果为None则自动计算
    :return: (filled_times, filled_data_arrays) 补齐后的时间序列和数据
    """
    if len(todaytimes) == 0:
        return todaytimes, data_arrays
    
    todaytimes = np.array(todaytimes)
    
    # 计算采样间隔
    if min_interval is None:
        if len(todaytimes) > 1:
            intervals = np.diff(np.sort(todaytimes))
            min_interval = int(np.min(intervals[intervals > 0])) if np.any(intervals > 0) else 1
        else:
            min_interval = 1
    
    # 从时间戳中确定日期（输入时间是北京时，需要转换为UTC）
    # 北京时 = UTC + 8小时，所以UTC = 北京时 - 8小时
    BEIJING_UTC_OFFSET = 8 * 3600  # 8小时 = 28800秒
    
    # 将输入时间戳从北京时转换为UTC（减去8小时）
    todaytimes_utc = todaytimes - BEIJING_UTC_OFFSET
    
    # 使用UTC时间确定日期
    first_timestamp_utc = int(todaytimes_utc[0])
    first_time_struct = time.gmtime(first_timestamp_utc)  # 使用gmtime获取UTC时间
    year, month, day = first_time_struct.tm_year, first_time_struct.tm_mon, first_time_struct.tm_mday
    
    # 生成完整一天的时间序列（UTC时间，从该天的00:00:00开始）
    # 由于运行环境是UTC，mktime将时间元组视为UTC时间
    day_start_time = int(time.mktime((year, month, day, 0, 0, 0, 0, 0, 0)))
    day_end_time = int(time.mktime((year, month, day, 23, 59, 59, 0, 0, 0)))
    
    # 生成完整的时间序列（从00:00:00到23:59:59，按采样间隔）
    filled_times = np.arange(day_start_time, day_end_time + min_interval, min_interval)
    
    # 使用pandas进行填充（更高效）
    filled_data_arrays = []
    for data in data_arrays:
        # 创建DataFrame（使用UTC时间戳）
        df = pd.DataFrame({
            'time': todaytimes_utc,
            'data': data
        })
        df = df.sort_values('time')
        
        # 创建完整时间序列的DataFrame
        full_df = pd.DataFrame({'time': filled_times})
        
        # 合并
        merged_df = pd.merge(full_df, df, on='time', how='left')
        
        # 前向填充和后向填充
        merged_df['data'] = merged_df['data'].fillna(method='ffill').fillna(method='bfill')
        
        # 如果开头还有NaN（即第一个数据点之前），用第一个有效值填充
        if merged_df['data'].isna().any():
            first_valid = merged_df['data'].dropna().iloc[0] if merged_df['data'].dropna().size > 0 else 0
            merged_df['data'] = merged_df['data'].fillna(first_valid)
        
        filled_data_arrays.append(merged_df['data'].values.tolist())
    
    return filled_times.tolist(), filled_data_arrays


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
        
        # 补齐缺失数据
        data_arrays = [every_day_data[i] for i in range(len(every_day_data))]
        filled_times, filled_data_arrays = fill_missing_data(todaytimes, data_arrays)
        filled_every_day_data = np.asarray(filled_data_arrays)
        
        p_index_c = config["P_Charge"]
        p_index_b = config["P_Branch"]
        p_index_l = config["P_Loss"]
        p_index = config["P_Load"]

        index_list = [p_index_c, p_index_b, p_index_l, p_index]

        if p_index_c == -1 and p_index_b == -1 and p_index_l == -1:
            y_SAT = abs(filled_every_day_data[p_index])
            y_SA = list(np.zeros(len(filled_times)))
            for i in range(len(filled_every_day_data)):
                if i not in index_list:
                    y_SA += abs(filled_every_day_data[i])

            # 积分
            Q_SA = np.trapz(y_SA, np.array(filled_times), axis=-1)
            Q_SAT = np.trapz(y_SAT, np.array(filled_times), axis=-1)
            rst.append(Q_SA / 3600.0 - Q_SAT / 3600.0)
        else:
            if p_index != -1:
                y_SAT_Load = abs(filled_every_day_data[p_index])
            else:
                y_SAT_Load = list(np.zeros(len(filled_times)))
            Q_SAT_Load = np.trapz(y_SAT_Load, np.array(filled_times), axis=-1)

            if p_index_l != -1:
                y_SAT_Loss = abs(filled_every_day_data[p_index_l])
            else:
                y_SAT_Loss = list(np.zeros(len(filled_times)))
            Q_SAT_Loss = np.trapz(y_SAT_Loss, np.array(filled_times), axis=-1)

            if p_index_c != -1:
                y_SAT_Charge = abs(filled_every_day_data[p_index_c])
            else:
                y_SAT_Charge = list(np.zeros(len(filled_times)))

            if p_index_b != -1:
                y_SAT_Branch = abs(filled_every_day_data[p_index_b])
            else:
                y_SAT_Branch = list(np.zeros(len(filled_times)))

            # 获取cal_type，如果没有则默认为0
            cal_type = eval(config.get('calType', config.get('cal_type', '0')))
            
            if cal_type == 0:
                y_SA = list(np.zeros(len(filled_times)))
                for i in range(len(filled_every_day_data)):
                    if i not in index_list:
                        y_SA += abs(filled_every_day_data[i])
            else:
                y_SA = list(np.ones(len(filled_times)))
                for i in range(len(filled_every_day_data)):
                    if i not in index_list:
                        y_SA *= list(np.multiply(y_SA, abs(filled_every_day_data[i])))
            Q_SA = np.trapz(y_SA, np.array(filled_times), axis=-1)

            count = eval(config['count'])
            I_SA = abs(filled_every_day_data[config['I_SA']])
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

                if p_index_c != -1:
                    Q_SAT_Charge = [np.trapz(y_SAT_Charge[tmpitem.tolist()[0]: tmpitem.tolist()[-1] + 1],
                                             np.array(filled_times[tmpitem.tolist()[0]: tmpitem.tolist()[-1] + 1]),
                                             axis=-1)
                                    for tmpitem in valid_idx]
                else:
                    Q_SAT_Charge = 0

                if p_index_b != -1:
                    Q_SAT_Branch = [np.trapz(y_SAT_Branch[tmpitem.tolist()[0]: tmpitem.tolist()[-1] + 1],
                                             np.array(filled_times[tmpitem.tolist()[0]: tmpitem.tolist()[-1] + 1]),
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
