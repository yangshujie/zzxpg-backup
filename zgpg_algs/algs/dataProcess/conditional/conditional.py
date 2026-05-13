import pandas as pd
import numpy as np


def algsMain(*args, **kwargs):
    """
    条件交叉合并：根据条件进行交叉合并。

    :param args: 第一个参数为 {name: DataFrame} 字典。
    :param kwargs:
        sources: list, 数据源名称列表 [left_source, right_source]。
        left_key: str, 左侧键列名。
        right_key: str, 右侧键列名。
        operator: str, 比较操作符，'==', '!=', '<', '>', '<=', '>='。
    :return: (bool, result) 成功返回合并后的 DataFrame，失败返回错误信息。
    """
    try:
        data = args[0] if args else kwargs.get('data')
        if not isinstance(data, dict):
            raise ValueError("第一个参数必须是 {name: DataFrame} 字典")

        params = kwargs.get('params', {})
        sources = params.get('sources', list(data.keys()))
        left_key = params.get('left_key')
        right_key = params.get('right_key')
        operator = params.get('operator', '==')

        if len(sources) < 2:
            raise ValueError("至少需要两个数据源")
        if not left_key or not right_key:
            raise ValueError("必须提供 left_key 和 right_key")

        df1 = data[sources[0]].copy()
        df2 = data[sources[1]].copy()

        if left_key not in df1.columns:
            raise ValueError(f"左侧键列 {left_key} 不存在")
        if right_key not in df2.columns:
            raise ValueError(f"右侧键列 {right_key} 不存在")

        # 执行条件合并
        result, match_count, total_pairs = _conditional_join(df1, df2, left_key, right_key, operator)

        # 构建返回数据，包含匹配后的表和未被选择的表
        ret_data = {}
        merged_name = f"merged_{sources[0]}_{sources[1]}"
        ret_data[merged_name] = result
        for name in data.keys():
            if name not in sources:
                ret_data[name] = data[name]

        ret = {
            'data': ret_data,
            'analysis': {
                'sources': sources,
                'left_key': left_key,
                'right_key': right_key,
                'operator': operator,
                'left_rows': len(df1),
                'right_rows': len(df2),
                'match_count': match_count,
                'total_pairs_evaluated': total_pairs,
                'merged_name': merged_name
            }
        }
        return True, ret

    except Exception as e:
        return False, str(e)


def _conditional_join(df1, df2, left_key, right_key, operator='=='):
    """
    执行条件合并
    :return: (merged_df, match_count, total_pairs)
    """
    total_pairs = len(df1) * len(df2)
    match_count = 0

    # 识别重复列（除了键列），仅保留左表的字段
    duplicate_cols = [col for col in df2.columns if col in df1.columns and col != right_key]
    df2_filtered = df2.drop(columns=duplicate_cols) if duplicate_cols else df2

    # 使用向量化操作进行条件合并
    results = []

    for idx1, row1 in df1.iterrows():
        left_val = row1[left_key]
        right_vals = df2[right_key].values

        # 向量化比较
        if operator == '==':
            mask = right_vals == left_val
        elif operator == '!=':
            mask = right_vals != left_val
        elif operator == '<':
            mask = right_vals < left_val
        elif operator == '>':
            mask = right_vals > left_val
        elif operator == '<=':
            mask = right_vals <= left_val
        elif operator == '>=':
            mask = right_vals >= left_val
        else:
            raise ValueError(f"不支持的操作符: {operator}")

        matched_indices = np.where(mask)[0]
        match_count += len(matched_indices)

        if len(matched_indices) > 0:
            matched = df2_filtered.iloc[matched_indices].copy()
            # 添加左表的列值（不添加前缀，直接使用原列名）
            for col in df1.columns:
                if col not in matched.columns:
                    matched[col] = row1[col]
            results.append(matched)

    if results:
        merged_df = pd.concat(results, ignore_index=True)
        # 重新排列列顺序：左表列在前，右表独有列在后
        left_cols = list(df1.columns)
        right_only_cols = [col for col in df2_filtered.columns if col not in left_cols and col != right_key]
        merged_cols = left_cols + right_only_cols
        merged_df = merged_df[merged_cols]
    else:
        merged_df = pd.DataFrame()

    return merged_df, match_count, total_pairs