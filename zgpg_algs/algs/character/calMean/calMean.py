#coding: utf-8

import pandas as pd
import numpy as np
import requests
import json,time,math

# [[[time],[data1],[data2]],[]]

def _is_scalar_number(x):
    if isinstance(x, bool):
        return False
    return isinstance(x, (int, float, np.integer, np.floating))


def _rows_are_flat_numeric_matrix(datas):
    """
    新链路：每行是一组待求均值的标量，如 [[64.9,61.2],[62.5,62.8],...]。
    旧遥测格式：每行首元为时间序列 list，其后为各通道 list。
    """
    if not isinstance(datas, (list, tuple)) or len(datas) == 0:
        return False
    for item in datas:
        if not isinstance(item, (list, tuple, np.ndarray)):
            return False
        if len(item) == 0:
            continue
        if isinstance(item[0], (list, tuple, np.ndarray)):
            return False
        for x in item:
            if isinstance(x, (list, tuple, np.ndarray)):
                return False
    return True


def algsMain(*args,**kwargs):
    '''
    均值
    :param args:
    :param kwargs:
    :return:
    '''
    try:
        rst = []

        datas = args[0]
        if isinstance(datas, np.ndarray):
            if datas.ndim == 0:
                datas = [[float(datas)]]
            elif datas.ndim == 1:
                datas = [[float(x)] for x in datas.tolist()]
            else:
                datas = datas.tolist()

        # 一维 Python 列表且元素全是数：视为 n×1 矩阵，禁止走旧遥测分支（会对 float 做 item[1:] → not subscriptable）
        if isinstance(datas, (list, tuple)) and len(datas) > 0 and all(_is_scalar_number(x) for x in datas):
            datas = [[float(x)] for x in datas]

        if isinstance(datas, tuple):
            datas = list(datas)

        # 综合评估：二维数值矩阵 → 按行 nanmean（与 Java 模拟多轮次样本一致）
        if _rows_are_flat_numeric_matrix(datas):
            for item in datas:
                row = np.asarray(item, dtype=np.float64)
                if row.size == 0 or not np.any(np.isfinite(row)):
                    rst.append("null")
                else:
                    rst.append(float(np.nanmean(row)))
            return True, rst

        all_days_datas = [np.asarray(item[1:]) for item in datas]
        # print(datas)
        for idx,every_day_data in enumerate(all_days_datas):
            # todaytimes=datas[idx][0]
            if len(every_day_data[0]) == 0:
                rst.append("null")
            else:
                tmpdata = every_day_data[0]
                for i in range(1,len(every_day_data)):
                    tmpdata+=every_day_data[i]
                if len(tmpdata) != 0:
                    rst.append(np.nanmean(tmpdata) / len(every_day_data))
                else:
                    rst.append("null")

        return True,rst
    except Exception as e:
        return False,e.args


# 1678204799
if __name__ == '__main__':
    args=[[[1678118400,1678118401,1678118402,1678118403,1678118404,1678118405,1678118406,1678118407,1678118410,1678118412],[2,2,2,2,2,2,2,2,2,2],[2,2,2,2,2,2,2,2,2,2],[2,2,2,2,2,2,2,2,2,2]],
     [[1678118400,1678118401,1678118402,1678118403,1678118404,1678118405,1678118406,1678118407,1678118408,1678204799],[2,2,2,2,2,2,2,2,2,2],[2,2,2,2,2,2,2,2,2,2],[2,2,2,2,2,2,2,2,2,2]],
          [[],[],[],[]]]
    kwargs = {}
    status, rst = algsMain(args[0], **kwargs)
    print(rst)
