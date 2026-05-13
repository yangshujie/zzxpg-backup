import numpy as np
import pandas as pd


def algsMain(*args, **kwargs):
    """
    后向填充：使用后一个有效值填充缺失值。

    :param args: 第一个参数为 {name: DataFrame} 字典。
    :param kwargs:
        drop: bool, 填充后是否删除仍包含 NaN 的行，默认为 True。
    :return: (bool, result) 成功返回填充后的 DataFrame，失败返回错误信息。
    """
    try:
        data = args[0] if args else kwargs.get('data')
        params = kwargs.get('params', {})
        drop = params.get('drop', True)

        ret = {
            'data': {},
            'analysis': {}
        }

        if isinstance(data, dict):
            for name, df in data.items():
                # 统计原始缺失情况
                orig_nan_count = df.isna().sum().sum()
                orig_inf_count = ((df == np.inf) | (df == -np.inf)).sum().sum()

                bfill_df = df.copy()
                # 替换无穷大为 NaN
                bfill_df.replace([np.inf, -np.inf], np.nan, inplace=True)
                # 后向填充
                bfill_df = bfill_df.bfill()
                # 可选：删除仍包含 NaN 的行
                if drop:
                    bfill_df = bfill_df.dropna(how="any")

                ret['data'][name] = bfill_df
                # 记录分析数据
                filled_nan_count = orig_nan_count - bfill_df.isna().sum().sum()
                ret['analysis'][name] = {
                    'original_nan_count': int(orig_nan_count),
                    'original_inf_count': int(orig_inf_count),
                    'filled_nan_count': int(filled_nan_count),
                    'remaining_nan_count': int(bfill_df.isna().sum().sum())
                }
            return True, ret
        else:
            raise ValueError("第一个参数必须是 {name: DataFrame} 字典")

    except Exception as e:
        return False, str(e)
