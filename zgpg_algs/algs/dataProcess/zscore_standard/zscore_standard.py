
import pandas as pd
import numpy as np


def algsMain(*args, **kwargs):
    """
    Z-Score 标准化：将数据转换为均值为 0，标准差为 1 的分布。

    :param args: 第一个参数为 {name: DataFrame} 字典。
    :param kwargs:
        data: {name: DataFrame} 字典。
        columns: list, 需要标准化的列名，默认为所有数值列。
    :return: (bool, result) 成功时返回处理后的DataFrame和分析信息，失败返回错误信息。
    """
    try:
        data = args[0] if args else kwargs.get('data')
        params = kwargs.get('params', {})
        columns = params.get('columns', None)

        ret = {
            'data': {},
            'analysis': {}
        }

        if isinstance(data, dict):
            for name, df in data.items():
                df_result, analysis = _zscore_standard(df, columns=columns)
                ret['data'][name] = df_result
                ret['analysis'][name] = analysis
            return True, ret
        else:
            raise ValueError("第一个参数必须是 {name: DataFrame} 字典")

    except Exception as e:
        return False, str(e)


def _zscore_standard(df, columns=None):
    """
    对单个 DataFrame 进行 Z-Score 标准化。
    :return: (处理后的DataFrame, 分析信息字典)
    """
    if columns is None:
        columns = df.select_dtypes(include=[np.number]).columns.tolist()

    df_copy = df.copy()
    analysis = {}

    for col in columns:
        if col not in df_copy.columns or df_copy[col].dtype not in [np.float64, np.int64]:
            continue

        series = df_copy[col]
        orig_mean = series.mean()
        orig_std = series.std()
        if orig_std > 0:
            df_copy[col] = (series - orig_mean) / orig_std
        else:
            df_copy[col] = 0

        new_series = df_copy[col]
        analysis[col] = {
            'before': {
                'mean': orig_mean,
                'std': orig_std,
                'min': series.min(),
                'max': series.max(),
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