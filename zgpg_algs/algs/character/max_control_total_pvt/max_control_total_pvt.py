import datetime
import numpy as np
import re
import math
import time
from scipy.signal import argrelextrema


def algsMain(*args, **kwargs):
    '''
    计算姿态机动时段平均速度
    :param args:
    :param kwargs:
    :return:
    '''
    try:
        print("计算最大控制总量")
        config = kwargs["config"]
        result_list = calMain(args[0], config)
        print("最大控制总量计算完成")
        return True, result_list
    except Exception as e:
        return False, str(e.args)


def calMain(datas, config):
    rst = []
    isp = eval(config["isp"])
    m_sat = eval(config["mSat"])
    Mi_str = [eval(strX) for strX in config["Mi"].split(",")]
    di_str = [eval(strX) for strX in config["di"].split(",")]
    de_str = [eval(strX) for strX in config["de"].split(",")]
    Vt_str = [eval(strX) for strX in config["Vt"].split(",")]
    Zi_str = [eval(strX) for strX in config["Zi"].split(",")]
    Ze_str = [eval(strX) for strX in config["Ze"].split(",")]
    Pvi_str = [eval(strX) for strX in config["Pvi"].split(",")]
    Pve_str = [eval(strX) for strX in config["Pve"].split(",")]
    num = eval(config["num"])
    all_days_datas = [np.asarray(item[1:]) for item in datas]
    for idx, every_day_data in enumerate(all_days_datas):
        today_times = datas[idx][0]
        if len(today_times) == 0:
            rst.append("null")
            continue

        Me_list = []
        for i in range(num):
            if "T" + str(i + 1) in config.keys() and "Pt" + str(i + 1) in config.keys():
                T = every_day_data[config["T" + str(i + 1)]]
                P = every_day_data[config["Pt" + str(i + 1)]]
                Te = T[-1]
                Ti = T[0]
                Pti = P[-1]
                Pte = P[0]

                Mi = Mi_str[i]
                di = di_str[i]
                de = de_str[i]
                Vt = Vt_str[i]
                Zi = Zi_str[i]
                Ze = Ze_str[i]
                Pvi = Pvi_str[i]
                Pve = Pve_str[i]

                Me = de * (Vt - ((Ze*Te)/(Zi*Ti))*((Pti-Pvi)/(Pte-Pve))*(Vt-Mi/di))
                Me_list.append(Me)
        Me_sum = np.nansum(Me_list)
        delta_max = isp * Me_sum / (m_sat + Me_sum)
        rst.append(delta_max)
        Mi_str = Me_list
    return rst





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


def str_to_datetime(time_str):
    return datetime.datetime.strptime(time_str, "%Y-%m-%d %H:%M:%S")





if __name__ == "__main__":
    print(stampToTime(1672505924))
    config = {
        "config": {
            "zgpg_start_time": "2019-04-01 00:00:00",
            "zgpg_end_time": "2019-04-10 23:59:59",
            "sat_type": "BD3G01"
        }
    }
    print(algsMain(**config))
