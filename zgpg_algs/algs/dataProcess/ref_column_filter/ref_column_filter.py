import pandas as pd
import numpy as np


def algsMain(*args, **kwargs):
    """
    参考列过滤：根据指定参考列的条件对数据进行行过滤。
    支持丢弃空值、值域范围过滤、指定有效值过滤，可组合使用。
    :param args: 第一个参数为 {name: DataFrame} 字典。
    :param kwargs:
        params: dict, 包含以下可选参数:
            sources: list, 数据源名称列表，支持多选，默认为全部表。
            ref_column: str, 参考列名（必填）。
            drop_null: bool, 是否丢弃参考列为空值的行，默认 True。
            value_range: list/tuple, [min, max] 参考列值域范围，保留范围内的行，默认 None 不启用。
            valid_values: list, 参考列有效值列表，保留匹配的行，默认 None 不启用。
            keep_columns: list, 输出保留的列名列表，默认 None 保留全部列。
    :return: (bool, ret) 成功返回过滤后的数据和分析信息，失败返回错误信息。
    """
    try:
        data = args[0] if args else kwargs.get('data')
        if not isinstance(data, dict):
            raise ValueError("第一个参数必须是 {name: DataFrame} 字典")

        params = kwargs.get('params', {})
        sources = params.get('sources', list(data.keys()))
        ref_column = params.get('ref_column')
        drop_null = params.get('drop_null', True)
        value_range = params.get('value_range', None)
        valid_values = params.get('valid_values', None)
        keep_columns = params.get('keep_columns', None)

        if not ref_column:
            raise ValueError("必须指定 ref_column 参数（参考列名）")

        ret = {
            'data': {},
            'analysis': {}
        }

        for name, df in data.items():
            if name in sources:
                df_filtered, analysis = _filter_by_ref_column(
                    df, ref_column=ref_column, drop_null=drop_null,
                    value_range=value_range, valid_values=valid_values,
                    keep_columns=keep_columns
                )
                ret['data'][name] = df_filtered
                ret['analysis'][name] = analysis
            else:
                ret['data'][name] = df

        ret['analysis']['sources'] = sources
        return True, ret

    except Exception as e:
        return False, str(e)


def _filter_by_ref_column(df, ref_column, drop_null=True, value_range=None,
                          valid_values=None, keep_columns=None):
    """
    根据参考列条件过滤 DataFrame。
    :param df: 输入 DataFrame。
    :param ref_column: 参考列名。
    :param drop_null: 是否丢弃参考列为空值的行。
    :param value_range: [min, max] 值域范围。
    :param valid_values: 有效值列表。
    :param keep_columns: 输出保留的列名列表。
    :return: (filtered_df, analysis_dict)
    """
    total_rows = len(df)

    if ref_column not in df.columns:
        raise ValueError(f"参考列 '{ref_column}' 不存在，可用列: {list(df.columns)}")

    mask = pd.Series(True, index=df.index)

    if drop_null:
        null_mask = df[ref_column].isna()
        mask = mask & ~null_mask
        dropped_null = int(null_mask.sum())
    else:
        dropped_null = 0

    dropped_range = 0
    if value_range is not None:
        if len(value_range) != 2:
            raise ValueError("value_range 必须是 [min, max] 格式的列表或元组")
        range_min, range_max = value_range
        ref_numeric = pd.to_numeric(df[ref_column], errors='coerce')
        out_of_range = (ref_numeric < range_min) | (ref_numeric > range_max)
        out_of_range = out_of_range & mask
        dropped_range = int(out_of_range.sum())
        mask = mask & ~((ref_numeric < range_min) | (ref_numeric > range_max))

    dropped_valid = 0
    if valid_values is not None:
        valid_set = set(valid_values)
        not_valid = ~df[ref_column].isin(valid_set)
        not_valid = not_valid & mask
        dropped_valid = int(not_valid.sum())
        mask = mask & ~not_valid

    filtered_df = df[mask].copy()
    filtered_rows = len(filtered_df)

    if keep_columns is not None:
        missing_cols = [c for c in keep_columns if c not in filtered_df.columns]
        if missing_cols:
            raise ValueError(f"keep_columns 中的列不存在: {missing_cols}")
        filtered_df = filtered_df[keep_columns]

    analysis = {
        'ref_column': ref_column,
        'total_rows': total_rows,
        'filtered_rows': filtered_rows,
        'removed_rows': total_rows - filtered_rows,
        'removal_rate': round((total_rows - filtered_rows) / total_rows, 4) if total_rows > 0 else 0.0,
        'drop_null': drop_null,
        'dropped_null_count': dropped_null,
        'value_range': value_range,
        'dropped_out_of_range_count': dropped_range,
        'valid_values': valid_values,
        'dropped_invalid_count': dropped_valid,
        'keep_columns': keep_columns
    }

    return filtered_df, analysis
