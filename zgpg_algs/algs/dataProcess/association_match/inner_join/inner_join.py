import pandas as pd


def algsMain(*args, **kwargs):
    """
    精准匹配：只保留两个表格中键值完全匹配的行，基于指定列的精确值匹配（类似 SQL INNER JOIN）。
        当两表有同名字段且非链接键时，仅保留左表。
    :param args: 第一个参数为 {name: DataFrame} 字典。
    :param kwargs:
        sources: list, 数据源名称列表。
        on: str, 连接键列名。
    :return: (bool, result) 成功返回合并后的 DataFrame，失败返回错误信息。
    """
    try:
        data = args[0] if args else kwargs.get('data')
        if not isinstance(data, dict):
            raise ValueError("第一个参数必须是 {name: DataFrame} 字典")

        params = kwargs.get('params', {})
        sources = params.get('sources', list(data.keys()))
        on = params.get('on')

        if on is None:
            raise ValueError("必须提供 on 参数指定连接键")

        if len(sources) < 2:
            raise ValueError("至少需要两个数据源进行合并")

        # 获取各表行数
        row_counts = {name: len(data[name]) for name in sources}

        # 执行内连接
        result = data[sources[0]].copy()
        for name in sources[1:]:
            right_df = data[name].copy()
            # 识别重复列（除了 on 列），仅保留左表的字段
            if isinstance(on, list):
                duplicate_cols = [col for col in right_df.columns if col in result.columns and col not in on]
            else:
                duplicate_cols = [col for col in right_df.columns if col in result.columns and col != on]
            # 删除右表中的重复列
            right_df = right_df.drop(columns=duplicate_cols)
            result = pd.merge(result, right_df, on=on, how='inner')

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
                'on': on,
                'row_counts': row_counts,
                'result_rows': len(result),
                'merged_name': merged_name
            }
        }
        return True, ret

    except Exception as e:
        return False, str(e)