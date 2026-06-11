# -*- coding: UTF-8 -*-
"""
@Project ：wxwx_xa_algorithm 
@File    ：grey_analysis.py
@Author  ：探长
@Date    ：2023/8/30 18:13 
"""
import datetime

import numpy as np


def algsMain(*args, **kwargs):
    """
    算法名称 : 灰色分析


    功能 :
    使用灰色关联分析方法对一组数据序列和一个参考序列进行关联度量。

    输入参数 :
    *args:
        - input_matrix: 数据部分，包括待分析的源数据矩阵。
        - reference_sequence: 参考序列。
    **kwargs:
        - config: 包含配置参数的字典，包括灰色关联分析的参数 p值。

    输出参数 :
    True, List[float]: 每个数据序列与参考序列的灰色关联度。
    或
    False, str: 出现错误时的错误信息。
    """

    try:
        # 从 *args 中提取输入矩阵
        input_matrix = args[0] if args else []

        count = 0
        for i in range(1, len(input_matrix)):
            if input_matrix[0] != input_matrix[i]:
                count += 1
        # print(count)
        if count == 0:
            return True, [1] * len(input_matrix)


        # 从 **kwargs 中提取配置参数
        config = kwargs.get('config', {})
        p = 1
        # p = config.get('p', 1000000)

        # p = int(p)
        reference_sequence_max = []
        input_matrix = list(map(list, zip(*input_matrix)))

        for i in range(len(input_matrix)):
            reference_sequence_max.append(max(input_matrix[i]))
        # print(reference_sequence_max)
        if any(reference_sequence_max) != 0:


            input_matrix = list(map(list, zip(*input_matrix)))
            # print(reference_sequence_max)

            reference_sequence = config.get('reference_sequence',reference_sequence_max)  # 默认为reference_sequence_max

            # 将输入列表转化为 NumPy 数组以方便操作
            input_matrix = np.array(input_matrix)
            reference_sequence = np.array(reference_sequence)

            # 计算每个数据序列与参考序列的绝对差值矩阵
            abs_diff_matrix = np.abs(input_matrix - reference_sequence)

            # 计算灰色关联系数公式中的最小值和最大值
            min_val = np.min(abs_diff_matrix)
            max_val = np.max(abs_diff_matrix)

            # 计算灰色关联系数
            grey_relational_coefficient = (min_val + p*max_val)/(abs_diff_matrix + p*max_val)
            # print(grey_relational_coefficient)
            # print("_______________________")
            # 计算灰色关联度（每个数据序列的灰色关联系数的平均值）
            grey_relational_grade = np.mean(grey_relational_coefficient, axis=1)

            # 转换为列表以进行最终输出
            grey_relational_grade_list = list(grey_relational_grade)
            # print(grey_relational_grade_list)
            # with open("D:/log.log", "a", encoding="UTF-8") as f:
            #     f.write(str(datetime.datetime.now()) + " [INFO]:" + "灰色评估得分:" + str(grey_relational_grade_list))
            #     f.write("\n")

            indices = list(np.argsort(grey_relational_grade_list))
            # print(indices)

            ser = [0] * len(indices)
            for i in range(len(indices)):
                ser[indices[i]] += len(indices) - i

            # ser = []
            # for i in range(len(indices)):
            #     ser.append(len(indices)-indices[i])

            # 取整数
            for i in range(len(indices)):
                ser[i] = int(ser[i])
            # print(ser)

            return True, ser
        else:
            return True,[0]*len(input_matrix[0])
    except Exception as e:
        # 异常处理: 捕获所有异常，并返回 False 和错误信息
        return False, str(e)


# 测试重构后的函数
if __name__ == "__main__":
    try:
        test_data = [
            [8, 9, 8, 7, 5, 2, 9,0],
            [7, 8, 7, 5, 7, 3, 8,0],
            [9, 7, 9, 6, 6, 4, 7,0],
            [6, 8, 8, 8, 4, 3, 6,0],
            [8, 6, 6, 9, 8, 3, 8,0],
            [8, 9, 5, 7, 6, 4, 8,0],

        ]
        test_data = [

            [97, 92, 105, 3, 2.0, 0.7],
            [97, 87, 100, 4, 1.5, 0.9],
            [93, 94, 103, 3, 3.0, 0.9],
            [92, 89, 97, 2, 2.0, 0.3],
            [96, 93, 103, 3, 2.5, 0.5]

        ]
        # test_data = [[0, 0, 0, 0, 0, 0, 0,0],
        #              [0, 0, 0, 0, 0, 0, 0,0],
        #              [0, 0, 0, 0, 0, 0, 0,0],
        #              [0, 0, 0, 0, 0, 0, 0,0],
        #              [0, 0, 0, 0, 0, 0, 0,0]]
        test_config = {
            "config": {
                'p': 1,  # 灰色关联分析的参数 p
                # 'reference_sequence': [9, 9, 9, 9, 9, 9, 9]  # 参考序列
            }
        }

        result, output = algsMain(test_data, **test_config)
        if result:
            print("每个数据序列与参考序列的灰色关联度: ", output)
        else:
            print("计算出错，错误信息：", output)
    except Exception as e:
        print("执行出错，错误信息：", str(e))
