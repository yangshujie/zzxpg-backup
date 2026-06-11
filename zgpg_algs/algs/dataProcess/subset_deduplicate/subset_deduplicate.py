import pandas as pd


def algsMain(*args, **kwargs):
    """
    按列去重：根据指定列判断重复。

    :param args: 第一个参数为 {name: DataFrame} 字典。
    :param kwargs:
        subset: list, 用于判断重复的列名列表。
        keep: str, 保留策略，'first' 保留第一条，'last' 保留最后一条，False 删除所有重复。
    :return: (bool, result) 成功返回去重后的 DataFrame，失败返回错误信息。
    """
    try:
        data = args[0] if args else kwargs.get('data')
        params = kwargs.get('params', {})
        subset = params.get('subset')
        if subset is None:
            raise ValueError("必须提供 subset 参数指定去重列")
        keep = params.get('keep', 'first')

        ret = {
            'data': {},
            'analysis': {}
        }
        if isinstance(data, dict):
            for name, df in data.items():
                dedup_df = df.drop_duplicates(subset=subset, keep=keep)
                ret['data'][name] = dedup_df

                # 记录每个表的去重分析数据
                orig_rows = len(df)
                dedup_rows = len(dedup_df)
                ret['analysis'][name] = {
                    'original_rows': orig_rows,
                    'deduplicated_rows': dedup_rows,
                    'duplicates_removed': orig_rows - dedup_rows
                }
            return True, ret
        else:
            raise ValueError("第一个参数必须是 {name: DataFrame} 字典")

    except Exception as e:
        return False, str(e)