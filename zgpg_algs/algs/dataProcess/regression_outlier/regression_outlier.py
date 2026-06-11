import pandas as pd
import numpy as np
from sklearn.linear_model import LinearRegression


def algsMain(*args, **kwargs):
    """
    基于回归残差剔除野值，支持异常值修复。
    对时序数据拟合一元线性回归模型，将残差超过阈值（标准差倍数）的点判定为异常值。

    :param args: 第一个参数为 {name: DataFrame} 字典。
    :param kwargs:
        threshold: float, 残差阈值（标准差倍数），默认为 3.0。
        time_col: str, 时间列名。
        columns: list, 需要处理的列名，默认为所有数值列（除时间列外）。
        drop: bool, 是否删除包含 NaN 的行，默认为 False。
    :return: (bool, result) 成功返回处理后的 DataFrame 和分析信息，失败返回错误信息。
    """
    try:
        data = args[0] if args else kwargs.get('data')
        params = kwargs.get('params', {})
        threshold = params.get('threshold', 3.0)
        time_col = params.get('time_col', 'timestamp')
        columns = params.get('columns', None)
        drop = params.get('drop', False)

        ret = {
            'data': {},
            'analysis': {}
        }

        if isinstance(data, dict):
            for name, df in data.items():
                df_result, analysis = _regression_outlier(df, threshold=threshold, time_col=time_col,
                                                          columns=columns, drop=drop)
                ret['data'][name] = df_result
                ret['analysis'][name] = analysis
            return True, ret
        else:
            raise ValueError("第一个参数必须是 {name: DataFrame} 字典")

    except Exception as e:
        return False, str(e)


def _regression_outlier(df, threshold=3.0, time_col=None, columns=None, drop=False):
    """
    处理单个 DataFrame，检测并修复异常值。
    :return: (处理后的DataFrame, 分析信息字典)
    """
    if time_col is None:
        time_col = df.columns[0]

    df_result = df.copy()
    analysis = {}

    # 获取时间索引
    X = np.arange(len(df_result)).reshape(-1, 1)

    if columns is None:
        columns = df_result.select_dtypes(include=[np.number]).columns.tolist()
        if time_col in columns:
            columns.remove(time_col)

    for col in columns:
        if col not in df_result.columns or df_result[col].dtype not in [np.float64, np.int64]:
            continue

        series = df_result[col].values.astype(float)
        n = len(series)

        # 处理缺失值
        valid_mask = ~np.isnan(series)
        if valid_mask.sum() < 3:
            continue

        X_valid = X[valid_mask]
        y_valid = series[valid_mask]

        # 拟合线性回归
        model = LinearRegression()
        model.fit(X_valid, y_valid)

        # 计算残差
        y_pred = model.predict(X)
        residuals = series - y_pred

        # 计算残差的标准差
        residual_std = np.nanstd(residuals)

        # 检测异常值
        if residual_std > 0:
            anomaly_mask = np.abs(residuals) > threshold * residual_std
        else:
            anomaly_mask = np.zeros(n, dtype=bool)

        anomaly_indices = np.where(anomaly_mask)[0]

        # 修复异常值（基于双向线性插值）
        if len(anomaly_indices) > 0:
            filtered_y = series.copy()
            for idx in anomaly_indices:
                # 向左搜索最近正常点
                left_idx = idx - 1
                while left_idx >= 0 and anomaly_mask[left_idx]:
                    left_idx -= 1
                # 向右搜索最近正常点
                right_idx = idx + 1
                while right_idx < n and anomaly_mask[right_idx]:
                    right_idx += 1

                if left_idx >= 0 and right_idx < n:
                    # 两侧都有正常点，线性插值
                    filtered_y[idx] = series[left_idx] + (series[right_idx] - series[left_idx]) * (idx - left_idx) / (right_idx - left_idx)
                elif left_idx >= 0:
                    # 仅左侧有正常点
                    filtered_y[idx] = series[left_idx]
                elif right_idx < n:
                    # 仅右侧有正常点
                    filtered_y[idx] = series[right_idx]
                else:
                    # 整个序列都是异常点，保持原值
                    filtered_y[idx] = series[idx]

            df_result[col] = filtered_y

        # 记录分析信息
        anomaly_values = series[anomaly_indices]
        filtered_values = df_result[col].values[anomaly_indices]
        analysis[col] = {
            'anomaly_count': len(anomaly_indices),
            'anomaly_rate': len(anomaly_indices) / n if n > 0 else 0,
            'threshold': threshold,
            'residual_std': residual_std,
            'r_squared': model.score(X_valid, y_valid),
            'anomalies': list(zip(anomaly_indices.tolist(), anomaly_values.tolist(), filtered_values.tolist()))
        }

    if drop:
        df_result = df_result.dropna(how='any')

    return df_result, analysis
