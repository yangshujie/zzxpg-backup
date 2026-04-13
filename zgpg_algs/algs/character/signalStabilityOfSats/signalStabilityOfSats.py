# coding: utf-8

import pandas as pd
import numpy as np
import requests
import json, time, math, re


# [[[time],[data1],[data2]],[]]

def algsMain(*args, **kwargs):
    '''
    星间链路信号稳定性  (RZCK6<=52 ，总建链时间RZCK5！=0)
    :param args:
    :param kwargs:
    :return:
    '''
    try:
        print('星间链路信号稳定性开始计算')
        config = kwargs["config"]
        rst = calMain(args[0], config)
        print('星间链路信号稳定性计算完成')
        return True, rst
    except Exception as e:
        return False, e.args

def calMain(datas, config):
    thresholds = eval(config["thresholds"])
    # thresholds = config["thresholds"]
    index_tel = config["flag"]
    rst = []
    all_days_datas = [np.asarray(item[1:]) for item in datas]
    for idx, every_day_data in enumerate(all_days_datas):
        today_times = datas[idx][0]
        # RZCK5 = np.array(every_day_data[index_tel])
        RZCK6 = []
        for i in range(len(every_day_data)):
            if i != index_tel:
                RZCK6.extend(every_day_data[i])

        #  不稳定时间段
        time1 = 0

        tmp_idx =np.where(thresholds <= np.array(RZCK6))[0]
        idxs = np.array([(i in tmp_idx) for i in range(len(today_times))])
        signal_idx = np.where(idxs == True, 1, 0)
        # signal_idx = np.where(thresholds >= np.array(RZCK6) > 0, 1, 0)

        # 把 1 置为 1，0置为0
        norm = signal_idx
        # 找连续5次触发1的置为c，其他不替换
        norm_continous = re.sub(r'1{2,}', lambda x: 'c' * len(x.group()), ''.join(map(str, norm)))
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

            split_today_times = [today_times[tmpitem.tolist()[-1]] - today_times[tmpitem.tolist()[0]] for tmpitem in
                                 valid_idx]
            time1 = np.nansum(split_today_times)
        else:
            time1 = 0

        rst.append(time1 / 86400)
    return rst


# def calMain(datas, config):
#     thresholds = eval(config["thresholds"])
#     # thresholds = config["thresholds"]
#     index_tel = config["flag"]
#     rst = []
#     all_days_datas = [np.asarray(item[1:]) for item in datas]
#     for idx, every_day_data in enumerate(all_days_datas):
#         today_times = datas[idx][0]
#         RZCK5 = np.array(every_day_data[index_tel])
#         RZCK6 = []
#         for i in range(len(every_day_data)):
#             if i != index_tel:
#                 RZCK6.extend(every_day_data[i])

#         #  不稳定时间段
#         time1 = 0

#         tmp_idx = np.intersect1d(np.where(thresholds >= np.array(RZCK6)), np.where(0 < np.array(RZCK6)))
#         idxs = np.array([(i in tmp_idx) for i in range(len(today_times))])
#         signal_idx = np.where(idxs == True, 1, 0)
#         # signal_idx = np.where(thresholds >= np.array(RZCK6) > 0, 1, 0)

#         # 把 1 置为 1，0置为0
#         norm = signal_idx
#         # 找连续5次触发1的置为c，其他不替换
#         norm_continous = re.sub(r'1{2,}', lambda x: 'c' * len(x.group()), ''.join(map(str, norm)))
#         # 找出连续为c的索引
#         norm_continous_idx = np.array(list(norm_continous)) == 'c'
#         norm_continous_idx = np.where(norm_continous_idx == True)
#         if len(norm_continous_idx[0]) != 0:
#             # 找出索引差分>1的索引，即每一段连续的开始的索引
#             diffidxofidx = np.where((np.diff(norm_continous_idx) > 1)[0])
#             diffidxofidx = diffidxofidx[0]

#             list_norm_continous_idx = list(norm_continous_idx)[0]
#             valid_idx = []
#             for i in range(-1, len(diffidxofidx)):
#                 if len(diffidxofidx) == 0:
#                     valid_idx.append(list_norm_continous_idx[:])
#                 else:
#                     if i == -1:
#                         valid_idx.append(list_norm_continous_idx[:diffidxofidx[i + 1] + 1])
#                     elif i == len(diffidxofidx) - 1:
#                         valid_idx.append(list_norm_continous_idx[diffidxofidx[i] + 1:])
#                     else:
#                         valid_idx.append(list_norm_continous_idx[diffidxofidx[i] + 1:diffidxofidx[i + 1] + 1])

#             split_today_times = [today_times[tmpitem.tolist()[-1]] - today_times[tmpitem.tolist()[0]] for tmpitem in
#                                  valid_idx]
#             time1 = np.nansum(split_today_times)
#         else:
#             time1 = 0

#         #  总建链时间
#         time2 = 0
#         norm2_idx = np.where(abs(np.array(RZCK5)) >= 1, 1, 0)
#         # 把 1 置为 1，0置为0
#         norm2 = norm2_idx
#         # 找连续5次触发1的置为c，其他不替换
#         norm2_continous = re.sub(r'1{2,}', lambda x: 'c' * len(x.group()), ''.join(map(str, norm2)))
#         # 找出连续为c的索引
#         norm2_continous_idx = np.array(list(norm2_continous)) == 'c'
#         norm2_continous_idx = np.where(norm2_continous_idx == True)
#         if len(norm2_continous_idx[0]) != 0:
#             # 找出索引差分>1的索引，即每一段连续的开始的索引
#             diffidxofidx2 = np.where((np.diff(norm2_continous_idx) > 1)[0])
#             diffidxofidx2 = diffidxofidx2[0]

#             list_norm2_continous_idx = list(norm2_continous_idx)[0]
#             valid_idx2 = []
#             for i in range(-1, len(diffidxofidx2)):
#                 if len(diffidxofidx2) == 0:
#                     valid_idx2.append(list_norm2_continous_idx[:])
#                 else:
#                     if i == -1:
#                         valid_idx2.append(list_norm2_continous_idx[:diffidxofidx2[i + 1] + 1])
#                     elif i == len(diffidxofidx2) - 1:
#                         valid_idx2.append(list_norm2_continous_idx[diffidxofidx2[i] + 1:])
#                     else:
#                         valid_idx2.append(list_norm2_continous_idx[diffidxofidx2[i] + 1:diffidxofidx2[i + 1] + 1])

#             today_times = datas[idx][0]

#             split_today_times2 = [today_times[tmpitem.tolist()[-1]] - today_times[tmpitem.tolist()[0]] for
#                                   tmpitem in
#                                   valid_idx2]
#             time2 = np.nansum(split_today_times2)
#         else:
#             time2 = 0

#         if time2 == 0:
#             rst.append(1)
#         else:
#             rst.append(time1 / time2)
#     return rst

def timeToStamp(time_in):
    timearray = time.strptime(time_in, "%Y-%m-%d %H:%M:%S")
    return time.mktime(timearray)


def stampToTime(stamp):
    strtime = time.strftime("%Y-%m-%d %H:%M:%S", time.localtime(stamp))
    return strtime


def cal_k(datas):
    '''
    计算1套对地应答机的基础能力k值
    （WCA4，5，6>0.5，其余同上行的测控通道稳定性）
    :return:
    '''
    rst = np.any(datas < 0.5)
    if rst == True:
        return 0
    else:
        return 1


def cal_lamda(data, sumhours):
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
            lambda_value = N / sumhours
    return lambda_value


if __name__ == '__main__':
    print(stampToTime(1644336000))
    args = [[[1678118400, 1678118401, 1678118402, 1678118403, 1678118404, 1678118405, 1678118406, 1678118407,
              1678118408, 1678204799], [1.1, 1.1, 1.1, 1.1, 1.1, 1.1, 1.1, 1, 1.1, 1.1],
             [1.1, 1.1, 1.1, 1.1, 1.1, 1.1, 1.1, 1.1, 1.1, 1.1], [1.1, 1.1, 1.1, 1.1, 1.1, 1.1, 1.1, 1.1, 1.1, 1.1],
             [1.1, 1.1, 1.1, 1.1, 1.1, 1.1, 1.1, 1.1, 1, 1.1], [1.1, 1.1, 1.1, 1.1, 1.1, 1.1, 1.1, 1.1, 1.1, 1.1]],
            [[1678118400, 1678118401, 1678118402, 1678118403, 1678118404, 1678118405, 1678118406, 1678118407,
              1678118408, 1678204799], [1.1, 1.1, 1.1, 1.1, 1.1, 1.1, 0, 1, 1.1, 1.1],
             [1.1, 1.1, 1.1, 1.1, 1.1, 1.1, 1.1, 1.1, 1.1, 1.1], [1.1, 1.1, 1.1, 1.1, 1.1, 1.1, 1.1, 1.1, 1.1, 1.1],
             [1.1, 1.1, 1.1, 1.1, 1.1, 1.1, 1.1, 1.1, 1.1, 1.1], [1.1, 1.1, 1.1, 1.1, 1.1, 1.1, 1.1, 1.1, 1.1, 1.1]]]
    kwargs = {
        "config": {
            "zgpg_start_time": "2019-04-01 00:00:00",
            "zgpg_end_time": "2019-04-10 23:59:59",
            "sat_type": "BD3G01",
            "thresholds": 50
        }
    }
    status, rst = algsMain(args, **kwargs)

    print(rst)

    print(timeToStamp("2023-03-07 00:00:00"))
    print(timeToStamp("2023-03-07 23:59:59"))
