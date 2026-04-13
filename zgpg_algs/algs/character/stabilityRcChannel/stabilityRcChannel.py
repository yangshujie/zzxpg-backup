# coding: utf-8

import pandas as pd
import numpy as np
import requests
import json, time, math


# [[[time],[data1],[data2]],[]]

def algsMain(*args, **kwargs):
    '''
    遥测通道稳定性
    :param args:
    :param kwargs:
    :return:
    '''
    try:
        # starttime = "2023-03-07 16:30:00"
        # endtime = "2023-03-15 00:00:00"
        # sattype="BD3G01"
        print('遥测通道稳定性开始计算')
        config = kwargs["config"]
        starttime = config["zgpg_start_time"]
        endtime = config["zgpg_end_time"]
        sattype = config["sat_type"]
        sgn_flag = config["cal_Flag"]
        exceptionValue = list(eval(config["exc_value"]))

        #########################################

        rst = calMain(args[0], sattype, starttime, endtime, sgn_flag, exceptionValue, config)

        print('遥测通道稳定性计算完成')
        return True, rst
    except Exception as e:
        return False, e.args


def calMain(datas, sattype, starttime, endtime, sgn_flag, exceptionvalue, config):
    rst = []
    # 测控计划前后各推一天，防止搜不出来
    startstamp = int(timeToStamp(starttime)) - 86400
    endstamp = int(timeToStamp(endtime)) + 86400
    scTime1 = stampToTime(startstamp)
    scTime2 = stampToTime(endstamp)
    get_data = {
        "taskcode": sattype.split("-")[0],
        "beginTime": scTime1,
        "endTime": scTime2
    }

    rsps_data = requests.get(url=config["url1"], params=get_data)
    rsps_jsondata = eval(rsps_data.text)

    rows = rsps_jsondata["datas"]
    tklist = []
    for item in rows:
        if item["sgn_flag"] == sgn_flag:
            tklist.append([timeToStamp(item["begintime"]), timeToStamp(item["endtime"])])

    get_data = {
        "taskCode": sattype,
        "startTime": scTime1,
        "endTime": scTime2
    }
    rsps_data = requests.get(url=config["url2"],
                             params=get_data)
    rsps_jsondata = eval(rsps_data.text)
    rows = rsps_jsondata["datas"]
    exlist = [[item['exceptionfindtime'], item["exceptionsum"]] for item in rows]

    all_days_datas = [np.asarray(item[1:]) for item in datas]
    for idx, every_day_data in enumerate(all_days_datas):
        if len(every_day_data[0]) == 0:
            rst.append("null")
            continue
        n_split = eval(config["equip_num"])
        flag = []
        for k_index in range(n_split):
            flag.append(cal_k(every_day_data[k_index * int(len(every_day_data) / n_split):(k_index + 1) * int(
                len(every_day_data) / n_split)], exceptionvalue[
                                                 k_index * int(len(every_day_data) / n_split):(k_index + 1) * int(
                                                     len(every_day_data) / n_split)]))
        if np.sum(flag) == 0:
            k = 0
        else:
            k = 1

        today_times = datas[idx][0]
        tmpstart = today_times[0]
        tmpend = today_times[-1]
        sum_hours = 0.0
        for tmptime in tklist:
            if tmpstart >= tmptime[0] and tmpend <= tmptime[1]:
                sum_hours += tmpend - tmpstart
            elif tmpstart >= tmptime[0] and tmpstart <= tmptime[1] and tmpend >= tmptime[1]:
                sum_hours += tmptime[1] - tmpstart
            elif tmpstart <= tmptime[0] and tmpend >= tmptime[0] and tmpend <= tmptime[1]:
                sum_hours += tmpend - tmptime[0]
            elif tmpstart <= tmptime[0] and tmpend >= tmptime[1]:
                sum_hours += tmptime[1] - tmptime[0]
        sum_hours = sum_hours / 3600.0

        if eval(config['calType']) == 0:
            exception = []
            for ex in exlist:
                if tmpstart <= timeToStamp(ex[0]) <= tmpend:
                    exception.append(ex[1])

            exsumList = list(np.zeros(n_split))
            ex_word = config["exc_value"].split(',')
            for exceptionsum in exception:
                for wordId, word in enumerate(ex_word):
                    if word in exceptionsum:
                        exsumList[wordId] = exsumList[wordId] + 1
            if len(exsumList) != len(ex_word):
                rst.append(k * (1 - cal_lamda(np.sum(exsumList), sum_hours)))
            else:
                rst.append(np.nanmax([k * (1 - cal_lamda(exsumList[countId], sum_hours)) * flag[countId] for countId in
                                      range(len(exsumList))]))
        else:
            tmprst = []
            for k_index in range(n_split):
                if 'reset' + str(k_index + 1) in config.keys():
                    lambda_value = cal_lamda_tel(every_day_data[config['reset' + str(k_index + 1)]], sum_hours)
                else:
                    lambda_value = 0
                tmprst.append(k * (1 - lambda_value))
            rst.append(max(tmprst))

    return rst


def calSigmoid(x, indicatorType, min_x, max_x):
    result = 0
    # min_x = floor_min(min_x)
    # max_x = floor_max(max_x)

    if min_x == max_x or min_x is None:
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


def cal_k(datas, exceptionvalue, times=None):
    '''
    计算1套对地应答机的基础能力k值，并判断工作区间和统计工作时长
    （WCA4，5，6>0.5，其余同上行的测控通道稳定性）
    
    :param datas: 遥测数据数组列表，每个元素是一个数组
    :param exceptionvalue: 下限值列表，与datas对应。遥测值小于此下限值时不工作
    :param times: 时间戳数组，可选，用于计算工作时长。如果提供，长度应与datas的第一个数组长度相同
    :return: 如果只提供datas和exceptionvalue，返回k值(0或1)
             如果提供了times，返回字典:
             {
                 'k': k值(0或1),
                 'work_intervals': 工作区间列表[[start_time, end_time], ...],
                 'total_work_time': 总工作时长(秒)
             }
    '''

    # 检查是否有数据小于下限值（不工作）
    # 如果任何一个通道的任何一个数据点小于对应的下限值，k值为0
    rst = False
    for index in range(len(datas)):
        data = np.asarray(datas[index])
        lower_limit = exceptionvalue[index]
        # 检查是否有数据小于下限值
        rst = rst or np.any(data < lower_limit)

    if rst is True:
        k_value = 0  # 存在小于下限值的数据，不工作
    else:
        k_value = 1  # 所有数据都大于等于下限值，正常工作
    
    # 如果没有提供时间，保持原有行为，只返回k值
    if times is None:
        return k_value
    
    # 扩展功能：判断工作区间和统计工作时长
    times = np.asarray(times)
    work_intervals = []
    total_work_time = 0.0
    
    # 判断每个数据点是否在工作状态（至少有一个通道大于等于下限值）
    if len(datas) > 0 and len(datas[0]) > 0:
        # 创建一个工作状态标志数组，初始为False
        work_status = np.zeros(len(datas[0]), dtype=bool)
        
        # 检查每个通道：只要有一个通道的值 >= 下限值，就认为是工作状态
        for index in range(len(datas)):
            data = np.asarray(datas[index])
            lower_limit = exceptionvalue[index]
            # 如果该通道的数据大于等于下限值，标记为工作
            work_status = work_status | (data >= lower_limit)
        
        # 找出连续的工作区间
        if np.any(work_status):
            # 找到状态变化的位置
            status_diff = np.diff(work_status.astype(int))
            start_indices = np.where(status_diff == 1)[0] + 1  # 从False变True的位置
            end_indices = np.where(status_diff == -1)[0] + 1   # 从True变False的位置
            
            # 处理边界情况
            if work_status[0]:
                start_indices = np.insert(start_indices, 0, 0)
            if work_status[-1]:
                end_indices = np.append(end_indices, len(work_status))
            
            # 构建工作区间
            for i in range(len(start_indices)):
                start_idx = start_indices[i]
                end_idx = end_indices[i] if i < len(end_indices) else len(work_status)
                
                if start_idx < len(times) and end_idx <= len(times) and end_idx > start_idx:
                    start_time = times[start_idx]
                    end_time = times[end_idx - 1] if end_idx > 0 else times[start_idx]
                    
                    # 计算该区间的工作时长（秒）
                    # 直接使用时间戳差值
                    interval_time = end_time - start_time
                    
                    # 如果区间只有一个点，使用采样间隔估算（尝试从相邻点推断）
                    if end_idx == start_idx + 1:
                        # 尝试使用前一个或后一个点的间隔来估算
                        if start_idx > 0:
                            interval_time = times[start_idx] - times[start_idx - 1]
                        elif end_idx < len(times):
                            interval_time = times[end_idx] - times[end_idx - 1]
                        else:
                            interval_time = 1  # 默认1秒
                    elif interval_time <= 0:
                        # 如果时间差异常，使用数据点数和平均间隔估算
                        if end_idx > start_idx + 1:
                            avg_interval = (times[min(end_idx, len(times)-1)] - times[start_idx]) / (end_idx - start_idx - 1)
                            interval_time = avg_interval * (end_idx - start_idx)
                    
                    work_intervals.append([start_time, end_time])
                    total_work_time += max(interval_time, 0)  # 确保非负
    
    return {
        'k': k_value,
        'work_intervals': work_intervals,
        'total_work_time': total_work_time  # 单位：秒
    }


def cal_lamda(data, sumhours):
    '''
    计算1套对地应答机的失效率   lamda_value = N/hours
    （1）获取测控计划，每天所有测控计划endtime-starttime之和 /  QX_ZCC1_27和QX_ZCC2_27在当天首尾的差值之和   分别计算，取最大值（，如果拥有多套对地应答机，分别计算，取最大值作为最终值。）
    :return:
    '''
    if sumhours == 0.0:
        lambda_value = 0.0
    else:
        lambda_value = data / sumhours
    return lambda_value


def cal_lamda_tel(data,sumhours):
    '''
    计算1套对地应答机的失效率   lamda_value = N/hours
    （1）获取测控计划，每天所有测控计划endtime-starttime之和 /  QX_ZCC1_27和QX_ZCC2_27在当天首尾的差值之和   分别计算，取最大值（，如果拥有多套对地应答机，分别计算，取最大值作为最终值。）
    :return:
    '''
    lambda_value = 0
    if len(data) != 0:
        if sumhours == 0:
            lambda_value = 0
        else:
            N = data[-1] - data[0]
            lambda_value = N/sumhours
    return lambda_value


if __name__ == '__main__':
    stampToTime(1648690507)
    args = [[[1678118400, 1678118401, 1678118402, 1678118403, 1678118404, 1678118405, 1678118406, 1678118407,
              1678118408, 1678204799], [1.1, 1.1, 1.1, 1.1, 1.1, 1.1, 1.1, 1, 1.1, 1.1],
             [1.1, 1.1, 1.1, 1.1, 1.1, 1.1, 1.1, 1.1, 1.1, 1.1], [1.1, 1.1, 1.1, 1.1, 1.1, 1.1, 1.1, 1.1, 1.1, 1.1],
             [1.1, 1.1, 1.1, 1.1, 1.1, 1.1, 1.1, 1.1, 1, 1.1], [1.1, 1.1, 1.1, 1.1, 1.1, 1.1, 1.1, 1.1, 1.1, 1.1]],
            [[1678118400, 1678118401, 1678118402, 1678118403, 1678118404, 1678118405, 1678118406, 1678118407,
              1678118408, 1678204799], [1.1, 1.1, 1.1, 1.1, 1.1, 1.1, 0, 1, 1.1, 1.1],
             [1.1, 1.1, 1.1, 1.1, 1.1, 1.1, 1.1, 1.1, 1.1, 1.1], [1.1, 1.1, 1.1, 1.1, 1.1, 1.1, 1.1, 1.1, 1.1, 1.1],
             [1.1, 1.1, 1.1, 1.1, 1.1, 1.1, 1.1, 1.1, 1.1, 1.1], [1.1, 1.1, 1.1, 1.1, 1.1, 1.1, 1.1, 1.1, 1.1, 1.1]]]
    kwargs = {}
    status, rst = algsMain(args, *kwargs)
    print(rst)

    print(timeToStamp("2023-03-07 00:00:00"))
    print(timeToStamp("2023-03-07 23:59:59"))
