import pandas as pd
import numpy as np


def algsMain(*args, **kwargs):
    """
    多项式插补拟合：基于非缺失点拟合多项式，预测缺失值。
    适用于缺失值较少且数据呈现曲线趋势的场景。

    :param args: 第一个参数为 {name: DataFrame} 字典。
    :param kwargs:
        params:
            sources: list|None, 要处理的数据源名称列表，None表示所有数据源。
            columns: list/None, 要处理的列名列表，None表示所有数值列。
            degree: int, 多项式阶数，默认 3。
            drop: bool, 是否删除填充后仍含NaN的行，默认 False。
    :return: (bool, result) 成功返回处理后的数据，失败返回错误信息。
    """
    try:
        data = args[0] if args else kwargs.get('data')
        if not isinstance(data, dict):
            raise ValueError("第一个参数必须是 {name: DataFrame} 字典")

        params = kwargs.get('params', {})
        sources = params.get('sources')
        columns = params.get('columns')
        degree = params.get('degree', 3)
        drop = params.get('drop', False)

        if degree < 1:
            raise ValueError("degree 必须 >= 1")

        if sources is not None:
            missing = [s for s in sources if s not in data]
            if missing:
                raise ValueError(f"数据源 {missing} 不存在")

        target_sources = sources if sources is not None else list(data.keys())
        ret_data = {}
        analysis = {}

        for name in target_sources:
            df = data[name].copy()
            original_missing = int(df.isnull().sum().sum())

            if columns is not None:
                missing_cols = [c for c in columns if c not in df.columns]
                if missing_cols:
                    raise ValueError(f"数据源 {name} 中列 {missing_cols} 不存在，可用列: {list(df.columns)}")
                numeric_cols = columns
            else:
                numeric_cols = list(df.select_dtypes(include='number').columns)

            col_details = {}
            for col in numeric_cols:
                before_missing = int(df[col].isnull().sum())
                filled, r2 = _poly_fit_column(df, col, degree)
                df[col] = filled
                after_missing = int(df[col].isnull().sum())
                col_details[col] = {
                    'filled': before_missing - after_missing,
                    'remaining_missing': after_missing,
                    'r2': round(r2, 4) if r2 is not None else None
                }

            if drop:
                df = df.dropna()

            ret_data[name] = df
            analysis[name] = {
                'columns_processed': numeric_cols,
                'degree': degree,
                'total_filled': original_missing - int(df.isnull().sum().sum()),
                'remaining_missing': int(df.isnull().sum().sum()),
                'column_details': col_details
            }

        for name in data.keys():
            if name not in ret_data:
                ret_data[name] = data[name]

        ret = {
            'data': ret_data,
            'analysis': {
                'sources': sources,
                'columns': columns,
                'degree': degree,
                'drop': drop,
                'details': analysis
            }
        }
        return True, ret

    except Exception as e:
        return False, str(e)


def _poly_fit_column(df, col, degree):
    """
    对单列进行多项式插补拟合。
    :return: (filled_series, r2_score)
    """
    s = df[col].copy()
    known_mask = s.notna()

    if known_mask.sum() <= degree:
        return s, None

    known_idx = np.where(known_mask)[0].astype(float)
    known_vals = s[known_mask].values.astype(float)

    coeffs = np.polyfit(known_idx, known_vals, degree)
    r2 = 1 - np.sum((known_vals - np.polyval(coeffs, known_idx)) ** 2) / np.sum((known_vals - known_vals.mean()) ** 2) if np.sum((known_vals - known_vals.mean()) ** 2) > 0 else 1.0

    missing_mask = s.isna()
    if missing_mask.any():
        missing_idx = np.where(missing_mask)[0].astype(float)
        predicted = np.polyval(coeffs, missing_idx)
        s[missing_mask] = predicted

    return s, float(r2)
