import pandas as pd


def algsMain(*args, **kwargs):
    """
    交叉合并（笛卡尔积）：将两个表进行笛卡尔积合并。

    :param args: 第一个参数为 {name: DataFrame} 字典。
    :param kwargs:
        left: str, 左表名称。
        right: str, 右表名称。
    :return: (bool, result) 成功返回合并后的 DataFrame，失败返回错误信息。
    """
    try:
        data = args[0] if args else kwargs.get('data')
        if not isinstance(data, dict):
            raise ValueError("第一个参数必须是 {name: DataFrame} 字典")

        params = kwargs.get('params', {})
        left_key = params.get('left')
        right_key = params.get('right')

        if left_key is None or right_key is None:
            raise ValueError("必须提供 left 和 right 参数指定左右表")

        if left_key not in data:
            raise ValueError(f"左表标识 {left_key} 不存在")
        if right_key not in data:
            raise ValueError(f"右表标识 {right_key} 不存在")

        left_df = data[left_key].copy()
        right_df = data[right_key].copy()

        # 识别重复列，仅保留左表的字段
        duplicate_cols = [col for col in right_df.columns if col in left_df.columns]
        right_df_filtered = right_df.drop(columns=duplicate_cols) if duplicate_cols else right_df

        # 交叉合并：添加一个临时键，然后merge，再删除
        left_df['_key'] = 1
        right_df_filtered['_key'] = 1
        cross = pd.merge(left_df, right_df_filtered, on='_key')
        cross.drop('_key', axis=1, inplace=True)

        # 构建返回数据，包含匹配后的表和未被选择的表
        ret_data = {}
        merged_name = f"merged_{left_key}_{right_key}"
        ret_data[merged_name] = cross
        for name in data.keys():
            if name not in [left_key, right_key]:
                ret_data[name] = data[name]

        ret = {
            'data': ret_data,
            'analysis': {
                'left': left_key,
                'right': right_key,
                'left_rows': len(data[left_key]),
                'right_rows': len(data[right_key]),
                'result_rows': len(cross),
                'merged_name': merged_name
            }
        }
        return True, ret

    except Exception as e:
        return False, str(e)