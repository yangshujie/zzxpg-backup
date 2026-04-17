import requests
import datetime,time,math
import numpy as np


def algsMain(*args, **kwargs):
    '''
    计算最大控制总量
    :param args:
    :param kwargs:
    :return:
    '''


    print('计算最大控制总量')
    try:
        config = kwargs["config"]
        isp = eval(config["isp"])
        m_sat = eval(config["mSat"])
        start_time = config["zgpg_start_time"]
        end_time = config["zgpg_end_time"]
        sat_type = config["sat_type"]
        result_list = calMain(isp, m_sat, start_time, end_time, sat_type, config)

        # fx_list = []
        # indicator_type = int(config["indicatorType"])
        # round_t_flag = int(config["roundTFlag"])
        # base_data = config["base_data"]
        # base_data_start_time = stampToTime(base_data[0][0][0])
        # base_data_end_time = stampToTime(base_data[-1][0][-1])
        #
        # '''    sigmoid    '''
        # if round_t_flag == 1:
        #     rst_data = []
        #     for rst_item in result_list:
        #         if rst_item != 'null':
        #             rst_data.append(rst_item)
        #     if len(rst_data) == 0:
        #         max_value = None
        #         min_value = None
        #     else:
        #         min_value = config["min"]
        #         max_value = config["max"]
        #     for item in result_list:
        #         fx_list.append(calSigmoid(item, indicator_type, min_value, max_value))
        # else:
        #     rst_base = calMain(isp, m_sat, base_data_start_time, base_data_end_time, sat_type, config)
        #     rst_base_data = []
        #     for rst_base_item in rst_base:
        #         if rst_base_item != 'null':
        #             rst_base_data.append(rst_base_item)
        #     if len(rst_base_data) == 0:
        #         max_value = None
        #         min_value = None
        #     else:
        #         min_value = min(rst_base)
        #         max_value = max(rst_base)
        #     for item in result_list:
        #         fx_list.append(calSigmoid(item, indicator_type, min_value, max_value))

        # result_list.extend(fx_list)
        # result_list.extend(result_list)
        print('最大控制总量计算完成')
        return True,result_list
    except Exception as e:
        return False,e.args

def calMain(isp, m_sat, start_time, end_time, sat_type, config):
    # start_time = "2018-12-25 00:00:00"
    # end_time = "2019-01-25 23:59:59"
    start_date = str_to_datetime(start_time)
    end_date = str_to_datetime(end_time)
    data = {
        "taskCode": sat_type.split("-")[0]
    }
    rsp = requests.get(url=config["url"], params=data)
    result = eval(rsp.text)["datas"]

    result_list = []
    days = (end_date - start_date).days

    # if len(result) == 0:
    #     for day in range(days + 1):
    #         result_list.append("null")
    # else:
    #     residual_0 = result[-1]["residualall"]
    #     for item in result:
    #         if str_to_datetime(item["workouttime"]) <= start_date:
    #             residual_0 = item["residualall"]
    #             break
    #
    #     for day in range(days + 1):
    #         current_day = start_date + datetime.timedelta(days=day + 1)
    #         residual_all = residual_0
    #         for item in result:
    #             if str_to_datetime(item["workouttime"]) <= current_day:
    #                 residual_all = item["residualall"]
    #                 break
    #         delta_max = isp * residual_all / (m_sat + residual_all)
    #         result_list.append(delta_max)

    st = False
    for item in result:
        if str_to_datetime(item["workouttime"]) <= start_date:
            st = True
            break

    for day in range(days + 1):
        if st is True:
            current_day = start_date + datetime.timedelta(days=day + 1)
            residual_all = None
            for item in result:
                if str_to_datetime(item["workouttime"]) <= current_day:
                    residual_all = item["residualall"]
                    break
            delta_max = isp * residual_all / (m_sat + residual_all)
            result_list.append(delta_max)
        else:
            result_list.append("null")
    return result_list

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

def str_to_datetime(time_str):
    return datetime.datetime.strptime(time_str, "%Y-%m-%d %H:%M:%S")


if __name__ == "__main__":
    # start_time = "2018-12-25 00:00:00"
    # end_time = "2018-12-26 23:59:59"
    # start_date = str_to_datetime(start_time)
    # end_date = str_to_datetime(end_time)
    # print((end_date - start_date).days)
    config = {
        "config": {
            "zgpg_start_time": "2022-02-01 00:00:00",
            "zgpg_end_time": "2022-02-14 00:00:00",
            "isp": '2842',
            "mSat": '2325',
            "sat_type": 'BD3G01'
        }
    }
    print(algsMain(**config))
