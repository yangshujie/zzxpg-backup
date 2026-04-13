"""
test_quick_large_scale.py - 快速大规模测试
只测试关键场景
"""
import numpy as np
import time
from tabulate import tabulate
from test_large_scale import generate_half_year_telemetry, evaluate_detection
from adaptive_outlier_detection import OutlierDetector


def quick_test():
    """快速测试：只测试最大规模"""
    
    print("\n" + "="*80)
    print("快速大规模测试 (半年数据，1秒采样)")
    print("="*80)
    
    # 生成姿态角数据
    print("\n生成姿态角遥测数据...")
    time_data, data, true_outliers, metadata = generate_half_year_telemetry(
        'attitude', sampling_rate=1
    )
    
    n_points = len(data)
    print(f"数据规模: {n_points:,} 点")
    
    # 测试三种算法
    algorithms = {
        '固定3sigma': 'original',
        '平衡版': 'balanced',
        '智能版': 'smart',
    }
    
    results = {}
    
    print("\n开始性能测试...")
    print("-" * 80)
    
    for algo_name, algo_type in algorithms.items():
        print(f"\n{algo_name}:", end=' ', flush=True)
        
        # 多次测试取平均
        times = []
        for run in range(3):
            print(f"[第{run+1}次]", end=' ', flush=True)
            
            start_time = time.time()
            detector = OutlierDetector(algo_type)
            success, result, _ = detector.detect(time_data.copy(), data.copy())
            elapsed = time.time() - start_time
            
            times.append(elapsed)
        
        avg_time = np.mean(times)
        std_time = np.std(times)
        
        print(f"\n  平均耗时: {avg_time:.3f}±{std_time:.3f}秒")
        
        # 计算准确率
        if success:
            detected_outliers = np.ones(len(data), dtype=bool)
            result_times = result[0]
            for i, t in enumerate(time_data):
                if t in result_times:
                    detected_outliers[i] = False
            
            metrics = evaluate_detection(detected_outliers, true_outliers)
            metrics['elapsed_time'] = avg_time
            metrics['std_time'] = std_time
            metrics['throughput'] = n_points / avg_time
            
            results[algo_name] = metrics
    
    # 结果对比
    print("\n" + "="*80)
    print("性能对比")
    print("="*80)
    
    headers = ['算法', '平均耗时(秒)', '吞吐量(点/秒)', 'F1分数', '精确率', '召回率']
    table_data = []
    
    for algo_name, metrics in results.items():
        table_data.append([
            algo_name,
            f"{metrics['elapsed_time']:.3f}±{metrics['std_time']:.3f}",
            f"{metrics['throughput']:,.0f}",
            f"{metrics['f1_score']:.3f}",
            f"{metrics['precision']:.3f}",
            f"{metrics['recall']:.3f}"
        ])
    
    print(tabulate(table_data, headers=headers, tablefmt='grid'))
    
    # 相对性能
    if '固定3sigma' in results:
        baseline = results['固定3sigma']['elapsed_time']
        
        print("\n相对性能 (以固定3sigma为基准):")
        for algo_name, metrics in results.items():
            ratio = metrics['elapsed_time'] / baseline
            speedup = baseline / metrics['elapsed_time']
            f1_diff = metrics['f1_score'] - results['固定3sigma']['f1_score']
            
            if speedup >= 1:
                speed_str = f"{speedup:.2f}x faster"
            else:
                speed_str = f"{1/speedup:.2f}x slower"
            
            print(f"  {algo_name}: {speed_str}, F1差异: {f1_diff:+.3f}")


if __name__ == "__main__":
    quick_test()