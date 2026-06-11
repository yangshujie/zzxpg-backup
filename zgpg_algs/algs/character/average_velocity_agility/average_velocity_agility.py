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
    index = config["flag"]
    count = eval(config["count"])
    order = eval(config["order"])
    door = eval(config["door"])
    diffValue = float(config["diffValue"])
    flag_value_str_list = []
    if config["flag_value"] is not '':
        flag_value_str_list = config["flag_value"].split(',')

    all_days_datas = [np.asarray(item[1:]) for item in datas]
    for idx, every_day_data in enumerate(all_days_datas):
        today_times = datas[idx][0]
        if len(today_times) == 0:
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
            if i not in ang_tel_id and i != index:
                y_posible.extend(every_day_data[i])

        if index != -1:
            y_control = np.array(every_day_data[index])
            slice_time_list = get_control_period(today_times, flag_value_str_list, y_control, count)
        else:
            y_ang.append(y_posible)
            y_ang_idx = get_y_ang_stability_idx(y_ang[0], today_times, diffValue, count)
            for y_ang_item in y_ang[1:]:
                y_ang_idx = list(np.intersect1d(y_ang_idx, get_y_ang_stability_idx(y_ang_item, today_times, diffValue, count)))
            slice_time_list = get_control_period2(y_posible, y_ang_idx)

        if len(slice_time_list) == 0:
            rst.append(0)
            continue

        delta_v_list = []
        for valid_idx_single in slice_time_list:
            if len(valid_idx_single) < 2:
                continue
            final_data = []
            final_time = []
            for data_index in valid_idx_single:
                if data_index >= len(today_times) or data_index >= len(y_posible):
                    continue
                final_data.append(y_posible[data_index])
                final_time.append(today_times[data_index])
            y_posible_max_list = argrelextrema(np.array(final_data), np.greater, 0, order)
            y_posible_min_list = argrelextrema(np.array(final_data), np.less, 0, order)
            if len(y_posible_min_list[0]) == 0 and len(y_posible_max_list[0]) == 0:
                y_posible_peak_list = [0, len(final_data) - 1]
            else:
                y_posible_peak_list = list(np.union1d(y_posible_min_list[0], y_posible_max_list[0]))
                if 0 not in y_posible_peak_list:
                    y_posible_peak_list.insert(0, 0)
                if (len(final_data) - 1) not in y_posible_peak_list:
                    y_posible_peak_list.append(len(final_data) - 1)
            for i in range(len(y_posible_peak_list) - 1):
                control_time_period = final_time[y_posible_peak_list[i + 1]] - final_time[
                    y_posible_peak_list[i]]
                if control_time_period <= 0:
                    continue
                delta_v = abs(final_data[y_posible_peak_list[i + 1]] - final_data[
                    y_posible_peak_list[i]]) / control_time_period
                if abs(delta_v) < door:
                    delta_v_list.append(delta_v)
        if len(delta_v_list) == 0:
            rst.append(0)
        else:
            rst.append(np.nanmean(delta_v_list))
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


def get_control_period(today_times, flag_value_str_list, y_control, count):
    valid_idx = []
    for flag_value_str in flag_value_str_list:
        flag_value = eval(flag_value_str)
        y_control_idx = np.where(abs(np.array(y_control) - flag_value) <= 0.01)
        y_control_idx = y_control_idx[0]
        if len(y_control_idx) == 0:
            break

        idxs = np.array([(i in y_control_idx) for i in range(len(today_times))])
        idxs = np.where(idxs == True, 1, 0)

        # 稳态时段
        # 把 1 置为 1，0置为0
        norm = idxs
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


def get_control_period2(y_posible, y_ang_idx):
    if len(y_ang_idx) == 0:
        time_slice_index = [0, len(y_posible) - 1]
        return [list(range(time_slice_index[0], time_slice_index[-1] + 1))]

    # 找出所有非稳定状态的索引
    unstable_indices = [i for i in range(len(y_posible)) if i not in y_ang_idx]
    if len(unstable_indices) == 0:
        return []

    # 将非稳定状态的索引划分为连续的机动块
    unstable_indices_arr = np.array(unstable_indices)
    split_indices = np.where(np.diff(unstable_indices_arr) > 1)[0] + 1
    blocks = np.split(unstable_indices_arr, split_indices)

    # 过滤掉长度小于2的区间（去抖，至少需要2个点才能计算速度）
    time_slice_list_index = [list(block) for block in blocks if len(block) >= 2]
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

    # 4. 从原始稳定点中移除被剔除的漂移点
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
