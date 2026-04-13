# 优化版本说明

## 文件结构

```
optimized/
├── attitude_stability_for_control_optimized.py  # 优化后的主代码文件
├── OPTIMIZATION_NOTES.md                        # 详细优化说明文档
└── README.md                                    # 本文件
```

## 主要优化内容

### 1. 安全性优化
- ✅ 移除所有 `eval()` 调用，使用安全的类型转换
- ✅ 使用 `config.get()` 方法提供默认值，避免 KeyError

### 2. 性能优化
- ✅ 使用 NumPy 向量化操作替代正则表达式（连续段检测）
- ✅ 优化控制时段检测，减少重复的数组转换
- ✅ 提前进行类型转换，避免重复计算
- ✅ 使用 `np.asarray()` 替代 `np.array()`，减少不必要的数组复制

### 3. 代码质量优化
- ✅ 提取公共逻辑为独立函数
- ✅ 改进函数命名和代码结构
- ✅ 添加详细的函数文档字符串

## 使用方法

### 基本使用

优化版本的接口与原始版本完全兼容：

```python
from algs.character.attitude_stability_for_control.optimized.attitude_stability_for_control_optimized import algsMain

# 调用方式与原始版本相同
result = algsMain(all_data, config=config_dict)
```

### 配置参数

配置参数格式保持不变，但不再支持 `eval()` 表达式：

```python
config = {
    "count": 10,              # 整数，不再使用 eval()
    "thrust1": 2,             # 整数，推力器1的通道索引
    "thrust2": 3,             # 整数，推力器2的通道索引（可选）
    "reConductDate": "2023-01-01"  # 字符串，重新执行日期（可选）
}
```

**注意**：配置参数必须是纯数值，不能包含 Python 表达式。

## 性能对比

根据数据规模，预期性能提升：

- **小规模数据（< 1000 点）**：2-5 倍
- **中等规模数据（1000-10000 点）**：5-20 倍
- **大规模数据（> 10000 点）**：10-100 倍

## 兼容性

- ✅ 接口完全兼容：`algsMain(*args, **kwargs)` 签名不变
- ✅ 返回格式一致：`(bool, list)` 格式保持不变
- ✅ 数据格式要求：输入数据格式要求相同
- ✅ 计算结果：逻辑与原始版本一致，结果应该相同

## 测试建议

在使用优化版本前，建议：

1. **功能测试**：使用相同数据对比原始版本和优化版本的结果
2. **性能测试**：测量计算耗时，验证性能提升
3. **边界测试**：测试边界情况（空数据、单点数据等）

## 回滚方案

如果需要回滚到原始版本：

```python
# 使用原始版本
from algs.character.attitude_stability_for_control.attitude_stability_for_control import algsMain
```

原始版本文件保持不变，可以随时切换。

## 问题反馈

如果发现优化版本有任何问题，请：

1. 记录问题现象和输入数据
2. 对比原始版本和优化版本的结果
3. 检查配置参数格式是否正确

## 版本信息

- **优化日期**：2026-01-07
- **原始版本**：`attitude_stability_for_control.py`
- **优化版本**：`attitude_stability_for_control_optimized.py`

## 算法说明

本算法用于计算轨控期间姿态稳定度，通过检测推力器喷气来判断控制时段：

1. **控制时段检测**：通过分析推力器数据的差分，找出连续 count 次以上没有变化的部分，这些部分被过滤掉，保留有变化的部分作为控制时段
2. **数据提取**：从所有非推力器通道中提取姿态数据
3. **稳定度计算**：对每个控制时段计算标准差，然后取平均值作为最终结果

