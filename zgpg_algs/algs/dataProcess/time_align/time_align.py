import pandas as pd


def _resample_df(df, time_col, granularity, agg_method):
    """将 DataFrame 按指定时间粒度重采样，每个窗口取一个聚合值。"""
    df = df.set_index(time_col)
    numeric_cols = df.select_dtypes(include='number').columns.tolist()
    non_numeric_cols = [c for c in df.columns if c not in numeric_cols]

    agg_dict = {col: agg_method for col in numeric_cols}
    for col in non_numeric_cols:
        agg_dict[col] = 'first'

    resampled = df.resample(granularity).agg(agg_dict)
    return resampled.reset_index()


def algsMain(*args, **kwargs):
    """
    时间粒度对齐：按指定时间粒度统一所有表的采样频率，每个时间窗口取一个样本。
    :param args: 第一个参数为 {name: DataFrame} 字典。
    :param kwargs:
        sources: list, 数据源名称列表，支持多选，默认为全部表。
        time_col: str, 时间列名。
        granularity: str, 时间粒度，如 '1s', '5min', '1h', '1d'，必填。
        agg_method: str, 聚合方法，默认 'mean'，支持 pandas 聚合函数名。
    :return: (bool, result) 成功返回合并后的 DataFrame，失败返回错误信息。
    """
    try:
        data = args[0] if args else kwargs.get('data')
        if not isinstance(data, dict):
            raise ValueError("第一个参数必须是 {name: DataFrame} 字典")

        params = kwargs.get('params', {})
        sources = params.get('sources', list(data.keys()))
        time_col = params.get('time_col', 'timestamp')
        granularity = params.get('granularity')
        agg_method = params.get('agg_method', 'mean')

        if not granularity:
            raise ValueError("必须通过 granularity 参数指定时间粒度，如 '1s', '5min', '1h'")

        if len(sources) < 2:
            raise ValueError("至少需要两个数据源进行时间对齐")

        # 自动检测时间列
        if time_col is None:
            for col in ['time', 'Time', 'TIME', 'timestamp', 'datetime', 'date']:
                if col in data[sources[0]].columns:
                    time_col = col
                    break
        if time_col is None:
            raise ValueError("未找到时间列，请通过 time_col 参数指定")
        if time_col not in data[sources[0]].columns:
            raise ValueError(f"时间列 {time_col} 不存在")

        # 按粒度重采样每个表
        resampled = []
        for name in sources:
            df_copy = data[name].copy()
            df_copy[time_col] = pd.to_datetime(df_copy[time_col], errors='coerce')
            df_copy = df_copy.dropna(subset=[time_col]).sort_values(time_col)
            rs = _resample_df(df_copy, time_col, granularity, agg_method)
            resampled.append(rs)

        # 合并所有重采样后的表，处理重复列（仅保留第一个表的字段）
        result = resampled[0]
        for df in resampled[1:]:
            duplicate_cols = [col for col in df.columns if col in result.columns and col != time_col]
            df_filtered = df.drop(columns=duplicate_cols) if duplicate_cols else df
            result = pd.merge(result, df_filtered, on=time_col, how='outer').sort_values(time_col)

        result = result.reset_index(drop=True)

        # 构建返回数据
        ret_data = {}
        merged_name = f"merged_{'_'.join(sources)}"
        ret_data[merged_name] = result
        for name in data.keys():
            if name not in sources:
                ret_data[name] = data[name]

        ret = {
            'data': ret_data,
            'analysis': {
                'sources': sources,
                'time_col': time_col,
                'granularity': granularity,
                'agg_method': agg_method,
                'input_rows': [len(data[s]) for s in sources],
                'result_rows': len(result),
                'merged_name': merged_name
            }
        }
        return True, ret

    except Exception as e:
        return False, str(e)