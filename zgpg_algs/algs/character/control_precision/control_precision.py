import requests
import numpy as np
import datetime
import math
import time
import json
import re

def algsMain(*args, **kwargs):
    '''
    计算轨控精度
    :param args:
    :param kwargs:
    :return:
    '''
    # data = {
    #     "taskCode": "BD3G01",
    #     "startTime":"2018-12-25 15:29:47",
    #     "endTime":"2019-12-25 15:29:47"
    # }
    try:
        print('计算轨控精度')
        config = kwargs["config"]
        start_time = config["zgpg_start_time"]
        end_time = config["zgpg_end_time"]
        sattype = config["sat_type"]
        result_list = calMain(sattype, start_time, end_time, config)

        print('轨控精度计算完成')
        return True,result_list
    except Exception as e:
        return False,e.args

def calMain(sattype, starttime, endtime, config):
    result_list = []
    start_date = str_to_datetime(starttime)
    end_date = str_to_datetime(endtime)
    days = (end_date - start_date).days
    data = {
        "taskCode": sattype.split("-")[0]
    }
    rsp = requests.get(url=config["url"], params=data)
    response_data = json.loads(rsp.text)
    raw_datas = response_data.get("datas", [])
    
    if len(raw_datas) == 0:
        for day in range(days + 1):
            result_list.append(1.0)
    else:
        # 根据planid分组数据
        planid_groups = {}
        for item in raw_datas:
            planid = item.get("planid")
            if planid not in planid_groups:
                planid_groups[planid] = {}
            
            resultvaluename = item.get("resultvaluename", "")
            resultvalue = item.get("resultvalue", "")
            
            if resultvaluename == "实际半长轴控制精度":
                planid_groups[planid]["controlprecision"] = float(resultvalue) if resultvalue else None
            elif resultvaluename == "策略计算":
                planid_groups[planid]["strategy_result"] = resultvalue
        
        # 构建result列表，包含flameouttime和controlprecision
        result = []
        for planid, group_data in planid_groups.items():
            controlprecision = group_data.get("controlprecision")
            strategy_result = group_data.get("strategy_result", "")
            
            # 从策略计算中提取历元时刻
            flameouttime = extract_epoch_time(strategy_result)
            
            # 只有当历元时刻和精度值都存在时才添加
            if flameouttime and controlprecision is not None:
                result.append({
                    "flameouttime": flameouttime,
                    "controlprecision": controlprecision
                })
        
        # 过滤掉无效数据并排序
        result = [item for item in result if item["flameouttime"] != "" and item["flameouttime"] is not None]
        result = sorted(result, key=lambda x: str_to_datetime(x["flameouttime"]), reverse=True)
        control_precision_list = []
        for day in range(days + 1):
            flag = True
            current_day = start_date + datetime.timedelta(days=1)
            for item in result:
                if start_date < str_to_datetime(item["flameouttime"]):
                    if str_to_datetime(item["flameouttime"]) < current_day:
                        flag = False
                        control_precision = item["controlprecision"]
                        control_precision_list.append(control_precision)
                else:
                    flag = False
                    if control_precision_list:
                        result_list.append(np.mean(control_precision_list))
                    else:
                        result_list.append(item["controlprecision"])
                    control_precision_list.clear()
                    break
            if control_precision_list:
                result_list.append(np.mean(control_precision_list))
                control_precision_list.clear()
            # start_date = start_date + datetime.timedelta(days=1)
            # 往前取最近的但没有记录的情况
            if flag:
                # 如果没有找到历元时刻或半长轴控制精度，置为1
                result_list.append(1.0)
            start_date = start_date + datetime.timedelta(days=1)
    
    # 将百分制值转换为小数（除以100），找不到数据的已经是1.0，保持不变
    result_list_new = []
    for res in result_list:
        if res == 1.0:
            result_list_new.append(1.0)
        else:
            result_list_new.append(res / 100.0)
    return result_list_new

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

def str_to_datetime(time_str):
    return datetime.datetime.strptime(time_str, "%Y-%m-%d %H:%M:%S")

def extract_epoch_time(strategy_result):
    """
    从策略计算的resultvalue中提取历元时刻
    优先查找平根熄火点5历元、平根熄火点4历元等，如果没有则查找平根熄火点历元
    :param strategy_result: 策略计算的resultvalue（JSON字符串）
    :return: 历元时刻字符串，格式为 "YYYY-MM-DD HH:MM:SS"，如果未找到则返回None
    """
    try:
        # 解析JSON字符串
        strategy_data = json.loads(strategy_result)
        
        # 优先查找数字最大的历元（从5开始往前找）
        for num in range(5, 0, -1):
            key = f"平根熄火点{num}历元"
            if key in strategy_data:
                epoch_str = strategy_data[key]
                # 转换中文时间格式为标准格式
                return convert_chinese_time_to_standard(epoch_str)
        
        # 如果没有找到带数字的，查找"平根熄火点历元"
        if "平根熄火点历元" in strategy_data:
            epoch_str = strategy_data["平根熄火点历元"]
            return convert_chinese_time_to_standard(epoch_str)
        
        return None
    except (json.JSONDecodeError, KeyError, Exception) as e:
        print(f"提取历元时刻失败: {e}")
        return None

def convert_chinese_time_to_standard(chinese_time_str):
    """
    将中文时间格式转换为标准格式
    例如: "2025年12月25日16时45分10.560秒" -> "2025-12-25 16:45:10"
    :param chinese_time_str: 中文时间字符串
    :return: 标准时间字符串 "YYYY-MM-DD HH:MM:SS"
    """
    try:
        # 使用正则表达式提取年月日时分秒
        pattern = r'(\d{4})年(\d{1,2})月(\d{1,2})日(\d{1,2})时(\d{1,2})分(\d+(?:\.\d+)?)秒'
        match = re.match(pattern, chinese_time_str)
        
        if match:
            year, month, day, hour, minute, second = match.groups()
            # 处理秒数（去掉小数部分）
            second_int = int(float(second))
            # 格式化时间字符串
            return f"{year}-{month.zfill(2)}-{day.zfill(2)} {hour.zfill(2)}:{minute.zfill(2)}:{second_int:02d}"
        else:
            print(f"无法解析时间格式: {chinese_time_str}")
            return None
    except Exception as e:
        print(f"转换时间格式失败: {e}")
        return None


if __name__ == "__main__":
    config = {
        "config": {
            "zgpg_start_time": "2022-02-01 00:00:00",
            "zgpg_end_time": "2022-02-14 00:00:00",
            "sat_type": 'BD3G01'
        }
    }
    print(algsMain(**config))
