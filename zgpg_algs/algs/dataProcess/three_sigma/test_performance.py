"""
性能和正确性测试
对比不同算法的时效性和检测准确性
"""
import numpy as np
import time
from tabulate import tabulate
import matplotlib.pyplot as plt
from matplotlib import rcParams

from adaptive_outlier_detection import OutlierDetector
from test_generator import generate_test_data, TEST_SCENARIOS

# 设置中文字体
rcParams['font.sans-serif'] = ['SimHei', 'DejaVu Sans']
rcParams['axes.unicode_minus'] = False


def evaluate_detection(detected_outliers, true_outliers):
    """
    评估检测效果
    """
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
        'true_positive': true_positive,
        'false_positive': false_positive,
        'false_negative': false_negative,
    }


def test_single_scenario(test_type, test_name, n_points, detectors, seed=42, verbose=True):
    """
    测试单个场景
    """
    # 生成测试数据
    time_data, data, true_outliers = generate_test_data(n_points, test_type, seed)
    
    results = {}
    
    for detector_name, detector in detectors.items():
        # 执行检测
        success, result, elapsed = detector.detect(time_data.copy(), data.copy())
        
        if not success:
            if verbose:
                print(f"  {detector_name}: 执行失败")
            continue
        
        # 计算哪些点被剔除
        detected_outliers = np.ones(len(data), dtype=bool)
        result_times = result[0]
        for i, t in enumerate(time_data):
            if t in result_times:
                detected_outliers[i] = False
        
        # 评估性能
        metrics = evaluate_detection(detected_outliers, true_outliers)
        metrics['elapsed_time'] = elapsed
        metrics['n_detected'] = np.sum(detected_outliers)
        metrics['n_true'] = np.sum(true_outliers)
        
        results[detector_name] = metrics
    
    if verbose:
        print(f"\n{'='*80}")
        print(f"测试场景: {test_name} (n={n_points})")
        print(f"真实异常值: {np.sum(true_outliers)}")
        print(f"{'='*80}")
        
        headers = ['算法', '精确率', '召回率', 'F1分数', '检出数', '耗时(ms)']
        table_data = []
        
        for name, metrics in results.items():
            table_data.append([
                name,
                f"{metrics['precision']:.3f}",
                f"{metrics['recall']:.3f}",
                f"{metrics['f1_score']:.3f}",
                f"{metrics['n_detected']}/{metrics['n_true']}",
                f"{metrics['elapsed_time']*1000:.2f}"
            ])
        
        print(tabulate(table_data, headers=headers, tablefmt='grid'))
    
    return results


def test_scalability(test_type='small_value', sizes=[100, 500, 1000, 5000, 10000], detectors=None):
    """
    测试算法的可扩展性
    """
    print(f"\n{'='*80}")
    print(f"可扩展性测试: {TEST_SCENARIOS[test_type][0]}")
    print(f"{'='*80}")
    
    results = {name: {'sizes': [], 'times': []} for name in detectors.keys()}
    
    for n in sizes:
        print(f"\n数据规模: {n}")
        time_data, data, true_outliers = generate_test_data(n, test_type, seed=42)
        
        for detector_name, detector in detectors.items():
            success, result, elapsed = detector.detect(time_data.copy(), data.copy())
            
            if success:
                results[detector_name]['sizes'].append(n)
                results[detector_name]['times'].append(elapsed * 1000)  # 转换为ms
                print(f"  {detector_name}: {elapsed*1000:.2f}ms")
    
    return results


def visualize_scalability(results, save_path='scalability.png'):
    """
    可视化可扩展性测试结果
    """
    plt.figure(figsize=(12, 6))
    
    for name, data in results.items():
        plt.plot(data['sizes'], data['times'], marker='o', label=name, linewidth=2)
    
    plt.xlabel('数据规模', fontsize=12)
    plt.ylabel('执行时间 (ms)', fontsize=12)
    plt.title('算法可扩展性对比', fontsize=14, fontweight='bold')
    plt.legend(fontsize=10)
    plt.grid(True, alpha=0.3)
    plt.tight_layout()
    
    plt.savefig(save_path, dpi=150, bbox_inches='tight')
    print(f"\n可扩展性图表已保存到: {save_path}")
    plt.close()


def visualize_comparison(all_results, save_path='comparison.png'):
    """
    可视化所有场景的对比结果
    """
    scenarios = list(all_results.keys())
    detectors = list(all_results[scenarios[0]].keys())
    
    metrics = ['f1_score', 'precision', 'recall']
    metric_names = ['F1分数', '精确率', '召回率']
    
    fig, axes = plt.subplots(1, 3, figsize=(18, 6))
    
    for idx, (metric, metric_name) in enumerate(zip(metrics, metric_names)):
        ax = axes[idx]
        
        x = np.arange(len(scenarios))
        width = 0.25
        
        for i, detector in enumerate(detectors):
            values = [all_results[scenario][detector][metric] for scenario in scenarios]
            ax.bar(x + i*width, values, width, label=detector, alpha=0.8)
        
        ax.set_xlabel('测试场景', fontsize=10)
        ax.set_ylabel(metric_name, fontsize=10)
        ax.set_title(f'{metric_name}对比', fontsize=12, fontweight='bold')
        ax.set_xticks(x + width)
        ax.set_xticklabels([s.split('(')[0] for s in scenarios], rotation=45, ha='right', fontsize=8)
        ax.legend(fontsize=8)
        ax.grid(True, alpha=0.3, axis='y')
        ax.set_ylim([0, 1.1])
    
    plt.tight_layout()
    plt.savefig(save_path, dpi=150, bbox_inches='tight')
    print(f"对比图表已保存到: {save_path}")
    plt.close()


def visualize_detection_result(time_data, data, true_outliers, detected_outliers, 
                               test_name, detector_name, save_path=None):
    """
    可视化单个检测结果
    """
    fig, axes = plt.subplots(2, 1, figsize=(14, 8))
    
    time_arr = np.array(time_data)
    data_arr = np.array(data)
    true_outliers = np.array(true_outliers)
    detected_outliers = np.array(detected_outliers)
    
    # 上图：原始数据和真实异常值
    axes[0].plot(time_arr, data_arr, 'b-', alpha=0.5, label='原始数据', linewidth=1)
    axes[0].scatter(time_arr[true_outliers], data_arr[true_outliers], c='red', s=50, 
                    label=f'真实异常值 ({np.sum(true_outliers)}个)', marker='x', linewidths=2)
    axes[0].set_title(f'{test_name} - 原始数据与真实异常值', fontsize=12, fontweight='bold')
    axes[0].set_ylabel('数值')
    axes[0].legend()
    axes[0].grid(True, alpha=0.3)
    
    # 下图：检测结果
    true_negative = ~detected_outliers & ~true_outliers
    true_positive = detected_outliers & true_outliers
    false_positive = detected_outliers & ~true_outliers
    false_negative = ~detected_outliers & true_outliers
    
    axes[1].scatter(time_arr[true_negative], data_arr[true_negative], c='green', s=20, 
                    label=f'正确保留 ({np.sum(true_negative)}个)', alpha=0.6)
    axes[1].scatter(time_arr[true_positive], data_arr[true_positive], c='blue', s=80, 
                    label=f'正确检出 ({np.sum(true_positive)}个)', marker='o', edgecolors='darkblue', linewidths=2)
    axes[1].scatter(time_arr[false_positive], data_arr[false_positive], c='orange', s=80, 
                    label=f'误删 ({np.sum(false_positive)}个)', marker='s', edgecolors='darkorange', linewidths=2)
    axes[1].scatter(time_arr[false_negative], data_arr[false_negative], c='red', s=100, 
                    label=f'漏检 ({np.sum(false_negative)}个)', marker='^', edgecolors='darkred', linewidths=2)
    
    axes[1].set_title(f'{detector_name} - 检测结果', fontsize=12, fontweight='bold')
    axes[1].set_xlabel('时间')
    axes[1].set_ylabel('数值')
    axes[1].legend(loc='best')
    axes[1].grid(True, alpha=0.3)
    
    plt.tight_layout()
    
    if save_path:
        plt.savefig(save_path, dpi=150, bbox_inches='tight')
        print(f"检测结果图已保存到: {save_path}")
    
    plt.close()


def run_comprehensive_tests():
    """
    运行综合测试
    """
    print("\n" + "="*80)
    print("开始综合测试")
    print("="*80)
    
    # 定义测试的检测器
    detectors = {
        '固定3sigma': OutlierDetector('original'),
        '自适应k': OutlierDetector('adaptive', min_k=2.0, max_k=5.0),
        '自适应k+状态检测': OutlierDetector('adaptive_with_state', min_k=2.0, max_k=5.0, transition_k=5.0),
    }
    
    # 测试所有场景
    all_results = {}
    
    for test_type, (test_name, n_points) in TEST_SCENARIOS.items():
        results = test_single_scenario(test_type, test_name, n_points, detectors, verbose=True)
        all_results[test_name] = results
        
        # 为每个场景可视化最佳算法的结果
        best_detector = max(results.items(), key=lambda x: x[1]['f1_score'])
        best_name = best_detector[0]
        
        # 重新运行以获取详细结果
        time_data, data, true_outliers = generate_test_data(n_points, test_type, seed=42)
        detector = detectors[best_name]
        success, result, _ = detector.detect(time_data.copy(), data.copy())
        
        if success:
            detected_outliers = np.ones(len(data), dtype=bool)
            result_times = result[0]
            for i, t in enumerate(time_data):
                if t in result_times:
                    detected_outliers[i] = False
            
            visualize_detection_result(
                time_data, data, true_outliers, detected_outliers,
                test_name, best_name, 
                save_path=f'result_{test_type}.png'
            )
    
    # 生成汇总对比图
    visualize_comparison(all_results, 'comparison_summary.png')
    
    # 可扩展性测试
    print("\n" + "="*80)
    print("开始可扩展性测试")
    print("="*80)
    
    scalability_results = test_scalability(
        'small_value', 
        sizes=[100, 500, 1000, 2000, 5000, 10000],
        detectors=detectors
    )
    
    visualize_scalability(scalability_results, 'scalability.png')
    
    # 生成汇总报告
    print("\n" + "="*80)
    print("综合测试报告")
    print("="*80)
    
    # 计算各算法的平均性能
    avg_metrics = {name: {'f1': [], 'time': []} for name in detectors.keys()}
    
    for test_name, results in all_results.items():
        for detector_name, metrics in results.items():
            avg_metrics[detector_name]['f1'].append(metrics['f1_score'])
            avg_metrics[detector_name]['time'].append(metrics['elapsed_time'] * 1000)
    
    print("\n算法平均性能:")
    headers = ['算法', '平均F1分数', '平均耗时(ms)', '综合评价']
    table_data = []
    
    for name in detectors.keys():
        avg_f1 = np.mean(avg_metrics[name]['f1'])
        avg_time = np.mean(avg_metrics[name]['time'])
        
        # 综合评分：F1分数 * 0.7 + (1 - 归一化时间) * 0.3
        max_time = max([np.mean(m['time']) for m in avg_metrics.values()])
        normalized_time = avg_time / max_time if max_time > 0 else 0
        综合评分 = avg_f1 * 0.7 + (1 - normalized_time) * 0.3
        
        table_data.append([
            name,
            f"{avg_f1:.3f}",
            f"{avg_time:.2f}",
            f"{综合评分:.3f}"
        ])
    
    print(tabulate(table_data, headers=headers, tablefmt='grid'))
    
    print("\n推荐:")
    best = max(table_data, key=lambda x: float(x[3]))
    print(f"  综合表现最佳: {best[0]}")
    print(f"  - 平均F1分数: {best[1]}")
    print(f"  - 平均耗时: {best[2]}ms")
    print(f"  - 综合评分: {best[3]}")


if __name__ == "__main__":
    # 安装依赖提示
    try:
        from tabulate import tabulate
    except ImportError:
        print("请先安装 tabulate: pip install tabulate")
        exit(1)
    
    # 运行测试
    run_comprehensive_tests()