import pandas as pd


def algsMain(*args, **kwargs):
    """
    线性插值拟合：对缺失值进行线性插值填补。
    基于已知点的线性关系，计算缺失位置的估计值。

    :param args: 第一个参数为 {name: DataFrame} 字典。
    :param kwargs:
        params:
            sources: list|None, 要处理的数据源名称列表，None表示所有数据源。
            columns: list/None, 要处理的列名列表，None表示所有数值列。
            limit: int/None, 连续填充的最大缺失数量，None表示不限制。
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
        limit = params.get('limit')
        drop = params.get('drop', False)

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
                df[col] = df[col].interpolate(method='linear', limit=limit, limit_direction='forward')
                after_missing = int(df[col].isnull().sum())
                col_details[col] = {
                    'filled': before_missing - after_missing,
                    'remaining_missing': after_missing
                }

            if drop:
                df = df.dropna()

            ret_data[name] = df
            analysis[name] = {
                'columns_processed': numeric_cols,
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
                'method': 'linear',
                'limit': limit,
                'drop': drop,
                'details': analysis
            }
        }
        return True, ret

    except Exception as e:
        return False, str(e)
