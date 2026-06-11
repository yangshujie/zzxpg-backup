import pandas as pd
import numpy as np


def algsMain(*args, **kwargs):
    """
    多重插补拟合：基于多列关联信息进行迭代插补。
    利用其他列的相关性构建回归模型，迭代估计缺失值，
    适用于存在多列相关性的数据集。

    :param args: 第一个参数为 {name: DataFrame} 字典。
    :param kwargs:
        params:
            sources: list|None, 要处理的数据源名称列表，None表示所有数据源。
            columns: list/None, 要处理的列名列表，None表示所有数值列。
            max_iter: int, 最大迭代次数，默认 10。
            tol: float, 收敛阈值（相邻迭代差值），默认 1e-4。
            random_state: int/None, 随机种子，None表示不固定。
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
        max_iter = params.get('max_iter', 10)
        tol = params.get('tol', 1e-4)
        random_state = params.get('random_state')
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

            if columns is not None:
                missing_cols = [c for c in columns if c not in df.columns]
                if missing_cols:
                    raise ValueError(f"数据源 {name} 中列 {missing_cols} 不存在，可用列: {list(df.columns)}")
                numeric_cols = columns
            else:
                numeric_cols = list(df.select_dtypes(include='number').columns)

            if len(numeric_cols) < 2:
                raise ValueError(f"多重插补至少需要 2 个数值列（数据源 {name} 中仅有 {len(numeric_cols)} 个）")

            original_missing = int(df[numeric_cols].isnull().sum().sum())

            imputed_df, col_details, n_iters = _multiple_impute(
                df, numeric_cols, max_iter, tol, random_state
            )

            if drop:
                imputed_df = imputed_df.dropna()

            ret_data[name] = imputed_df
            analysis[name] = {
                'columns_processed': numeric_cols,
                'max_iter': max_iter,
                'tol': tol,
                'iterations_used': n_iters,
                'total_filled': original_missing - int(imputed_df[numeric_cols].isnull().sum().sum()),
                'remaining_missing': int(imputed_df[numeric_cols].isnull().sum().sum()),
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
                'max_iter': max_iter,
                'tol': tol,
                'drop': drop,
                'details': analysis
            }
        }
        return True, ret

    except Exception as e:
        return False, str(e)


def _multiple_impute(df, numeric_cols, max_iter, tol, random_state):
    """
    迭代多重插补（类 MICE 简化版）。
    每轮迭代中，对每个有缺失的列用其他列做线性回归预测，
    重复直到收敛或达到最大迭代次数。
    """
    rng = np.random.RandomState(random_state)
    imputed_df = df.copy()
    col_details = {}

    # 用各列中位数初始化缺失值
    for col in numeric_cols:
        median_val = imputed_df[col].median()
        imputed_df[col] = imputed_df[col].fillna(median_val)

    missing_mask = df[numeric_cols].isnull()
    col_missing_count = missing_mask.sum()

    # 只处理有缺失的列
    cols_to_impute = [col for col in numeric_cols if col_missing_count[col] > 0]

    prev_values = {}
    for col in cols_to_impute:
        prev_values[col] = imputed_df[col].values.copy()

    n_iters = 0
    for iteration in range(max_iter):
        max_change = 0.0
        n_iters = iteration + 1

        for col in cols_to_impute:
            other_cols = [c for c in numeric_cols if c != col]
            if not other_cols:
                continue

            # 训练集：该列在原始数据中非缺失的行
            train_mask = ~missing_mask[col]
            X_train = imputed_df.loc[train_mask, other_cols].values
            y_train = imputed_df.loc[train_mask, col].values

            if len(X_train) < 2:
                continue

            # 加截距项
            X_train_b = np.column_stack([np.ones(len(X_train)), X_train])
            try:
                # 最小二乘求解
                beta, _, _, _ = np.linalg.lstsq(X_train_b, y_train, rcond=None)
            except np.linalg.LinAlgError:
                continue

            # 预测缺失值
            predict_mask = missing_mask[col]
            X_pred = imputed_df.loc[predict_mask, other_cols].values
            X_pred_b = np.column_stack([np.ones(len(X_pred)), X_pred])
            predicted = X_pred_b @ beta

            # 添加小扰动防止退化
            noise = rng.normal(0, max(predicted.std() * 0.01, 1e-6), size=len(predicted))
            predicted += noise

            imputed_df.loc[predict_mask, col] = predicted

            # 收敛判断
            change = np.max(np.abs(imputed_df[col].values - prev_values[col]))
            max_change = max(max_change, change)
            prev_values[col] = imputed_df[col].values.copy()

        if max_change < tol:
            break

    # 构建逐列结果
    for col in numeric_cols:
        before = int(missing_mask[col].sum())
        after = int(imputed_df[col].isnull().sum())
        col_details[col] = {
            'filled': before - after,
            'remaining_missing': after
        }

    return imputed_df, col_details, n_iters
