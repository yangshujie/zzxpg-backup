import pandas as pd
import numpy as np


def algsMain(*args, **kwargs):
    """
    基于3σ准则剔除野值（异常值），支持异常值修复。
    :param args: 第一个参数为 DataFrame 或 {name: DataFrame} 字典。
    :param kwargs:
        data: DataFrame 或字典。
        drop: bool, 是否删除包含NaN的行，默认为False（修复模式）。
        columns: list, 需要处理的列名，默认为所有数值列。
        repair: bool, 是否修复异常值（基于双向线性插值），默认为True。
    :return: (bool, result) 成功时返回处理后的DataFrame和分析信息，失败返回错误信息。
    """
    try:
        data = args[0] if args else kwargs.get('data')
        params = kwargs.get('params', {})
        drop = params.get('drop', False)
        columns = params.get('columns', None)
        repair = params.get('repair', True)
        ret = {
            'data': {},
            'analysis': {}
        }
        if isinstance(data, dict):
            for name, df in data.items():
                df_result, analysis = _three_sigma(df, drop=drop, columns=columns, repair=repair)
                ret['data'][name] = df_result
                ret['analysis'][name] = analysis
            return True, ret
        else:
            raise ValueError("第一个参数必须是 {name: DataFrame} 字典")

    except Exception as e:
        return False, str(e)


def _three_sigma(df, drop=False, columns=None, repair=True):
    """
    处理单个 DataFrame，检测并修复异常值。
    :return: (处理后的DataFrame, 分析信息字典)
    """
    if columns is None:
        columns = df.select_dtypes(include=[np.number]).columns.tolist()

    df_clean = df.copy()
    analysis = {}

    for col in columns:
        if col not in df_clean.columns or df_clean[col].dtype not in [np.float64, np.int64]:
            continue

        series = df_clean[col].values.astype(float)
        n = len(series)

        # 计算3σ边界
        mean = np.nanmean(series)
        std = np.nanstd(series)
        lower = mean - 3 * std
        upper = mean + 3 * std

        # 检测异常值
        anomaly_mask = (series < lower) | (series > upper)
        anomaly_indices = np.where(anomaly_mask)[0]

        # 修复异常值（基于双向线性插值）
        if repair and len(anomaly_indices) > 0:
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

            df_clean[col] = filtered_y
        elif not repair:
            # 不修复，将异常值设为NaN
            df_clean.loc[anomaly_mask, col] = np.nan

        # 记录分析信息
        anomaly_values = series[anomaly_indices]
        filtered_values = df_clean[col].values[anomaly_indices] if repair else np.full(len(anomaly_indices), np.nan)
        analysis[col] = {
            'anomaly_count': len(anomaly_indices),
            'anomaly_rate': len(anomaly_indices) / n if n > 0 else 0,
            'mean': mean,
            'std': std,
            'lower_bound': lower,
            'upper_bound': upper,
            'anomalies': list(zip(anomaly_indices.tolist(), anomaly_values.tolist(), filtered_values.tolist()))
        }

    if drop:
        df_clean = df_clean.dropna(how='any')

    return df_clean, analysis
