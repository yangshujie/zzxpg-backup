import pandas as pd
import numpy as np


def algsMain(*args, **kwargs):
    """
    基于 Smooth Z-Score 算法剔除野值（适用于时序数据），支持异常值修复。
    使用滑动窗口计算均值和标准差，更适合时序数据的特点。

    :param args: 第一个参数为 DataFrame 或 {name: DataFrame} 字典。
    :param kwargs:
        data: DataFrame 或字典。
        window_size: int, 滑动窗口大小，默认为 10。
        threshold: float, Z-Score 阈值，超过该值判定为野值，默认为 3.0。
        influence: float, 保留仅用于兼容，实际修复使用插值。
        epsilon: float, 标准差最小值，防止除零，默认为 1e-10。
    :return: (bool, result) 成功返回处理后的 DataFrame 和分析信息，失败返回错误信息。
    """
    try:
        data = args[0] if args else kwargs.get('data')
        params = kwargs.get('params', {})
        window_size = params.get('window_size', 10)
        threshold = params.get('threshold', 3.0)
        influence = params.get('influence', 0.5)
        epsilon = params.get('epsilon', 1e-10)

        ret = {
            'data': {},
            'analysis': {}
        }

        if isinstance(data, dict):
            for name, df in data.items():
                df_result, analysis = _process_single(df, window_size=window_size,
                                                      threshold=threshold,
                                                      influence=influence,
                                                      epsilon=epsilon)
                ret['data'][name] = df_result
                ret['analysis'][name] = analysis
            return True, ret
        else:
            raise ValueError("第一个参数必须是 {name: DataFrame} 字典")

    except Exception as e:
        return False, str(e)


def _process_single(df, window_size=10, threshold=3.0, influence=0.5, epsilon=1e-10):
    """
    处理单个 DataFrame，检测并修复异常值。
    :return: (处理后的DataFrame, 分析信息字典)
    """
    # 检查并删除特定的关键字列
    keywords_to_remove = ['time', 'Time', 'TIME', 'data']
    columns = [col for col in df.columns if col not in keywords_to_remove]

    df_result = df.copy()
    analysis = {}

    for col in columns:
        if col not in df_result.columns or df_result[col].dtype not in [np.float64, np.int64]:
            continue

        series = df_result[col].values.astype(float)
        n = len(series)

        # 计算滑动窗口的均值和标准差
        rolling_mean = df_result[col].rolling(window=window_size, center=True, min_periods=1).mean().values
        rolling_std = df_result[col].rolling(window=window_size, center=True, min_periods=1).std().values
        rolling_std = np.maximum(rolling_std, epsilon)  # 防止除零

        # 计算 Z-Score
        z_score = np.abs((series - rolling_mean) / rolling_std)

        # 检测异常值
        anomaly_mask = z_score > threshold
        anomaly_indices = np.where(anomaly_mask)[0]

        # 修复异常值（基于双向线性插值）
        if len(anomaly_indices) > 0:
            filtered_y = series.copy()
            for idx in anomaly_indices:
                # 向左搜索最近正常点
                left_idx = idx - 1
                while left_idx >= 0 and anomaly_mask[left_idx]:
                    left_idx -= 1
                # 向右搜索最近正常点
                right_idx = idx + 1
                while right_idx < n and anomaly_mask[right_idx]:
                    right_idx += 1

                if left_idx >= 0 and right_idx < n:
                    # 两侧都有正常点，线性插值
                    filtered_y[idx] = series[left_idx] + (series[right_idx] - series[left_idx]) * (idx - left_idx) / (right_idx - left_idx)
                elif left_idx >= 0:
                    # 仅左侧有正常点
                    filtered_y[idx] = series[left_idx]
                elif right_idx < n:
                    # 仅右侧有正常点
                    filtered_y[idx] = series[right_idx]
                else:
                    # 整个序列都是异常点，保持原值
                    filtered_y[idx] = series[idx]

            df_result[col] = filtered_y

        # 记录分析信息
        anomaly_values = series[anomaly_indices]
        filtered_values = df_result[col].values[anomaly_indices]
        analysis[col] = {
            'anomaly_count': len(anomaly_indices),
            'anomaly_rate': len(anomaly_indices) / n if n > 0 else 0,
            'window_size': window_size,
            'threshold': threshold,
            'anomalies': list(zip(anomaly_indices.tolist(), anomaly_values.tolist(), filtered_values.tolist()))
        }

    return df_result, analysis