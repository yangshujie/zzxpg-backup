import pandas as pd
import numpy as np


def algsMain(*args, **kwargs):
    """
    MAD（中位数绝对偏差）方法剔除野值，支持异常值修复。
    MAD 方法对异常值鲁棒，适用于含有大量异常值的数据。

    :param args: 第一个参数为 {name: DataFrame} 字典。
    :param kwargs:
        threshold: float, MAD 阈值，默认为 3.0。
        columns: list, 需要处理的列名，默认为所有数值列。
        drop: bool, 是否删除包含 NaN 的行，默认为 False。
    :return: (bool, result) 成功返回处理后的 DataFrame 和分析信息，失败返回错误信息。
    """
    try:
        data = args[0] if args else kwargs.get('data')
        params = kwargs.get('params', {})
        threshold = params.get('threshold', 3.0)
        columns = params.get('columns', None)
        drop = params.get('drop', False)

        ret = {
            'data': {},
            'analysis': {}
        }

        if isinstance(data, dict):
            for name, df in data.items():
                df_result, analysis = _mad_outlier(df, threshold=threshold, columns=columns, drop=drop)
                ret['data'][name] = df_result
                ret['analysis'][name] = analysis
            return True, ret
        else:
            raise ValueError("第一个参数必须是 {name: DataFrame} 字典")

    except Exception as e:
        return False, str(e)


def _mad_outlier(df, threshold=3.0, columns=None, drop=False):
    """
    处理单个 DataFrame，检测并修复异常值。
    :return: (处理后的DataFrame, 分析信息字典)
    """
    if columns is None:
        columns = df.select_dtypes(include=[np.number]).columns.tolist()

    df_result = df.copy()
    analysis = {}

    for col in columns:
        if col not in df_result.columns or df_result[col].dtype not in [np.float64, np.int64]:
            continue

        series = df_result[col].values.astype(float)
        n = len(series)

        # 计算中位数
        median = np.nanmedian(series)

        # 计算 MAD（中位数绝对偏差）
        mad = np.nanmedian(np.abs(series - median))

        # 避免除零
        if mad == 0:
            mad = 1e-10

        # 计算修正 Z-Score
        modified_z_score = 0.6745 * (series - median) / mad

        # 检测异常值
        anomaly_mask = np.abs(modified_z_score) > threshold
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
            'median': median,
            'mad': mad,
            'threshold': threshold,
            'anomalies': list(zip(anomaly_indices.tolist(), anomaly_values.tolist(), filtered_values.tolist()))
        }

    if drop:
        df_result = df_result.dropna(how='any')

    return df_result, analysis