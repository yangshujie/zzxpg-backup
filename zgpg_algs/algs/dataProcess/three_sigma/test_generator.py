"""
测试数据生成器
生成各种场景的测试数据
"""
import numpy as np


def generate_test_data(n_points=1000, test_type='small_value', seed=None):
    """
    生成各种类型的测试数据
    
    参数:
        n_points: 数据点数
        test_type: 测试类型
        seed: 随机种子
    
    返回:
        time, data, true_outliers (时间、数据、真实异常值标记)
    """
    if seed is not None:
        np.random.seed(seed)
    
    time = np.arange(n_points)
    true_outliers = np.zeros(n_points, dtype=bool)
    
    if test_type == 'small_value':
        # 姿态角类型：均值0.15度，波动0.01度
        baseline = 0.15
        noise_level = 0.01
        data = baseline + noise_level * np.random.randn(n_points)
        
        # 添加异常值（5%的点）
        n_outliers = int(n_points * 0.05)
        outlier_indices = np.random.choice(n_points, n_outliers, replace=False)
        data[outlier_indices] += np.random.choice([-1, 1], n_outliers) * np.random.uniform(0.5, 2.0, n_outliers)
        true_outliers[outlier_indices] = True
        
    elif test_type == 'large_value':
        # 大值数据：均值1000，波动50
        baseline = 1000
        noise_level = 50
        data = baseline + noise_level * np.random.randn(n_points)
        
        # 添加异常值（3%）
        n_outliers = int(n_points * 0.03)
        outlier_indices = np.random.choice(n_points, n_outliers, replace=False)
        data[outlier_indices] += np.random.choice([-1, 1], n_outliers) * np.random.uniform(300, 800, n_outliers)
        true_outliers[outlier_indices] = True
        
    elif test_type == 'state_change':
        # 状态变化：从0.15跳变到0.35
        baseline1 = 0.15
        baseline2 = 0.35
        noise_level = 0.01
        
        change_point = n_points // 2
        data = np.concatenate([
            baseline1 + noise_level * np.random.randn(change_point),
            baseline2 + noise_level * np.random.randn(n_points - change_point)
        ])
        
        # 添加异常值（避开状态变化区域）
        n_outliers = int(n_points * 0.03)
        safe_indices = np.concatenate([
            np.arange(0, change_point - 20),
            np.arange(change_point + 20, n_points)
        ])
        if len(safe_indices) > 0:
            outlier_indices = np.random.choice(safe_indices, min(n_outliers, len(safe_indices)), replace=False)
            data[outlier_indices] += np.random.choice([-1, 1], len(outlier_indices)) * np.random.uniform(0.5, 1.5, len(outlier_indices))
            true_outliers[outlier_indices] = True
        
    elif test_type == 'multiple_states':
        # 多次状态变化
        baseline_levels = [0.1, 0.3, 0.15, 0.4, 0.2]
        noise_level = 0.01
        points_per_state = n_points // len(baseline_levels)
        
        data = np.array([])
        for level in baseline_levels:
            segment = level + noise_level * np.random.randn(points_per_state)
            data = np.concatenate([data, segment])
        
        if len(data) < n_points:
            data = np.concatenate([data, baseline_levels[-1] + noise_level * np.random.randn(n_points - len(data))])
        
        # 添加异常值
        n_outliers = int(n_points * 0.02)
        outlier_indices = np.random.choice(n_points, n_outliers, replace=False)
        data[outlier_indices] += np.random.choice([-1, 1], n_outliers) * np.random.uniform(0.5, 1.0, n_outliers)
        true_outliers[outlier_indices] = True
        
    elif test_type == 'high_noise':
        # 高噪声数据
        baseline = 0.2
        noise_level = 0.05
        data = baseline + noise_level * np.random.randn(n_points)
        
        # 添加较难区分的异常值
        n_outliers = int(n_points * 0.08)
        outlier_indices = np.random.choice(n_points, n_outliers, replace=False)
        data[outlier_indices] += np.random.choice([-1, 1], n_outliers) * np.random.uniform(0.2, 0.5, n_outliers)
        true_outliers[outlier_indices] = True
        
    elif test_type == 'sparse_outliers':
        # 稀疏异常值
        baseline = 0.15
        noise_level = 0.01
        data = baseline + noise_level * np.random.randn(n_points)
        
        n_outliers = max(int(n_points * 0.01), 5)
        outlier_indices = np.random.choice(n_points, n_outliers, replace=False)
        data[outlier_indices] += np.random.choice([-1, 1], n_outliers) * np.random.uniform(1.0, 3.0, n_outliers)
        true_outliers[outlier_indices] = True
        
    elif test_type == 'clustered_outliers':
        # 聚集异常值
        baseline = 0.15
        noise_level = 0.01
        data = baseline + noise_level * np.random.randn(n_points)
        
        n_clusters = np.random.randint(3, 6)
        for _ in range(n_clusters):
            cluster_center = np.random.randint(50, n_points - 50)
            cluster_size = np.random.randint(5, 15)
            cluster_indices = np.arange(cluster_center, min(cluster_center + cluster_size, n_points))
            data[cluster_indices] += np.random.choice([-1, 1]) * np.random.uniform(0.5, 1.5)
            true_outliers[cluster_indices] = True
            
    elif test_type == 'trend':
        # 带趋势的数据
        baseline = 0.15
        noise_level = 0.01
        trend = np.linspace(0, 0.3, n_points)
        data = baseline + trend + noise_level * np.random.randn(n_points)
        
        n_outliers = int(n_points * 0.03)
        outlier_indices = np.random.choice(n_points, n_outliers, replace=False)
        data[outlier_indices] += np.random.choice([-1, 1], n_outliers) * np.random.uniform(0.5, 1.5, n_outliers)
        true_outliers[outlier_indices] = True
        
    elif test_type == 'periodic':
        # 周期性数据
        baseline = 0.2
        noise_level = 0.01
        period = n_points / 10
        periodic_component = 0.1 * np.sin(2 * np.pi * time / period)
        data = baseline + periodic_component + noise_level * np.random.randn(n_points)
        
        n_outliers = int(n_points * 0.03)
        outlier_indices = np.random.choice(n_points, n_outliers, replace=False)
        data[outlier_indices] += np.random.choice([-1, 1], n_outliers) * np.random.uniform(0.3, 0.8, n_outliers)
        true_outliers[outlier_indices] = True
        
    elif test_type == 'mixed':
        # 混合场景
        baseline = 0.15
        noise_level = 0.01
        data = baseline + noise_level * np.random.randn(n_points)
        
        # 状态变化
        change_points = [n_points // 3, 2 * n_points // 3]
        data[change_points[0]:change_points[1]] += 0.2
        
        # 趋势
        trend = np.linspace(0, 0.1, n_points)
        data += trend * 0.5
        
        # 周期
        period = n_points / 8
        periodic = 0.03 * np.sin(2 * np.pi * time / period)
        data += periodic
        
        # 稀疏强异常
        n_sparse = int(n_points * 0.01)
        sparse_indices = np.random.choice(n_points, n_sparse, replace=False)
        data[sparse_indices] += np.random.choice([-1, 1], n_sparse) * np.random.uniform(1.0, 2.0, n_sparse)
        true_outliers[sparse_indices] = True
        
        # 聚集弱异常
        if n_points > 50:
            cluster_center = n_points // 4
            cluster_indices = np.arange(cluster_center, min(cluster_center + 20, n_points))
            data[cluster_indices] += 0.3
            true_outliers[cluster_indices] = True
        
    else:
        raise ValueError(f"Unknown test_type: {test_type}")
    
    return time.tolist(), data.tolist(), true_outliers


# 测试场景配置
TEST_SCENARIOS = {
    'small_value': ('小值数据(姿态角)', 1000),
    'large_value': ('大值数据', 1000),
    'state_change': ('单次状态变化', 1000),
    'multiple_states': ('多次状态变化', 1000),
    'high_noise': ('高噪声数据', 1000),
    'sparse_outliers': ('稀疏异常值', 1000),
    'clustered_outliers': ('聚集异常值', 1000),
    'trend': ('带趋势数据', 1000),
    'periodic': ('周期性数据', 1000),
    'mixed': ('混合场景', 1000),
}


if __name__ == "__main__":
    print("=== 测试数据生成器 ===")
    
    for test_type, (name, n_points) in TEST_SCENARIOS.items():
        time, data, true_outliers = generate_test_data(n_points, test_type, seed=42)
        print(f"\n{name}:")
        print(f"  数据点数: {len(data)}")
        print(f"  真实异常值: {np.sum(true_outliers)}")
        print(f"  数据范围: [{min(data):.3f}, {max(data):.3f}]")
        print(f"  均值: {np.mean(data):.3f}")
        print(f"  标准差: {np.std(data):.3f}")