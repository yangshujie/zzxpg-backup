"""
优化的自适应异常值检测算法 - 平衡版
重点：兼顾准确率和速度
"""
import numpy as np
import time as time_module


def calculate_adaptive_k_v2(data, min_k=2.5, max_k=4.0, small_value_threshold=1.0):
    """
    改进的自适应k值计算
    关键改进：
    1. 提高min_k到2.5（原来2.0太激进）
    2. 降低max_k到4.0（原来5.0太宽松）
    3. 简化计算逻辑，提高速度
    """
    if len(data) == 0:
        return 3.0
    
    mean = np.nanmean(data)
    std = np.nanstd(data)
    magnitude = abs(mean)
    
    # 简化策略：主要根据数据量级和变异系数
    if magnitude > 1e-6:
        cv = std / magnitude  # 变异系数
    else:
        cv = 1.0
    
    # 根据CV和量级综合判断
    if magnitude < 0.1:
        # 极小值数据（如0.0x度）
        if cv < 0.1:  # 稳定
            k = min_k
        else:  # 波动大
            k = min_k + 0.5
    elif magnitude < small_value_threshold:
        # 小值数据（如0.x度）
        if cv < 0.1:
            k = min_k + 0.3
        elif cv < 0.3:
            k = 3.0  # 标准3sigma
        else:
            k = 3.5
    else:
        # 大值数据
        if cv < 0.1:
            k = 3.0
        elif cv < 0.3:
            k = 3.5
        else:
            k = max_k
    
    return np.clip(k, min_k, max_k)


def detect_state_change_v2(data, threshold_factor=3.0):
    """
    改进的状态检测 - 更保守、更快速
    关键改进：
    1. 使用更严格的阈值（3倍MAD而非百分位数）
    2. 减小窗口大小（3而非5）
    3. 只在明显变化时才标记
    """
    if len(data) < 5:
        return np.zeros(len(data), dtype=bool)
    
    # 计算一阶差分
    diff = np.abs(np.diff(data, prepend=data[0]))
    
    # 使用MAD（中位数绝对偏差）判断显著变化
    median_diff = np.median(diff)
    mad = np.median(np.abs(diff - median_diff))
    
    if mad < 1e-8:
        # 差分几乎为0，没有状态变化
        return np.zeros(len(data), dtype=bool)
    
    # 只有远超过正常波动的才算状态变化
    change_threshold = median_diff + threshold_factor * mad * 1.4826
    is_changing = diff > change_threshold
    
    # 缩小扩展窗口
    transition_window = 3  # 从5减到3
    in_transition = np.zeros(len(data), dtype=bool)
    
    for i in range(len(data)):
        start = max(0, i - transition_window)
        end = min(len(data), i + transition_window + 1)
        if np.any(is_changing[start:end]):
            in_transition[i] = True
    
    return in_transition


def algsMain_balanced(*args, **kwargs):
    """
    平衡版：速度和准确率的最佳平衡
    
    关键改进：
    1. 提高最小k值，减少过度剔野
    2. 优化状态检测，减少误判
    3. 简化计算，提高速度
    """
    try:
        time = args[0]
        data = args[1]
        
        # 参数调整：更保守的k值范围
        min_k = kwargs.get('min_k', 2.5)  # 从2.0提高到2.5
        max_k = kwargs.get('max_k', 4.0)  # 从5.0降低到4.0
        small_value_threshold = kwargs.get('small_value_threshold', 1.0)
        enable_state_detection = kwargs.get('enable_state_detection', True)
        transition_k_boost = kwargs.get('transition_k_boost', 0.5)  # 状态变化时k值增量
        max_iterations = kwargs.get('max_iterations', 5)

        data_size = len(data)
        time = np.array(time)
        data = np.array(data)
        
        for iteration in range(max_iterations):
            average = np.nanmean(data)
            std = np.nanstd(data)
            
            if std < 1e-8:
                # 标准差极小，只剔除极端异常
                mask = np.abs(data - average) <= 0.1
            else:
                # 计算自适应k值
                k = calculate_adaptive_k_v2(data, min_k, max_k, small_value_threshold)
                
                # 状态检测（可选，且更保守）
                if enable_state_detection and len(data) > 10:
                    in_transition = detect_state_change_v2(data)
                    # 状态变化时增加k值（而非设置固定值）
                    k_array = np.where(in_transition, k + transition_k_boost, k)
                    mask = (data <= (average + k_array * std)) & \
                           (data >= (average - k_array * std)) & \
                           (np.abs(data) <= 10000)
                else:
                    # 不检测状态或数据太少
                    mask = (data <= (average + k * std)) & \
                           (data >= (average - k * std)) & \
                           (np.abs(data) <= 10000)
            
            # 提前退出条件
            if np.all(mask):
                break
                
            time = time[mask]
            data = data[mask]
            
            if len(data) == data_size:
                break
            data_size = len(data)

        return True, [time.tolist(), data.tolist()]
    except Exception as e:
        return False, e.args


def algsMain_smart(*args, **kwargs):
    """
    智能版：根据数据特征自动选择策略
    
    策略：
    1. 先快速分析数据特征
    2. 对稳定数据用固定3sigma（最快）
    3. 对小值波动数据用轻度自适应
    4. 对复杂数据才用完整自适应
    """
    try:
        time = args[0]
        data = args[1]
        
        time = np.array(time)
        data = np.array(data)
        
        # 快速预分析
        mean = np.nanmean(data)
        std = np.nanstd(data)
        magnitude = abs(mean)
        cv = std / magnitude if magnitude > 1e-6 else 1.0
        
        # 计算差分特征
        if len(data) > 5:
            diff = np.abs(np.diff(data))
            max_diff = np.max(diff)
            median_diff = np.median(diff)
            has_jumps = max_diff > 5 * median_diff  # 是否有明显跳变
        else:
            has_jumps = False
        
        # 智能选择策略
        if cv < 0.05 and not has_jumps:
            # 数据非常稳定，用固定3sigma（最快）
            strategy = 'fixed'
            k = 3.0
        elif magnitude < 1.0 and cv > 0.15:
            # 小值且波动大，用轻度自适应
            strategy = 'light_adaptive'
            k = 2.5 if cv < 0.3 else 2.8
        elif has_jumps:
            # 有明显跳变，用带状态检测的自适应
            strategy = 'full_adaptive'
        else:
            # 其他情况用标准3sigma
            strategy = 'fixed'
            k = 3.0
        
        # 执行相应策略
        count = 5
        data_size = len(data)
        
        for _ in range(count):
            average = np.nanmean(data)
            std = np.nanstd(data)
            
            if strategy == 'fixed':
                k = 3.0
                mask = (data <= (average + k * std)) & \
                       (data >= (average - k * std)) & \
                       (np.abs(data) <= 10000)
                       
            elif strategy == 'light_adaptive':
                # 轻度自适应：只根据CV调整
                current_cv = std / abs(average) if abs(average) > 1e-6 else 1.0
                if current_cv < 0.1:
                    k = 2.5
                elif current_cv < 0.3:
                    k = 2.8
                else:
                    k = 3.2
                    
                mask = (data <= (average + k * std)) & \
                       (data >= (average - k * std)) & \
                       (np.abs(data) <= 10000)
                       
            else:  # full_adaptive
                # 完整自适应
                k = calculate_adaptive_k_v2(data, 2.5, 4.0, 1.0)
                in_transition = detect_state_change_v2(data)
                k_array = np.where(in_transition, k + 0.5, k)
                mask = (data <= (average + k_array * std)) & \
                       (data >= (average - k_array * std)) & \
                       (np.abs(data) <= 10000)
            
            if np.all(mask):
                break
                
            time = time[mask]
            data = data[mask]
            
            if len(data) == data_size:
                break
            data_size = len(data)

        return True, [time.tolist(), data.tolist()]
    except Exception as e:
        return False, e.args


# 更新统一接口
def algsMain(*args, **kwargs):
    """
    统一接口
    
    算法选项：
    - 'original': 固定3sigma（最快，基准）
    - 'balanced': 平衡版（推荐：准确+速度平衡）
    - 'smart': 智能版（自动选择策略）
    - 'adaptive': 旧版自适应（准确但慢）
    - 'adaptive_with_state': 旧版自适应+状态检测（最慢）
    """
    algorithm = kwargs.get('algorithm', 'balanced')
    
    if algorithm == 'original':
        return algsMain_original(*args, **kwargs)
    elif algorithm == 'balanced':
        return algsMain_balanced(*args, **kwargs)
    elif algorithm == 'smart':
        return algsMain_smart(*args, **kwargs)
    elif algorithm == 'adaptive':
        return algsMain_adaptive(*args, **kwargs)
    elif algorithm == 'adaptive_with_state':
        return algsMain_adaptive_with_state(*args, **kwargs)
    else:
        raise ValueError(f"Unknown algorithm: {algorithm}")


class OutlierDetector:
    """更新检测器类"""
    
    def __init__(self, algorithm='balanced', **params):
        self.algorithm = algorithm
        self.params = params
    
    def detect(self, time, data):
        start_time = time_module.time()
        success, result = algsMain(time, data, algorithm=self.algorithm, **self.params)
        elapsed_time = time_module.time() - start_time
        return success, result, elapsed_time
    
    def __str__(self):
        if self.algorithm == 'original':
            return "固定3sigma"
        elif self.algorithm == 'balanced':
            return "平衡版(推荐)"
        elif self.algorithm == 'smart':
            return "智能版"
        elif self.algorithm == 'adaptive':
            return "自适应k"
        else:
            return "自适应k+状态检测"


# 保留原来的函数定义...
def algsMain_original(*args, **kwargs):
    """原始固定3sigma"""
    try:
        time = args[0]
        data = args[1]
        count = 5
        data_size = len(data)
        time = np.array(time)
        data = np.array(data)
        
        for _ in range(count):
            average = np.nanmean(data)
            std = np.nanstd(data)
            mask = (data <= (average + 3 * std)) & (data >= (average - 3 * std)) & (np.abs(data) <= 10000)
            if np.all(mask):
                break
            time = time[mask]
            data = data[mask]
            if len(data) == data_size:
                break
            data_size = len(data)
        return True, [time.tolist(), data.tolist()]
    except Exception as e:
        return False, e.args


if __name__ == "__main__":
    print("=== 改进算法测试 ===")
    
    # 测试用例1：小值数据
    print("\n测试1：小值数据（姿态角）")
    time1 = list(range(10))
    data1 = [0.12, 0.15, 0.13, 0.14, 0.89, 0.13, 0.12, 0.14, 0.11, 0.13]
    
    detectors = {
        '固定3sigma': OutlierDetector('original'),
        '平衡版': OutlierDetector('balanced'),
        '智能版': OutlierDetector('smart'),
    }
    
    for name, detector in detectors.items():
        success, result, elapsed = detector.detect(time1, data1)
        print(f"{name}: {result[1]} (耗时: {elapsed*1000:.2f}ms)")
    
    # 测试用例2：状态变化
    print("\n测试2：状态变化数据")
    time2 = list(range(15))
    data2 = [0.12, 0.13, 0.12, 0.14, 0.35, 0.37, 0.36, 0.38, 0.37, 0.36, 0.35, 0.34, 0.36, 0.89, 0.35]
    
    for name, detector in detectors.items():
        success, result, elapsed = detector.detect(time2, data2)
        print(f"{name}: 保留{len(result[1])}个点 (耗时: {elapsed*1000:.2f}ms)")