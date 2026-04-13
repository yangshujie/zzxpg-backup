import numpy as np
import random
import math

def algsMain(*args, **kwargs):
    try:
        RI = [0, 0, 0.58, 0.9, 1.12, 1.24, 1.32, 1.41, 1.45, 1.49, 1.513, 1.527]
        data = args[0]
        matrix = np.array(data)
        eigenvalues, eigenvectors = np.linalg.eig(matrix)
        max_eigenvalue = max(eigenvalues)
        n = len(matrix)
        if n <= 2:
            return True, 0
        if n > 12:
            # ri = calRI(n)
            ri = RI[11]
        else:
            ri = RI[n - 1]
        ci = (max_eigenvalue - n) / (n - 1)
        cr = ci / ri

        return True, cr.real
    except Exception as e:
        return False, e.args


def calRI(size):
    data_list = [1, 2, 3, 4, 5, 6, 7, 8, 9, 1 / 2, 1 / 3, 1 / 4, 1 / 5, 1 / 6, 1 / 7, 1 / 8, 1 / 9]
    max_eigenvalue_list = []
    for i in range(500):
        criteria = [random.choice(data_list) for i in range(math.factorial(size-1))]
        matrix = build_comparison_matrix(size, criteria)
        eigenvalues, eigenvectors = np.linalg.eig(matrix)
        max_eigenvalue_list.append(max(eigenvalues))
    return (np.nanmean(max_eigenvalue_list).real - size)/(size-1)



def build_comparison_matrix(size, criteria):
    # size = len(criteria)
    matrix = np.ones((size, size))
    count = 0
    for i in range(size):
        for j in range(i + 1, size):
            # 通过用户输入获取两两比较的重要性，这里简化为1到9的标度
            value = criteria[count]
            count += 1
            matrix[i, j] = 1 / value
            matrix[j, i] = value

    return matrix

if __name__ == "__main__":
    # data = [[1, 1 / 2, 1 / 3, 1 / 4], [2, 1, 1 / 2, 1 / 3], [3, 2, 1, 1 / 2], [4, 3, 2, 1]]
    # print(round(algMains(data)[1], 3))
    print(calRI(13))

    # print(math.factorial(3))

