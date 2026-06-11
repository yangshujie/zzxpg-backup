import pandas as pd
from scipy import stats as sp_stats
from itertools import combinations


def algsMain(*args, **kwargs):
    """
    方差一致性检验：检验多个数据源的离散程度是否相同。
    支持的检验方法：Levene 检验、Bartlett 检验。
    sources 为 None 时使用所有数据源。

    :param args: 第一个参数为 {name: DataFrame} 字典。
    :param kwargs:
        params:
            sources: list|None, 要检验的数据源名称列表，None 表示所有数据源。
            column: str, 要检验的列名。
            methods: list, 检验方法列表，可选 'levene', 'bartlett'，默认全部。
            center: str, Levene 检验的中心度量，'mean'/'median'/'trimmed'，默认 'median'。
    :return: (bool, result) 成功返回检验结果，失败返回错误信息。
    """
    try:
        data = args[0] if args else kwargs.get('data')
        if not isinstance(data, dict):
            raise ValueError("第一个参数必须是 {name: DataFrame} 字典")

        params = kwargs.get('params', {})
        sources = params.get('sources')
        column = params.get('column')
        methods = params.get('methods', ['levene', 'bartlett'])
        center = params.get('center', 'median')

        if column is None:
            raise ValueError("必须提供 column 参数")
        if sources is not None and len(sources) < 2:
            raise ValueError("方差一致性检验至少需要两个数据源")

        if sources is not None:
            missing = [s for s in sources if s not in data]
            if missing:
                raise ValueError(f"数据源 {missing} 不存在")

        target_sources = sources if sources is not None else list(data.keys())
        if len(target_sources) < 2:
            raise ValueError("方差一致性检验至少需要两个数据源（当前仅有一个）")

        # 提取各源数据
        source_data = {}
        for name in target_sources:
            df = data[name]
            if column not in df.columns:
                raise ValueError(f"数据源 {name} 中列 {column} 不存在，可用列: {list(df.columns)}")
            vals = df[column].dropna().values
            if len(vals) < 2:
                raise ValueError(f"数据源 {name} 中列 {column} 有效数据不足（至少需要 2 条）")
            source_data[name] = vals

        pairs = list(combinations(target_sources, 2))
        pair_results = []

        for src_a, src_b in pairs:
            data_a = source_data[src_a]
            data_b = source_data[src_b]
            pair_result = {'source_a': src_a, 'source_b': src_b, 'tests': {}}

            if 'levene' in methods:
                stat, p_value = sp_stats.levene(data_a, data_b, center=center)
                pair_result['tests']['levene'] = {
                    'statistic': float(stat),
                    'p_value': float(p_value),
                    'center': center,
                    'equal_variance': p_value > 0.05
                }

            if 'bartlett' in methods:
                stat, p_value = sp_stats.bartlett(data_a, data_b)
                pair_result['tests']['bartlett'] = {
                    'statistic': float(stat),
                    'p_value': float(p_value),
                    'equal_variance': p_value > 0.05
                }

            pair_results.append(pair_result)

        all_same = all(
            all(t.get('equal_variance', False) for t in pr['tests'].values())
            for pr in pair_results
        )

        ret = {
            'data': data,
            'analysis': {
                'sources': sources,
                'column': column,
                'methods': methods,
                'consistent': all_same,
                'details': pair_results
            }
        }
        return True, ret

    except Exception as e:
        return False, str(e)
