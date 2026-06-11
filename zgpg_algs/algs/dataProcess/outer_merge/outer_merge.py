import pandas as pd


def algsMain(*args, **kwargs):
    """
    外连接合并：保留所有数据，包括没有匹配的行。

    :param args: 第一个参数为 {name: DataFrame} 字典。
    :param kwargs:
        sources: list, 数据源名称列表，支持多选，默认为所有表。
        on: str/list, 连接键列名，支持单个字符串或列表（复合键）。
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

        # 统一键列名为列表，便于后续处理
        on_list = [on] if isinstance(on, str) else on

        if len(sources) < 2:
            raise ValueError("至少需要两个数据源进行合并")

        # 执行外连接合并，处理重复列（仅保留优先级高的表的字段）
        result = data[sources[0]].copy()
        for name in sources[1:]:
            next_df = data[name].copy()
            # 识别重复列（除了连接键），仅保留左表的字段
            duplicate_cols = [col for col in next_df.columns if col in result.columns and col not in on_list]
            next_df_filtered = next_df.drop(columns=duplicate_cols) if duplicate_cols else next_df
            result = pd.merge(result, next_df_filtered, on=on, how='outer')

        # 构建返回数据，包含合并后的表和未被选择的表
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
                'on': on,
                'input_rows': [len(data[s]) for s in sources],
                'result_rows': len(result),
                'merged_name': merged_name
            }
        }
        return True, ret

    except Exception as e:
        return False, str(e)