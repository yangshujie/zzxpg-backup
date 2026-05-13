import pandas as pd
from datetime import datetime


def algsMain(*args, **kwargs):
    """
    将指定列（默认为'time'）转换为统一格式：yyyy-mm-dd hh:mm:ss。
    :param args: 第一个参数为DataFrame。
    :param kwargs:
        column: str, 需要转换的列名，默认为'time'。
        errors: str, 错误处理方式，'raise'或'coerce'，默认为'coerce'（无效值转为NaT）。
    :return: (bool, result) 成功返回转换后的DataFrame，失败返回错误信息。
    """
    try:
        data = args[0] if args else kwargs.get('data')
        params = kwargs.get('params', {})

        if isinstance(data, dict):
            results = {}
            for name, df in data.items():
                col = params.get('column', 'time')
                errors = params.get('errors', 'coerce')

                if col not in df.columns:
                    raise ValueError(f"列'{col}'不存在")

                # 转换为datetime类型，然后格式化为字符串
                df_copy = df.copy()
                dt_series = pd.to_datetime(df_copy[col], errors=errors)
                if dt_series.isna().any() and errors == 'coerce':
                    # 可记录警告，但不中断
                    pass
                df_copy[col] = dt_series.dt.strftime('%Y-%m-%d %H:%M:%S')

                results[name] = df_copy
            return True, results
        elif isinstance(data, pd.DataFrame):
            df = data
            col = params.get('column', 'time')
            errors = params.get('errors', 'coerce')

            if col not in df.columns:
                raise ValueError(f"列'{col}'不存在")

            # 转换为datetime类型，然后格式化为字符串
            df_copy = df.copy()
            dt_series = pd.to_datetime(df_copy[col], errors=errors)
            if dt_series.isna().any() and errors == 'coerce':
                # 可记录警告，但不中断
                pass
            df_copy[col] = dt_series.dt.strftime('%Y-%m-%d %H:%M:%S')

            return True, df_copy
        else:
            raise ValueError("第一个参数必须是 DataFrame 或 {name: DataFrame} 字典")

    except Exception as e:
        return False, str(e)