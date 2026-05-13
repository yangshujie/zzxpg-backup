import pandas as pd
import numpy as np


def algsMain(*args, **kwargs):
    """
    鲁棒标准化：使用中位数和四分位距进行标准化，对异常值鲁棒。

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
                df_result, analysis = _robust_scale(df, columns=columns)
                ret['data'][name] = df_result
                ret['analysis'][name] = analysis
            return True, ret
        else:
            raise ValueError("第一个参数必须是 {name: DataFrame} 字典")

    except Exception as e:
        return False, str(e)


def _robust_scale(df, columns=None):
    """
    对单个 DataFrame 进行鲁棒标准化。
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
        orig_min = series.min()
        orig_max = series.max()
        orig_median = series.median()
        orig_q1 = series.quantile(0.25)
        orig_q3 = series.quantile(0.75)
        orig_iqr = orig_q3 - orig_q1

        if orig_iqr > 0:
            df_copy[col] = (series - orig_median) / orig_iqr
        else:
            df_copy[col] = 0

        new_series = df_copy[col]
        analysis[col] = {
            'before': {
                'mean': orig_mean,
                'std': orig_std,
                'median': orig_median,
                'Q1': orig_q1,
                'Q3': orig_q3,
                'IQR': orig_iqr,
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
