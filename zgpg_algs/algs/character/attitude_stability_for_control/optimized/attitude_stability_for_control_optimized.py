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
        print("轨控姿态稳定度(使用推力器喷气判断)")
        all_data = args[0]
        config = kwargs["config"]
        result_list = calMain(all_data, config)

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
    count = int(config.get("count", 10))
    std_pre = 0
    
    for tel_by_day in all_data:
        time_list = tel_by_day[0]
        if len(time_list) == 0:
            result_list.append(std_pre)
            continue
        
        # 转换为 NumPy 数组，避免重复转换
        time_list = np.asarray(time_list, dtype=np.float64)
        
        # 提取推力器通道和姿态数据
        index = []
        other_tel = []
        y_tel_parts = []
        
        # 检查是否需要跳过当前日期
        if "reConductDate" in config.keys() and config["reConductDate"] != '':
            re_conduct_date = config["reConductDate"]
            if re_conduct_date != '' and stampToTime(time_list[0]).split(' ')[0] == re_conduct_date.split(' ')[0]:
                result_list.append(std_pre)
                continue
        
        # 提取推力器通道和姿态数据
        for i in range(0, len(tel_by_day) - 1):
            thrust_key = 'thrust' + str(i + 1)
            if thrust_key in config.keys():
                thrust_idx = int(config[thrust_key])
                other_tel.append(np.asarray(tel_by_day[thrust_idx + 1], dtype=np.float64))
                index.append(thrust_idx)
            if i not in index:
                y_tel_parts.append(np.asarray(tel_by_day[i + 1], dtype=np.float64))
        
        # 合并姿态数据
        if len(y_tel_parts) == 1:
            y_tel = y_tel_parts[0]
        elif len(y_tel_parts) > 1:
            y_tel = np.concatenate(y_tel_parts)
        else:
            y_tel = np.array([], dtype=np.float64)
        
        # 获取所有控制时段
        all_time_list = []
        for one_tel in other_tel:
            slice_time_list = get_control_period_optimized(time_list, one_tel, count)
            all_time_list.extend(slice_time_list)
        
        if all_time_list:
            # 合并所有时段的索引
            index_list = merge_index_segments(all_time_list)
            
            # 添加检查：确保 index_list 不为空且长度为偶数
            if len(index_list) == 0:
                result_list.append(std_pre)
                continue
            
            if len(index_list) % 2 != 0:
                # 如果长度为奇数，跳过本次处理
                result_list.append(std_pre)
                continue
            
            # 确保 y_tel 不为空
            if len(y_tel) == 0:
                result_list.append(std_pre)
                continue
            
            # 对每个时段计算标准差
            res = []
            for i in range(int(len(index_list) / 2)):
                start_idx = int(index_list[2 * i])
                end_idx = int(index_list[2 * i + 1]) + 1
                
                # 添加边界检查：确保索引不越界
                if start_idx < 0 or start_idx >= len(time_list) or start_idx >= len(y_tel):
                    continue
                if end_idx < 0 or end_idx > len(time_list) or end_idx > len(y_tel):
                    continue
                
                # 确保索引不越界
                start_idx = max(0, min(start_idx, len(y_tel) - 1))
                end_idx = max(start_idx + 1, min(end_idx, len(y_tel)))
                
                if start_idx < len(y_tel) and end_idx <= len(y_tel):
                    segment_data = y_tel[start_idx:end_idx]
                    if len(segment_data) > 0:
                        tel_np = np.abs(np.asarray(segment_data, dtype=np.float64))
                        std_val = np.std(tel_np, dtype=np.float64, ddof=1)
                        res.append(std_val)
                        
                        # 确保时间索引不越界
                        time_start_idx = min(start_idx, len(time_list) - 1)
                        time_end_idx = min(end_idx - 1, len(time_list) - 1)
                        if time_start_idx < len(time_list) and time_end_idx < len(time_list):
                            print('轨控时间段为' + stampToTime(time_list[time_start_idx]) + "-" + stampToTime(time_list[time_end_idx]))
            
            if len(res) > 0:
                result_list.append(np.mean(res))
                std_pre = result_list[-1]
            else:
                result_list.append(std_pre)
        else:
            result_list.append(std_pre)
    
    return result_list


def merge_index_segments(segments):
    """
    合并索引段
    使用 NumPy 的 union1d 合并所有索引段
    
    参数:
        segments: 索引段列表，每个段为 [start_idx, end_idx]
    
    返回:
        合并后的索引数组（已排序）
    """
    if len(segments) == 0:
        return np.array([], dtype=np.int64)
    
    # 将所有段转换为索引数组并合并
    all_indices = segments[0]
    for segment in segments[1:]:
        all_indices = np.union1d(all_indices, segment)
    
    return all_indices


def get_control_period_optimized(time_list, tel_list, count):
    """
    优化的控制时段检测函数
    使用 NumPy 向量化操作替代正则表达式
    
    参数:
        time_list: 时间列表
        tel_list: 遥测数据列表
        count: 最小连续次数（连续 count 次以上没有变化的部分被过滤）
    
    返回:
        时间段的索引列表，每个段为 [start_idx, end_idx]
    """
    tel_list = np.asarray(tel_list, dtype=np.float64)
    time_list = np.asarray(time_list, dtype=np.float64)
    
    # 计算差分
    tel_diff = np.diff(tel_list)
    
    # standard_tiff: 0 表示没有变化，1 表示有变化
    standard_tiff = np.where(tel_diff == 0, 0, 1)
    
    # 找出连续 count 次以上为 0 的段（连续 count 次以上没有变化）
    # 这些段在原始代码中被标记为 'c'，需要被过滤掉
    zero_segments = find_continuous_segments_optimized(standard_tiff, count, find_zeros=True)
    
    # 创建掩码：标记哪些位置是连续 count 次以上的 0
    long_zero_mask = np.zeros(len(standard_tiff), dtype=bool)
    for seg in zero_segments:
        if seg[0] < len(long_zero_mask) and seg[1] < len(long_zero_mask):
            long_zero_mask[seg[0]:seg[1]+1] = True
    
    # satisfied_index: 不在长连续 0 段中的索引（对应原始代码的 tel_sub != "c"）
    satisfied_index = np.where(~long_zero_mask)[0]
    
    if len(satisfied_index) == 0:
        return []
    
    # 找出连续段的边界
    time_slice_list = []
    index_diff = np.where(np.diff(satisfied_index) > 1)[0]
    
    if len(index_diff) > 0:
        # 多个连续段
        for i, item in enumerate(index_diff):
            if i == 0:
                # 第一段
                end_idx = satisfied_index[item] + 2
                if end_idx >= len(time_list):
                    end_idx = len(time_list) - 1
                time_slice = [satisfied_index[0] + 1, end_idx]
                time_slice_list.append(time_slice)
            else:
                # 中间段
                start_idx = satisfied_index[index_diff[i - 1] + 1] + 1
                end_idx = satisfied_index[item] + 2
                if end_idx >= len(time_list):
                    end_idx = len(time_list) - 1
                time_slice = [start_idx, end_idx]
                time_slice_list.append(time_slice)
        
        # 最后一段
        start_idx = satisfied_index[index_diff[-1] + 1] + 1
        end_idx = satisfied_index[-1] + 2
        if end_idx >= len(time_list):
            end_idx = len(time_list) - 1
        time_slice = [start_idx, end_idx]
        time_slice_list.append(time_slice)
    else:
        # 单个连续段
        start_idx = satisfied_index[0] + 1
        end_idx = satisfied_index[-1] + 2
        if end_idx >= len(time_list):
            end_idx = len(time_list) - 1
        time_slice = [start_idx, end_idx]
        time_slice_list.append(time_slice)
    
    return time_slice_list


def find_continuous_segments_optimized(data, min_count, find_zeros=False):
    """
    优化的连续段检测
    使用 NumPy 向量化操作替代正则表达式
    
    参数:
        data: 数据数组（0 和 1 的数组）
        min_count: 最小连续次数
        find_zeros: 如果为 True，找出连续 0 的段；如果为 False，找出连续 1 的段
    
    返回:
        连续段的列表，每个段为 [start_idx, end_idx]
    """
    if len(data) == 0:
        return []
    
    # 根据 find_zeros 参数决定查找 0 还是 1
    if find_zeros:
        target_value = 0
    else:
        target_value = 1
    
    # 创建掩码
    mask = (data == target_value)
    
    # 使用差分检测连续段的开始和结束
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

