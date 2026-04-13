# coding: utf-8

import pandas as pd
import numpy as np
import requests
import json, time, math


# [[[time],[data1],[data2]],[]]

def algsMain(*args, **kwargs):
    '''
    测控通道稳定性
    :param args:
    :param kwargs:
    :return:
    '''
    try:
        # starttime = "2023-03-07 16:30:00"
        # endtime = "2023-03-15 00:00:00"
        # sattype="BD3G01"
        print('测控通道稳定性开始计算')
        config = kwargs["config"]
        starttime = config["zgpg_start_time"]
        endtime = config["zgpg_end_time"]
        sattype = config["sat_type"]
        sgn_flag = config["cal_Flag"]
        exceptionValue = list(eval(config["exc_value"]))

        #########################################

        rst = calMain(args[0], sattype, starttime, endtime, sgn_flag, exceptionValue, config)

        print('测控通道稳定性属性值计算完成')

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


def cal_k(datas, exceptionvalue):
    '''
    计算1套对地应答机的基础能力k值
    （VCA1、2、3确定A的，7、8、9确定B，123同时非0，789同时非0，AB只要有一套可用k即为1）
    :return:
    '''

    rst = False
    for index in range(len(datas)):
        data = datas[index]
        value = exceptionvalue[index]
        rst = rst or np.any(data == value)

    if rst is True:
        return 0
    else:
        return 1


def cal_lamda(data, sumhours):
    '''
    计算1套对地应答机的失效率   lamda_value = N/hours
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
    args = [[[1678118400, 1678118401, 1678118402, 1678118403, 1678118404, 1678118405, 1678118406, 1678118407,
              1678118408, 1678204799], [1.1, 1.1, 1.1, 1.1, 1.1, 1.1, 1.1, 1, 1.1, 1.1],
             [1.1, 1.1, 1.1, 1.1, 1.1, 1.1, 1.1, 1.1, 1.1, 1.1], [1.1, 1.1, 1.1, 1.1, 1.1, 1.1, 1.1, 1.1, 1.1, 1.1],
             [1.1, 1.1, 1.1, 1.1, 1.1, 1.1, 1.1, 1.1, 1, 1.1], [1.1, 1.1, 1.1, 1.1, 1.1, 1.1, 1.1, 1.1, 1.1, 1.1],
             [1.1, 1.1, 1.1, 1.1, 1.1, 1.1, 1.1, 1.1, 1.1, 1.1], [2, 4, 6, 8, 10, 12, 14, 16, 18, 20],
             [2, 4, 6, 8, 10, 12, 14, 16, 18, 21]],
            [[1678118400, 1678118401, 1678118402, 1678118403, 1678118404, 1678118405, 1678118406, 1678118407,
              1678118408, 1678204799], [1.1, 1.1, 1.1, 1.1, 1.1, 1.1, 0, 1, 1.1, 1.1],
             [1.1, 1.1, 1.1, 1.1, 1.1, 1.1, 1.1, 1.1, 1.1, 1.1], [1.1, 1.1, 1.1, 1.1, 1.1, 1.1, 1.1, 1.1, 1.1, 1.1],
             [1.1, 1.1, 1.1, 1.1, 1.1, 1.1, 1.1, 1.1, 1.1, 1.1], [1.1, 1.1, 1.1, 1.1, 1.1, 1.1, 1.1, 1.1, 1.1, 1.1],
             [1.1, 1.1, 1.1, 1.1, 1.1, 1.1, 1.1, 1.1, 1.1, 1.1], [2, 4, 6, 8, 10, 12, 14, 16, 18, 20],
             [2, 4, 6, 8, 10, 12, 14, 16, 18, 21]]]
    kwargs = {"config": {"scName": "BD3G01", "zgpg_start_time": "2022-02-01 00:00:00",
                         "zgpg_end_time": "2022-02-14 00:00:00", "sat_type": "BD3G01"}}
    config = {
        "config": {
            "zgpg_start_time": "2019-04-01 00:00:00",
            "zgpg_end_time": "2019-04-10 23:59:59",
            "sat_type": "BD3G01"
        }
    }
    status, rst = algsMain(args, **kwargs)
    print(rst)

    print(timeToStamp("2023-03-07 00:00:00"))
    print(timeToStamp("2023-03-07 23:59:59"))
