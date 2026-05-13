# -*- coding: UTF-8 -*-
"""
@Project ：wxwx_xa_algorithm 
@File    ：system_efficiency_assessment.py
@Author  ：探长
@Date    ：2023/8/14

20230820更新：更改计算逻辑。

注意'system_requirements'的值是4组，和列一致。
算式：
data的第一行这样计算：每个数字减去下界，除以上界 - 下界，最后所有的值相乘。

其中data的第一行是：    ' [0.6, 0.5, 0.4, 0.2] '。每个数字都在上下界在system_requirements的第一行。
 每一行的分子都是 当前数字 - 下界，每一行都分母都是  当前上界 - 下界
具体算式的第一行是：
 计算公式: (0.6 - 0.1) / (0.3 - 0.1) * (0.5 - 0.05) / (0.2 - 0.05) * (0.4 - 0.01) / (0.1 - 0.01) * (0.2 - 0.22) / (0.02 - 0.22)
系统0的效能值: 3.250
"""


def algsMain(*args, **kwargs):
    """
    算法名称: System Efficiency Evaluation (系统效能评估)

    功能: 根据提供的系统和要求计算效能。

    输入参数:
    *args:
        - 一个列表，代表各系统的数据，格式为 [[S0_data], [S1_data], [S2_data]]
    **kwargs:
        - system_requirements: 代表各系统的上下界数据, 格式为 [[S0_bounds], [S1_bounds], ...]

    输出参数:
    True, List[float], List[str]: 系统的效能值及其计算公式。
    或
    False, 错误信息，空列表。
    """
    try:
        # 解析输入的系统数据
        data = args[0]


        data.pop(0)

        # 从kwargs中提取系统要求的上下界数据
        # system_requirements = kwargs.get('system_requirements', [])
        system_requirements = [[0, 1]] * len(data[0])
        results = []  # 存储每个系统的效能值
        formulas = []  # 存储每个系统效能值的计算公式

        # 遍历每个系统的数据
        for system in data:
            system_efficiency = 1  # 初始化当前系统的效能值
            system_formula = []  # 初始化当前系统的计算公式

            # 遍历当前系统的每一个数据值
            for idx, value in enumerate(system):
                # 获取当前数据值对应的上下界
                lower_bound, upper_bound = system_requirements[idx]

                # 计算范围
                range_value = upper_bound - lower_bound

                # 计算当前数据值的效能值
                efficiency_value = (value - lower_bound)/range_value

                # 更新系统效能值
                system_efficiency *= efficiency_value

                # 记录当前数据值的计算公式
                system_formula.append(f"({value} - {lower_bound}) / ({upper_bound} - {lower_bound})")

            # 将当前系统的效能值和计算公式添加到结果列表中
            results.append(system_efficiency)
            formulas.append(" * ".join(system_formula))

        return True, results
    except Exception as e:
        # 如果遇到任何异常，返回False和错误信息
        return False, str(e), []


if __name__ == "__main__":
    try:
        # 示例数据
        data = [
            [0.6, 0.5, 0.4, 0.2],
            [0.5, 0.6, 0.3, 0.2],
            [0.2, 0.3, 0.4, 0.5]
        ]
        data = [
            [0.998, 0.998,0.998, 0.95],
            [0.998, 0.998, 0.998, 0.95],
            [0.5, 0.998, 0.998, 0.95]
        ]

        # 系统要求的上下界数据
        config = {
            'system_requirements': [
                [0.1, 1],
                [0.05, 1],
                [0.01,0.7],
                [0.02, 0.66]
            ]
        }
        config = {
            'system_requirements': [
                [0, 1],
                [0, 1],
                [0, 1],
                [0, 1]
            ]
        }

        # 调用algsMain函数计算效能值
        result = algsMain(data, **config)
        print(result)

        # # 输出结果
        # if result:
        #     for i, (efficiency, formula) in enumerate(zip(efficiencies, formulas)):
        #         print(f"系统{i}的效能值: {efficiency:.3f}")
        #         print(f"计算公式: {formula}")
        #         print("---------------------------")
        # else:
        #     print("效能评估出错，错误信息：", efficiencies)
    except Exception as e:
        print("执行出错，错误信息：", str(e))
