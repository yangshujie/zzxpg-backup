import pandas as pd


def algsMain(*args, **kwargs):
    """
    时间窗口匹配：将两个表格按时间窗口进行最近时间匹配。
    基于时间戳的最近时间匹配（使用 merge_asof），在指定时间窗口内找到最近的对应记录。
       处理重复列，仅保留左表的字段。
    :param args: 第一个参数为 {name: DataFrame} 字典。
    :param kwargs:
        sources: list, 数据源名称列表 [left_source, right_source]。
        time_col: str, 时间列名。
        window: str, 时间窗口大小，如 '5min', '10s', '1h'。
    :return: (bool, result) 成功返回合并后的 DataFrame，失败返回错误信息。
    """
    try:
        data = args[0] if args else kwargs.get('data')
        if not isinstance(data, dict):
            raise ValueError("第一个参数必须是 {name: DataFrame} 字典")

        params = kwargs.get('params', {})
        sources = params.get('sources', list(data.keys()))
        time_col = params.get('time_col', 'timestamp')
        window = params.get('window', '5min')

        if len(sources) < 2:
            raise ValueError("至少需要两个数据源进行时间窗口匹配")

        if time_col not in data[sources[0]].columns or time_col not in data[sources[1]].columns:
            raise ValueError(f"时间列 {time_col} 不存在")

        # 获取各表行数
        row_counts = {name: len(data[name]) for name in sources}

        # 转换时间列
        df1 = data[sources[0]].copy()
        df2 = data[sources[1]].copy()
        df1[time_col] = pd.to_datetime(df1[time_col])
        df2[time_col] = pd.to_datetime(df2[time_col])

        # 识别重复列（除了时间列），仅保留左表的字段
        duplicate_cols = [col for col in df2.columns if col in df1.columns and col != time_col]
        df2_filtered = df2.drop(columns=duplicate_cols) if duplicate_cols else df2

        # 排序
        df1 = df1.sort_values(time_col)
        df2_filtered = df2_filtered.sort_values(time_col)

        # 使用 merge_asof 进行最近时间匹配
        result = pd.merge_asof(df1, df2_filtered, on=time_col, direction='nearest', tolerance=pd.Timedelta(window))

        # 统计匹配结果
        matched_count = result.dropna(how='all', subset=[col for col in result.columns if col not in df1.columns]).shape[0]

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
                'time_col': time_col,
                'window': window,
                'row_counts': row_counts,
                'result_rows': len(result),
                'matched_rows': matched_count,
                'merged_name': merged_name
            }
        }
        return True, ret

    except Exception as e:
        return False, str(e)