import numpy as np
import requests
import datetime
import math
import time


def algsMain(*args, **kwargs):
    '''
    计算轨控推力
    :param args:
    :param kwargs:
    :return:
    '''
    # data = {
    #     "taskcode": "BD3G01",
    #     "starttime": "2020-01-25 15:29:47",
    #     "endtime": "2021-12-25 15:29:47"
    # }
    try:
        print('计算轨控推力')
        config = kwargs["config"]
        start_time = config["zgpg_start_time"]
        end_time = config["zgpg_end_time"]
        sattype = config["sat_type"]
        result_list = calMain(sattype, start_time, end_time, config)

        print('轨控推力计算完成')
        return True,result_list
    except Exception as e:
        return False,e.args


def calMain(sattype, starttime, endtime, config):
    result_list = []
    start_date = str_to_datetime(starttime)
    end_date = str_to_datetime(endtime)
    days = (end_date - start_date).days
    data = {
        "taskcode": sattype.split("-")[0]
    }
    rsp = requests.get(url=config["url"], params=data)
    result = eval(rsp.text)["datas"]

    if len(result) == 0:
        for day in range(days + 1):
            result_list.append("null")
    else:
        result = [item for item in result if item["firingtime"] != ""]
        result = sorted(result, key=lambda x: str_to_datetime(x["firingtime"]), reverse=True)
        control_thrust_list = []
        flag = True
        tmp_list = []
        for item in result:
            tmp_list.append({item["firingtime"]: float(eval(item["resultjson"])["轨控推力大小（N）"])})

        for day in range(days + 1):
            current_day = start_date + datetime.timedelta(days=1)
            for item in result:
                if start_date < str_to_datetime(item["firingtime"]):
                    if str_to_datetime(item["firingtime"]) < current_day:
                        if item["resultjson"] != "":
                            flag = False
                            control_thrust = abs(float(eval(item["resultjson"])["轨控推力大小（N）"]))
                            control_thrust_list.append(control_thrust)
                else:
                    if item["resultjson"] != "":
                        flag = False
                        if control_thrust_list:
                            result_list.append(np.mean(control_thrust_list))
                        else:
                            result_list.append(abs(float(eval(item["resultjson"])["轨控推力大小（N）"])))
                        control_thrust_list.clear()
                        break
            if control_thrust_list:
                result_list.append(np.mean(control_thrust_list))
                control_thrust_list.clear()
            # 往前取最近的但没有记录的情况
            if flag:
                # if len(result) > 0:
                #     result_list.append(abs(float(eval(result[-1]["resultjson"])["轨控推力大小（N）"])))
                # else:
                #     result_list.append(0)
                result_list.append('null')
            start_date = start_date + datetime.timedelta(days=1)

    return result_list

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


def timeToStamp(time_in):
    timearray = time.strptime(time_in, "%Y-%m-%d %H:%M:%S")
    return time.mktime(timearray)


def stampToTime(stamp):
    strtime = time.strftime("%Y-%m-%d %H:%M:%S", time.localtime(stamp))
    return strtime

def str_to_datetime(time_str):
    return datetime.datetime.strptime(time_str, "%Y-%m-%d %H:%M:%S")

if __name__ == "__main__":
    config = {
        "config": {
            "zgpg_start_time": "2022-02-01 00:00:00",
            "zgpg_end_time": "2022-02-14 00:00:00",
            "sat_type": 'BD3G01'
        }
    }
    print(algsMain(**config))
