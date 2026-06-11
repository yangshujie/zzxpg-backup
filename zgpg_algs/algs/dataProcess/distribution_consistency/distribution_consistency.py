import pandas as pd
from scipy import stats as sp_stats
from itertools import combinations


def algsMain(*args, **kwargs):
    """
    分布一致性检验：检验多个数据源是否来自相同的分布。
    支持的检验方法：K-S 检验、卡方检验、Anderson-Darling 检验。
    sources 为 None 时使用所有数据源。

    :param args: 第一个参数为 {name: DataFrame} 字典。
    :param kwargs:
        params:
            sources: list|None, 要检验的数据源名称列表，None 表示所有数据源。
            column: str, 要检验的列名。
            methods: list, 检验方法列表，可选 'ks', 'chi2', 'anderson'，默认全部。
    :return: (bool, result) 成功返回检验结果，失败返回错误信息。
    """
    try:
        data = args[0] if args else kwargs.get('data')
        if not isinstance(data, dict):
            raise ValueError("第一个参数必须是 {name: DataFrame} 字典")

        params = kwargs.get('params', {})
        sources = params.get('sources')
        column = params.get('column')
        methods = params.get('methods', ['ks', 'chi2', 'anderson'])

        if column is None:
            raise ValueError("必须提供 column 参数")
        if sources is not None and len(sources) < 2:
            raise ValueError("分布一致性检验至少需要两个数据源")

        if sources is not None:
            missing = [s for s in sources if s not in data]
            if missing:
                raise ValueError(f"数据源 {missing} 不存在")

        target_sources = sources if sources is not None else list(data.keys())
        if len(target_sources) < 2:
            raise ValueError("分布一致性检验至少需要两个数据源（当前仅有一个）")

        # 提取各源数据
        source_data = {}
        for name in target_sources:
            df = data[name]
            if column not in df.columns:
                raise ValueError(f"数据源 {name} 中列 {column} 不存在，可用列: {list(df.columns)}")
            source_data[name] = df[column].dropna().values

        pairs = list(combinations(target_sources, 2))
        pair_results = []

        for src_a, src_b in pairs:
            data_a = source_data[src_a]
            data_b = source_data[src_b]
            pair_result = {'source_a': src_a, 'source_b': src_b, 'tests': {}}

            if 'ks' in methods:
                stat, p_value = sp_stats.ks_2samp(data_a, data_b)
                pair_result['tests']['ks'] = {
                    'statistic': float(stat),
                    'p_value': float(p_value),
                    'same_distribution': p_value > 0.05
                }

            if 'chi2' in methods:
                min_val = min(data_a.min(), data_b.min())
                max_val = max(data_a.max(), data_b.max())
                n_bins = max(5, min(50, int(min(len(data_a), len(data_b)) / 5)))
                bins = pd.cut(
                    pd.concat([pd.Series(data_a), pd.Series(data_b)]),
                    bins=n_bins,
                    retbins=True
                )[1]
                hist_a, _ = pd.Series(data_a).histogram(bins=bins, density=False)
                hist_b, _ = pd.Series(data_b).histogram(bins=bins, density=False)
                hist_a = hist_a.astype(float)
                hist_b = hist_b.astype(float)
                total_a, total_b = hist_a.sum(), hist_b.sum()
                if total_a > 0 and total_b > 0:
                    expected_a = hist_a * (total_a + total_b) / (2 * total_a)
                    expected_b = hist_b * (total_b + total_a) / (2 * total_b)
                    chi_stat = float(((hist_a - expected_a) ** 2 / expected_a).sum() +
                                     ((hist_b - expected_b) ** 2 / expected_b).sum())
                    dof = n_bins - 1
                    p_value = float(1 - sp_stats.chi2.cdf(chi_stat, dof)) if dof > 0 else 1.0
                    pair_result['tests']['chi2'] = {
                        'statistic': chi_stat,
                        'p_value': p_value,
                        'dof': dof,
                        'same_distribution': p_value > 0.05
                    }

            if 'anderson' in methods:
                min_len = min(len(data_a), len(data_b))
                combined = pd.Series(data_a[:min_len]) - pd.Series(data_b[:min_len])
                result_ad = sp_stats.anderson(combined.dropna().values, dist='norm')
                ad_stat = float(result_ad.statistic)
                critical_values = {str(round(v, 2)): float(c) for v, c in zip([15, 10, 5, 2.5, 1], result_ad.critical_values)}
                pair_result['tests']['anderson'] = {
                    'statistic': ad_stat,
                    'critical_values': critical_values,
                    'same_distribution': ad_stat < result_ad.critical_values[2]
                }

            pair_results.append(pair_result)

        all_same = all(
            all(t.get('same_distribution', False) for t in pr['tests'].values())
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
