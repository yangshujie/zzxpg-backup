import numpy as np


# 构建比较矩阵
def build_comparison_matrix(criteria):
    size = len(criteria)
    matrix = np.ones((size, size))
    for i in range(size):
        for j in range(i + 1, size):
            # 通过用户输入获取两两比较的重要性，这里简化为1到9的标度
            value = float(input(f"相对于'{criteria[i]}'，'{criteria[j]}'的重要性（1-9或其倒数）： "))
            matrix[i, j] =1 /  value
            matrix[j, i] = value
    print(matrix)
    return matrix


# 计算权重
def calculate_weights(matrix):
    eigenvalues, eigenvectors = np.linalg.eig(matrix)
    max_eigenvalue_index = np.argmax(eigenvalues)
    principal_eigenvector = eigenvectors[:, max_eigenvalue_index]
    weights = principal_eigenvector / np.sum(principal_eigenvector)
    return weights


def buildData(data):
    """
    将输入数据转化为比较矩阵
    """
    dt = data[0]
    res = []
    for r in range(len(dt)):
        rData = []
        for c in range(len(dt)):
            rData.append(dt[r]/dt[c])
        pass
        res.append(rData)
    return res
    pass

# 主函数
def algsMain(*args, **kwargs):
    try:

        comparison_matrix = args[0]
        weights = calculate_weights(comparison_matrix)
        weights = np.real(weights)

        weights = list(weights)




        # 输出权重
        # for i in range(len(criteria)):
        #     print(f"'{criteria[i]}'的权重为: {weights[i]}")
        return True, weights
    except Exception as e:
        return False, str(e)


if __name__ == "__main__":
    try:
        datalist = [[1.5, 2, 5, 91.5]]
        datalist = [[0.08, 0.08, 0.08, 0.08, 0.08, 0.08, 0.08, 0.08, 0.08, 0.08, 0.08, 0.12]]
        datalist = [[1,2,3,4],[2,1,3,4],[2,3,4,1],[2,1,4,2]]
        config = {}
        bol,result = algsMain(datalist, **config)
        print(result)
    except Exception as e:
        print(str(e))

#
# if [1,2,3]==[1,2,3]:
#     print("相等")