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
        print("计算姿态机动时段平均速度")
        config = kwargs["config"]
        result_list = calMain(args[0], config)

        print("姿态机动时段平均速度计算完成")
        return True, result_list
    except Exception as e:
        return False, str(e.args)


def calMain(datas, config):
    rst = []
    tel_id = config["cal_append"]
    door = eval(config["door"])
    count = eval(config["count"])
    if "diffValue" in config.keys():
        diffValue = float(config["diffValue"])
    else:
        diffValue = 0.15
    all_days_datas = [np.asarray(item[1:]) for item in datas]
    for idx, every_day_data in enumerate(all_days_datas):
        today_times = datas[idx][0]
        y_control = np.array(every_day_data[tel_id])
        if len(y_control) == 0:
            rst.append("null")
            continue
        y_posible = []
        y_ang = []
        ang_tel_id = []
        for ang_id_index in range(1, 3):
            if config["ang_append" + str(ang_id_index)] != -1:
                ang_tel_id.append(config["ang_append" + str(ang_id_index)])
                y_ang.append(every_day_data[config["ang_append" + str(ang_id_index)]])
        for i in range(len(every_day_data)):
            if i not in ang_tel_id and i != tel_id:
                y_posible.extend(every_day_data[i])
        delta_v_list = []

        if tel_id != -1:
            control_time_list_index = get_control_period(y_control, config, y_posible, diffValue)
        else:
            y_ang.append(y_posible)
            y_ang_idx = get_y_ang_stability_idx(y_ang[0], today_times, diffValue, count)
            for y_ang_item in y_ang[1:]:
                y_ang_idx = list(
                    np.intersect1d(y_ang_idx, get_y_ang_stability_idx(y_ang_item, today_times, diffValue, count)))
            control_time_list_index = get_control_period2(y_posible, y_ang_idx)

        if len(control_time_list_index) > 0:
            for item in control_time_list_index:
                delta_v_item_list = []
                for item_index in list(range(item[0], item[1])):
                    if item_index + 1 >= len(today_times) or item_index + 1 >= len(y_posible):
                        continue
                    dt = today_times[item_index + 1] - today_times[item_index]
                    if dt <= 0:
                        continue
                    delta_v_value = abs(y_posible[item_index + 1] - y_posible[item_index]) / dt
                    if abs(delta_v_value) < door:
                        delta_v_item_list.append(delta_v_value)

                if len(delta_v_item_list) == 0:
                    delta_v = 0
                else:
                    delta_v = np.nanmean(delta_v_item_list)
                delta_v_list.append(delta_v)

            if len(delta_v_list) == 0:
                rst.append(0)
            else:
                rst.append(np.nanmean(delta_v_list))
        else:
            rst.append(0)
    return rst


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


def get_control_period(y_control, config, y_posible, diffValue):
    maneuver_num = eval(config["maneuver"])
    stability_num = eval(config["stability"])
    time_slice_list_index = []
    time_slice_index = []
    pre_maneuver = False
    continous_idx_list = []
    continous_idx = []
    for index, item in enumerate(y_control):
        if len(time_slice_index) == 2:
            time_slice_list_index.append(time_slice_index.copy())
            time_slice_index.clear()
            continous_idx.clear()

        if y_control[index] in stability_num and len(continous_idx) > 0:
            continous_idx_list.append(continous_idx.copy())
            continous_idx.clear()

        if item in maneuver_num and pre_maneuver is False:
            if len(time_slice_index) == 1:
                time_slice_index.clear()
            time_slice_index.append(index)
            pre_maneuver = True

        if index >= 2:
            if len(time_slice_index) == 1 and pre_maneuver is True and abs(y_posible[index] - y_posible[index - 1]) <= diffValue and abs(y_posible[index - 1] - y_posible[index - 2]) <= diffValue and index - 2 > time_slice_index[0]:
                if y_posible[index - 2] != y_posible[time_slice_index[0]]:
                    time_slice_index.append(index - 2)
                    pre_maneuver = False
                else:
                    time_slice_index[0] = index - 2
                if time_slice_index[0] + 1 == index - 2:
                    continous_idx.extend([time_slice_index[0], index - 2])
                    time_slice_index.clear()

    if len(time_slice_index) == 2:
        time_slice_list_index.append(time_slice_index.copy())
    if len(continous_idx) > 0:
        continous_idx_list.append(continous_idx.copy())

    for continous_idx in continous_idx_list:
        time_slice_list_index.append([continous_idx[0], continous_idx[-1]])
    return time_slice_list_index


def get_control_period2(y_posible, y_ang_idx):
    if len(y_ang_idx) == 0:
        time_slice_index = [0, len(y_posible) - 1]
        return [[time_slice_index[0], time_slice_index[-1]]]

    # 找出所有非稳定状态的索引
    unstable_indices = [i for i in range(len(y_posible)) if i not in y_ang_idx]
    if len(unstable_indices) == 0:
        return []

    # 将非稳定状态的索引划分为连续的机动块
    unstable_indices_arr = np.array(unstable_indices)
    split_indices = np.where(np.diff(unstable_indices_arr) > 1)[0] + 1
    blocks = np.split(unstable_indices_arr, split_indices)

    # 过滤掉长度小于2的区间（去抖，至少需要2个点才能计算速度）
    time_slice_list_index = [[block[0], block[-1]] for block in blocks if len(block) >= 2]
    return time_slice_list_index


def get_y_ang_stability_idx(y_ang, today_times, diffValue, count):
    # 1. 初始找出差分小于阈值的稳定点候选索引
    y_ang_idx = list(np.where(abs(np.diff(y_ang)) < diffValue)[0])
    if len(y_ang_idx) == 0:
        return []

    # 2. 将稳定点划分为在时间上连续的稳定时间块（索引连续且采样时间间隔 <= 10秒）
    y_ang_idx_arr = np.array(y_ang_idx)
    diff_indices = np.diff(y_ang_idx_arr)
    diff_times = np.diff(np.array(today_times)[y_ang_idx_arr])
    split_cond = (diff_indices > 1) | (diff_times > 10)
    split_indices = np.where(split_cond)[0] + 1
    stable_blocks = np.split(y_ang_idx_arr, split_indices)

    delete_idx = set([])
    # 3. 对每个独立的连续稳定块，在其内部进行长时间累积漂移检测，剔除缓慢大范围漂移的点（机动点）
    for block in stable_blocks:
        if len(block) < 5:
            continue
        for idx_in_block, item_ang in enumerate(block):
            if idx_in_block >= 5:
                if abs(y_ang[item_ang] - y_ang[block[idx_in_block - 5]]) >= diffValue:
                    for delete_item in block[idx_in_block - 5 : idx_in_block + 1]:
                        delete_idx.add(delete_item)
            if idx_in_block >= 10:
                if abs(y_ang[item_ang] - y_ang[block[idx_in_block - 10]]) >= diffValue:
                    for delete_item in block[idx_in_block - 10 : idx_in_block + 1]:
                        delete_idx.add(delete_item)
            if idx_in_block >= 20:
                if abs(y_ang[item_ang] - y_ang[block[idx_in_block - 20]]) >= diffValue:
                    for delete_item in block[idx_in_block - 20 : idx_in_block + 1]:
                        delete_idx.add(delete_item)
            if idx_in_block >= 30:
                if abs(y_ang[item_ang] - y_ang[block[idx_in_block - 30]]) >= diffValue:
                    for delete_item in block[idx_in_block - 30 : idx_in_block + 1]:
                        delete_idx.add(delete_item)

    # 4. 从原始稳定点中移除被剔除 of 漂移点
    y_ang_idx_final = [item for item in y_ang_idx if item not in delete_idx]
    return y_ang_idx_final


def split_continous_idx(tmp_idx, today_times, count):
    idxs = np.array([(i in tmp_idx) for i in range(len(today_times))])
    idxs = np.where(idxs == True, 1, 0)

    # 稳态时段
    # 把 1 置为 1，0置为0
    norm = idxs
    # 找连续5次触发1的置为c，其他不替换
    norm_continous = re.sub(r'1{' + str(count) + ',}', lambda x: 'c' * len(x.group()), ''.join(map(str, norm)))
    # 找出连续为c的索引
    norm_continous_idx = np.array(list(norm_continous)) == 'c'
    norm_continous_idx = np.where(norm_continous_idx == True)
    valid_idx = []
    if len(norm_continous_idx[0]) != 0:
        # 找出索引差分>1的索引，即每一段连续的开始的索引
        diffidxofidx = np.where((np.diff(norm_continous_idx) > 1)[0])
        diffidxofidx = diffidxofidx[0]
        list_norm_continous_idx = list(norm_continous_idx)[0]
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
    return valid_idx


def log(logStr):
    with open("D:/log.log", "a", encoding="UTF-8") as f:
        # f.write(str(datetime.datetime.now()) + " [INFO]:    " + str(logStr))
        f.write(str(logStr))
        f.write("\n")


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
