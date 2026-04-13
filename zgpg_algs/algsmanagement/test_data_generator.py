import numpy as np
import random
import time


def generate_test_data(days=10, points_per_day=1000, seed=42):
    """
    生成测试数据

    Args:
        days: 生成多少天的数据
        points_per_day: 每天的数据点数
        seed: 随机种子，保证结果可重复

    Returns:
        符合您代码格式的测试数据
    """
    np.random.seed(seed)
    random.seed(seed)

    test_data = []
    base_timestamp = int(time.mktime(time.strptime("2023-01-01 00:00:00", "%Y-%m-%d %H:%M:%S")))

    for day in range(days):
        # 生成时间序列（每天的时间戳）
        day_start = base_timestamp + day * 24 * 3600
        timestamps = [day_start + i * 60 for i in range(points_per_day)]  # 每分钟一个数据点

        # 生成姿态角度数据（滚动角、俯仰角、偏航角）
        # 添加一些周期性变化和噪声
        t = np.linspace(0, 24, points_per_day)  # 24小时

        # 滚动角数据（通道0）
        roll_data = 0.1 * np.sin(2 * np.pi * t / 12) + 0.05 * np.random.normal(0, 1, points_per_day)

        # 俯仰角数据（通道1）
        pitch_data = 0.15 * np.cos(2 * np.pi * t / 8) + 0.03 * np.random.normal(0, 1, points_per_day)

        # 偏航角数据（通道2）
        yaw_data = 0.08 * np.sin(2 * np.pi * t / 16) + 0.02 * np.random.normal(0, 1, points_per_day)

        # 控制模式数据（通道3）- 大部分时间为6（稳态控制模式）
        control_mode = np.full(points_per_day, 6.0)
        # 随机添加一些其他模式
        noise_indices = np.random.choice(points_per_day, size=points_per_day // 20, replace=False)
        control_mode[noise_indices] = np.random.choice([0, 1, 2, 3, 4, 5, 7, 8], size=len(noise_indices))

        # 按照您的数据格式组织：[timestamps, channel0, channel1, channel2, channel3]
        day_data = [
            timestamps,
            roll_data.tolist(),
            pitch_data.tolist(),
            yaw_data.tolist(),
            control_mode.tolist()
        ]

        test_data.append(day_data)

    return test_data


def generate_large_test_data(days=180, points_per_day=2000000):
    """
    生成大量测试数据用于性能测试
    """
    return generate_test_data(days, points_per_day)


def get_test_config():
    """
    返回测试配置
    """
    return {
        "diffValue": 0.1,
        "count": "10",
        "calType": "0",
        "attitudeMode": 0,  # 使用第0个通道（滚动角）
        "flagValue": "6",  # 控制模式为6
        "ang_append1": 1,  # 俯仰角通道
        "ang_append2": 2,  # 偏航角通道
        "ang_append3": -1,  # 不使用第3个角度通道

        # 多核配置
        "enable_parallel": False,  # 先设为False用于对比
        "n_processes": 8,
        "parallel_threshold": 3
    }