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


def calMain(datas, config):
    """
    优化后的主计算函数
    主要优化：
    1. 使用类型转换替代 eval()
    2. 优化数据提取和索引计算
    3. 减少重复的数组转换
    """
    # 预处理配置参数，避免重复转换
    diffValue = float(config.get("diffValue", 0.1))
    count = int(config.get("count", 10))
    calType = int(config.get("calType", 0))
    # attitudeMode 默认 -1，表示不使用控制/姿态通道（则 y_control 为空）
    index = int(config.get("attitudeMode", -1))
    
    # 预处理 flagValue（控制标志值）
    flag_value_str = config.get("flagValue", "").strip()
    flag_values = []
    if flag_value_str:
        flag_values = [float(v.strip()) for v in flag_value_str.split(',') if v.strip()]
    
    # 预处理角度索引（姿态角 φ/θ/ψ）
    ang_indices = []
    for i in range(1, 4):
        ang_idx = config.get(f"ang_append{i}", -1)
        if ang_idx != -1:
            ang_indices.append(int(ang_idx))

    # 预处理多参数联合判读配置（例如：Z轴角度/角速度 与 Z轴姿态角偏置量 做差）
    # combineMode 取值示例：
    #   - "none" / ""  : 不做联合判读（默认）
    #   - "diff"       : main - ref
    #   - "sum"        : main + ref
    #   - "ratio"      : main / ref（ref 为 0 时结果置 0）
    #   - "main"       : 仅使用 main
    #   - "ref"        : 仅使用 ref
    # 此处约定：
    #   - main_series  固定为 y_posible（即原始可用姿态量序列）
    #   - ref_series   来自单独配置的字段（例如 Z 轴姿态角偏置量），通过其在 every_day_data 中的索引获取
    combine_mode = str(config.get("combineMode", "none")).strip().lower()

    # 单独配置的参考序列索引（类似 attitudeMode，但仅用于联合判读）
    combine_ref_idx = config.get("combineRefIndex", None)
    try:
        combine_ref_idx = int(combine_ref_idx) if combine_ref_idx not in (None, "") else None
    except Exception:
        combine_ref_idx = None
    
    rst = []
    # 一次性转换所有数据，避免重复转换
    all_days_datas = [np.asarray(item[1:], dtype=object) for item in datas]
    
    for idx, every_day_data in enumerate(all_days_datas):
        # 快速检查控制数据
        if 0 <= index < len(every_day_data):
            y_control = np.asarray(every_day_data[index], dtype=np.float64)
            # 若配置了控制通道但该通道无数据，
            # 仅在 calType 使用控制量（0 或 1）时才将本天结果记为 "null"
            if len(y_control) == 0 and calType in (0, 1):
                rst.append("null")
                continue
        else:
            # 未配置 attitudeMode（index = -1）或索引越界，视为不使用控制量
            y_control = np.array([], dtype=np.float64)
        
        today_times = np.asarray(datas[idx][0], dtype=np.float64)
        
        # 优化角度数据提取
        y_ang = []
        for ang_idx in ang_indices:
            y_ang.append(np.asarray(every_day_data[ang_idx], dtype=np.float64))

        # ---------------- 原始 y_posible 提取逻辑 ----------------
        # 注意：原始代码使用 extend，所以 y_posible 可能是多个数组的拼接
        # 这里排除角度通道和（如果配置了）控制/姿态通道
        exclude_indices = set(ang_indices)
        if 0 <= index < len(every_day_data):
            exclude_indices.add(index)
        y_posible_parts = []
        for i in range(len(every_day_data)):
            if i not in exclude_indices:
                y_posible_parts.append(np.asarray(every_day_data[i], dtype=np.float64))
        
        # 如果只有一个部分，直接使用；否则拼接（保持原始行为）
        if len(y_posible_parts) == 1:
            y_posible = y_posible_parts[0]
        elif len(y_posible_parts) > 1:
            # 原始代码使用 extend，这里拼接所有部分
            y_posible = np.concatenate(y_posible_parts)
        else:
            y_posible = np.array([])

        # ---------------- 多参数联合判读：构造“观测量”序列 ----------------
        # 需求：
        #   - main_series 与 y_posible 保持一致（主观测量）
        #   - ref_series 为单独配置的字段，通过 combineRefIndex 在 every_day_data 中获取
        if (
            combine_mode
            and combine_mode != "none"
            and len(y_posible) > 0
            and combine_ref_idx is not None
            and 0 <= combine_ref_idx < len(every_day_data)
        ):
            ref_series = np.asarray(every_day_data[combine_ref_idx], dtype=np.float64)
            if len(ref_series) > 0:
                measure_series = compute_combined_series(y_posible, ref_series, combine_mode)
            else:
                measure_series = y_posible
        else:
            # 未配置联合判读或数据不足时，退回原始 y_posible
            measure_series = y_posible
        
        # 根据计算类型选择不同的计算路径
        if calType == 0:
            # 计算角度索引
            y_ang_idx = None
            if len(y_ang) > 0:
                y_ang_idx = get_y_ang_idx_optimized(y_ang[0], today_times, diffValue, count)
                for y_ang_item in y_ang[1:]:
                    y_ang_idx = np.intersect1d(y_ang_idx, 
                                               get_y_ang_idx_optimized(y_ang_item, today_times, diffValue, count))
            
            # 计算 y_posible 索引
            y_posible_idx = None
            if len(y_posible) > 0:
                if len(y_ang) <= 2:
                    # 如果角度数据不足，将 y_posible 作为角度数据
                    if y_ang_idx is None:
                        y_ang_idx = get_y_ang_idx_optimized(y_posible, today_times, diffValue, count)
                    else:
                        y_ang_idx = np.intersect1d(y_ang_idx, 
                                                   get_y_ang_idx_optimized(y_posible, today_times, diffValue, count))
                else:
                    y_posible_idx = np.where(np.abs(np.diff(y_posible)) < diffValue)[0]
                    if y_ang_idx is not None:
                        y_ang_idx = np.intersect1d(y_ang_idx, y_posible_idx)
            
            # 计算控制索引
            if flag_values:
                y_control_idx = get_flag_indices_optimized(y_control, flag_values)
                if y_ang_idx is not None:
                    tmp_idx = np.intersect1d(y_ang_idx, y_control_idx)
                else:
                    tmp_idx = y_control_idx
            else:
                rst.append(1)
                continue
                
        elif calType == 1:
            # 仅使用控制标志
            if flag_values:
                tmp_idx = get_flag_indices_optimized(y_control, flag_values)
            else:
                rst.append(1)
                continue
        else:
            # 仅使用角度数据
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
                rst.append(1)
                continue
        
        # 检测连续段并计算标准差
        if len(tmp_idx) == 0:
            rst.append(1)
            continue
        
        # 使用优化的连续段检测
        valid_segments = detect_continuous_segments_optimized(tmp_idx, len(today_times), count)
        if len(valid_segments) == 0:
            # 如果没有连续时间段，统计各个散点的标准差
            final_data = []
            if len(measure_series) > 0:
                valid_idx = tmp_idx[tmp_idx < len(measure_series)]
                if len(valid_idx) > 0:
                    final_data.extend(np.abs(measure_series[valid_idx]).tolist())
            elif len(y_ang) > 0:
                valid_idx = tmp_idx[tmp_idx < len(y_ang[0])]
                if len(valid_idx) > 0:
                    final_data.extend(np.abs(y_ang[0][valid_idx]).tolist())
            
            if len(final_data) > 0:
                final_data = np.array(final_data, dtype=np.float64)
                rst.append(float(np.std(final_data, dtype=np.float64, ddof=1)))
            else:
                rst.append(1)
            continue
        
        # 提取有效数据并计算标准差
        # 注意：measure_series / y_posible 可能是拼接的数组，需要根据实际长度处理
        final_data = []
        for segment in valid_segments:
            if len(measure_series) > 0:
                # 确保索引不越界
                valid_segment = segment[segment < len(measure_series)]
                if len(valid_segment) > 0:
                    final_data.extend(np.abs(measure_series[valid_segment]).tolist())
            elif len(y_ang) > 0:
                valid_segment = segment[segment < len(y_ang[0])]
                if len(valid_segment) > 0:
                    final_data.extend(np.abs(y_ang[0][valid_segment]).tolist())
        
        if len(final_data) > 0:
            final_data = np.array(final_data, dtype=np.float64)
            rst.append(float(np.std(final_data, dtype=np.float64, ddof=1)))
        else:
            rst.append(1)
    
    return rst


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
# 多参数联合判读：观测量组合函数
# ===========================================================

def compute_combined_series(main, ref, mode="diff"):
    """
    多参数联合判读时，用于生成组合观测量的函数。

    参数:
        main: 主遥测序列（如 Z 轴角度 / 角速度）
        ref:  参考遥测序列（如 Z 轴姿态角偏置量）
        mode: 组合方式:
              - "diff":  main - ref  （Z 轴角度 - 偏置量）
              - "sum":   main + ref
              - "ratio": main / ref  （ref 为 0 时结果置 0）
              - "main":  仅使用 main
              - "ref":   仅使用 ref
    返回:
        NumPy 数组，长度与 main/ref 对齐（截断到两者的最短长度）
    """
    main = np.asarray(main, dtype=np.float64)
    ref = np.asarray(ref, dtype=np.float64)

    # 对齐长度，避免索引越界
    min_len = min(len(main), len(ref))
    if min_len == 0:
        return np.array([], dtype=np.float64)
    main = main[:min_len]
    ref = ref[:min_len]

    mode = (mode or "diff").lower()

    if mode == "diff":
        return main - ref
    elif mode == "sum":
        return main + ref
    elif mode == "ratio":
        with np.errstate(divide='ignore', invalid='ignore'):
            out = np.zeros_like(main)
            non_zero = ref != 0
            out[non_zero] = main[non_zero] / ref[non_zero]
            return out
    elif mode == "main":
        return main
    elif mode == "ref":
        return ref
    else:
        # 未知模式时，退回到差分
        return main - ref


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

