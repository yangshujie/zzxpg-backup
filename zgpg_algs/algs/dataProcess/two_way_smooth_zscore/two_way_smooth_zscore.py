# 2的问题：异常值的修复值仍然偏差较大，
# 修改异常值对称修复公式，使其不受异常值影响，只进行非异常值计算，并考虑到其双向相邻数据都可能存在其他异常值
# 现在采用双向搜索最近正常点进行线性插值，确保修复值平滑且合理
"""
Outlier Detection - Fully Optimized Version with Recursive Statistics and Interpolation-based Repair
Author: Adapted from Chen Yidan's original code, with patent-inspired enhancements
"""
import numpy as np
from collections import deque

def algsMain(*args, **kwargs):
    """
    type: 野值剔除
    name: 双向平滑Z分数
    优化版双向平滑Z分数异常值检测（递推统计量，异常修复基于最近正常点线性插值）

    :param data: 输入数据数组
    :param lag: 窗口大小，若为None则根据param_type自动选择
    :param threshold: 基础异常阈值
    :param influence: 保留仅用于兼容，实际修复使用插值
    :param epsilon: 标准差最小值，防止除零
    :param influence_sliding: 是否在滑动过程中用平滑值更新统计量（防止异常污染）
    :return: (anomalies, filtered_y) 或字典
    """

    try:
        data = args[0] if args else kwargs.get('data')
        params = kwargs.get('params', {})
        threshold = params.get('threshold',3.5)
        influence = params.get('influence',0.5)
        epsilon = params.get('epsilon',1e-6)
        influence_sliding = params.get('influence_sliding',False)
        lag = params.get('lag',None)
        ret = {
            'data': {},
            'analysis': {}
        }
        if isinstance(data, dict):
            for name, df in data.items():
                df_od = df
                ret['analysis'][name] = {}
                for column in df.columns:
                    if column == 'time' or column == 'timestamp':
                        pass
                    else:
                        this_column_ret = _two_way_smoothed_z_score_optimized(data=df[column],
                                                                              threshold=threshold,
                                                                              influence = influence,
                                                                              epsilon = epsilon,
                                                                              influence_sliding = influence_sliding,
                                                                              lag = lag)
                        df_od[column] = this_column_ret["filtered_y"]
                        ret['analysis'][name][column] = this_column_ret["anomalies"]
                ret['data'][name] = df_od
            return True, ret
        else:
            raise ValueError("第一个参数必须是  {name: DataFrame} 字典")
    except Exception as e:
        return False, str(e)

def _two_way_smoothed_z_score_optimized(
    data,
    lag=None,
    threshold=3.5,
    influence=0.5,          # 注：修复逻辑已不使用此参数，保留仅用于接口兼容
    epsilon=1e-6,
    param_type='auto',
    dynamic_threshold=False,
    alpha=0.1,
    influence_sliding=False
):
    """
    优化版双向平滑Z分数异常值检测（递推统计量，异常修复基于最近正常点线性插值）

    :param data: 输入数据数组
    :param lag: 窗口大小，若为None则根据param_type自动选择
    :param threshold: 基础异常阈值
    :param influence: 保留仅用于兼容，实际修复使用插值
    :param epsilon: 标准差最小值，防止除零
    :param param_type: 参数类型，'slow'（缓变参数）或'step'（阶跃参数），用于自动选择lag
    :param dynamic_threshold: 是否启用动态阈值（基于IQR，暂未实现）
    :param alpha: 动态阈值调整系数（预留）
    :param influence_sliding: 是否在滑动过程中用平滑值更新统计量（防止异常污染）
    :param return_params: 若为True，返回包含异常点、修正数据和所用参数的字典
    :return: (anomalies, filtered_y) 或字典
    """
    n = len(data)
    # ----- 自适应窗口 -----
    if lag is None:
        if param_type == 'slow':
            lag = 20
        elif param_type == 'step':
            lag = 5
        else:  # auto
            lag = max(3, min(20, int(n * 0.01)))
    lag = max(1, min(lag, n))  # 确保窗口合理

    # ----- 初始化 -----
    signals = np.zeros(n, dtype=int)      # 异常标记（0/1/2）
    filtered_y = np.array(data, dtype=float)  # 最终修正序列

    # 统计量数组（可选保留，便于调试）
    avg_forward = np.zeros(n)
    std_forward = np.zeros(n)
    avg_backward = np.zeros(n)
    std_backward = np.zeros(n)

    # ========== 前向滑动（递推统计） ==========
    # 初始窗口 [0, lag)
    init_window = data[:lag]
    window_deque = deque(init_window)          # 存储当前窗口内的 filtered_y 值（初始为 data）
    window_sum = sum(init_window)
    window_sum2 = sum(x * x for x in init_window)

    # 记录初始窗口统计量
    avg_forward[lag - 1] = window_sum / lag
    std_forward[lag - 1] = max(np.sqrt(max(0, window_sum2 / lag - (window_sum / lag) ** 2)), epsilon)

    #正向
    for i in range(lag, n):
        # 当前窗口为 filtered_y[i-lag:i]，即 window_deque 中的内容
        # 计算均值和标准差（使用递推维护的 sum 和 sum2）
        mean = window_sum / lag
        variance = max(0, window_sum2 / lag - mean * mean)
        std = max(np.sqrt(variance), epsilon)
        avg_forward[i] = mean
        std_forward[i] = std

        # 异常判断
        if abs(data[i] - avg_forward[i - 1]) > threshold * std_forward[i - 1]:
            signals[i] += 1
            if influence_sliding:
                # 用平滑值参与后续统计
                filtered_y[i] = influence * data[i] + (1 - influence) * filtered_y[i - 1]
            else:
                filtered_y[i] = data[i]  # 暂不修正
        else:
            signals[i] += 0
            filtered_y[i] = data[i]

        # 窗口滑动：移除最左边的元素（filtered_y[i-lag]），加入 filtered_y[i]
        left_old = window_deque.popleft()          # 移出的元素
        window_deque.append(filtered_y[i])         # 加入新元素

        # 更新总和与平方和
        window_sum = window_sum - left_old + filtered_y[i]
        window_sum2 = window_sum2 - left_old * left_old + filtered_y[i] * filtered_y[i]

    # ========== 反向滑动（递推统计，使用原始 data） ==========
    # 初始窗口 [n-lag, n)
    init_window_back = data[-lag:]
    window_deque_back = deque(init_window_back)
    window_sum_back = sum(init_window_back)
    window_sum2_back = sum(x * x for x in init_window_back)

    avg_backward[n - lag] = window_sum_back / lag
    std_backward[n - lag] = max(np.sqrt(max(0, window_sum2_back / lag - (window_sum_back / lag) ** 2)), epsilon)

    #反向
    for i in range(n - lag - 1, -1, -1):
        # 当前窗口为 data[i+1:i+lag+1]，即 window_deque_back 中的内容
        mean = window_sum_back / lag
        variance = max(0, window_sum2_back / lag - mean * mean)
        std = max(np.sqrt(variance), epsilon)
        avg_backward[i] = mean
        std_backward[i] = std

        # 异常判断
        if abs(data[i] - avg_backward[i + 1]) > threshold * std_backward[i + 1]:
            signals[i] += 1

        # 窗口向左滑动：移除最右边的元素（data[i+lag]），加入 data[i]
        right_old = window_deque_back.pop()          # 移出的元素
        window_deque_back.appendleft(data[i])        # 加入新元素

        # 更新总和与平方和
        window_sum_back = window_sum_back - right_old + data[i]
        window_sum2_back = window_sum2_back - right_old * right_old + data[i] * data[i]

    # ----- 双向融合 -----
    final_anomaly_mask = (signals == 2)

    # ----- 基于非异常点的双向线性插值修复 -----
    normal_mask = ~final_anomaly_mask
    for i in np.where(final_anomaly_mask)[0]:
        # 向左搜索最近正常点
        left_idx = i - 1
        while left_idx >= 0 and final_anomaly_mask[left_idx]:
            left_idx -= 1
        # 向右搜索最近正常点
        right_idx = i + 1
        while right_idx < n and final_anomaly_mask[right_idx]:
            right_idx += 1

        if left_idx >= 0 and right_idx < n:
            # 两侧都有正常点，线性插值
            left_val = data[left_idx]
            right_val = data[right_idx]
            filtered_y[i] = left_val + (right_val - left_val) * (i - left_idx) / (right_idx - left_idx)
        elif left_idx >= 0:
            # 仅左侧有正常点，使用左侧值
            filtered_y[i] = data[left_idx]
        elif right_idx < n:
            # 仅右侧有正常点，使用右侧值
            filtered_y[i] = data[right_idx]
        else:
            # 整个序列都是异常点（极少见），保持原值
            filtered_y[i] = data[i]

    # ----- 提取异常点列表 -----
    anomaly_indices = np.where(final_anomaly_mask)[0]
    anomaly_values = data[anomaly_indices]
    filtered_values = filtered_y[anomaly_indices]
    anomalies = list(zip(anomaly_indices, anomaly_values, filtered_values))

    # ----- 返回结果 -----
    params = {
        'lag': lag,
        'threshold': threshold,
        'influence': influence,
        'epsilon': epsilon,
        'param_type': param_type,
        'dynamic_threshold': dynamic_threshold,
        'alpha': alpha,
        'influence_sliding': influence_sliding,
        'n': n
    }
    return {
        'anomalies': anomalies,
        'filtered_y': filtered_y,
        'params': params
    }



