# coding: utf-8
"""
优化版本的姿态控制精度算法
主要优化点：
1. 使用类型转换替代 eval()，提高安全性
2. 使用 NumPy 向量化操作替代循环，提高性能
3. 用 NumPy 替代正则表达式检测连续段
4. 减少重复计算和内存分配
5. 优化索引操作
"""

import pandas as pd
import numpy as np
import json
import time
import math
import re


def algsMain(*args, **kwargs):
    '''
    稳态时段姿态控制精度、稳态时段姿态控制精度  φi、θi、ψi

    控制精度阈值是写死的
    :param args:
    :param kwargs:
    :return:
    '''
    try:
        print("稳态时段姿态控制精度、稳态时段姿态控制精度算法已开始计算")
        start_time = time.time()
        
        datas = args[0]
        config = kwargs["config"]
        result_list = calMain(datas, config)
        
        elapsed_time = time.time() - start_time
        print(f"稳态时段姿态控制精度、稳态时段姿态控制精度算法计算完成，耗时: {elapsed_time:.3f}秒")
        return True, result_list
    except Exception as e:
        print("稳态控制精度算法:" + str(e.args))
        return False, e.args


def process_single_day(args):
    """
    处理单日数据的独立函数，用于并行计算
    """
    idx, day_data, today_times_data, diffValue, count, calType, index, flag_values, ang_indices = args
    
    perf_counter = time.perf_counter
    day_start = perf_counter()
    
    every_day_data = np.asarray(day_data, dtype=object)
    today_times = np.asarray(today_times_data, dtype=np.float64)
    
    # 快速检查控制数据
    y_control = np.asarray(every_day_data[index], dtype=np.float64)
    if len(y_control) == 0:
        return idx, "null", perf_counter() - day_start
    
    # 优化角度数据提取
    y_ang = []
    for ang_idx in ang_indices:
        y_ang.append(np.asarray(every_day_data[ang_idx], dtype=np.float64))
    
    # 优化 y_posible 提取
    exclude_indices = set(ang_indices + [index])
    y_posible_parts = []
    for i in range(len(every_day_data)):
        if i not in exclude_indices:
            y_posible_parts.append(np.asarray(every_day_data[i], dtype=np.float64))
    
    if len(y_posible_parts) == 1:
        y_posible = y_posible_parts[0]
    elif len(y_posible_parts) > 1:
        y_posible = np.concatenate(y_posible_parts)
    else:
        y_posible = np.array([])
    
    # 根据计算类型选择不同的计算路径
    if calType == 0:
        y_ang_idx = None
        if len(y_ang) > 0:
            y_ang_idx = get_y_ang_idx_optimized(y_ang[0], today_times, diffValue, count)
            for y_ang_item in y_ang[1:]:
                y_ang_idx = np.intersect1d(y_ang_idx, 
                                           get_y_ang_idx_optimized(y_ang_item, today_times, diffValue, count))
        
        y_posible_idx = None
        if len(y_posible) > 0:
            if len(y_ang) <= 2:
                if y_ang_idx is None:
                    y_ang_idx = get_y_ang_idx_optimized(y_posible, today_times, diffValue, count)
                else:
                    y_ang_idx = np.intersect1d(y_ang_idx, 
                                               get_y_ang_idx_optimized(y_posible, today_times, diffValue, count))
            else:
                y_posible_idx = np.where(np.abs(np.diff(y_posible)) < diffValue)[0]
                if y_ang_idx is not None:
                    y_ang_idx = np.intersect1d(y_ang_idx, y_posible_idx)
        
        if flag_values:
            y_control_idx = get_flag_indices_optimized(y_control, flag_values)
            if y_ang_idx is not None:
                tmp_idx = np.intersect1d(y_ang_idx, y_control_idx)
            else:
                tmp_idx = y_control_idx
        else:
            return idx, 1, perf_counter() - day_start
            
    elif calType == 1:
        if flag_values:
            tmp_idx = get_flag_indices_optimized(y_control, flag_values)
        else:
            return idx, 1, perf_counter() - day_start
    else:
        if len(y_ang) > 0:
            y_ang_idx = get_y_ang_idx_optimized(y_ang[0], today_times, diffValue, count)
            for y_ang_item in y_ang[1:]:
                y_ang_idx = np.intersect1d(y_ang_idx, 
                                           get_y_ang_idx_optimized(y_ang_item, today_times, diffValue, count))
            
            if len(y_posible) > 0 and len(y_ang) <= 2:
                y_ang_idx = np.intersect1d(y_ang_idx, 
                                           get_y_ang_idx_optimized(y_posible, today_times, diffValue, count))
            elif len(y_posible) > 0:
                y_posible_idx = np.where(np.abs(np.diff(y_posible)) < diffValue)[0]
                y_ang_idx = np.intersect1d(y_ang_idx, y_posible_idx)
            
            tmp_idx = y_ang_idx
        else:
            return idx, 1, perf_counter() - day_start
    
    # 检测连续段并计算标准差
    if len(tmp_idx) == 0:
        return idx, 1, perf_counter() - day_start
    
    valid_segments = detect_continuous_segments_optimized(tmp_idx, len(today_times), count)
    if len(valid_segments) == 0:
        # 如果没有连续时间段，统计各个散点的标准差
        final_data = []
        if len(y_posible) > 0:
            valid_idx = tmp_idx[tmp_idx < len(y_posible)]
            if len(valid_idx) > 0:
                final_data.extend(np.abs(y_posible[valid_idx]).tolist())
        elif len(y_ang) > 0:
            valid_idx = tmp_idx[tmp_idx < len(y_ang[0])]
            if len(valid_idx) > 0:
                final_data.extend(np.abs(y_ang[0][valid_idx]).tolist())
        
        if len(final_data) > 0:
            final_data = np.array(final_data, dtype=np.float64)
            result = float(np.std(final_data, dtype=np.float64, ddof=1))
        else:
            result = 1
        return idx, result, perf_counter() - day_start
    
    # 提取有效数据并计算标准差
    final_data = []
    for segment in valid_segments:
        if len(y_posible) > 0:
            valid_segment = segment[segment < len(y_posible)]
            if len(valid_segment) > 0:
                final_data.extend(np.abs(y_posible[valid_segment]).tolist())
        elif len(y_ang) > 0:
            valid_segment = segment[segment < len(y_ang[0])]
            if len(valid_segment) > 0:
                final_data.extend(np.abs(y_ang[0][valid_segment]).tolist())
    
    if len(final_data) > 0:
        final_data = np.array(final_data, dtype=np.float64)
        result = float(np.std(final_data, dtype=np.float64, ddof=1))
    else:
        result = 1
    
    return idx, result, perf_counter() - day_start


def calMain(datas, config):
    """
    优化后的主计算函数（串行版本）
    主要优化：
    1. 使用类型转换替代 eval()
    2. 使用 NumPy 向量化操作替代循环
    3. 串行处理每日数据
    4. 优化索引操作
    """
    # 记录耗时信息
    perf_counter = time.perf_counter
    total_start = perf_counter()

    # 预处理配置参数，避免重复转换
    diffValue = float(config.get("diffValue", 0.1))
    count = int(config.get("count", 10))
    calType = int(config.get("calType", 0))
    index = int(config.get("attitudeMode", 0))
    
    # 预处理 flagValue
    flag_value_str = config.get("flagValue", "").strip()
    flag_values = []
    if flag_value_str:
        flag_values = [float(v.strip()) for v in flag_value_str.split(',') if v.strip()]
    
    # 预处理角度索引
    ang_indices = []
    for i in range(1, 4):
        ang_idx = config.get(f"ang_append{i}", -1)
        if ang_idx != -1:
            ang_indices.append(int(ang_idx))
    
    # 初始化结果列表和计时字典
    results = [None] * len(datas)
    day_timings = {}
    
    print(f"开始串行计算，处理 {len(datas)} 天数据")
    
    # 串行处理每日数据
    for idx, item in enumerate(datas):
        day_data = item[1:]
        today_times_data = item[0]
        task_arg = (idx, day_data, today_times_data, diffValue, count, calType, index, flag_values, ang_indices)
        
        try:
            idx, result, duration = process_single_day(task_arg)
            results[idx] = result
            day_timings[idx] = duration
            print(f"姿态控制精度第{idx + 1}日计算完成，耗时: {duration:.4f}s")
        except Exception as e:
            print(f"姿态控制精度第{idx + 1}日计算失败: {str(e)}")
            results[idx] = 1
            day_timings[idx] = 0.0
    
    total_elapsed = perf_counter() - total_start
    print(f"姿态控制精度总耗时: {total_elapsed:.4f}s")
    return results


def get_flag_indices_optimized(y_control, flag_values):
    """
    优化的标志索引计算
    使用向量化操作替代循环
    """
    y_control = np.asarray(y_control, dtype=np.float64)
    # 使用广播计算所有标志值的匹配
    masks = [np.abs(y_control - fv) <= 0.01 for fv in flag_values]
    # 使用逻辑或合并所有掩码
    combined_mask = np.any(masks, axis=0) if masks else np.zeros(len(y_control), dtype=bool)
    return np.where(combined_mask)[0]


def get_y_ang_idx_optimized(y_ang, today_times, diffValue, count):
    """
    优化的角度索引计算
    主要优化：
    1. 减少嵌套循环
    2. 使用向量化操作
    3. 优化删除索引的逻辑
    """
    y_ang = np.asarray(y_ang, dtype=np.float64)
    today_times = np.asarray(today_times, dtype=np.float64)
    
    # 计算角度差分小于阈值的索引
    y_ang_diff = np.abs(np.diff(y_ang))
    y_ang_idx = np.where(y_ang_diff < diffValue)[0]
    
    if len(y_ang_idx) == 0:
        return np.array([], dtype=np.int64)
    
    # 计算时间差分，找出连续时间段
    time_diff = np.diff(today_times)
    time_continuous_idx = np.where(time_diff <= 10)[0]
    
    if len(time_continuous_idx) == 0:
        return y_ang_idx
    
    # 获取连续时间段
    time_segments = split_continous_idx_optimized(time_continuous_idx, len(today_times), count)
    
    # 优化的删除逻辑：使用向量化操作
    delete_idx = set()
    for seg in time_segments:
        # 找出在段内且在 y_ang_idx 中的索引
        seg_in_ang = np.intersect1d(y_ang_idx, seg)
        if len(seg_in_ang) == 0:
            continue
        
        # 对每个步长进行向量化检查
        for step in [5, 10, 20, 30]:
            if len(seg_in_ang) <= step:
                continue
            
            # 向量化计算差分
            seg_values = y_ang[seg_in_ang]
            # 使用滑动窗口比较
            for i in range(step, len(seg_in_ang)):
                if abs(seg_values[i] - seg_values[i - step]) >= diffValue:
                    # 标记需要删除的索引范围
                    start_idx = max(0, i - step)
                    end_idx = min(len(seg_in_ang), i + 1)
                    delete_idx.update(seg_in_ang[start_idx:end_idx])
    
    # 从 y_ang_idx 中删除标记的索引
    if delete_idx:
        delete_array = np.array(list(delete_idx), dtype=np.int64)
        y_ang_idx = np.setdiff1d(y_ang_idx, delete_array)
    
    return y_ang_idx


def split_continous_idx_optimized(tmp_idx, data_len, count):
    """
    优化的连续索引段检测
    使用 NumPy 替代正则表达式，大幅提升性能
    """
    if len(tmp_idx) == 0:
        return []
    
    # 创建布尔掩码
    mask = np.zeros(data_len, dtype=bool)
    mask[tmp_idx] = True
    
    # 使用 NumPy 检测连续段
    # 找到所有连续段的起始和结束位置
    diff_mask = np.diff(np.concatenate(([False], mask, [False])).astype(int))
    starts = np.where(diff_mask == 1)[0]
    ends = np.where(diff_mask == -1)[0]
    
    # 筛选长度 >= count 的段
    valid_segments = []
    for start, end in zip(starts, ends):
        if end - start >= count:
            valid_segments.append(np.arange(start, end))
    
    return valid_segments


def detect_continuous_segments_optimized(tmp_idx, data_len, count):
    """
    优化的连续段检测函数
    直接使用索引数组，避免中间转换
    """
    if len(tmp_idx) == 0:
        return []
    
    # 创建布尔掩码
    mask = np.zeros(data_len, dtype=bool)
    mask[tmp_idx] = True
    
    # 使用 NumPy 检测连续段
    diff_mask = np.diff(np.concatenate(([False], mask, [False])).astype(int))
    starts = np.where(diff_mask == 1)[0]
    ends = np.where(diff_mask == -1)[0]
    
    # 筛选长度 >= count 的段
    valid_segments = []
    for start, end in zip(starts, ends):
        if end - start >= count:
            valid_segments.append(np.arange(start, end))
    
    return valid_segments


# ===========================================================
# 保留原有接口的兼容函数（如果需要）
# ===========================================================

def split_continous_idx(tmp_idx, today_times, count):
    """
    兼容原有接口的函数
    内部调用优化版本
    """
    return split_continous_idx_optimized(tmp_idx, len(today_times), count)


# ===========================================================
# 其他辅助函数（保持原样，未使用但保留兼容性）
# ===========================================================

def calSigmoid(x, indicatorType, min_x, max_x):
    result = 0
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


def three_sigma(df_col):
    '''
    剔野
    :param df_col:
    :return:
    '''
    rule = (df_col.mean() - 2 * df_col.std() > df_col) | (df_col.mean() + 2 * df_col.std() < df_col)
    index = np.arange(df_col.shape[0])[rule]
    return index


if __name__ == '__main__':
    # 测试代码
    print("优化版本姿态控制精度算法")
    args = [[[1678118400, 1678118401, 1678118402, 1678118403, 1678118404, 1678118405, 1678118406, 1678118407,
              1678118410, 1678118412], [20, 84.7, 84.7, 84.7, 84.7, 84.7, 0, 84.7, 84.7, 84.7],
             [20, 84.7, 84.7, 84.7, 84.7, 84.7, 0, 84.7, 84.7, 84.7]],
            [[1678118400, 1678118401, 1678118402, 1678118403, 1678118404, 1678118405, 1678118406, 1678118407,
              1678118408, 1678204799], [20, 84.7, 84.7, 84.7, 84.7, 84.7, 0, 84.7, 84.7, 84.7],
             [20, 84.7, 84.7, 84.7, 84.7, 84.7, 0, 84.7, 84.7, 84.7]]]
    kwargs = {
        "config": {
            "diffValue": "0.1",
            "count": "10",
            "calType": "0",
            "attitudeMode": 0,
            "flagValue": "6",
            "ang_append1": -1,
            "ang_append2": -1,
            "ang_append3": -1
        }
    }

    status, rst = algsMain(args, **kwargs)
    print(f"状态: {status}, 结果: {rst}")

