import pandas as pd


def algsMain(*args, **kwargs):
    """
    多列排序：对指定数据源按多列进行带优先级的排序。
    列表中靠前的列优先级更高。sources 为 None 时对所有数据表执行排序。

    :param args: 第一个参数为 {name: DataFrame} 字典。
    :param kwargs:
        params:
            sources: list|None, 要排序的数据源名称列表，None 表示所有数据源。
            columns: list, 排序列名列表，按优先级从高到低排列。
            ascending: list|bool, 每列的排序方向，True 升序/False 降序。
                       若为 bool 则所有列统一使用该方向，默认 True。
    :return: (bool, result) 成功返回排序后的 DataFrame，失败返回错误信息。
    """
    try:
        data = args[0] if args else kwargs.get('data')
        if not isinstance(data, dict):
            raise ValueError("第一个参数必须是 {name: DataFrame} 字典")

        params = kwargs.get('params', {})
        sources = params.get('sources')
        columns = params.get('columns', [])
        ascending = params.get('ascending', True)

        if sources is not None:
            missing = [s for s in sources if s not in data]
            if missing:
                raise ValueError(f"数据源 {missing} 不存在")
        if not columns:
            raise ValueError("必须提供 columns 参数指定排序列列表")

        target_sources = sources if sources is not None else list(data.keys())

        if isinstance(ascending, bool):
            ascending_list = [ascending] * len(columns)
        elif isinstance(ascending, list):
            if len(ascending) != len(columns):
                raise ValueError(f"ascending 长度 {len(ascending)} 与 columns 长度 {len(columns)} 不一致")
            ascending_list = ascending
        else:
            raise ValueError("ascending 必须为 bool 或 list 类型")

        ret_data = {}
        analysis_list = []

        for name in target_sources:
            df = data[name].copy()

            missing_cols = [col for col in columns if col not in df.columns]
            if missing_cols:
                raise ValueError(f"数据源 {name} 中排序列 {missing_cols} 不存在，可用列: {list(df.columns)}")

            original_rows = len(df)
            original_first = dict(df.iloc[0][columns]) if original_rows > 0 else {}
            original_last = dict(df.iloc[-1][columns]) if original_rows > 0 else {}

            df = df.sort_values(by=columns, ascending=ascending_list, ignore_index=True)

            sorted_first = dict(df.iloc[0][columns]) if original_rows > 0 else {}
            sorted_last = dict(df.iloc[-1][columns]) if original_rows > 0 else {}

            ret_data[name] = df
            analysis_list.append({
                'source': name,
                'total_rows': original_rows,
                'original_first': original_first,
                'original_last': original_last,
                'sorted_first': sorted_first,
                'sorted_last': sorted_last
            })

        for name in data.keys():
            if name not in ret_data:
                ret_data[name] = data[name]

        ret = {
            'data': ret_data,
            'analysis': {
                'sources': sources,
                'columns': columns,
                'ascending': ascending_list,
                'details': analysis_list
            }
        }
        return True, ret

    except Exception as e:
        return False, str(e)
