import pandas as pd


def algsMain(*args, **kwargs):
    """
    离散程度算子：计算极差、四分位差、方差、标准差、变异系数。
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
                mean_val = float(s.mean()) if count > 0 else None
                std_val = float(s.std()) if count > 1 else None

                col_stats[col] = {
                    'count': count,
                    'range': float(s.max() - s.min()) if count > 0 else None,
                    'iqr': float(s.quantile(0.75) - s.quantile(0.25)) if count > 0 else None,
                    'variance': float(s.var()) if count > 1 else None,
                    'std': std_val,
                    'cv': float(std_val / mean_val) if std_val is not None and mean_val is not None and mean_val != 0 else None
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
