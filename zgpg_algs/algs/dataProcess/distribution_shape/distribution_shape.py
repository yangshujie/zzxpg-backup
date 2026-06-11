import pandas as pd
from scipy import stats as sp_stats


def algsMain(*args, **kwargs):
    """
    分布形态算子：计算偏度和峰度。
    不修改原始数据。

    :param args: 第一个参数为 {name: DataFrame} 字典。
    :param kwargs:
        params:
            sources: list|None, 要分析的数据源名称列表，None 表示所有数据源。
            column: str|None, 要分析的列名，None 表示所有数值列。
    :return: (bool, result) 成功返回统计结果，失败返回错误信息。
    """
    try:
        data = args[0] if args else kwargs.get('data')
        if not isinstance(data, dict):
            raise ValueError("第一个参数必须是 {name: DataFrame} 字典")

        params = kwargs.get('params', {})
        sources = params.get('sources')
        column = params.get('column')

        if sources is not None:
            missing = [s for s in sources if s not in data]
            if missing:
                raise ValueError(f"数据源 {missing} 不存在")

        target_sources = sources if sources is not None else list(data.keys())
        analysis = {}

        for name in target_sources:
            df = data[name]
            if column is not None:
                if column not in df.columns:
                    raise ValueError(f"数据源 {name} 中列 {column} 不存在，可用列: {list(df.columns)}")
                numeric_cols = [column]
            else:
                numeric_cols = list(df.select_dtypes(include='number').columns)

            if not numeric_cols:
                raise ValueError(f"数据源 {name} 中没有可分析的数值列")

            col_stats = {}
            for col in numeric_cols:
                s = df[col].dropna()
                count = int(s.count())
                skew_val = float(sp_stats.skew(s, bias=False)) if count > 2 else None
                kurt_val = float(sp_stats.kurtosis(s, bias=False)) if count > 3 else None

                col_stats[col] = {
                    'count': count,
                    'skewness': skew_val,
                    'kurtosis': kurt_val
                }
            analysis[name] = col_stats

        ret = {
            'data': data,
            'analysis': {
                'sources': sources,
                'column': column,
                'details': analysis
            }
        }
        return True, ret

    except Exception as e:
        return False, str(e)
