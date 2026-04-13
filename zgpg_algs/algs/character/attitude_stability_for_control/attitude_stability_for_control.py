import numpy as np
import re, time, math
import pandas as pd


def algsMainOld(*args, **kwargs):
    '''
    计算轨控期间姿态稳定度
    :param args:
    :param kwargs:
    :return:
    '''
    try:
        all_data = args[0]
        result_list = []
        for tel_by_day in all_data:
            time_list = tel_by_day[0]
            attitude_tel = tel_by_day[1]
            other_tel = tel_by_day[2:]
            all_time_list = []
            res_att = []
            res_tmp1 = {}
            res_tmp2 = []
            for one_tel in other_tel:
                slice_time_list = get_control_period(time_list, one_tel)
                all_time_list.extend(slice_time_list)
            if all_time_list:
                time_union = all_time_list[0]
                for item in all_time_list:
                    time_union = np.union1d(time_union, item)

                # test
                time_union = np.array([1644369837, 1644371063])

                # time_union = np.array([1644370187, 1644370212])
                # index_list = [np.where(time_list == time)[0][0] for time in time_union]
                # tel_np = abs(np.array(list(attitude_tel[index_list[0]:index_list[1]+1])).astype("double"))
                # res = np.std(tel_np,dtype=np.float64,ddof=1)
                # result_list.append(res)
                # test

                index_list = [np.where(time_list == time)[0][0] for time in time_union]
                for i in range(int(len(index_list) / 2)):
                    res_att.extend(attitude_tel[index_list[2 * i]:index_list[2 * i + 1] + 1])
                    index_len = index_list[2 * i + 1] - index_list[2 * i] + 1
                    for i1 in range(index_len):
                        # if time_list[i1 + index_list[2 * i]] not in res_tmp1.keys():
                        res_tmp1[time_list[i1 + index_list[2 * i]]] = attitude_tel[i1 + index_list[2 * i]]
                        res_tmp2.append({time_list[i1 + index_list[2 * i]]: attitude_tel[i1 + index_list[2 * i]]})
                tel_np = abs(np.array(list(res_tmp1.values())).astype("double"))
                # tel_np = np.array([attitude_tel[item] for item in index_list])
                res = np.std(tel_np, dtype=np.float64, ddof=1)
                result_list.append(res)
            else:
                result_list.append(1)
        return True, result_list
    except Exception as e:
        return False, e.args


def algsMain(*args, **kwargs):
    '''
    计算轨控期间姿态稳定度
    :param args:
    :param kwargs:
    :return:
    '''
    try:
        print("轨控姿态稳定度(使用推力器喷气判断)")
        all_data = args[0]
        config = kwargs["config"]
        result_list = calMain(all_data, config)

        return True, result_list
    except Exception as e:
        return False, e.args


def calMain(all_data, config):
    result_list = []
    index = []
    std_pre = 0
    count = eval(config['count'])
    for tel_by_day in all_data:
        y_tel = []
        other_tel = []
        time_list = tel_by_day[0]
        
        # 添加空值判断：确保 time_list 不为空
        if len(time_list) == 0:
            result_list.append(std_pre)
            continue

        if "reConductDate" in config.keys() and config["reConductDate"] != '':
            re_conduct_date = config["reConductDate"]
            if re_conduct_date != '' and stampToTime(time_list[0]).split(' ')[0] == re_conduct_date.split(' ')[0]:
                result_list.append(std_pre)
                continue

        for i in range(0, len(tel_by_day) - 1):
            if 'thrust' + str(i + 1) in config.keys():
                other_tel.append(tel_by_day[config['thrust' + str(i + 1)] + 1])
                index.append(config['thrust' + str(i + 1)])
            if i not in index:
                y_tel.extend(tel_by_day[i + 1])

        all_time_list = []

        for one_tel in other_tel:
            slice_time_list = get_control_period(time_list, one_tel, count)
            all_time_list.extend(slice_time_list)
        if all_time_list:
            index_list = all_time_list[0]
            for item in all_time_list:
                index_list = np.union1d(index_list, item)
            
            # 添加检查：确保 index_list 不为空且长度为偶数
            if len(index_list) == 0:
                result_list.append(std_pre)
                continue
            
            if len(index_list) % 2 != 0:
                # 如果长度为奇数，跳过本次处理
                result_list.append(std_pre)
                continue
            
            # 确保 y_tel 不为空
            if len(y_tel) == 0:
                result_list.append(std_pre)
                continue
            
            # index_list = [np.where(time_list == time)[0][0] for time in time_union]
            res = []
            for i in range(int(len(index_list) / 2)):
                start_idx = int(index_list[2 * i])
                end_idx = int(index_list[2 * i + 1])
                
                # 添加边界检查：确保索引不越界
                if start_idx >= len(time_list) or end_idx >= len(time_list):
                    continue
                if start_idx >= len(y_tel) or end_idx >= len(y_tel):
                    continue
                
                res_tmp1 = {}
                res_tmp2 = []
                index_len = end_idx - start_idx + 1
                for i1 in range(index_len):
                    actual_idx = start_idx + i1
                    # 再次检查实际索引是否越界
                    if actual_idx < len(time_list) and actual_idx < len(y_tel):
                        res_tmp1[time_list[actual_idx]] = y_tel[actual_idx]
                        res_tmp2.append({time_list[actual_idx]: y_tel[actual_idx]})
                
                # 只有当 res_tmp1 不为空时才计算标准差
                if len(res_tmp1) > 0:
                    print('轨控时间段为' + stampToTime(time_list[start_idx]) + "-" + stampToTime(time_list[end_idx]))
                    tel_np = abs(np.array(list(res_tmp1.values())).astype("double"))
                    res.append(np.std(tel_np, dtype=np.float64, ddof=1))
            
            # 只有当 res 不为空时才计算均值
            if len(res) > 0:
                result_list.append(np.mean(res))
                std_pre = result_list[-1]
            else:
                result_list.append(std_pre)
        else:
            result_list.append(std_pre)
    return result_list


def calSigmoid(x, indicatorType, min_x, max_x):
    result = 0
    # min_x = floor_min(min_x)
    # max_x = floor_max(max_x)

    if min_x == max_x:
        result = 1
    else:
        b = (math.log(99) * max_x + 2 * math.log(10) * min_x) / (2 * math.log(10) + math.log(99))
        a = 2 * math.log(10) / (max_x - b)

        if indicatorType == 0:
            result = 1 - (1 / (1 + math.exp(-a * (x - b))))
        elif indicatorType == 1:
            result = (1 / (1 + math.exp(-a * (x - b))))
        elif indicatorType == 2:
            result = (1 / (1 + math.exp(-a * (x - b))))

    return result

def calGuassian(x, ideal_x, indicatorType, k, roundTFlag, min_x, max_x):
    result = 0
    if roundTFlag == 1:
        if min_x <= x <= max_x:
            result = 1
            return result
        elif x < min_x:
            ideal_x = min_x
        elif x > max_x:
            ideal_x = max_x

    if ideal_x > 0.0001:
        min_x = floor_min(min_x)
        if k == 0:
            # k = 1.0
            k = pow((min_x - ideal_x), 2) / (pow(ideal_x, 2) * math.log(10, math.e))
        if k <= 0.0001:
            k = 1
        fx = 0
        if abs(x - ideal_x) > 0.0005:
            fx = math.exp(-pow((x - ideal_x) / (ideal_x), 2) / k)
        else:
            fx = 1

        if indicatorType == 0:
            result = 1 - fx
        elif indicatorType == 1:
            result = fx
        elif indicatorType == 2:
            result = fx
    else:
        result = 1

    return result


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


def three_sigma(df_col):
    '''
    剔野
    :param df_col:
    :return:
    '''
    rule = (df_col.mean() - 3 * df_col.std() > df_col) | (df_col.mean() + 3 * df_col.std() < df_col)
    index = np.arange(df_col.shape[0])[rule]
    return index


def get_control_period_Old(time_list, tel_list, count):
    tel_diff = np.diff(tel_list)
    standard_tiff = np.where(tel_diff == 0, 0, 1)
    tel_sub = np.array(list(re.sub(r'0{' + str(count) + ',}', lambda x: 'c' * len(x.group()), ''.join(map(str, standard_tiff)))))
    satisfied_index = np.where(tel_sub != "c")[0]  # TODO:有零的情况
    time_slice_list = []
    if satisfied_index.any():
        index_diff = np.where(np.diff(satisfied_index) > 1)[0]
        if index_diff.any():
            for i, item in enumerate(index_diff):
                if i == 0:
                    if satisfied_index[item] + 3 > len(time_list):
                        time_slice = time_list[satisfied_index[0] + 1: len(time_list)]
                    else:
                        time_slice = time_list[satisfied_index[0] + 1: satisfied_index[item] + 3]
                    time_slice_list.append(time_slice)
                else:
                    if satisfied_index[item] + 3 > len(time_list):
                        time_slice = time_list[satisfied_index[index_diff[i - 1] + 1] + 1: len(time_list)]
                    else:
                        time_slice = time_list[satisfied_index[index_diff[i - 1] + 1] + 1: satisfied_index[item] + 3]
                    time_slice_list.append(time_slice)
            if satisfied_index[-1] + 3 > len(time_list):
                time_slice = time_list[satisfied_index[index_diff[-1] + 1] + 1: len(time_list)]
            else:
                time_slice = time_list[satisfied_index[index_diff[-1] + 1] + 1: satisfied_index[-1] + 3]
            time_slice_list.append(time_slice)
        else:
            if satisfied_index[-1] + 3 > len(time_list):
                time_slice = time_list[satisfied_index[0] + 1: len(time_list)]
            else:
                time_slice = time_list[satisfied_index[0] + 1:satisfied_index[-1] + 3]
            time_slice_list.append(time_slice)
    else:
        time_slice_list = []
    return time_slice_list


def get_control_period(time_list, tel_list, count):
    # 添加检查：如果tel_list全为0或变化很小，直接返回空列表
    tel_diff = np.diff(tel_list)
    # 如果所有差值都为0（即tel_list全为0或全为相同值），且count小于等于数组长度，则没有控制时段
    if len(tel_diff) > 0 and np.all(tel_diff == 0) and count <= len(tel_diff):
        return []
    
    standard_tiff = np.where(tel_diff == 0, 0, 1)
    tel_sub = np.array(list(re.sub(r'0{' + str(count) + ',}', lambda x: 'c' * len(x.group()), ''.join(map(str, standard_tiff)))))
    satisfied_index = np.where(tel_sub != "c")[0]  # TODO:有零的情况
    time_slice_list = []
    if satisfied_index.any():
        index_diff = np.where(np.diff(satisfied_index) > 1)[0]
        if index_diff.any():
            for i, item in enumerate(index_diff):
                if i == 0:
                    if satisfied_index[item] + 2 >= len(time_list):
                        time_slice = [satisfied_index[0] + 1, len(time_list) - 1]
                    else:
                        time_slice = [satisfied_index[0] + 1, satisfied_index[item] + 2]
                    time_slice_list.append(time_slice)
                else:
                    if satisfied_index[item] + 2 >= len(time_list):
                        time_slice = [satisfied_index[index_diff[i - 1] + 1] + 1, len(time_list) - 1]
                    else:
                        time_slice = [satisfied_index[index_diff[i - 1] + 1] + 1, satisfied_index[item] + 2]
                    time_slice_list.append(time_slice)
            if satisfied_index[-1] + 2 >= len(time_list):
                time_slice = [satisfied_index[index_diff[-1] + 1] + 1, len(time_list) - 1]
            else:
                time_slice = [satisfied_index[index_diff[-1] + 1] + 1, satisfied_index[-1] + 2]
            time_slice_list.append(time_slice)
        else:
            if satisfied_index[-1] + 2 >= len(time_list):
                time_slice = [satisfied_index[0] + 1, len(time_list) - 1]
            else:
                time_slice = [satisfied_index[0] + 1, satisfied_index[-1] + 2]
            time_slice_list.append(time_slice)
    else:
        time_slice_list = []
    return time_slice_list


def timeToStamp(time_in):
    timearray = time.strptime(time_in, "%Y-%m-%d %H:%M:%S")
    return time.mktime(timearray)


def stampToTime(stamp):
    strtime = time.strftime("%Y-%m-%d %H:%M:%S", time.localtime(stamp))
    return strtime


if __name__ == "__main__":
    # a = [1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 2, 3, 5, 5, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 7, 8, 10, 11, 12,
    # 12, 12, 12, 12, 12, 12, 12, 12, 12] time_list = [1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 2, 3, 5, 5, 6, 6, 6, 6, 6, 6, 6,
    # 6, 6, 6, 6, 6, 6, 6, 6, 7, 8, 10, 11, 12, 12, 12, 12, 12, 12, 12, 12, 12, 12]

    # tel_np = [0.0017000000000000001, 2.0E-4, 5.0E-4, 5.0E-4]
    # res = np.std(tel_np)
    #
    # input_data = [
    #     [[1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 2, 3, 5, 5, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 7, 8, 10, 11, 12, 12,
    #       12, 12, 12, 12, 12, 12, 12, 12],
    #      [1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 2, 3, 5, 5, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 7, 8, 10, 11, 12, 12,
    #       12, 12, 12, 12, 12, 12, 12, 12],
    #      [1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 2, 3, 5, 5, 5, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 7, 8, 10, 11, 12, 12,
    #       12, 12, 12, 12, 12, 12, 12, 12]]]
    for i in range(1, 6):
        print(i)
    print(stampToTime(1594224000))
    print(stampToTime(1554134400))
    print(stampToTime(1554134400))
    print(stampToTime(1644369837), stampToTime(1644371063))
    # print(timeToStamp("2022-02-09 10:30:00"))
    # print(algsMain(input_data))

    # print(np.array(a))
    # b = np.diff(a)
    # print(b)
    # c = re.sub(r'0{9,}', lambda x: 'c' * len(x.group()), ''.join(map(str, b)))
    # d = np.array(list(c))
    # print(d)
    # e = np.where(d != "c")[0]
    # f = np.where(d == "c")[0]
    # # print(f)
    # print(e)
    # g = list(np.where(np.diff(e) > 1)[0])
    # print(g)
    # time_slice_list = []
    # for i, item in enumerate(g):
    #     if i == 0:
    #         time_slice = time_list[e[0] + 1:e[item] + 2]
    #         time_slice_list.append(time_slice)
    #     else:
    #         time_slice = time_list[e[g[i-1] + 2]:e[item] + 2]
    #         time_slice_list.append(time_slice)
    # time_slice = time_list[e[g[-1] + 2]:e[-1] + 2]
    # time_slice_list.append(time_slice)
    # print(time_slice_list)

    # aaa = [[1, 2, 3, 4, 5], [11, 12, 13, 14]]
    # bbb = [[2, 3, 4, 5, 6], [16, 17]]
    # ccc = [np.union1d(time_slice, time_slice_2) for time_slice in aaa for time_slice_2 in bbb]
    # [np.union1d(ccc[i], ccc[i + 1]) for i in range(len(ccc) - 1)]

    # print(np.array(time_list)[2, 3])
    # a_pd = pd.DataFrame(aaa)
    # b_pd = pd.DataFrame(bbb)
    # print(pd.merge(a_pd, b_pd, how="outer"))
    # if list(f):
    #     print(111)
    # start = e[0]
    # end = 0
    # res = [start]
    # while True:
    #     end_np = np.where(f > start)[0]
    #     if not list(end_np):
    #         break
    #     end = f[end_np][0]
    #     res.append(end)
    #     start_np = np.where(e > end)[0]
    #     if not list(start_np):
    #         break
    #     start = e[start_np][0]
    #     res.append(start)
    # print(res)
