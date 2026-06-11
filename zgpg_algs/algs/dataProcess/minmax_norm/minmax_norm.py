import pandas as pd
import numpy as np


def algsMain(*args, **kwargs):
    """
    Min-Max 归一化：将数据缩放到 [0, 1] 范围。

    :param args: 第一个参数为 {name: DataFrame} 字典。
    :param kwargs:
        data: {name: DataFrame} 字典。
        columns: list, 需要归一化的列名，默认为所有数值列。
        feature_range: tuple, 目标范围，默认为 (0, 1)。
    :return: (bool, result) 成功时返回处理后的DataFrame和分析信息，失败返回错误信息。
    """
    try:
        data = args[0] if args else kwargs.get('data')
        params = kwargs.get('params', {})
        columns = params.get('columns', None)
        feature_range = params.get('feature_range', (0, 1))

        ret = {
            'data': {},
            'analysis': {}
        }

        if isinstance(data, dict):
            for name, df in data.items():
                df_result, analysis = _minmax_norm(df, columns=columns, feature_range=feature_range)
                ret['data'][name] = df_result
                ret['analysis'][name] = analysis
            return True, ret
        else:
            raise ValueError("第一个参数必须是 {name: DataFrame} 字典")

    except Exception as e:
        return False, str(e)


def _minmax_norm(df, columns=None, feature_range=(0, 1)):
    """
    对单个 DataFrame 进行 Min-Max 归一化。
    :return: (处理后的DataFrame, 分析信息字典)
    """
    if columns is None:
        columns = df.select_dtypes(include=[np.number]).columns.tolist()

    df_copy = df.copy()
    analysis = {}
    min_val, max_val = feature_range

    for col in columns:
        if col not in df_copy.columns or df_copy[col].dtype not in [np.float64, np.int64]:
            continue

        series = df_copy[col]
        orig_mean = series.mean()
        orig_std = series.std()
        orig_min = series.min()
        orig_max = series.max()

        col_min = series.min()
        col_max = series.max()
        if col_max > col_min:
            df_copy[col] = (series - col_min) / (col_max - col_min) * (max_val - min_val) + min_val
        else:
            df_copy[col] = min_val

        new_series = df_copy[col]
        analysis[col] = {
            'before': {
                'mean': orig_mean,
                'std': orig_std,
                'min': orig_min,
                'max': orig_max,
                'count': len(series),
            },
            'after': {
                'mean': new_series.mean(),
                'std': new_series.std(),
                'min': new_series.min(),
                'max': new_series.max(),
                'count': len(new_series),
            },
        }

    return df_copy, analysis
