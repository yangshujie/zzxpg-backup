import numpy as np
import time, math
import pandas as pd


def algsMain(*args, **kwargs):
    '''
    计算轨控期间姿态稳定度（优化版本）
    :param args:
    :param kwargs:
    :return:
    '''
    try:
        print("计算轨控期间姿态稳定度")
        all_data = args[0]
        config = kwargs["config"]
        result_list = calMain(all_data, config)

        print("轨控期间姿态稳定度计算完成")

        return True, result_list
    except Exception as e:
        return False, e.args


def calMain(all_data, config):
    """
    优化后的主计算函数
    主要优化：
    1. 使用类型转换替代 eval()
    2. 使用 NumPy 向量化操作替代正则表达式
    3. 优化索引操作和数据处理
    4. 减少重复的类型转换
    """
    result_list = []
    
    # 预处理配置参数，避免重复转换
    index = int(config.get("maneuver", 0))
    count = int(config.get("count", 10))
    mode_value = int(config.get("modeValue", 0))
    threshold = 0.01  # 阈值常量
    
    # 预处理 flagValue
    flag_value_str = config.get("flagValue", "").strip()
    flag_values = []
    if flag_value_str:
        flag_values = [float(v.strip()) for v in flag_value_str.split(',') if v.strip()]
    
    if not flag_values:
        # 如果没有配置 flagValue，使用默认值
        flag_values = [0.0]
    
    std_pre = 0
    
    for tel_by_day in all_data:
        time_list = tel_by_day[0]
        if len(time_list) == 0:
            result_list.append("null")
            continue
        
        # 转换为 NumPy 数组，避免重复转换
        time_list = np.asarray(time_list, dtype=np.float64)
        
        # 提取所有非 maneuver 通道的数据
        y_tel_parts = []
        for i in range(0, len(tel_by_day) - 1):
            if i != index:
                y_tel_parts.append(np.asarray(tel_by_day[i + 1], dtype=np.float64))
        
        # 合并数据并取绝对值（与原始逻辑一致）
        if len(y_tel_parts) == 1:
            y_posible = np.abs(y_tel_parts[0])
        elif len(y_tel_parts) > 1:
            y_posible = np.abs(np.concatenate(y_tel_parts))
        else:
            y_posible = np.array([], dtype=np.float64)
        
        # 提取控制通道数据并取绝对值（与原始逻辑一致）
        y_control = np.abs(np.asarray(tel_by_day[index + 1], dtype=np.float64))
        
        # 优化的标志索引计算：使用向量化操作
        y_control_idx = get_flag_indices_optimized(y_control, flag_values, mode_value, threshold)
        
        if len(y_control_idx) == 0:
            result_list.append(std_pre)
            continue
        
        # 创建布尔掩码数组（对应原始代码的 idxs）
        # 原始逻辑：idxs = np.array([(i in tmp_idx) for i in range(len(time_list))])
        idxs = np.zeros(len(time_list), dtype=int)
        idxs[y_control_idx] = 1
        
        # 优化的连续段检测：使用 NumPy 向量化操作替代正则表达式
        # 找出连续 count 次以上为 1 的段
        valid_segments = find_continuous_segments_optimized_from_mask(idxs, count)
        
        if len(valid_segments) > 0:
            final_data = []
            for valid_idx_single in valid_segments:
                # 从每个连续段中提取数据
                for data_index in valid_idx_single:
                    if data_index < len(y_posible):
                        final_data.append(y_posible[data_index])
                if len(valid_idx_single) > 0:
                    start_idx = valid_idx_single[0]
                    end_idx = valid_idx_single[-1]
                    print('轨控时间段为' + stampToTime(time_list[start_idx]) + "-" + stampToTime(time_list[end_idx]))
            
            if len(final_data) > 0:
                # y_posible 已经取过绝对值，这里直接使用
                final_data_array = np.asarray(final_data, dtype=np.float64)
                result = np.std(final_data_array, dtype=np.float64, ddof=1)
                result_list.append(result)
                std_pre = result
            else:
                result_list.append(std_pre)
        else:
            result_list.append(std_pre)
    
    return result_list


def get_flag_indices_optimized(y_control, flag_values, mode_value, threshold):
    """
    优化的标志索引计算
    使用向量化操作替代循环和重复的 np.where 调用
    """
    y_control = np.asarray(y_control, dtype=np.float64)
    
    # 为每个标志值创建掩码
    masks = []
    for flag_value in flag_values:
        diff = np.abs(y_control - flag_value)
        if mode_value == 0:
            # >= 0.01
            mask = diff >= threshold
        else:
            # <= 0.01
            mask = diff <= threshold
        masks.append(mask)
    
    # 合并所有掩码
    if masks:
        combined_mask = np.any(masks, axis=0) if len(masks) > 1 else masks[0]
        return np.where(combined_mask)[0]
    else:
        return np.array([], dtype=np.int64)


def find_continuous_segments_optimized(indices, data_len, min_count):
    """
    优化的连续段检测（从索引数组）
    使用 NumPy 向量化操作替代正则表达式
    
    参数:
        indices: 满足条件的索引数组
        data_len: 数据总长度
        min_count: 最小连续次数
    
    返回:
        连续段的列表，每个段为 [start_idx, end_idx]
    """
    if len(indices) == 0:
        return []
    
    # 创建完整的掩码数组
    mask = np.zeros(data_len, dtype=bool)
    mask[indices] = True
    
    # 使用差分检测连续段的开始和结束
    # 在数组前后添加 False，以便检测边界
    extended_mask = np.concatenate(([False], mask, [False]))
    diff_mask = np.diff(extended_mask.astype(int))
    
    # 找到段的开始（从 False 到 True）和结束（从 True 到 False）
    starts = np.where(diff_mask == 1)[0]
    ends = np.where(diff_mask == -1)[0]
    
    # 过滤出长度 >= min_count 的段
    valid_segments = []
    for start, end in zip(starts, ends):
        if end - start >= min_count:
            valid_segments.append([start, end - 1])
    
    return valid_segments


def find_continuous_segments_optimized_from_mask(idxs, min_count):
    """
    优化的连续段检测（从掩码数组）
    对应原始代码中使用正则表达式检测连续 '1' 的逻辑
    
    参数:
        idxs: 掩码数组，1 表示满足条件，0 表示不满足
        min_count: 最小连续次数
    
    返回:
        连续段的列表，每个段为索引数组
    """
    if len(idxs) == 0:
        return []
    
    # 使用差分检测连续段的开始和结束
    # 在数组前后添加 0，以便检测边界
    extended_idxs = np.concatenate(([0], idxs, [0]))
    diff_mask = np.diff(extended_idxs)
    
    # 找到段的开始（从 0 到 1）和结束（从 1 到 0）
    starts = np.where(diff_mask == 1)[0]
    ends = np.where(diff_mask == -1)[0]
    
    # 过滤出长度 >= min_count 的段，并返回每个段的索引数组
    valid_segments = []
    for start, end in zip(starts, ends):
        if end - start >= min_count:
            # 返回该段的索引数组
            segment_indices = np.arange(start, end)
            valid_segments.append(segment_indices)
    
    return valid_segments


def get_control_period_optimized(time_list, tel_list, count):
    """
    优化的控制时段检测函数
    使用 NumPy 向量化操作替代正则表达式
    
    参数:
        time_list: 时间列表
        tel_list: 遥测数据列表
        count: 最小连续次数
    
    返回:
        时间段的索引列表，每个段为 [start_idx, end_idx]
    """
    tel_list = np.asarray(tel_list, dtype=np.float64)
    time_list = np.asarray(time_list, dtype=np.float64)
    
    # 计算差分
    tel_diff = np.diff(tel_list)
    
    # 找出非零差分的位置（变化点）
    change_mask = tel_diff != 0
    
    # 使用优化的连续段检测找出满足条件的段
    change_indices = np.where(change_mask)[0]
    
    if len(change_indices) == 0:
        return []
    
    # 找出连续的变化段（连续 count 次以上）
    valid_segments = find_continuous_segments_optimized(change_indices, len(tel_diff), count)
    
    # 转换为时间索引（注意：change_indices 是差分索引，需要 +1 才是原始索引）
    time_slice_list = []
    for seg in valid_segments:
        start_idx = seg[0] + 1  # 差分索引转原始索引
        end_idx = min(seg[1] + 2, len(time_list) - 1)  # 包含结束点
        if start_idx < len(time_list):
            time_slice_list.append([start_idx, end_idx])
    
    return time_slice_list


def calSigmoid(x, indicatorType, min_x, max_x):
    result = 0

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


def calGuassian(x, ideal_x, indicatorType, k, roundTFlag, min_x, max_x):
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


def three_sigma(df_col):
    '''
    剔野
    :param df_col:
    :return:
    '''
    rule = (df_col.mean() - 3 * df_col.std() > df_col) | (df_col.mean() + 3 * df_col.std() < df_col)
    index = np.arange(df_col.shape[0])[rule]
    return index


def timeToStamp(time_in):
    timearray = time.strptime(time_in, "%Y-%m-%d %H:%M:%S")
    return time.mktime(timearray)


def stampToTime(stamp):
    strtime = time.strftime("%Y-%m-%d %H:%M:%S", time.localtime(stamp))
    return strtime


if __name__ == "__main__":
    # 测试代码
    for i in range(1, 6):
        print(i)
    print(stampToTime(1594224000))
    print(stampToTime(1554134400))
    print(stampToTime(1644369837), stampToTime(1644371063))

