import pandas as pd


def algsMain(*args, **kwargs):
    """
    时间范围选择：按时间范围筛选数据。

    :param args: 第一个参数为 {name: DataFrame} 字典。
    :param kwargs:
        time_col: str, 时间列名。
        start_time: str, 开始时间。
        end_time: str, 结束时间。
    :return: (bool, result) 成功返回筛选后的 DataFrame，失败返回错误信息。
    """
    try:
        data = args[0] if args else kwargs.get('data')
        params = kwargs.get('params', {})
        time_col = params.get('time_col', None)
        start_time = params.get('start_time')
        end_time = params.get('end_time')

        ret = {
            'data': {},
            'analysis': {}
        }

        if isinstance(data, dict):
            for name, df in data.items():
                # 自动检测时间列
                actual_time_col = time_col
                if actual_time_col is None:
                    for col in ['time', 'Time', 'TIME', 'timestamp', 'datetime', 'date']:
                        if col in df.columns:
                            actual_time_col = col
                            break

                if actual_time_col is None:
                    raise ValueError("无法找到时间列")

                if actual_time_col not in df.columns:
                    raise ValueError(f"时间列 {actual_time_col} 不存在")

                result_df = _process_single(df, actual_time_col, start_time, end_time)
                ret['data'][name] = result_df

                # 记录分析数据
                ret['analysis'][name] = {
                    'time_col': actual_time_col,
                    'start_time': start_time,
                    'end_time': end_time,
                    'original_rows': len(df),
                    'filtered_rows': len(result_df),
                    'rows_removed': len(df) - len(result_df)
                }
            return True, ret
        else:
            raise ValueError("第一个参数必须是 {name: DataFrame} 字典")

    except Exception as e:
        return False, str(e)


def _process_single(df, time_col, start_time, end_time):
    """处理单个 DataFrame"""
    df_copy = df.copy()
    df_copy[time_col] = pd.to_datetime(df_copy[time_col], errors='coerce')

    result = df_copy

    if start_time:
        start_dt = pd.to_datetime(start_time)
        result = result[result[time_col] >= start_dt]

    if end_time:
        end_dt = pd.to_datetime(end_time)
        result = result[result[time_col] <= end_dt]

    return result