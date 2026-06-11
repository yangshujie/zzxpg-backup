import pandas as pd


def algsMain(*args, **kwargs):
    """
    集中趋势算子：计算平均数、众数、中位数和各分位数。
    不修改原始数据。

    :param args: 第一个参数为 {name: DataFrame} 字典。
    :param kwargs:
        params:
            sources: list|None, 要分析的数据源名称列表，None 表示所有数据源。
            column: str|None, 要分析的列名，None 表示所有数值列。
            quantiles: list, 分位数列表，如 [0.25, 0.5, 0.75]，默认 [0.25, 0.5, 0.75]。
    :return: (bool, result) 成功返回统计结果，失败返回错误信息。
    """
    try:
        data = args[0] if args else kwargs.get('data')
        if not isinstance(data, dict):
            raise ValueError("第一个参数必须是 {name: DataFrame} 字典")

        params = kwargs.get('params', {})
        sources = params.get('sources')
        column = params.get('column')
        quantiles = params.get('quantiles', [0.25, 0.5, 0.75])

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
                stats = {
                    'count': int(s.count()),
                    'mean': float(s.mean()) if len(s) > 0 else None,
                    'median': float(s.median()) if len(s) > 0 else None,
                }

                mode_vals = s.mode()
                stats['mode'] = [float(v) for v in mode_vals] if len(mode_vals) > 0 else []

                q_stats = {}
                for q in quantiles:
                    q_stats[str(q)] = float(s.quantile(q)) if len(s) > 0 else None
                stats['quantiles'] = q_stats

                col_stats[col] = stats
            analysis[name] = col_stats

        ret = {
            'data': data,
            'analysis': {
                'sources': sources,
                'column': column,
                'quantiles': quantiles,
                'details': analysis
            }
        }
        return True, ret

    except Exception as e:
        return False, str(e)
