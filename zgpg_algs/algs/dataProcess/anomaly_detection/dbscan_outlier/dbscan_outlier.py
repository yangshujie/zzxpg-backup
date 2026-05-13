import pandas as pd
import numpy as np
from sklearn.cluster import DBSCAN


def algsMain(*args, **kwargs):
    """
    基于 DBSCAN 聚类剔除野值，支持异常值修复。
    将不能归入任何聚类的点判定为离群点。

    :param args: 第一个参数为 {name: DataFrame} 字典。
    :param kwargs:
        eps: float, DBSCAN 邻域半径，默认为 0.5。
        min_samples: int, DBSCAN 最小样本数，默认为 5。
        columns: list, 需要处理的列名，默认为所有数值列。
        drop: bool, 是否删除离群点行，默认为 False。
    :return: (bool, result) 成功返回处理后的 DataFrame 和分析信息，失败返回错误信息。
    """
    try:
        data = args[0] if args else kwargs.get('data')
        params = kwargs.get('params', {})
        eps = params.get('eps', 0.5)
        min_samples = params.get('min_samples', 5)
        columns = params.get('columns', None)
        drop = params.get('drop', False)

        ret = {
            'data': {},
            'analysis': {}
        }

        if isinstance(data, dict):
            for name, df in data.items():
                df_result, analysis = _dbscan_outlier(df, eps=eps, min_samples=min_samples,
                                                      columns=columns, drop=drop)
                ret['data'][name] = df_result
                ret['analysis'][name] = analysis
            return True, ret
        else:
            raise ValueError("第一个参数必须是 {name: DataFrame} 字典")

    except Exception as e:
        return False, str(e)


def _dbscan_outlier(df, eps=0.5, min_samples=5, columns=None, drop=False):
    """
    处理单个 DataFrame，检测并修复异常值。
    :return: (处理后的DataFrame, 分析信息字典)
    """
    if columns is None:
        columns = df.select_dtypes(include=[np.number]).columns.tolist()

    df_result = df.copy()
    analysis = {}

    # 准备数据（去除 NaN）
    X = df_result[columns].dropna()
    if len(X) < min_samples:
        # 数据量不足，返回空分析
        for col in columns:
            analysis[col] = {
                'anomaly_count': 0,
                'anomaly_rate': 0,
                'n_clusters': 0,
                'eps': eps,
                'min_samples': min_samples,
                'anomalies': []
            }
        return df_result, analysis

    # 标准化数据
    X_normalized = (X - X.mean()) / (X.std() + 1e-10)

    # 运行 DBSCAN
    dbscan = DBSCAN(eps=eps, min_samples=min_samples)
    labels = dbscan.fit_predict(X_normalized)

    # 标记噪声点（标签为 -1）
    noise_mask = labels == -1
    noise_indices = X.index[noise_mask]
    n_noise = len(noise_indices)

    # 对每个列进行修复
    for col in columns:
        series = df_result[col].values.astype(float)
        n = len(series)

        # 获取该列的异常点在原始数据中的位置
        col_anomaly_indices = df_result.index.get_indexer(noise_indices)
        # 过滤有效索引
        col_anomaly_indices = col_anomaly_indices[col_anomaly_indices >= 0]

        # 修复异常值（基于双向线性插值）
        if len(col_anomaly_indices) > 0:
            anomaly_mask = np.zeros(n, dtype=bool)
            anomaly_mask[col_anomaly_indices] = True

            filtered_y = series.copy()
            for idx in col_anomaly_indices:
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
        anomaly_values = series[col_anomaly_indices]
        filtered_values = df_result[col].values[col_anomaly_indices]
        analysis[col] = {
            'anomaly_count': len(col_anomaly_indices),
            'anomaly_rate': len(col_anomaly_indices) / n if n > 0 else 0,
            'n_clusters': len(set(labels)) - (1 if -1 in labels else 0),
            'eps': eps,
            'min_samples': min_samples,
            'anomalies': list(zip(col_anomaly_indices.tolist(), anomaly_values.tolist(), filtered_values.tolist()))
        }

    if drop:
        df_result = df_result.dropna(how='any')

    return df_result, analysis