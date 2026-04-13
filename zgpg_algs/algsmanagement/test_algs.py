import time

import numpy as np
import psutil
import os
import multiprocessing as mp
from test_data_generator import generate_test_data, generate_large_test_data, get_test_config
from algs.character.attitudeControlAccuracy.attitudeControlAccuracybak import algsMain

def monitor_cpu_usage():
    """监控CPU使用率"""
    return psutil.cpu_percent(interval=1, percpu=True)

def test_multicore_effectiveness():
    """
    测试多核计算的有效性
    """
    print("=" * 60)
    print("多核计算有效性测试")
    print("=" * 60)

    # 检测系统信息
    cpu_count = mp.cpu_count()
    print(f"系统CPU核心数: {cpu_count}")
    print(f"当前进程ID: {os.getpid()}")
    print(f"是否在Docker中: {'是' if os.path.exists('/.dockerenv') else '否'}")

    # 生成测试数据
    print("\n生成测试数据...")
    small_data = generate_test_data(days=5, points_per_day=500)
    large_data = generate_large_test_data(days=180, points_per_day=20000)

    config = get_test_config()

    print(f"小数据集: {len(small_data)} 天")
    print(f"大数据集: {len(large_data)} 天")

    # 测试1: 小数据集（应该使用串行）
    print("\n" + "-" * 40)
    print("测试1: 小数据集")
    print("-" * 40)

    config["enable_parallel"] = False
    start_time = time.time()
    cpu_before = psutil.cpu_percent()

    status1, result1 = algsMain(small_data, config=config)

    serial_time_small = time.time() - start_time
    cpu_after = psutil.cpu_percent()

    print(f"串行计算结果: 状态={status1}, 结果数量={len(result1) if status1 else 0}")
    print(f"串行计算耗时: {serial_time_small:.2f}秒")
    print(f"CPU使用率变化: {cpu_before:.1f}% -> {cpu_after:.1f}%")

    # 测试2: 大数据集串行计算
    print("\n" + "-" * 40)
    print("测试2: 大数据集 - 串行计算")
    print("-" * 40)

    config["enable_parallel"] = False
    start_time = time.time()
    cpu_before = monitor_cpu_usage()

    status2, result2 = algsMain(large_data, config=config)

    serial_time_large = time.time() - start_time
    cpu_after = monitor_cpu_usage()

    print(f"串行计算结果: 状态={status2}, 结果数量={len(result2) if status2 else 0}")
    print(f"串行计算耗时: {serial_time_large:.2f}秒")
    print(f"CPU使用率: {np.mean(cpu_before):.1f}% -> {np.mean(cpu_after):.1f}%")

    # 测试3: 大数据集并行计算
    print("\n" + "-" * 40)
    print("测试3: 大数据集 - 并行计算")
    print("-" * 40)

    config["enable_parallel"] = True
    config["n_processes"] = min(4, cpu_count - 1)

    start_time = time.time()
    cpu_before = monitor_cpu_usage()

    status3, result3 = algsMain(large_data, config=config)

    parallel_time_large = time.time() - start_time
    cpu_after = monitor_cpu_usage()

    print(f"并行计算结果: 状态={status3}, 结果数量={len(result3) if status3 else 0}")
    print(f"并行计算耗时: {parallel_time_large:.2f}秒")
    print(f"CPU使用率: {np.mean(cpu_before):.1f}% -> {np.mean(cpu_after):.1f}%")

    # 测试4: 验证结果一致性
    print("\n" + "-" * 40)
    print("测试4: 结果一致性验证")
    print("-" * 40)

    if status2 and status3:
        # 比较结果
        results_match = True
        try:
            for i, (serial_val, parallel_val) in enumerate(zip(result2, result3)):
                if isinstance(serial_val, (int, float)) and isinstance(parallel_val, (int, float)):
                    if abs(serial_val - parallel_val) > 1e-10:
                        print(f"第{i}天结果不一致: 串行={serial_val}, 并行={parallel_val}")
                        results_match = False
                elif serial_val != parallel_val:
                    print(f"第{i}天结果不一致: 串行={serial_val}, 并行={parallel_val}")
                    results_match = False
        except Exception as e:
            print(f"结果比较失败: {e}")
            results_match = False

        if results_match:
            print("✓ 串行和并行计算结果完全一致")
        else:
            print("✗ 串行和并行计算结果存在差异")
    else:
        print("✗ 某个计算失败，无法比较结果")

    # 性能分析
    print("\n" + "=" * 60)
    print("性能分析总结")
    print("=" * 60)

    if serial_time_large > 0 and parallel_time_large > 0:
        speedup = serial_time_large / parallel_time_large
        efficiency = speedup / config["n_processes"] * 100

        print(f"大数据集串行耗时:    {serial_time_large:.2f}秒")
        print(f"大数据集并行耗时:    {parallel_time_large:.2f}秒")
        print(f"加速比:              {speedup:.2f}x")
        print(f"并行效率:            {efficiency:.1f}%")

        if speedup > 1.2:
            print("✓ 多核并行计算有效，性能有显著提升")
        elif speedup > 1.0:
            print("△ 多核并行计算有轻微提升")
        else:
            print("✗ 多核并行计算无效或性能下降")
            print("  可能原因:")
            print("  - 数据量太小，进程创建开销大于计算时间")
            print("  - Docker容器CPU限制")
            print("  - 算法不适合并行化")
            print("  - 系统资源不足")

    return {
        'small_data_time': serial_time_small,
        'serial_time': serial_time_large,
        'parallel_time': parallel_time_large,
        'speedup': serial_time_large / parallel_time_large if parallel_time_large > 0 else 0,
        'results_match': results_match if 'results_match' in locals() else False
    }

if __name__ == "__main__":
    # 确保在主进程中运行
    test_results = test_multicore_effectiveness()

    print(f"\n测试完成!")
    print(f"详细结果: {test_results}")