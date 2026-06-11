import numpy as np


def algsMain(*args, **kwargs):
    try:
        # 初始化结果列表

        order = args[0]

        # 这部分用算综合排序结果部分替换


        ideal_order = order[0][2]
        order = order[1]
        similarities = []
        for result in order:
        # 计算均方根误差
            rmse = np.sqrt(np.mean(np.square(np.array(result) - np.array(ideal_order))))
            # print((5*rmse)**2)
            similarities.append(1 / (rmse + 1))
            # print(similarities)
        finally_Confidence = np.average(similarities)
        result_list = []
        result_list.append(finally_Confidence)

        # RMSE越小，相似性越高
        return True,result_list

    except Exception as e:
        return False, str(e)


if __name__ == "__main__":
    try:
        datalist = [[1, 5, 3, 4, 5], [2, 2, 3, 4, 5], [5, 2, 3, 4, 1], [2, 1, 3, 4, 5], [19, 2, 3, 4, 5]]
        # datalist = [[1,2,3,4,5],[1,2,3,4,5],[1,2,3,4,5],[1,2,3,4,5],[1,2,3,4,5]]
    #     datalist = [ [
    #   [
    #     0.030952380952380953,
    #     0.07857142857142857,
    #     0.07857142857142857,
    #     0.08809523809523807,
    #     0.030952380952380953,
    #     0.3928571428571429
    #   ],
    #   [
    #     0,
    #     1,
    #     2,
    #     3,
    #     4
    #   ],
    #   [
    #     1,
    #     4,
    #     6,
    #     3,
    #     2,
    #     5
    #   ],
    #   [
    #     0.8514285714285714
    #   ]
    # ],[[1,2,3,4,5],[2,1,3,4,5]]]
        config = {}
        result = algsMain(datalist, **config)
        print(result)
    except Exception as e:
        print(str(e))


