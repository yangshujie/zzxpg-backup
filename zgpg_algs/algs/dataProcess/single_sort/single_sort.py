import pandas as pd


def algsMain(*args, **kwargs):
    """
    单列排序：对指定数据源按单列进行升序或降序排序。
    sources 为 None 时对所有数据表执行排序。

    :param args: 第一个参数为 {name: DataFrame} 字典。
    :param kwargs:
        params:
            sources: list|None, 要排序的数据源名称列表，None 表示所有数据源。
            column: str, 排序列名，默认 'timestamp'。
            ascending: bool, 升序为 True，降序为 False，默认 True。
    :return: (bool, result) 成功返回排序后的 DataFrame，失败返回错误信息。
    """
    try:
        data = args[0] if args else kwargs.get('data')
        if not isinstance(data, dict):
            raise ValueError("第一个参数必须是 {name: DataFrame} 字典")

        params = kwargs.get('params', {})
        sources = params.get('sources')
        column = params.get('column', 'timestamp')
        ascending = params.get('ascending', True)

        if sources is not None:
            missing = [s for s in sources if s not in data]
            if missing:
                raise ValueError(f"数据源 {missing} 不存在")

        target_sources = sources if sources is not None else list(data.keys())
        ret_data = {}
        analysis_list = []

        for name in target_sources:
            df = data[name].copy()

            if column not in df.columns:
                raise ValueError(f"数据源 {name} 中排序列 {column} 不存在，可用列: {list(df.columns)}")

            original_rows = len(df)
            original_first = df.iloc[0][column] if original_rows > 0 else None
            original_last = df.iloc[-1][column] if original_rows > 0 else None

            df = df.sort_values(by=column, ascending=ascending, ignore_index=True)

            sorted_first = df.iloc[0][column] if original_rows > 0 else None
            sorted_last = df.iloc[-1][column] if original_rows > 0 else None

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
                'column': column,
                'ascending': ascending,
                'details': analysis_list
            }
        }
        return True, ret

    except Exception as e:
        return False, str(e)
