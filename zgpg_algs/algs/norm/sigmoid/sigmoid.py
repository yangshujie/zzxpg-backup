import pandas as pd
import numpy as np
import requests
import json, time, math


def _coerce_1d_list(v):
    """把算法入参统一成一维 list，避免 0 维 ndarray / numpy 标量导致 replaceList / for 迭代报错。"""
    if v is None:
        return []
    if isinstance(v, np.ndarray):
        if v.size == 0:
            return []
        if v.ndim == 0:
            return [float(v)]
        return np.ravel(v).tolist()
    if isinstance(v, list):
        seq = v
    elif isinstance(v, (np.floating, np.integer)) or (np.isscalar(v) and not isinstance(v, (str, bytes))):
        return [float(v)]
    elif isinstance(v, (tuple,)):
        seq = list(v)
    else:
        try:
            seq = list(v)
        except TypeError:
            return [v]
    # 元素中的 numpy 标量 / 0 维数组：统一成 Python float，避免后续 math / len 异常
    out = []
    for el in seq:
        if el is None or el == 'null':
            out.append(el)
            continue
        if isinstance(el, np.ndarray) and el.ndim == 0:
            out.append(float(el))
            continue
        if isinstance(el, (np.floating, np.integer)):
            out.append(float(el))
            continue
        if isinstance(el, (list, tuple, np.ndarray)):
            out.extend(float(t) for t in np.ravel(el).tolist())
            continue
        try:
            out.append(float(el))
        except (TypeError, ValueError):
            out.append(el)
    return out


def _to_float_or_none(v):
    if v is None or v == '' or v == 'null':
        return None
    try:
        return float(v)
    except (TypeError, ValueError):
        return None


def algsMain(*args, **kwargs):
    '''
    sigmoid效用函数
    :param args:
    :param kwargs:
    :return:
    '''
    # try:
    print('最新sigmoid效用函数开始量化')
    rst = args[0]
    config = kwargs["config"]
    # indicatorType 在业务 JSON 中常为「太空攻防」等字符串，勿 int()；用 valueCategory 区分成本/效益
    vc = config.get("valueCategory")
    if vc == "成本型":
        indicator_type = 0
    elif vc == "效益型":
        indicator_type = 1
    else:
        it = config.get("indicatorType")
        if isinstance(it, int):
            indicator_type = it
        elif isinstance(it, str) and it.isdigit():
            indicator_type = int(it)
        else:
            indicator_type = 1
    rst_base = config.get("base_data")

    rst = _coerce_1d_list(rst)
    rst_base = _coerce_1d_list(rst_base)

    replaceList(rst, 'null', None)
    replaceList(rst_base, 'null', None)

    rst_base_data = []
    for rst_base_item in rst_base:
        if rst_base_item is not None:
            rst_base_data.append(rst_base_item)

    if len(rst_base_data) == 0:
        min_value = None
        max_value = None
    else:
        min_value = float(np.min(rst_base_data))
        max_value = float(np.max(rst_base_data))
    if "min" in config:
        min_value = _to_float_or_none(config["min"])
    if "max" in config:
        max_value = _to_float_or_none(config["max"])
    # Java/指标体系常用 valueMin、valueMax；无 base_data 时用其作为量化区间
    if min_value is None and "valueMin" in config:
        min_value = _to_float_or_none(config.get("valueMin"))
    if max_value is None and "valueMax" in config:
        max_value = _to_float_or_none(config.get("valueMax"))
    mapRange = 'sigmoid效用函数使用的区间为' + '[' + str(min_value) + ', ' + str(max_value) + ']'

    fx_list = []
    for item in rst:
        fx_list.append(calSigmoid(item, indicator_type, min_value, max_value))

    rst.extend(fx_list)
    return_data = {
        "mapRange": mapRange,
        "value": rst
    }
    print("sigmoid效用函数已计算完成")
    return True, return_data


def replaceList(inputList, pre, current):
    if inputList is None or not isinstance(inputList, list):
        return
    for index, item in enumerate(inputList):
        if item == pre:
            inputList[index] = current


def calSigmoid(x, indicatorType, min_x, max_x):
    # min_x = floor_min(min_x)
    # max_x = floor_max(max_x)
    if x is None:
        return 1
    try:
        xv = float(x)
    except (TypeError, ValueError):
        return 1
    min_x = _to_float_or_none(min_x)
    max_x = _to_float_or_none(max_x)
    if min_x is None or max_x is None or min_x == max_x:
        return 1
    b = (math.log(99) * max_x + 2 * math.log(10) * min_x) / (2 * math.log(10) + math.log(99))
    denom = max_x - b
    if denom == 0 or math.isnan(denom):
        return 1
    a = 2 * math.log(10) / denom

    if indicatorType == 0:
        return 1 - (1 / (1 + math.exp(-a * (xv - b))))
    if indicatorType == 1:
        return 1 / (1 + math.exp(-a * (xv - b)))
    if indicatorType == 2:
        return 1 / (1 + math.exp(-a * (xv - b)))
    return 1 / (1 + math.exp(-a * (xv - b)))


def floor_min(min_x):
    if min_x <= 0.0001:
        return 0
    if abs(min_x) >= 1:
        output = math.floor(min_x)
    else:
        output = min_x
        count = 0
        while abs(output) < 1:
            output = output * 10
            count += 1
        output = math.floor(output)
        while count > 0:
            output = output / 10.0
            count -= 1
    return output


def floor_max(max_x):
    if max_x <= 0.0001:
        return 0
    if abs(max_x) >= 1:
        output = math.floor(max_x + 1)
    else:
        output = max_x
        count = 0
        while abs(output) < 1:
            output = output * 10
            count += 1
        output = math.floor(output + 1)
        while count > 0:
            output = output / 10.0
            count -= 1
    return output
