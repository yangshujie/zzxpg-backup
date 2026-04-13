"""
test_performance_v2.py - 改进版测试
"""
import numpy as np
from tabulate import tabulate
from adaptive_outlier_detection import OutlierDetector
from test_generator import generate_test_data, TEST_SCENARIOS


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
        'true_positive': true_positive,
        'false_positive': false_positive,
        'false_negative': false_negative,
    }


def run_comparison_test():
    """运行对比测试"""
    print("\n" + "="*80)
    print("改进算法对比测试")
    print("="*80)
    
    # 定义测试的检测器
    detectors = {
        '固定3sigma': OutlierDetector('original'),
        '平衡版': OutlierDetector('balanced', min_k=2.5, max_k=4.0),
        '智能版': OutlierDetector('smart'),
    }
    
    # 汇总结果
    all_results = {}
    
    for test_type, (test_name, n_points) in TEST_SCENARIOS.items():
        time_data, data, true_outliers = generate_test_data(n_points, test_type, seed=42)
        
        results = {}
        
        for detector_name, detector in detectors.items():
            success, result, elapsed = detector.detect(time_data.copy(), data.copy())
            
            if success:
                detected_outliers = np.ones(len(data), dtype=bool)
                result_times = result[0]
                for i, t in enumerate(time_data):
                    if t in result_times:
                        detected_outliers[i] = False
                
                metrics = evaluate_detection(detected_outliers, true_outliers)
                metrics['elapsed_time'] = elapsed
                metrics['n_detected'] = np.sum(detected_outliers)
                metrics['n_true'] = np.sum(true_outliers)
                
                results[detector_name] = metrics
        
        all_results[test_name] = results
        
        # 打印结果
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
    
    # 生成汇总报告
    print("\n" + "="*80)
    print("综合性能汇总")
    print("="*80)
    
    avg_metrics = {name: {'f1': [], 'time': []} for name in detectors.keys()}
    
    for test_name, results in all_results.items():
        for detector_name, metrics in results.items():
            avg_metrics[detector_name]['f1'].append(metrics['f1_score'])
            avg_metrics[detector_name]['time'].append(metrics['elapsed_time'] * 1000)
    
    headers = ['算法', '平均F1分数', '平均耗时(ms)', '综合评分']
    table_data = []
    
    for name in detectors.keys():
        avg_f1 = np.mean(avg_metrics[name]['f1'])
        avg_time = np.mean(avg_metrics[name]['time'])
        
        max_time = max([np.mean(m['time']) for m in avg_metrics.values()])
        normalized_time = avg_time / max_time if max_time > 0 else 0
        score = avg_f1 * 0.7 + (1 - normalized_time) * 0.3
        
        table_data.append([
            name,
            f"{avg_f1:.3f}",
            f"{avg_time:.2f}",
            f"{score:.3f}"
        ])
    
    print(tabulate(table_data, headers=headers, tablefmt='grid'))
    
    best = max(table_data, key=lambda x: float(x[3]))
    print(f"\n推荐算法: {best[0]}")
    print(f"  平均F1分数: {best[1]}")
    print(f"  平均耗时: {best[2]}ms")
    print(f"  综合评分: {best[3]}")


if __name__ == "__main__":
    run_comparison_test()