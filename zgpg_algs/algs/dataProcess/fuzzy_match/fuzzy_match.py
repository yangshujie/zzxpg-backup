import pandas as pd
import numpy as np
from difflib import SequenceMatcher


def algsMain(*args, **kwargs):
    """
    模糊匹配：基基于字符串相似度（SequenceMatcher）或包含关系（contains）进行模糊匹配
        处理重复列，仅保留左表的字段。
    :param args: 第一个参数为 {name: DataFrame} 字典。
    :param kwargs:
        sources: list, 数据源名称列表 [left_source, right_source]。
        left_col: str, 左侧匹配列名。
        right_col: str, 右侧匹配列名。
        threshold: float, 相似度阈值 0-1，默认为 0.8。
        method: str, 匹配方法，'contains'（包含匹配）或 'similarity'（相似度匹配），默认为 'similarity'。
    :return: (bool, result) 成功返回合并后的 DataFrame，失败返回错误信息。
    """
    try:
        data = args[0] if args else kwargs.get('data')
        if not isinstance(data, dict):
            raise ValueError("第一个参数必须是 {name: DataFrame} 字典")

        params = kwargs.get('params', {})
        sources = params.get('sources', list(data.keys()))
        left_col = params.get('left_col')
        right_col = params.get('right_col')
        threshold = params.get('threshold', 0.8)
        method = params.get('method', 'similarity')

        if len(sources) < 2:
            raise ValueError("至少需要两个数据源进行模糊匹配")

        if left_col is None or right_col is None:
            raise ValueError("必须提供 left_col 和 right_col 参数")

        df1 = data[sources[0]].copy()
        df2 = data[sources[1]].copy()

        if left_col not in df1.columns or right_col not in df2.columns:
            raise ValueError("指定的匹配列不存在")

        # 执行模糊匹配
        merged_df, match_count, total_pairs = _fuzzy_match(df1, df2, left_col, right_col, threshold, method)

        # 构建返回数据，包含匹配后的表和未被选择的表
        ret_data = {}
        merged_name = f"merged_{sources[0]}_{sources[1]}"
        ret_data[merged_name] = merged_df
        for name in data.keys():
            if name not in sources:
                ret_data[name] = data[name]

        ret = {
            'data': ret_data,
            'analysis': {
                'sources': sources,
                'left_rows': len(df1),
                'right_rows': len(df2),
                'match_count': match_count,
                'total_pairs_evaluated': total_pairs,
                'threshold': threshold,
                'method': method,
                'merged_name': merged_name
            }
        }
        return True, ret

    except Exception as e:
        return False, str(e)


def _calculate_similarity(s1, s2):
    """计算两个字符串的相似度"""
    return SequenceMatcher(None, str(s1).lower(), str(s2).lower()).ratio()


def _fuzzy_match(df1, df2, left_col, right_col, threshold, method='similarity'):
    """
    执行模糊匹配
    :return: (merged_df, match_count, total_pairs)
    """
    results = []
    match_count = 0
    total_pairs = len(df1) * len(df2)

    # 识别重复列（除了匹配列），仅保留左表的字段
    duplicate_cols = [col for col in df2.columns if col in df1.columns and col != right_col]
    df2_filtered = df2.drop(columns=duplicate_cols) if duplicate_cols else df2

    # 预处理右侧数据
    df2_right_vals = df2[right_col].astype(str).values

    for idx, row in df1.iterrows():
        left_val = str(row[left_col])

        if method == 'contains':
            # 包含匹配
            mask = df2[right_col].astype(str).str.contains(left_val, case=False, na=False)
            matched_indices = df2.index[mask]
        else:
            # 相似度匹配
            similarities = np.array([_calculate_similarity(left_val, rv) for rv in df2_right_vals])
            matched_indices = df2.index[similarities >= threshold]

        if len(matched_indices) > 0:
            match_count += len(matched_indices)
            matched = df2_filtered.loc[matched_indices].copy()
            # 添加左表的列值（不添加前缀，直接使用原列名）
            for col in df1.columns:
                if col not in matched.columns:
                    matched[col] = row[col]
            results.append(matched)

    if results:
        merged_df = pd.concat(results, ignore_index=True)
        # 重新排列列顺序：左表列在前，右表独有列在后
        left_cols = list(df1.columns)
        right_only_cols = [col for col in df2_filtered.columns if col not in left_cols and col != right_col]
        merged_cols = left_cols + right_only_cols
        merged_df = merged_df[merged_cols]
    else:
        merged_df = pd.DataFrame()

    return merged_df, match_count, total_pairs