"""
大规模数据测试 - 半年遥测数据
模拟实际卫星遥测场景
"""
import numpy as np
import time
from tabulate import tabulate
import matplotlib.pyplot as plt
from matplotlib import rcParams

from adaptive_outlier_detection import (
    OutlierDetector,
    algsMain_original,
    algsMain_balanced,
    algsMain_smart
)

# 设置中文字体
rcParams['font.sans-serif'] = ['SimHei', 'DejaVu Sans']
rcParams['axes.unicode_minus'] = False


def generate_half_year_telemetry(telemetry_type='attitude', sampling_rate=1):
    """
    生成半年的遥测数据
    
    参数:
        telemetry_type: 遥测类型
            - 'attitude': 姿态角（小值，0.1-0.3度）
            - 'temperature': 温度（中值，20-30度）
            - 'voltage': 电压（大值，1000-1100V）
            - 'position': 位置（大值，带周期）
        sampling_rate: 采样率（秒）
    
    返回:
        time, data, true_outliers, metadata
    """
    # 半年 = 6个月 * 30天 * 24小时 * 3600秒
    half_year_seconds = 6 * 30 * 24 * 3600
    n_points = half_year_seconds // sampling_rate
    
    print(f"生成数据：{n_points:,} 个数据点 (半年，{sampling_rate}秒采样)")
    
    time_data = np.arange(n_points)
    true_outliers = np.zeros(n_points, dtype=bool)
    
    if telemetry_type == 'attitude':
        # 姿态角：基线0.15度，小幅波动
        baseline = 0.15
        noise_level = 0.01
        
        # 基础噪声
        data = baseline + noise_level * np.random.randn(n_points)
        
        # 添加缓慢漂移（模拟姿态调整）
        drift_period = n_points // 20  # 20段
        for i in range(20):
            start = i * drift_period
            end = min((i + 1) * drift_period, n_points)
            drift = 0.05 * np.sin(i * 0.3)
            data[start:end] += drift
        
        # 添加机动（5次大幅状态变化）
        maneuver_points = np.random.choice(n_points, 5, replace=False)
        for mp in maneuver_points:
            maneuver_duration = 3600 // sampling_rate  # 1小时机动
            end = min(mp + maneuver_duration, n_points)
            data[mp:end] += np.random.choice([-0.2, 0.2])
        
        # 添加异常值（0.5%）
        n_outliers = int(n_points * 0.005)
        outlier_indices = np.random.choice(n_points, n_outliers, replace=False)
        data[outlier_indices] += np.random.choice([-1, 1], n_outliers) * np.random.uniform(0.5, 2.0, n_outliers)
        true_outliers[outlier_indices] = True
        
        metadata = {
            'type': '姿态角',
            'unit': '度',
            'baseline': baseline,
            'normal_range': f'[{baseline-0.3:.2f}, {baseline+0.3:.2f}]'
        }
        
    elif telemetry_type == 'temperature':
        # 温度：基线25度，日夜周期
        baseline = 25.0
        noise_level = 0.5
        
        # 日夜周期（每天波动）
        day_length = 24 * 3600 // sampling_rate
        daily_cycle = 5 * np.sin(2 * np.pi * time_data / day_length)
        
        # 季节变化（半年一个周期）
        seasonal_cycle = 3 * np.sin(2 * np.pi * time_data / n_points)
        
        data = baseline + daily_cycle + seasonal_cycle + noise_level * np.random.randn(n_points)
        
        # 异常值（0.3%）
        n_outliers = int(n_points * 0.003)
        outlier_indices = np.random.choice(n_points, n_outliers, replace=False)
        data[outlier_indices] += np.random.choice([-1, 1], n_outliers) * np.random.uniform(10, 20, n_outliers)
        true_outliers[outlier_indices] = True
        
        metadata = {
            'type': '温度',
            'unit': '℃',
            'baseline': baseline,
            'normal_range': f'[{baseline-10:.1f}, {baseline+10:.1f}]'
        }
        
    elif telemetry_type == 'voltage':
        # 电压：基线1050V，稳定
        baseline = 1050.0
        noise_level = 5.0
        
        data = baseline + noise_level * np.random.randn(n_points)
        
        # 充电/放电周期（每周一次）
        week_length = 7 * 24 * 3600 // sampling_rate
        for i in range(0, n_points, week_length):
            charge_duration = 3600 // sampling_rate  # 1小时充电
            end = min(i + charge_duration, n_points)
            data[i:end] += 30  # 充电时电压升高
        
        # 异常值（0.2%）
        n_outliers = int(n_points * 0.002)
        outlier_indices = np.random.choice(n_points, n_outliers, replace=False)
        data[outlier_indices] += np.random.choice([-1, 1], n_outliers) * np.random.uniform(200, 500, n_outliers)
        true_outliers[outlier_indices] = True
        
        metadata = {
            'type': '电压',
            'unit': 'V',
            'baseline': baseline,
            'normal_range': f'[{baseline-50:.0f}, {baseline+50:.0f}]'
        }
        
    elif telemetry_type == 'position':
        # 位置：轨道周期性
        baseline = 6800.0  # 轨道半径（km）
        noise_level = 10.0
        
        # 轨道周期（约90分钟一圈）
        orbit_period = 90 * 60 // sampling_rate
        orbital_variation = 200 * np.sin(2 * np.pi * time_data / orbit_period)
        
        data = baseline + orbital_variation + noise_level * np.random.randn(n_points)
        
        # 轨道机动（10次）
        maneuver_points = np.random.choice(n_points, 10, replace=False)
        for mp in maneuver_points:
            maneuver_duration = 1800 // sampling_rate  # 30分钟
            end = min(mp + maneuver_duration, n_points)
            data[mp:end] += np.random.choice([-100, 100])
        
        # 异常值（0.1%）
        n_outliers = int(n_points * 0.001)
        outlier_indices = np.random.choice(n_points, n_outliers, replace=False)
        data[outlier_indices] += np.random.choice([-1, 1], n_outliers) * np.random.uniform(500, 1000, n_outliers)
        true_outliers[outlier_indices] = True
        
        metadata = {
            'type': '位置',
            'unit': 'km',
            'baseline': baseline,
            'normal_range': f'[{baseline-300:.0f}, {baseline+300:.0f}]'
        }
    
    else:
        raise ValueError(f"Unknown telemetry_type: {telemetry_type}")
    
    print(f"  数据类型: {metadata['type']}")
    print(f"  真实异常值: {np.sum(true_outliers):,} ({np.sum(true_outliers)/n_points*100:.3f}%)")
    print(f"  数据范围: [{np.min(data):.2f}, {np.max(data):.2f}] {metadata['unit']}")
    
    return time_data.tolist(), data.tolist(), true_outliers, metadata


def evaluate_detection(detected_outliers, true_outliers):
    """评估检测效果"""
    detected_outliers = np.array(detected_outliers)
    true_outliers = np.array(true_outliers)
    
    true_positive = np.sum(detected_outliers & true_outliers)
    false_positive = np.sum(detected_outliers & ~true_outliers)
    false_negative = np.sum(~detected_outliers & true_outliers)
    
    precision = true_positive / (true_positive + false_positive) if (true_positive + false_positive) > 0 else 0
    recall = true_positive / (true_positive + false_negative) if (true_positive + false_negative) > 0 else 0
    f1_score = 2 * precision * recall / (precision + recall) if (precision + recall) > 0 else 0
    
    return {
        'precision': precision,
        'recall': recall,
        'f1_score': f1_score,
        'tp': true_positive,
        'fp': false_positive,
        'fn': false_negative,
    }


def test_large_scale_performance(telemetry_type='attitude', sampling_rate=1):
    """
    大规模性能测试
    """
    print("\n" + "="*80)
    print(f"大规模性能测试：{telemetry_type.upper()}")
    print("="*80)
    
    # 生成数据
    time_data, data, true_outliers, metadata = generate_half_year_telemetry(
        telemetry_type, sampling_rate
    )
    
    n_points = len(data)
    
    # 定义测试的算法
    algorithms = {
        '固定3sigma': 'original',
        '平衡版': 'balanced',
        '智能版': 'smart',
    }
    
    results = {}
    
    print(f"\n开始测试 ({n_points:,} 个数据点)...")
    print("-" * 80)
    
    for algo_name, algo_type in algorithms.items():
        print(f"\n正在测试: {algo_name}...", end=' ', flush=True)
        
        # 执行检测
        start_time = time.time()
        
        detector = OutlierDetector(algo_type)
        success, result, _ = detector.detect(time_data.copy(), data.copy())
        
        elapsed_time = time.time() - start_time
        
        if success:
            # 计算检测结果
            detected_outliers = np.ones(len(data), dtype=bool)
            result_times = result[0]
            for i, t in enumerate(time_data):
                if t in result_times:
                    detected_outliers[i] = False
            
            # 评估性能
            metrics = evaluate_detection(detected_outliers, true_outliers)
            metrics['elapsed_time'] = elapsed_time
            metrics['n_detected'] = np.sum(detected_outliers)
            metrics['n_kept'] = len(result_times)
            metrics['throughput'] = n_points / elapsed_time  # 数据点/秒
            
            results[algo_name] = metrics
            
            print(f"完成! 耗时 {elapsed_time:.2f}秒")
        else:
            print(f"失败!")
    
    # 打印结果
    print("\n" + "="*80)
    print("性能对比结果")
    print("="*80)
    
    headers = ['算法', '耗时(秒)', '吞吐量(点/秒)', 'F1分数', '精确率', '召回率', '检出数']
    table_data = []
    
    for algo_name, metrics in results.items():
        table_data.append([
            algo_name,
            f"{metrics['elapsed_time']:.2f}",
            f"{metrics['throughput']:,.0f}",
            f"{metrics['f1_score']:.3f}",
            f"{metrics['precision']:.3f}",
            f"{metrics['recall']:.3f}",
            f"{metrics['n_detected']:,}"
        ])
    
    print(tabulate(table_data, headers=headers, tablefmt='grid'))
    
    # 计算加速比
    if '固定3sigma' in results:
        baseline_time = results['固定3sigma']['elapsed_time']
        print("\n" + "="*80)
        print("相对固定3sigma的性能对比")
        print("="*80)
        
        headers2 = ['算法', '耗时比', '速度提升', 'F1差异']
        table_data2 = []
        
        for algo_name, metrics in results.items():
            time_ratio = metrics['elapsed_time'] / baseline_time
            speedup = baseline_time / metrics['elapsed_time']
            f1_diff = metrics['f1_score'] - results['固定3sigma']['f1_score']
            
            table_data2.append([
                algo_name,
                f"{time_ratio:.2f}x",
                f"{speedup:.2f}x" if speedup >= 1 else f"{1/speedup:.2f}x慢",
                f"{f1_diff:+.3f}"
            ])
        
        print(tabulate(table_data2, headers=headers2, tablefmt='grid'))
    
    # 详细统计
    print("\n" + "="*80)
    print("详细统计")
    print("="*80)
    
    print(f"数据集信息:")
    print(f"  总数据点: {n_points:,}")
    print(f"  真实异常值: {np.sum(true_outliers):,}")
    print(f"  数据类型: {metadata['type']}")
    print(f"  正常范围: {metadata['normal_range']} {metadata['unit']}")
    
    print(f"\n检测详情:")
    for algo_name, metrics in results.items():
        print(f"\n{algo_name}:")
        print(f"  真阳性(TP): {metrics['tp']:,}")
        print(f"  假阳性(FP): {metrics['fp']:,}")
        print(f"  假阴性(FN): {metrics['fn']:,}")
        print(f"  保留数据: {metrics['n_kept']:,} ({metrics['n_kept']/n_points*100:.2f}%)")
    
    return results


def test_multiple_scales(telemetry_type='attitude'):
    """
    测试不同数据规模下的性能
    """
    print("\n" + "="*80)
    print(f"多尺度性能测试：{telemetry_type.upper()}")
    print("="*80)
    
    # 不同采样率：1秒、5秒、10秒、30秒、60秒
    sampling_rates = [60, 30, 10, 5, 1]  # 从小到大测试
    
    all_results = {
        'sampling_rates': [],
        'n_points': [],
        '固定3sigma': [],
        '平衡版': [],
        '智能版': [],
    }
    
    for sr in sampling_rates:
        print(f"\n{'='*80}")
        print(f"采样率: {sr}秒/点")
        print('='*80)
        
        time_data, data, true_outliers, metadata = generate_half_year_telemetry(
            telemetry_type, sr
        )
        
        n_points = len(data)
        all_results['sampling_rates'].append(sr)
        all_results['n_points'].append(n_points)
        
        algorithms = {
            '固定3sigma': 'original',
            '平衡版': 'balanced',
            '智能版': 'smart',
        }
        
        for algo_name, algo_type in algorithms.items():
            print(f"  测试 {algo_name}...", end=' ', flush=True)
            
            start_time = time.time()
            detector = OutlierDetector(algo_type)
            success, result, _ = detector.detect(time_data.copy(), data.copy())
            elapsed = time.time() - start_time
            
            all_results[algo_name].append(elapsed)
            print(f"{elapsed:.2f}秒")
    
    # 可视化
    visualize_multi_scale_results(all_results, telemetry_type)
    
    return all_results


def visualize_multi_scale_results(results, telemetry_type):
    """
    可视化多尺度测试结果
    """
    fig, (ax1, ax2) = plt.subplots(1, 2, figsize=(16, 6))
    
    n_points = results['n_points']
    
    # 左图：执行时间 vs 数据量
    ax1.plot(n_points, results['固定3sigma'], marker='o', linewidth=2, label='固定3sigma')
    ax1.plot(n_points, results['平衡版'], marker='s', linewidth=2, label='平衡版')
    ax1.plot(n_points, results['智能版'], marker='^', linewidth=2, label='智能版')
    
    ax1.set_xlabel('数据点数', fontsize=12)
    ax1.set_ylabel('执行时间 (秒)', fontsize=12)
    ax1.set_title(f'{telemetry_type.upper()} - 执行时间对比', fontsize=14, fontweight='bold')
    ax1.legend(fontsize=10)
    ax1.grid(True, alpha=0.3)
    ax1.set_xscale('log')
    
    # 添加数据标签
    for i, n in enumerate(n_points):
        for algo in ['固定3sigma', '平衡版', '智能版']:
            ax1.annotate(f'{results[algo][i]:.1f}s', 
                        xy=(n, results[algo][i]),
                        xytext=(5, 5), textcoords='offset points',
                        fontsize=8, alpha=0.7)
    
    # 右图：吞吐量对比
    throughput_fixed = np.array(n_points) / np.array(results['固定3sigma'])
    throughput_balanced = np.array(n_points) / np.array(results['平衡版'])
    throughput_smart = np.array(n_points) / np.array(results['智能版'])
    
    x = np.arange(len(n_points))
    width = 0.25
    
    ax2.bar(x - width, throughput_fixed, width, label='固定3sigma', alpha=0.8)
    ax2.bar(x, throughput_balanced, width, label='平衡版', alpha=0.8)
    ax2.bar(x + width, throughput_smart, width, label='智能版', alpha=0.8)
    
    ax2.set_xlabel('数据规模', fontsize=12)
    ax2.set_ylabel('吞吐量 (数据点/秒)', fontsize=12)
    ax2.set_title('吞吐量对比', fontsize=14, fontweight='bold')
    ax2.set_xticks(x)
    ax2.set_xticklabels([f'{n:,}' for n in n_points], rotation=45, ha='right')
    ax2.legend(fontsize=10)
    ax2.grid(True, alpha=0.3, axis='y')
    
    plt.tight_layout()
    
    filename = f'large_scale_{telemetry_type}.png'
    plt.savefig(filename, dpi=150, bbox_inches='tight')
    print(f"\n图表已保存到: {filename}")
    plt.close()


def run_comprehensive_large_scale_test():
    """
    运行综合大规模测试
    """
    print("\n" + "="*80)
    print("半年遥测数据综合测试")
    print("="*80)
    
    telemetry_types = ['attitude', 'temperature', 'voltage', 'position']
    
    # 1. 对每种遥测类型进行单次大规模测试（1秒采样）
    print("\n【第一部分：单次大规模测试 (1秒采样)】")
    
    summary_results = {}
    
    for ttype in telemetry_types:
        results = test_large_scale_performance(ttype, sampling_rate=1)
        summary_results[ttype] = results
    
    # 2. 对姿态角进行多尺度测试
    print("\n\n【第二部分：多尺度测试】")
    multi_scale_results = test_multiple_scales('attitude')
    
    # 3. 生成总结报告
    print("\n\n" + "="*80)
    print("综合测试总结")
    print("="*80)
    
    print("\n所有遥测类型的平均性能:")
    
    avg_metrics = {
        '固定3sigma': {'time': [], 'f1': [], 'throughput': []},
        '平衡版': {'time': [], 'f1': [], 'throughput': []},
        '智能版': {'time': [], 'f1': [], 'throughput': []},
    }
    
    for ttype, results in summary_results.items():
        for algo_name in ['固定3sigma', '平衡版', '智能版']:
            if algo_name in results:
                avg_metrics[algo_name]['time'].append(results[algo_name]['elapsed_time'])
                avg_metrics[algo_name]['f1'].append(results[algo_name]['f1_score'])
                avg_metrics[algo_name]['throughput'].append(results[algo_name]['throughput'])
    
    headers = ['算法', '平均耗时(秒)', '平均吞吐量(点/秒)', '平均F1分数', '速度倍数']
    table_data = []
    
    baseline_time = np.mean(avg_metrics['固定3sigma']['time'])
    
    for algo_name in ['固定3sigma', '平衡版', '智能版']:
        avg_time = np.mean(avg_metrics[algo_name]['time'])
        avg_throughput = np.mean(avg_metrics[algo_name]['throughput'])
        avg_f1 = np.mean(avg_metrics[algo_name]['f1'])
        speedup = baseline_time / avg_time
        
        table_data.append([
            algo_name,
            f"{avg_time:.2f}",
            f"{avg_throughput:,.0f}",
            f"{avg_f1:.3f}",
            f"{speedup:.2f}x"
        ])
    
    print(tabulate(table_data, headers=headers, tablefmt='grid'))
    
    # 结论
    print("\n" + "="*80)
    print("测试结论")
    print("="*80)
    
    best_speed = min(table_data[1:], key=lambda x: float(x[1].replace(',', '')))
    best_f1 = max(table_data, key=lambda x: float(x[3]))
    
    print(f"\n速度最快: {best_speed[0]}")
    print(f"  耗时: {best_speed[1]}秒")
    print(f"  吞吐量: {best_speed[2]} 点/秒")
    print(f"  相对固定3sigma: {best_speed[4]}")
    
    print(f"\n准确率最高: {best_f1[0]}")
    print(f"  F1分数: {best_f1[3]}")
    
    print("\n推荐:")
    print("  - 对于实时处理: 使用固定3sigma或智能版")
    print("  - 对于离线批处理: 可使用平衡版")
    print("  - 对于小值遥测(姿态角): 推荐平衡版或智能版")


if __name__ == "__main__":
    # 运行综合测试
    run_comprehensive_large_scale_test()